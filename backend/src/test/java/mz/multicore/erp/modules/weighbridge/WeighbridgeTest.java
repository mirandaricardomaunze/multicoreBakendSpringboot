package mz.multicore.erp.modules.weighbridge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.weighbridge.model.WeighingDirection;
import mz.multicore.erp.modules.weighbridge.service.WeighingRules;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Truck scale: rules and HTTP end to end ({@code docs/BASCULA_CAMIAO_SPEC.md}). */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:weighbridge;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WeighbridgeTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    private record Ctx(String token, String companyId) {}

    @Test
    void netWeightFollowsTheDirectionAndRefusesSwappedWeighings() {
        assertThat(WeighingRules.net(WeighingDirection.OUT, new BigDecimal("9000"), new BigDecimal("21500"))).isEqualByComparingTo("12500");
        assertThat(WeighingRules.net(WeighingDirection.IN, new BigDecimal("30000"), new BigDecimal("12000"))).isEqualByComparingTo("18000");
        assertThatThrownBy(() -> WeighingRules.net(WeighingDirection.OUT, new BigDecimal("21500"), new BigDecimal("9000")))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(WeighingRules.normalizePlate(" abc-123 mc ")).isEqualTo("ABC123MC");
    }

    @Test
    void truckIsWeighedInAndOutAndTheTicketIsPrinted() throws Exception {
        Ctx c = context();
        String plate = "AAA-" + UUID.randomUUID().toString().substring(0, 3) + " MP";
        String first = """
                {"companyId":%s,"plate":"%s","driver":"Carlos","direction":"OUT","counterparty":"Construtora Lda",
                 "reference":"FT 2026/15","weightKg":9000,"fromScale":true,"requestKey":"tp-%s"}"""
                .formatted(c.companyId(), plate, UUID.randomUUID());
        JsonNode ticket = json(send(post("/api/weighbridge/tickets"), c, first));
        assertThat(ticket.get("number").asText()).startsWith("TP");
        assertThat(json(send(post("/api/weighbridge/tickets"), c, first)).get("id").asLong()).isEqualTo(ticket.get("id").asLong());
        send(post("/api/weighbridge/tickets"), c, first.replace("tp-", "tp2-")).andExpect(status().isBadRequest());

        long id = ticket.get("id").asLong();
        send(post("/api/weighbridge/tickets/" + id + "/second"), c, "{\"companyId\":%s,\"weightKg\":8000,\"fromScale\":true}"
                .formatted(c.companyId())).andExpect(status().isBadRequest());
        JsonNode closed = json(send(post("/api/weighbridge/tickets/" + id + "/second"), c,
                "{\"companyId\":%s,\"weightKg\":21500,\"fromScale\":false}".formatted(c.companyId())));
        assertThat(closed.get("status").asText()).isEqualTo("CLOSED");
        assertThat(new BigDecimal(closed.get("netWeight").asText())).isEqualByComparingTo("12500");
        assertThat(closed.get("manual").asBoolean()).as("a typed weight is flagged").isTrue();
        send(post("/api/weighbridge/tickets/" + id + "/second"), c, "{\"companyId\":%s,\"weightKg\":22000,\"fromScale\":true}"
                .formatted(c.companyId())).andExpect(status().isBadRequest());

        byte[] pdf = mockMvc.perform(auth(get("/api/weighbridge/tickets/" + id + "/pdf?companyId=" + c.companyId()), c))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    private Ctx context() throws Exception {
        JsonNode login = read(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        return new Ctx(login.get("token").asText(), login.get("companies").get(0).get("id").asText());
    }

    private JsonNode json(ResultActions result) throws Exception {
        return read(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req, Ctx c) {
        return req.header("Authorization", "Bearer " + c.token()).header("X-Company-Id", c.companyId());
    }

    private ResultActions send(MockHttpServletRequestBuilder req, Ctx c, String body) throws Exception {
        return mockMvc.perform(auth(req, c).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode read(String body) {
        try {
            return om.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException(body, e);
        }
    }
}
