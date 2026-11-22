package mz.multicore.erp.modules.purchases;

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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Harness LC-08 (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.3, §4.4): a pré-visualização mostra por artigo o custo
 * unitário original, o custo acrescentado e o custo unitário final sem gravar nada; a confirmação grava o CI, as
 * dívidas e o novo custo médio numa só transacção; sempre na empresa activa e só para os perfis que registam compras.
 * Utilizadores semeados: ana=ADMIN (PT+MZ), joao=MANAGER (PT), maria=EMPLOYEE (PT). Password "password".
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:landed-cost-http;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LandedCostHttpIntegrationTest {

    private static final String PREVIEW = "/api/purchases/landed-costs/preview";
    private static final String CONFIRM = "/api/purchases/landed-costs";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    private record Session(String token, String companyId) {}

    private record Received(long purchaseId, long supplierId, String sku) {}

    @Test
    void lc08_preVisualizacaoPorArtigoSemGravar() throws Exception {
        Session manager = login("joao", null);
        Received received = receivedPurchase(manager);
        int payablesBefore = payables(manager).size();

        JsonNode preview = json(send(post(PREVIEW), manager, freightRequest(received, "500")));

        assertThat(preview.get("allocationMethod").asText()).isEqualTo("VALOR");
        assertThat(money(preview, "capitalizableCost")).isEqualByComparingTo("500.00");
        JsonNode line = preview.get("lines").get(0);
        assertThat(preview.get("lines")).hasSize(1);
        assertThat(money(line, "unitCostBefore")).as("custo unitário original").isEqualByComparingTo("100.00");
        assertThat(money(line, "shareAmount")).as("custo acrescentado").isEqualByComparingTo("500.00");
        assertThat(money(line, "stockAmount")).isEqualByComparingTo("500.00");
        assertThat(money(line, "unitCostAfter")).as("custo unitário final").isEqualByComparingTo("150.00");

        assertThat(purchasePrice(manager, received.sku())).as("a pré-visualização não grava")
                .isEqualByComparingTo("100.00");
        assertThat(payables(manager)).as("nem cria dívidas").hasSize(payablesBefore);
    }

    @Test
    void lc08_confirmacaoGravaCiDividasECustoMedioNumaTransaccao() throws Exception {
        Session manager = login("joao", null);
        Received received = receivedPurchase(manager);

        // pedido inválido (fornecedor inexistente): regra de negócio e nada fica gravado
        send(post(CONFIRM), manager, """
                {"purchaseIds":[%d],"charges":[{"type":"FRETE","supplierId":%d,"amount":500},
                 {"type":"DESPACHANTE","supplierId":999999999,"amount":100}]}"""
                .formatted(received.purchaseId(), received.supplierId())).andExpect(status().isBadRequest());
        assertThat(purchasePrice(manager, received.sku())).isEqualByComparingTo("100.00");

        JsonNode document = json(send(post(CONFIRM), manager, freightRequest(received, "500")));

        assertThat(document.get("documentNumber").asText()).startsWith("CI-");
        assertThat(document.get("purchaseIds").get(0).asLong()).isEqualTo(received.purchaseId());
        JsonNode charge = document.get("charges").get(0);
        String payableNumber = charge.get("payableNumber").asText();
        assertThat(payableNumber).isEqualTo(document.get("documentNumber").asText() + "-1");
        assertThat(money(document.get("allocations").get(0), "unitCostAfter")).isEqualByComparingTo("150.00");

        assertThat(purchasePrice(manager, received.sku())).as("o custo médio subiu").isEqualByComparingTo("150.00");
        assertThat(payables(manager).findValuesAsText("purchaseNumber")).as("dívida em contas a pagar")
                .contains(payableNumber);
    }

    @Test
    void lc08_soEmpresaActivaESoPerfisDeCompras() throws Exception {
        Session manager = login("joao", null);
        Received received = receivedPurchase(manager);
        String body = freightRequest(received, "500");

        Session employee = login("maria", manager.companyId());
        send(post(PREVIEW), employee, body).andExpect(status().isForbidden());
        send(post(CONFIRM), employee, body).andExpect(status().isForbidden());

        JsonNode companies = loginJson("ana").get("companies");
        String otherCompany = null;
        for (JsonNode company : companies) {
            if (!company.get("id").asText().equals(manager.companyId())) otherCompany = company.get("id").asText();
        }
        assertThat(otherCompany).as("ana tem uma segunda empresa").isNotNull();
        Session adminElsewhere = login("ana", otherCompany);
        send(post(PREVIEW), adminElsewhere, body).andExpect(status().isBadRequest());
        send(post(CONFIRM), adminElsewhere, body).andExpect(status().isBadRequest());
        assertThat(purchasePrice(manager, received.sku())).isEqualByComparingTo("100.00");
    }

    // --- cenário ---

    /** Artigo novo, comprado e recebido: 10 unidades a 100 MZN, ficando com stock 10 e custo médio 100. */
    private Received receivedPurchase(Session s) throws Exception {
        long supplierId = getJson("/api/purchases/suppliers?companyId=" + s.companyId(), s).get(0).get("id").asLong();
        long warehouseId = getJson("/api/inventory/warehouses/sales?companyId=" + s.companyId(), s).get(0)
                .get("id").asLong();
        String sku = "CI-" + UUID.randomUUID().toString().substring(0, 8);
        long productId = json(send(post("/api/comercial/products"), s, """
                {"sku":"%s","name":"Artigo importado %s","unitPrice":1000,"purchasePrice":1,"minStock":0,
                 "unitsPerBox":1,"saleType":"UNIT","stockTracked":true}""".formatted(sku, sku))).get("id").asLong();
        long purchaseId = json(send(post("/api/purchases"), s, """
                {"supplierId":%d,"warehouseId":%d,"companyId":%s,
                 "lines":[{"productId":%d,"quantity":10,"unitPrice":100,"taxRate":0}]}"""
                .formatted(supplierId, warehouseId, s.companyId(), productId))).get("id").asLong();
        assertThat(purchasePrice(s, sku)).isEqualByComparingTo("100.00");
        return new Received(purchaseId, supplierId, sku);
    }

    private static String freightRequest(Received received, String amount) {
        return """
                {"purchaseIds":[%d],"allocationMethod":"VALOR",
                 "charges":[{"type":"FRETE","supplierId":%d,"amount":%s}]}"""
                .formatted(received.purchaseId(), received.supplierId(), amount);
    }

    private BigDecimal purchasePrice(Session s, String sku) throws Exception {
        JsonNode page = getJson("/api/comercial/products/page?query=" + sku, s);
        for (JsonNode product : page.get("items")) {
            if (sku.equals(product.get("sku").asText())) return money(product, "purchasePrice");
        }
        throw new AssertionError("Artigo " + sku + " não encontrado");
    }

    private JsonNode payables(Session s) throws Exception {
        return getJson("/api/purchases/payables?companyId=" + s.companyId(), s);
    }

    // --- HTTP ---

    private Session login(String username, String companyId) throws Exception {
        JsonNode login = loginJson(username);
        String active = companyId != null ? companyId : login.get("companies").get(0).get("id").asText();
        return new Session(login.get("token").asText(), active);
    }

    private JsonNode loginJson(String username) throws Exception {
        return read(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"password\"}".formatted(username)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
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
        return mockMvc.perform(auth(req, s).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode read(String body) {
        try {
            return om.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException(body, e);
        }
    }
}
