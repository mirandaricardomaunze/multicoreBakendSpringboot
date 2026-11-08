package mz.multicore.erp.modules.pickup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Paid material awaiting pickup, end to end over HTTP ({@code docs/MATERIAL_POR_LEVANTAR_SPEC.md}):
 * a client pays 200 bags, collects them in parts, can never collect more than paid, and the stock is
 * only moved once, by the invoice.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:pickups;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PickupHttpIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PlatformTransactionManager transactionManager;

    private record Ctx(String token, String companyId, long warehouseId, long clientId) {}

    @Test
    void clientCollectsPaidMaterialInPartsAndNeverMoreThanPaid() throws Exception {
        Ctx c = context();
        long productId = createProduct(c, "Cimento 42.5 (teste levantamento)");
        addStock(c, productId, "500");
        BigDecimal stockBefore = stock(c, productId);

        JsonNode invoice = read(send(post("/api/comercial/invoices"), c, """
                {"clientId":%d,"companyId":%s,"warehouseId":%d,
                 "lines":[{"productId":%d,"quantity":200,"taxRate":0.16,"discountPercentage":0}]}"""
                .formatted(c.clientId(), c.companyId(), c.warehouseId(), productId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        long invoiceId = invoice.get("id").asLong();
        assertThat(stock(c, productId)).as("the invoice moves the stock once")
                .isEqualByComparingTo(stockBefore.subtract(new BigDecimal("200")));

        JsonNode eligible = getJson("/api/pickups/eligible-invoices?companyId=" + c.companyId()
                + "&search=" + invoice.get("invoiceNumber").asText(), c);
        assertThat(eligible.isArray() && eligible.size() == 1).isTrue();

        JsonNode account = read(send(post("/api/pickups/accounts"), c, """
                {"companyId":%s,"invoiceId":%d}""".formatted(c.companyId(), invoiceId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        long accountId = account.get("summary").get("id").asLong();
        long lineId = account.get("lines").get(0).get("invoiceLineId").asLong();
        assertThat(new BigDecimal(account.get("lines").get(0).get("balance").asText())).isEqualByComparingTo("200");

        // The same invoice cannot be registered twice.
        send(post("/api/pickups/accounts"), c, """
                {"companyId":%s,"invoiceId":%d}""".formatted(c.companyId(), invoiceId)).andExpect(status().isBadRequest());

        String key = UUID.randomUUID().toString();
        JsonNode first = read(issue(c, accountId, lineId, "20", key).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(first.get("number").asText()).startsWith("LV-");
        // A retry with the same key returns the same slip instead of issuing a second one.
        JsonNode retry = read(issue(c, accountId, lineId, "20", key).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(retry.get("id").asLong()).isEqualTo(first.get("id").asLong());

        issue(c, accountId, lineId, "181", null).andExpect(status().isBadRequest());

        JsonNode afterFirst = getJson("/api/pickups/accounts/" + accountId + "?companyId=" + c.companyId(), c);
        assertThat(new BigDecimal(afterFirst.get("lines").get(0).get("balance").asText())).isEqualByComparingTo("180");
        assertThat(stock(c, productId)).as("pickups never move stock")
                .isEqualByComparingTo(stockBefore.subtract(new BigDecimal("200")));

        JsonNode pending = getJson("/api/pickups/pending-stock?companyId=" + c.companyId(), c);
        BigDecimal pendingForProduct = BigDecimal.ZERO;
        for (JsonNode row : pending) {
            if (row.get("productId").asLong() == productId) pendingForProduct = new BigDecimal(row.get("pendingQuantity").asText());
        }
        assertThat(pendingForProduct).isEqualByComparingTo("180");

        JsonNode last = read(issue(c, accountId, lineId, "180", null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        JsonNode completed = getJson("/api/pickups/accounts/" + accountId + "?companyId=" + c.companyId(), c);
        assertThat(completed.get("summary").get("status").asText()).isEqualTo("COMPLETED");
        issue(c, accountId, lineId, "1", null).andExpect(status().isBadRequest());

        // Cancelling a slip returns its quantity and reopens the account.
        send(post("/api/pickups/slips/" + last.get("id").asLong() + "/cancel"), c, """
                {"companyId":%s,"reason":"Camião avariou antes de sair"}""".formatted(c.companyId()))
                .andExpect(status().isOk());
        JsonNode reopened = getJson("/api/pickups/accounts/" + accountId + "?companyId=" + c.companyId(), c);
        assertThat(reopened.get("summary").get("status").asText()).isEqualTo("OPEN");
        assertThat(new BigDecimal(reopened.get("lines").get(0).get("balance").asText())).isEqualByComparingTo("180");

        byte[] pdf = mockMvc.perform(auth(get("/api/pickups/slips/" + first.get("id").asLong() + "/pdf?companyId="
                        + c.companyId()), c))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void sellerIssuesSlipsButCannotCancelThem() throws Exception {
        Ctx admin = context();
        long productId = createProduct(admin, "Varão 10mm (teste levantamento)");
        addStock(admin, productId, "50");
        JsonNode invoice = read(send(post("/api/comercial/invoices"), admin, """
                {"clientId":%d,"companyId":%s,"warehouseId":%d,
                 "lines":[{"productId":%d,"quantity":10,"taxRate":0.16,"discountPercentage":0}]}"""
                .formatted(admin.clientId(), admin.companyId(), admin.warehouseId(), productId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            var company = companyRepository.findById(Long.valueOf(admin.companyId())).orElseThrow();
            AppUser seller = appUserRepository.findByUsername("vendedor-levantamentos").orElseGet(AppUser::new);
            seller.setUsername("vendedor-levantamentos");
            seller.setName("Fiel de Armazém");
            seller.setPassword(passwordEncoder.encode("password"));
            seller.setRole("SELLER");
            seller.setActive(true);
            seller.grantCompany(company, "SELLER");
            appUserRepository.save(seller);
        });
        Ctx seller = new Ctx(login("vendedor-levantamentos"), admin.companyId(), admin.warehouseId(), admin.clientId());

        JsonNode account = read(send(post("/api/pickups/accounts"), seller, """
                {"companyId":%s,"invoiceId":%d}""".formatted(seller.companyId(), invoice.get("id").asLong()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        long accountId = account.get("summary").get("id").asLong();
        long lineId = account.get("lines").get(0).get("invoiceLineId").asLong();
        JsonNode slip = read(issue(seller, accountId, lineId, "4", null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        send(post("/api/pickups/slips/" + slip.get("id").asLong() + "/cancel"), seller, """
                {"companyId":%s,"reason":"teste"}""".formatted(seller.companyId()))
                .andExpect(status().isForbidden());
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private ResultActions issue(Ctx c, long accountId, long lineId, String qty, String key) throws Exception {
        String keyJson = key == null ? "" : ",\"requestKey\":\"" + key + "\"";
        return send(post("/api/pickups/slips"), c, """
                {"companyId":%s,"accountId":%d,"collectorName":"João Motorista","vehiclePlate":"aaa-123-mp"%s,
                 "lines":[{"invoiceLineId":%d,"quantity":%s}]}""".formatted(c.companyId(), accountId, keyJson, lineId, qty));
    }

    private long createProduct(Ctx c, String name) throws Exception {
        String body = send(post("/api/comercial/products"), c, """
                {"sku":"LV-%s","name":"%s","unitPrice":650,"purchasePrice":1,"minStock":0,"unitsPerBox":1,
                 "saleType":"UNIT","stockTracked":true}""".formatted(UUID.randomUUID().toString().substring(0, 8), name))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return read(body).get("id").asLong();
    }

    private void addStock(Ctx c, long productId, String quantity) throws Exception {
        send(post("/api/inventory/movements"), c, """
                {"productId":%d,"warehouseId":%d,"quantity":%s,"movementType":"ENTRY"}"""
                .formatted(productId, c.warehouseId(), quantity)).andExpect(status().isOk());
    }

    private BigDecimal stock(Ctx c, long productId) throws Exception {
        for (JsonNode s : getJson("/api/inventory/stocks?companyId=" + c.companyId() + "&warehouseId=" + c.warehouseId(), c)) {
            if (s.get("productId").asLong() == productId) return new BigDecimal(s.get("quantity").asText());
        }
        return BigDecimal.ZERO;
    }

    private Ctx context() throws Exception {
        String token = login("ana");
        JsonNode login = read(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        token = login.get("token").asText();
        for (JsonNode company : login.get("companies")) {
            String companyId = company.get("id").asText();
            Ctx probe = new Ctx(token, companyId, 0, 0);
            JsonNode warehouses = getJson("/api/inventory/warehouses/sales?companyId=" + companyId, probe);
            Long namedClient = null;
            for (JsonNode client : getJson("/api/comercial/clients", probe)) {
                if (!"000000000".equals(client.path("taxId").asText())) { namedClient = client.get("id").asLong(); break; }
            }
            if (warehouses.isEmpty() || namedClient == null) continue;
            return new Ctx(token, companyId, warehouses.get(0).get("id").asLong(), namedClient);
        }
        throw new IllegalStateException("No company with a sales warehouse and a named client in the seed.");
    }

    private String login(String username) throws Exception {
        return read(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("token").asText();
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req, Ctx c) {
        return req.header("Authorization", "Bearer " + c.token()).header("X-Company-Id", c.companyId());
    }

    private JsonNode getJson(String path, Ctx c) throws Exception {
        return read(mockMvc.perform(auth(get(path), c)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private ResultActions send(MockHttpServletRequestBuilder req, Ctx c, String json) throws Exception {
        return mockMvc.perform(auth(req, c).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private JsonNode read(String body) {
        try {
            return om.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException(body, e);
        }
    }
}
