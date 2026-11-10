package mz.multicore.erp.modules.exceptions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.modules.exceptions.dto.ExceptionFindingDTO;
import mz.multicore.erp.modules.exceptions.service.ButcheryExceptionChecksAccess;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Anti-fraud exception report ({@code docs/ANTIFRAUDE_SECTORES_SPEC.md}). */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:exceptions;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExceptionReportTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;
    @Autowired private mz.multicore.erp.modules.audit.repository.AuditLogRepository auditLogRepository;

    private record Ctx(String token, String companyId, long warehouseId) {}

    @Test
    void anOperatorFarAboveTheAverageIsFlaggedButOneOperatorAloneIsNot() {
        List<ExceptionFindingDTO> flagged = ButcheryExceptionChecksAccess.outliers(
                Map.of("joao", new BigDecimal("9000"), "ana", new BigDecimal("500"), "rui", new BigDecimal("700")),
                Map.of("joao", 12, "ana", 2, "rui", 3));
        assertThat(flagged).extracting(ExceptionFindingDTO::operator).containsExactly("joao");
        assertThat(ButcheryExceptionChecksAccess.outliers(Map.of("joao", new BigDecimal("9000")), Map.of("joao", 12))).isEmpty();
    }

    @Test
    void reportShowsLossyBreakdownsManualWeighingsAndIsPrinted() throws Exception {
        Ctx c = context();
        long carcass = product(c, "Carcaça (teste excepção)", "0", "200");
        long cut = product(c, "Corte (teste excepção)", "500", "0");
        send(post("/api/inventory/movements"), c, """
                {"productId":%d,"warehouseId":%d,"quantity":100,"movementType":"ENTRY"}""".formatted(carcass, c.warehouseId()))
                .andExpect(status().isOk());
        long template = json(send(put("/api/butchery/templates"), c, """
                {"companyId":%s,"name":"Modelo teste","sourceProductId":%d,"lossTolerancePct":5,"active":true,
                 "lines":[{"productId":%d,"expectedYieldPct":90}]}""".formatted(c.companyId(), carcass, cut))).get("id").asLong();
        json(send(post("/api/butchery/breakdowns"), c, """
                {"companyId":%s,"warehouseId":%d,"templateId":%d,"sourceProductId":%d,"inputKg":100,"requestKey":"ex-%s",
                 "lines":[{"productId":%d,"kg":70}]}""".formatted(c.companyId(), c.warehouseId(), template, carcass, UUID.randomUUID(), cut)));
        json(send(post("/api/weighbridge/tickets"), c, """
                {"companyId":%s,"plate":"EXC-%s","direction":"OUT","weightKg":9000,"fromScale":false,"requestKey":"ex-%s"}"""
                .formatted(c.companyId(), UUID.randomUUID().toString().substring(0, 4), UUID.randomUUID())));

        for (int i = 0; i < 3; i++) {
            var log = new mz.multicore.erp.modules.audit.model.AuditLog();
            log.setUsername("caixa-teste");
            log.setCompanyId(Long.valueOf(c.companyId()));
            log.setAction("POS_WEIGHT_MANUAL");
            log.setDetails("Venda ao peso com peso escrito à mão");
            auditLogRepository.save(log);
        }

        LocalDate today = LocalDate.now();
        JsonNode report = getJson("/api/exceptions?companyId=" + c.companyId() + "&from=" + today.minusDays(7) + "&to=" + today, c);
        String codes = report.get("findings").toString();
        assertThat(codes).contains("BREAKDOWN_LOSS").contains("WEIGHING_MANUAL").contains("POS_WEIGHT_MANUAL");
        assertThat(report.get("high").asLong()).as("30% loss against 5% accepted is serious").isGreaterThanOrEqualTo(1);
        assertThat(report.get("findings").get(0).get("severity").asText()).isEqualTo("HIGH");

        send(get("/api/exceptions?companyId=" + c.companyId() + "&from=" + today + "&to=" + today.minusDays(1)), c, "")
                .andExpect(status().isBadRequest());
        byte[] pdf = mockMvc.perform(auth(get("/api/exceptions/pdf?companyId=" + c.companyId() + "&from=" + today.minusDays(7)
                + "&to=" + today), c)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    private long product(Ctx c, String name, String price, String cost) throws Exception {
        return json(send(post("/api/comercial/products"), c, """
                {"sku":"EX-%s","name":"%s","unitPrice":%s,"purchasePrice":%s,"minStock":0,"unitsPerBox":1,
                 "saleType":"WEIGHT","stockTracked":true}""".formatted(UUID.randomUUID().toString().substring(0, 8), name, price, cost)))
                .get("id").asLong();
    }

    private Ctx context() throws Exception {
        JsonNode login = read(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String token = login.get("token").asText();
        for (JsonNode company : login.get("companies")) {
            String companyId = company.get("id").asText();
            JsonNode warehouses = getJson("/api/inventory/warehouses/sales?companyId=" + companyId, new Ctx(token, companyId, 0));
            if (!warehouses.isEmpty()) return new Ctx(token, companyId, warehouses.get(0).get("id").asLong());
        }
        throw new IllegalStateException("No company with a sales warehouse in the seed.");
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
        return mockMvc.perform(auth(req, c).contentType(MediaType.APPLICATION_JSON).content(body.isEmpty() ? "{}" : body));
    }

    private JsonNode read(String body) {
        try {
            return om.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException(body, e);
        }
    }
}
