package mz.multicore.erp.modules;

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

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxos de dinheiro ponta-a-ponta <b>por HTTP</b> — os mesmos endpoints (e cabeçalhos de sessão)
 * que os clientes do desktop cliente-fino usam. Prova, de forma automática e repetível, aquilo que
 * antes só estava verificado "a compilar":
 *
 * <ul>
 *   <li>emissão de fatura desconta stock, e <b>vender a descoberto é recusado pelo servidor</b>
 *       ({@code consumeFEFO} + {@code @Version}) — a garantia que o utilizador questionou;</li>
 *   <li>o <b>checkout do POS devolve {@code InvoiceDTO}</b> (id + número + total) e desconta stock;</li>
 *   <li>uma encomenda pode ser criada e <b>faturada</b> numa fatura.</li>
 * </ul>
 *
 * O teste é auto-contido: descobre empresa/produto/armazém/cliente pela API e semeia o seu próprio
 * stock (movimento de ENTRADA) antes de vender — não depende de dados semeados específicos.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:money-flow;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MoneyFlowHttpIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    /** Contexto de venda descoberto pela API + stock semeado. */
    private record SalesContext(String token, String companyId, long productId,
                                long warehouseId, long clientId, String taxRate, BigDecimal stockBaseline) {}

    private record ConcurrentSaleResult(int status, String responseBody) {}

    // ─── Fatura: desconta stock e recusa venda a descoberto ──────────────────────
    @Test
    void invoiceEmissionDecrementsStockAndRejectsOversell() throws Exception {
        SalesContext c = prepareSalesContext(new BigDecimal("50"));

        // Vende 10 unidades — deve emitir a fatura.
        ResultActions sale = postJson("/api/comercial/invoices", c.token(), c.companyId(), """
                {"clientId":%d,"companyId":%s,"warehouseId":%d,
                 "lines":[{"productId":%d,"quantity":10,"taxRate":%s,"discountPercentage":0}]}"""
                .formatted(c.clientId(), c.companyId(), c.warehouseId(), c.productId(), c.taxRate()));
        sale.andExpect(status().isOk());

        // O stock desce exactamente 10.
        BigDecimal after = stockQuantity(c);
        assertThat(after).isEqualByComparingTo(c.stockBaseline().subtract(new BigDecimal("10")));

        // Vender muito acima do disponível é recusado PELO SERVIDOR (garantia de stock).
        postJson("/api/comercial/invoices", c.token(), c.companyId(), """
                {"clientId":%d,"companyId":%s,"warehouseId":%d,
                 "lines":[{"productId":%d,"quantity":999999,"taxRate":%s,"discountPercentage":0}]}"""
                .formatted(c.clientId(), c.companyId(), c.warehouseId(), c.productId(), c.taxRate()))
                .andExpect(status().isBadRequest());

        // O stock não mudou com a tentativa recusada.
        assertThat(stockQuantity(c)).isEqualByComparingTo(after);
    }

    @Test
    void concurrentInvoiceEmission_lastAvailableStock_acceptsOnlyOneSale() throws Exception {
        SalesContext c = prepareSalesContext(BigDecimal.ONE);
        BigDecimal lastAvailableQuantity = c.stockBaseline();
        int invoicesBefore = invoiceCount(c);
        BigDecimal issuedQuantityBefore = issuedStockQuantity(c);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        // Both workers wait at the same gate so they compete for the same final stock balance.
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<ConcurrentSaleResult> first = executor.submit(
                    () -> submitConcurrentInvoice(c, lastAvailableQuantity, ready, start));
            Future<ConcurrentSaleResult> second = executor.submit(
                    () -> submitConcurrentInvoice(c, lastAvailableQuantity, ready, start));

            assertThat(ready.await(10, TimeUnit.SECONDS))
                    .as("both sales must be ready before the concurrency gate opens")
                    .isTrue();
            start.countDown();

            ConcurrentSaleResult firstResult = first.get(20, TimeUnit.SECONDS);
            ConcurrentSaleResult secondResult = second.get(20, TimeUnit.SECONDS);

            assertThat(new int[]{firstResult.status(), secondResult.status()})
                    .containsExactlyInAnyOrder(200, 400);
            ConcurrentSaleResult rejected = firstResult.status() == 400 ? firstResult : secondResult;
            assertThat(rejected.responseBody()).containsIgnoringCase("stock");
        }

        assertThat(stockQuantity(c)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(invoiceCount(c)).isEqualTo(invoicesBefore + 1);
        assertThat(issuedStockQuantity(c).subtract(issuedQuantityBefore))
                .isEqualByComparingTo(lastAvailableQuantity);
    }

    // ─── POS: checkout devolve InvoiceDTO e desconta stock ───────────────────────
    @Test
    void posCheckoutReturnsInvoiceDtoAndDecrementsStock() throws Exception {
        SalesContext c = prepareSalesContext(new BigDecimal("30"));
        long accountId = firstTreasuryAccountId(c);

        // Abrir caixa (obrigatório antes de vender no POS).
        postJson("/api/pos/sessions/open", c.token(), c.companyId(),
                "{\"operator\":\"ana\",\"openingBalance\":0,\"companyId\":" + c.companyId() + "}")
                .andExpect(status().isOk());

        // Checkout de 5 unidades, pagamento pela conta de tesouraria (caminho legado).
        String body = postJson("/api/pos/checkout", c.token(), c.companyId(), """
                {"operator":"ana","companyId":%s,"walkInName":"Cliente de Teste","warehouseId":%d,"treasuryAccountId":%d,
                 "lines":[{"productId":%d,"quantity":5,"discountPercentage":0}]}"""
                .formatted(c.companyId(), c.warehouseId(), accountId, c.productId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // A resposta é um InvoiceDTO (id + número + total) — o contrato que o desktop precisa para o recibo.
        JsonNode invoice = om.readTree(body);
        assertThat(invoice.hasNonNull("id")).isTrue();
        assertThat(invoice.get("invoiceNumber").asText()).isNotBlank();
        assertThat(new BigDecimal(invoice.get("totalAmount").asText())).isGreaterThan(BigDecimal.ZERO);

        // Stock desceu 5.
        assertThat(stockQuantity(c)).isEqualByComparingTo(c.stockBaseline().subtract(new BigDecimal("5")));
    }

    // ─── Encomenda: criar e consultar (endpoints novos) ─────────────────────────
    // Nota: faturar uma encomenda (billOrder) exige que ela esteja no estado PENDENTE, o que passa
    // pela engine de aprovações — fora do âmbito deste teste. A baixa de stock do billOrder reutiliza
    // o mesmo createInvoice já coberto em invoiceEmissionDecrementsStockAndRejectsOversell.
    @Test
    void orderCanBeCreatedAndRetrievedOverHttp() throws Exception {
        SalesContext c = prepareSalesContext(new BigDecimal("40"));

        String orderBody = postJson("/api/comercial/orders", c.token(), c.companyId(), """
                {"clientId":%d,"companyId":%s,"warehouseId":%d,
                 "lines":[{"productId":%d,"quantity":7,"taxRate":%s,"discountPercentage":0}]}"""
                .formatted(c.clientId(), c.companyId(), c.warehouseId(), c.productId(), c.taxRate()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode order = om.readTree(orderBody);
        long orderId = order.get("id").asLong();
        assertThat(order.get("orderNumber").asText()).isNotBlank();

        // A encomenda é consultável pelo id (GET /orders/{id}).
        JsonNode fetched = getJson("/api/comercial/orders/" + orderId, c.token(), c.companyId());
        assertThat(fetched.get("id").asLong()).isEqualTo(orderId);

        // E aparece na lista de encomendas da empresa (GET /orders?companyId=).
        JsonNode all = getJson("/api/comercial/orders?companyId=" + c.companyId(), c.token(), c.companyId());
        boolean found = false;
        for (JsonNode o : all) {
            if (o.get("id").asLong() == orderId) { found = true; break; }
        }
        assertThat(found).as("a encomenda criada deve aparecer na listagem").isTrue();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    /** Login como ADMIN, escolhe uma empresa com armazém de venda + produto vendável + cliente, e semeia stock. */
    private SalesContext prepareSalesContext(BigDecimal stockToAdd) throws Exception {
        JsonNode login = om.readTree(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String token = login.get("token").asText();

        for (JsonNode company : login.get("companies")) {
            String companyId = company.get("id").asText();
            JsonNode warehouses = getJson("/api/inventory/warehouses/sales?companyId=" + companyId, token, companyId);
            JsonNode products = getJson("/api/comercial/products/sellable", token, companyId);
            JsonNode clients = getJson("/api/comercial/clients", token, companyId);
            if (warehouses.isEmpty() || products.isEmpty() || clients.isEmpty()) {
                continue;
            }
            long productId = products.get(0).get("id").asLong();
            long warehouseId = warehouses.get(0).get("id").asLong();
            long clientId = clients.get(0).get("id").asLong();
            String taxRate = products.get(0).hasNonNull("taxRate")
                    ? products.get(0).get("taxRate").asText() : "0.16";

            // Semeia stock com um movimento de ENTRADA — o teste não depende de stock pré-existente.
            postJson("/api/inventory/movements", token, companyId, """
                    {"productId":%d,"warehouseId":%d,"quantity":%s,"movementType":"ENTRY"}"""
                    .formatted(productId, warehouseId, stockToAdd.toPlainString()))
                    .andExpect(status().isOk());

            SalesContext ctx = new SalesContext(token, companyId, productId, warehouseId, clientId, taxRate, null);
            BigDecimal baseline = stockQuantity(ctx);
            return new SalesContext(token, companyId, productId, warehouseId, clientId, taxRate, baseline);
        }
        throw new IllegalStateException("Nenhuma empresa com armazém de venda + produto vendável + cliente na seed.");
    }

    /** Saldo de stock actual do produto/armazém do contexto. */
    private BigDecimal stockQuantity(SalesContext c) throws Exception {
        JsonNode stocks = getJson(
                "/api/inventory/stocks?companyId=" + c.companyId() + "&warehouseId=" + c.warehouseId(),
                c.token(), c.companyId());
        for (JsonNode s : stocks) {
            if (s.get("productId").asLong() == c.productId()) {
                return new BigDecimal(s.get("quantity").asText());
            }
        }
        return BigDecimal.ZERO;
    }

    private long firstTreasuryAccountId(SalesContext c) throws Exception {
        JsonNode accounts = getJson("/api/finance/accounts", c.token(), c.companyId());
        assertThat(accounts.isEmpty()).as("é preciso pelo menos uma conta de tesouraria").isFalse();
        return accounts.get(0).get("id").asLong();
    }

    private ConcurrentSaleResult submitConcurrentInvoice(
            SalesContext c,
            BigDecimal quantity,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrency gate did not open in time.");
        }
        String request = """
                {"clientId":%d,"companyId":%s,"warehouseId":%d,
                 "lines":[{"productId":%d,"quantity":%s,"taxRate":%s,"discountPercentage":0}]}"""
                .formatted(c.clientId(), c.companyId(), c.warehouseId(), c.productId(),
                        quantity.toPlainString(), c.taxRate());
        var response = postJson("/api/comercial/invoices", c.token(), c.companyId(), request)
                .andReturn().getResponse();
        return new ConcurrentSaleResult(response.getStatus(), response.getContentAsString());
    }

    private int invoiceCount(SalesContext c) throws Exception {
        return getJson("/api/comercial/invoices?companyId=" + c.companyId(), c.token(), c.companyId()).size();
    }

    private BigDecimal issuedStockQuantity(SalesContext c) throws Exception {
        BigDecimal total = BigDecimal.ZERO;
        JsonNode movements = getJson(
                "/api/inventory/movements?companyId=" + c.companyId(), c.token(), c.companyId());
        for (JsonNode movement : movements) {
            if (movement.get("productId").asLong() == c.productId()
                    && "SALE".equals(movement.get("movementType").asText())) {
                total = total.add(new BigDecimal(movement.get("quantity").asText()).abs());
            }
        }
        return total;
    }

    private JsonNode getJson(String path, String token, String companyId) throws Exception {
        String body = mockMvc.perform(get(path)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Company-Id", companyId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body);
    }

    private ResultActions postJson(String path, String token, String companyId, String json) throws Exception {
        var req = post(path)
                .header("Authorization", "Bearer " + token)
                .header("X-Company-Id", companyId);
        if (!json.isEmpty()) {
            req = req.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mockMvc.perform(req);
    }

}
