package mz.multicore.erp.modules.accounting;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The goods account (3201) follows the physical stock when stock changes without a sale or purchase: wastes,
 * inventory adjustments and carcass breakdowns are posted at cost ({@code docs/CONTABILIDADE_SPEC.md}).
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:inventoryposting;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryPostingHttpIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    private record Ctx(String token, String companyId, long warehouseId) {}

    @Test
    void wastesAdjustmentsAndBreakdownsMoveTheGoodsAccount() throws Exception {
        Ctx c = context();
        long product = product(c, "Arroz 1 kg (teste existências)", "150", "100");
        entry(c, product, "50");
        BigDecimal goods = ledger(c, "3201");
        BigDecimal cost = ledger(c, "6101");

        // Waste of 2 units at cost 100: goods -200, cost of goods +200.
        JsonNode waste = json(send(post("/api/inventory/waste"), c, """
                {"companyId":%s,"warehouseId":%d,"productId":%d,"quantity":2,"reason":"DAMAGED","notes":"Sacos rotos",
                 "idempotencyKey":"w-%s"}""".formatted(c.companyId(), c.warehouseId(), product, UUID.randomUUID())));
        assertThat(waste.get("status").asText()).isEqualTo("APPROVED");
        assertThat(ledger(c, "3201").subtract(goods)).isEqualByComparingTo("-200");
        assertThat(ledger(c, "6101").subtract(cost)).isEqualByComparingTo("200");

        // Count finds 45 instead of 48: adjustment -3 at cost 100.
        json(send(post("/api/inventory/adjustments"), c, """
                {"companyId":%s,"productId":%d,"warehouseId":%d,"countedQuantity":45,"reason":"Contagem mensal",
                 "idempotencyKey":"a-%s"}""".formatted(c.companyId(), product, c.warehouseId(), UUID.randomUUID())));
        assertThat(ledger(c, "3201").subtract(goods)).isEqualByComparingTo("-500");
        // Count finds 46: adjustment +1 brings 100 back.
        json(send(post("/api/inventory/adjustments"), c, """
                {"companyId":%s,"productId":%d,"warehouseId":%d,"countedQuantity":46,"reason":"Recontagem",
                 "idempotencyKey":"a-%s"}""".formatted(c.companyId(), product, c.warehouseId(), UUID.randomUUID())));
        assertThat(ledger(c, "3201").subtract(goods)).isEqualByComparingTo("-400");

        // Breakdown: carcass of 100 kg at 200/kg book value (20 000) paid 18 000; the cuts enter at 18 000.
        long carcass = product(c, "Carcaça (teste existências)", "0", "200");
        long cut = product(c, "Bife (teste existências)", "500", "0");
        entry(c, carcass, "100");
        BigDecimal beforeBreakdown = ledger(c, "3201");
        json(send(post("/api/butchery/breakdowns"), c, """
                {"companyId":%s,"warehouseId":%d,"sourceProductId":%d,"inputKg":100,"inputCost":18000,"requestKey":"b-%s",
                 "lines":[{"productId":%d,"kg":80}]}""".formatted(c.companyId(), c.warehouseId(), carcass, UUID.randomUUID(), cut)));
        assertThat(ledger(c, "3201").subtract(beforeBreakdown)).isEqualByComparingTo("-2000");

        JsonNode trial = getJson("/api/accounting/trial-balance?from=2000-01-01&to=" + LocalDate.now().plusDays(1), c);
        assertThat(new BigDecimal(trial.get("totalDebit").asText())).isEqualByComparingTo(new BigDecimal(trial.get("totalCredit").asText()));
    }

    @Test
    void manufacturedProductsLiveInFinishedGoods() throws Exception {
        Ctx c = context();
        long flour = product(c, "Farinha (teste acabados)", "0", "40");
        long bread = product(c, "Pao caseiro (teste acabados)", "15", "10");
        json(send(post("/api/manufacturing/boms"), c, """
                {"bomCode":"PA-%s","name":"Pao caseiro","finishedProductId":%d,"baseQuantity":10,
                 "items":[{"rawMaterialProductId":%d,"requiredQuantity":1}]}"""
                .formatted(UUID.randomUUID().toString().substring(0, 6), bread, flour)));
        entry(c, bread, "20");
        BigDecimal goods = ledger(c, "3201");
        BigDecimal finished = ledger(c, "3401");

        // Waste of 3 loaves at cost 10: the finished goods account drops by 30, the goods account does not move.
        json(send(post("/api/inventory/waste"), c, """
                {"companyId":%s,"warehouseId":%d,"productId":%d,"quantity":3,"reason":"EXPIRED","notes":"Pao duro",
                 "idempotencyKey":"w-%s"}""".formatted(c.companyId(), c.warehouseId(), bread, UUID.randomUUID())));
        assertThat(ledger(c, "3401").subtract(finished)).isEqualByComparingTo("-30");
        assertThat(ledger(c, "3201").subtract(goods)).isEqualByComparingTo("0");
    }

    @Test
    void purchaseAtANewPriceMovesTheAverageCost() throws Exception {
        Ctx c = context();
        long product = product(c, "Oleo 1 L (teste custo medio)", "150", "100");
        entry(c, product, "10");
        JsonNode suppliers = getJson("/api/purchases/suppliers?companyId=" + c.companyId(), c);
        assertThat(suppliers.isEmpty()).as("seed needs a supplier").isFalse();

        // 10 on hand at 100 plus 10 bought at 120: the cost becomes 110, so the next sale costs what it really cost.
        json(send(post("/api/purchases"), c, """
                {"supplierId":%d,"warehouseId":%d,"companyId":%s,
                 "lines":[{"productId":%d,"quantity":10,"unitPrice":120,"taxRate":0}]}"""
                .formatted(suppliers.get(0).get("id").asLong(), c.warehouseId(), c.companyId(), product)));
        BigDecimal cost = null;
        for (JsonNode p : getJson("/api/comercial/products", c)) {
            if (p.get("id").asLong() == product) cost = new BigDecimal(p.get("purchasePrice").asText());
        }
        assertThat(cost).isEqualByComparingTo("110");
    }

    @Test
    void returnToSupplierLowersDebtStockAndGoodsAccount() throws Exception {
        Ctx c = context();
        long product = product(c, "Tinta 5 L (teste devolucao)", "900", "500");
        long supplier = getJson("/api/purchases/suppliers?companyId=" + c.companyId(), c).get(0).get("id").asLong();
        JsonNode purchase = json(send(post("/api/purchases"), c, """
                {"supplierId":%d,"warehouseId":%d,"companyId":%s,
                 "lines":[{"productId":%d,"quantity":10,"unitPrice":500,"taxRate":0.16}]}"""
                .formatted(supplier, c.warehouseId(), c.companyId(), product)));
        long purchaseId = purchase.get("id").asLong();
        long lineId = purchase.get("lines").get(0).get("id").asLong();
        BigDecimal goods = ledger(c, "3201");
        BigDecimal suppliers = ledger(c, "2201");

        // 4 tins back: 2000 net + 320 VAT come off the debt; the goods account drops 2000.
        JsonNode ret = json(send(post("/api/purchases/" + purchaseId + "/returns"), c, """
                {"reason":"Latas amolgadas","lines":[{"purchaseLineId":%d,"quantity":4}]}""".formatted(lineId)));
        assertThat(ret.get("returnNumber").asText()).startsWith("DF-");
        assertThat(new BigDecimal(ret.get("totalAmount").asText())).isEqualByComparingTo("2320");
        assertThat(ledger(c, "3201").subtract(goods)).isEqualByComparingTo("-2000");
        assertThat(ledger(c, "2201").subtract(suppliers).abs()).isEqualByComparingTo("2320");
        BigDecimal outstanding = null;
        for (JsonNode p : getJson("/api/purchases/payables?companyId=" + c.companyId(), c)) {
            if (p.get("purchaseId").asLong() == purchaseId) outstanding = new BigDecimal(p.get("outstanding").asText());
        }
        assertThat(outstanding).isEqualByComparingTo("3480");

        // Only 6 are left to return.
        send(post("/api/purchases/" + purchaseId + "/returns"), c, """
                {"reason":"Mais latas","lines":[{"purchaseLineId":%d,"quantity":7}]}""".formatted(lineId))
                .andExpect(status().is4xxClientError());
    }

    private BigDecimal ledger(Ctx c, String account) throws Exception {
        return new BigDecimal(getJson("/api/accounting/ledger/" + account + "?from=2000-01-01&to=" + LocalDate.now().plusDays(1), c)
                .get("closingBalance").asText());
    }

    private long product(Ctx c, String name, String price, String cost) throws Exception {
        return json(send(post("/api/comercial/products"), c, """
                {"sku":"IP-%s","name":"%s","unitPrice":%s,"purchasePrice":%s,"minStock":0,"unitsPerBox":1,
                 "saleType":"UNIT","stockTracked":true}""".formatted(UUID.randomUUID().toString().substring(0, 8), name, price, cost)))
                .get("id").asLong();
    }

    private void entry(Ctx c, long productId, String qty) throws Exception {
        send(post("/api/inventory/movements"), c, """
                {"productId":%d,"warehouseId":%d,"quantity":%s,"movementType":"ENTRY"}""".formatted(productId, c.warehouseId(), qty))
                .andExpect(status().isOk());
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
