package mz.multicore.erp.modules.returnables;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.modules.returnables.model.ReturnableMovementType;
import mz.multicore.erp.modules.returnables.service.ReturnableBalanceRules;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Returnables account: rules and HTTP end to end ({@code docs/VASILHAME_RETORNAVEIS_SPEC.md}). */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:returnables;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReturnablesTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    private record Ctx(String token, String companyId, long warehouseId, long clientId) {}

    @Test
    void balanceAndOldestOpenDeliveryFollowFifo() {
        LocalDate d = LocalDate.of(2026, 11, 1);
        List<ReturnableBalanceRules.Entry> entries = List.of(
                new ReturnableBalanceRules.Entry(d, ReturnableMovementType.OUT, new BigDecimal("10")),
                new ReturnableBalanceRules.Entry(d.plusDays(5), ReturnableMovementType.OUT, new BigDecimal("10")),
                new ReturnableBalanceRules.Entry(d.plusDays(6), ReturnableMovementType.IN, new BigDecimal("12")));
        assertThat(ReturnableBalanceRules.balance(entries)).isEqualByComparingTo("8");
        assertThat(ReturnableBalanceRules.oldestOpen(entries)).as("the first 10 came back").isEqualTo(d.plusDays(5));
        assertThat(ReturnableBalanceRules.oldestOpen(List.of(entries.get(0),
                new ReturnableBalanceRules.Entry(d, ReturnableMovementType.CHARGED, BigDecimal.TEN)))).isNull();
    }

    @Test
    void cratesGoOutComeBackAndTheLostOnesAreInvoiced() throws Exception {
        Ctx c = context();
        long crate = json(send(post("/api/comercial/products"), c, """
                {"sku":"GR-%s","name":"Grade de cerveja (teste)","unitPrice":250,"purchasePrice":150,"minStock":0,"unitsPerBox":1,
                 "saleType":"UNIT","stockTracked":true}""".formatted(UUID.randomUUID().toString().substring(0, 8)))).get("id").asLong();
        send(post("/api/inventory/movements"), c, """
                {"productId":%d,"warehouseId":%d,"quantity":100,"movementType":"ENTRY"}""".formatted(crate, c.warehouseId()))
                .andExpect(status().isOk());
        long itemId = json(send(put("/api/returnables/items"), c, "{\"companyId\":%s,\"productId\":%d,\"active\":true}"
                .formatted(c.companyId(), crate))).get("id").asLong();

        String out = movement(c, itemId, "OUT", "30");
        JsonNode delivered = json(send(post("/api/returnables/movements"), c, out));
        assertThat(json(send(post("/api/returnables/movements"), c, out)).get("id").asLong()).isEqualTo(delivered.get("id").asLong());
        assertThat(stock(c, crate)).isEqualByComparingTo("70");

        send(post("/api/returnables/movements"), c, movement(c, itemId, "IN", "31")).andExpect(status().isBadRequest());
        json(send(post("/api/returnables/movements"), c, movement(c, itemId, "IN", "25")));
        assertThat(stock(c, crate)).isEqualByComparingTo("95");

        JsonNode balance = balanceOf(c, itemId);
        assertThat(new BigDecimal(balance.get("balance").asText())).isEqualByComparingTo("5");
        assertThat(new BigDecimal(balance.get("value").asText())).isEqualByComparingTo("1250");

        send(post("/api/returnables/charges"), c, charge(c, itemId, "6")).andExpect(status().isBadRequest());
        JsonNode charged = json(send(post("/api/returnables/charges"), c, charge(c, itemId, "5")));
        assertThat(charged.get("invoiceNumber").asText()).startsWith("FT");
        assertThat(balanceOf(c, itemId)).as("charged closes the balance").isNull();
    }

    private JsonNode balanceOf(Ctx c, long itemId) throws Exception {
        for (JsonNode b : getJson("/api/returnables/balances?companyId=" + c.companyId() + "&clientId=" + c.clientId(), c)) {
            if (b.get("itemId").asLong() == itemId) return b;
        }
        return null;
    }

    private String movement(Ctx c, long itemId, String type, String qty) {
        return """
                {"companyId":%s,"clientId":%d,"itemId":%d,"warehouseId":%d,"type":"%s","quantity":%s,"reference":"Guia 12",
                 "requestKey":"rt-%s"}""".formatted(c.companyId(), c.clientId(), itemId, c.warehouseId(), type, qty, UUID.randomUUID());
    }

    private String charge(Ctx c, long itemId, String qty) {
        return """
                {"companyId":%s,"clientId":%d,"itemId":%d,"quantity":%s,"notes":"Partidas","requestKey":"ch-%s"}"""
                .formatted(c.companyId(), c.clientId(), itemId, qty, UUID.randomUUID());
    }

    private BigDecimal stock(Ctx c, long productId) throws Exception {
        for (JsonNode s : getJson("/api/inventory/stocks?warehouseId=" + c.warehouseId(), c)) {
            if (s.get("productId").asLong() == productId) return new BigDecimal(s.get("quantity").asText());
        }
        return BigDecimal.ZERO;
    }

    private Ctx context() throws Exception {
        JsonNode login = read(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String token = login.get("token").asText();
        for (JsonNode company : login.get("companies")) {
            String companyId = company.get("id").asText();
            Ctx probe = new Ctx(token, companyId, 0, 0);
            JsonNode warehouses = getJson("/api/inventory/warehouses/sales?companyId=" + companyId, probe);
            Long named = null;
            for (JsonNode client : getJson("/api/comercial/clients", probe)) {
                if (!"000000000".equals(client.path("taxId").asText()) && named == null) named = client.get("id").asLong();
            }
            if (warehouses.isEmpty() || named == null) continue;
            return new Ctx(token, companyId, warehouses.get(0).get("id").asLong(), named);
        }
        throw new IllegalStateException("No company with a sales warehouse and a named client in the seed.");
    }

    private JsonNode json(ResultActions result) throws Exception {
        return read(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req, Ctx c) {
        return req.header("Authorization", "Bearer " + c.token()).header("X-Company-Id", c.companyId());
    }

    private JsonNode getJson(String path, Ctx c) throws Exception {
        return read(mockMvc.perform(auth(get(path), c)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
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
