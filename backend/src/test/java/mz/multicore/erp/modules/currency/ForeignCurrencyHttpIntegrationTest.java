package mz.multicore.erp.modules.currency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Harness FX-12 (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3): endpoints da tabela de câmbios (criar e corrigir só
 * ADMIN, consultar quem regista compras), compra em moeda estrangeira com o câmbio proposto pela tabela e pagamento
 * com o câmbio do pagamento, sempre na empresa activa.
 * Utilizadores semeados: ana=ADMIN (PT+MZ), joao=MANAGER (PT). Password "password".
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:foreign-currency-http;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ForeignCurrencyHttpIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    private record Session(String token, String companyId) {}

    @Test
    void fx12_soAdminCriaECorrigeCambiosEQuemCompraConsulta() throws Exception {
        Session manager = login("joao", null);
        Session admin = login("ana", manager.companyId());
        LocalDate day = LocalDate.of(2026, 3, 10);

        String body = "{\"currency\":\"ZAR\",\"rateDate\":\"%s\",\"rate\":3.4512}".formatted(day);
        send(post("/api/exchange-rates"), manager, body).andExpect(status().isForbidden());

        JsonNode created = json(send(post("/api/exchange-rates"), admin, body));
        assertThat(created.get("currency").asText()).isEqualTo("ZAR");
        assertThat(money(created, "rate")).isEqualByComparingTo("3.4512");
        long id = created.get("id").asLong();

        // mesma moeda e data outra vez: regra de negócio, não erro técnico
        send(post("/api/exchange-rates"), admin, body).andExpect(status().isBadRequest());

        send(put("/api/exchange-rates/" + id), manager, "{\"rate\":3.5}").andExpect(status().isForbidden());
        JsonNode corrected = json(send(put("/api/exchange-rates/" + id), admin, "{\"rate\":3.5}"));
        assertThat(money(corrected, "rate")).isEqualByComparingTo("3.5000");

        JsonNode listed = getJson("/api/exchange-rates", manager);
        assertThat(listed.findValuesAsText("id")).contains(String.valueOf(id));

        JsonNode proposed = getJson("/api/exchange-rates/proposal?currency=ZAR&date=" + day.plusDays(5), manager);
        assertThat(money(proposed, "rate")).isEqualByComparingTo("3.5000");
        assertThat(proposed.get("rateDate").asText()).isEqualTo(day.toString());
        assertThat(proposed.get("manualRateRequired").asBoolean()).isFalse();

        JsonNode none = getJson("/api/exchange-rates/proposal?currency=ZAR&date=" + day.minusDays(1), manager);
        assertThat(none.get("manualRateRequired").asBoolean()).isTrue();
    }

    @Test
    void fx12_compraEmUsdComCambioDaTabelaEPagamentoComCambioDoPagamento() throws Exception {
        Session manager = login("joao", null);
        Session admin = login("ana", manager.companyId());
        json(send(post("/api/exchange-rates"), admin,
                "{\"currency\":\"USD\",\"rateDate\":\"%s\",\"rate\":63.9055}".formatted(LocalDate.now().minusDays(1))));

        long supplierId = getJson("/api/purchases/suppliers?companyId=" + manager.companyId(), manager).get(0)
                .get("id").asLong();
        long warehouseId = getJson("/api/inventory/warehouses/sales?companyId=" + manager.companyId(), manager).get(0)
                .get("id").asLong();
        long productId = json(send(post("/api/comercial/products"), manager, """
                {"sku":"USD-%s","name":"Peça importada (teste)","unitPrice":10000,"purchasePrice":1,"minStock":0,
                 "unitsPerBox":1,"saleType":"UNIT"}""".formatted(UUID.randomUUID().toString().substring(0, 8))))
                .get("id").asLong();

        JsonNode purchase = json(send(post("/api/purchases"), manager, """
                {"supplierId":%d,"warehouseId":%d,"companyId":%s,"currency":"USD",
                 "lines":[{"productId":%d,"quantity":2,"unitPrice":50,"taxRate":0}]}"""
                .formatted(supplierId, warehouseId, manager.companyId(), productId)));
        assertThat(purchase.get("currency").asText()).isEqualTo("USD");
        assertThat(money(purchase, "exchangeRate")).as("proposto pela tabela").isEqualByComparingTo("63.9055");
        assertThat(money(purchase, "totalAmount")).isEqualByComparingTo("100.00");
        assertThat(money(purchase, "baseTotalAmount")).isEqualByComparingTo("6390.55");
        long purchaseId = purchase.get("id").asLong();

        // moeda fora da lista fechada é recusada como regra de negócio
        send(post("/api/purchases"), manager, """
                {"supplierId":%d,"warehouseId":%d,"companyId":%s,"currency":"GBP",
                 "lines":[{"productId":%d,"quantity":1,"unitPrice":50,"taxRate":0}]}"""
                .formatted(supplierId, warehouseId, manager.companyId(), productId)).andExpect(status().isBadRequest());

        JsonNode account = getJson("/api/finance/accounts", manager).get(0);
        long accountId = account.get("id").asLong();
        BigDecimal before = money(account, "balance");

        JsonNode paid = json(send(post("/api/purchases/" + purchaseId + "/pay?amount=40&financeAccountId=" + accountId
                + "&exchangeRate=65"), manager, ""));
        assertThat(money(paid, "amountPaid")).as("na moeda do documento").isEqualByComparingTo("40");
        assertThat(before.subtract(balance(manager, accountId))).as("tesouraria sai 40 × 65")
                .isEqualByComparingTo("2600.00");

        JsonNode payable = payable(manager, purchaseId);
        assertThat(payable.get("currency").asText()).isEqualTo("USD");
        assertThat(money(payable, "baseOutstanding")).as("60 × 63,9055").isEqualByComparingTo("3834.33");
    }

    private JsonNode payable(Session s, long purchaseId) throws Exception {
        for (JsonNode payable : getJson("/api/purchases/payables?companyId=" + s.companyId(), s)) {
            if (payable.get("purchaseId").asLong() == purchaseId) return payable;
        }
        throw new IllegalStateException("Compra " + purchaseId + " não aparece em contas a pagar");
    }

    private BigDecimal balance(Session s, long accountId) throws Exception {
        for (JsonNode account : getJson("/api/finance/accounts", s)) {
            if (account.get("id").asLong() == accountId) return money(account, "balance");
        }
        throw new IllegalStateException("Conta " + accountId + " não listada");
    }

    /** joao só tem a empresa PT; ana entra na mesma empresa para os dois perfis verem a mesma tabela. */
    private Session login(String username, String companyId) throws Exception {
        JsonNode login = read(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"password\"}".formatted(username)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String active = companyId != null ? companyId : login.get("companies").get(0).get("id").asText();
        return new Session(login.get("token").asText(), active);
    }

    private static BigDecimal money(JsonNode node, String field) {
        return new BigDecimal(node.get(field).asText());
    }

    private JsonNode json(ResultActions result) throws Exception {
        return read(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req, Session s) {
        return req.header("Authorization", "Bearer " + s.token()).header("X-Company-Id", s.companyId());
    }

    private JsonNode getJson(String path, Session s) throws Exception {
        return read(mockMvc.perform(auth(get(path), s)).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString());
    }

    private ResultActions send(MockHttpServletRequestBuilder req, Session s, String body) throws Exception {
        return mockMvc.perform(auth(req, s).contentType(MediaType.APPLICATION_JSON).content(body.isEmpty() ? "{}" : body));
    }

    private JsonNode read(String body) {
        try {
            return om.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException(body, e);
        }
    }
}
