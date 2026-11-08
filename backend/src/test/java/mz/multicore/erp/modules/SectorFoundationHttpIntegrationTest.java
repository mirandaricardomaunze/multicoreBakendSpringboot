package mz.multicore.erp.modules;

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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Business sectors phase F1, end to end over HTTP ({@code docs/SECTORES_ACTIVIDADE_SPEC.md}):
 * activity profiles (SA-C01), units of measure (SA-C02) and price lists applied to invoices (SA-C03).
 * It also proves that, without an assigned list, invoices keep the existing price.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:sector-foundation;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SectorFoundationHttpIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PlatformTransactionManager transactionManager;

    private record Ctx(String token, String companyId, long warehouseId, long clientId) {}

    // ─── SA-C01 ─────────────────────────────────────────────────────────────────
    @Test
    void perfilDeActividadeComecaEmComercioGeralEPodeCombinarSectores() throws Exception {
        Ctx c = context();
        JsonNode catalog = getJson("/api/activity/catalog", c);
        assertThat(codes(catalog.get("profiles"))).contains("BUTCHER", "BAKERY", "BUILDING_MATERIALS", "BLOCK_FACTORY");

        JsonNode updated = sendJson(put("/api/activity/company"), c, """
                {"companyId":%s,"profiles":["BUILDING_MATERIALS","BLOCK_FACTORY"]}""".formatted(c.companyId()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString().transform(this::read);
        assertThat(updated.get("configured").asBoolean()).isTrue();
        assertThat(texts(updated.get("profiles"))).containsExactlyInAnyOrder("BUILDING_MATERIALS", "BLOCK_FACTORY");
        assertThat(texts(updated.get("capabilities"))).contains("PENDING_PICKUP", "PRODUCTION", "CUSTOMER_PROJECTS");

        JsonNode reread = getJson("/api/activity/company?companyId=" + c.companyId(), c);
        assertThat(texts(reread.get("profiles"))).containsExactlyInAnyOrder("BUILDING_MATERIALS", "BLOCK_FACTORY");

        sendJson(put("/api/activity/company"), c, """
                {"companyId":%s,"profiles":["CASINO"]}""".formatted(c.companyId()))
                .andExpect(status().isBadRequest());
        sendJson(put("/api/activity/company"), c, """
                {"companyId":%s,"profiles":[]}""".formatted(c.companyId()))
                .andExpect(status().isBadRequest());
    }

    // ─── SA-C02 ─────────────────────────────────────────────────────────────────
    @Test
    void unidadesDoProdutoConvertemESoMudamABaseEnquantoNaoHaUso() throws Exception {
        Ctx c = context();
        JsonNode units = getJson("/api/units?companyId=" + c.companyId(), c);
        assertThat(texts(units, "code")).contains("UN", "KG", "SACO", "PALETE", "M3", "TON");

        long productId = createProduct(c, "Cimento 42.5 (teste unidades)", "650.00");

        // A bag cannot be the base unit: packages are alternative units.
        sendJson(put("/api/units/products/base"), c, """
                {"companyId":%s,"productId":%d,"unitCode":"SACO"}""".formatted(c.companyId(), productId))
                .andExpect(status().isBadRequest());

        // One pallet holds 40 units.
        JsonNode withPallet = sendJson(put("/api/units/products"), c, """
                {"companyId":%s,"productId":%d,"unitCode":"PALETE","factorToBase":40,
                 "sellable":true,"purchasable":true,"defaultSale":false,"defaultPurchase":true}"""
                .formatted(c.companyId(), productId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString().transform(this::read);
        assertThat(withPallet.get("baseUnit").get("code").asText()).isEqualTo("UN");
        assertThat(withPallet.get("baseUnitLocked").asBoolean()).isTrue();
        assertThat(texts(withPallet.get("units"), "unitCode")).contains("PALETE");

        JsonNode conversion = getJson("/api/units/products/" + productId + "/convert?companyId=" + c.companyId()
                + "&unitCode=PALETE&quantity=3", c);
        assertThat(new BigDecimal(conversion.get("baseQuantity").asText())).isEqualByComparingTo("120");

        // 0.01 pallet would be 0.4 units, so it is rejected.
        mockMvc.perform(auth(get("/api/units/products/" + productId + "/convert?companyId=" + c.companyId()
                        + "&unitCode=PALETE&quantity=0.01"), c))
                .andExpect(status().isBadRequest());

        // Grams on a unit-based product would produce fractions, so they are rejected.
        sendJson(put("/api/units/products"), c, """
                {"companyId":%s,"productId":%d,"unitCode":"G","sellable":true,"purchasable":true,
                 "defaultSale":false,"defaultPurchase":false}""".formatted(c.companyId(), productId))
                .andExpect(status().isBadRequest());

        // The base can no longer change because an alternative unit depends on it.
        sendJson(put("/api/units/products/base"), c, """
                {"companyId":%s,"productId":%d,"unitCode":"KG"}""".formatted(c.companyId(), productId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void produtoNovoSemUsoPodeTerBaseEmMetroCubico() throws Exception {
        Ctx c = context();
        long productId = createProduct(c, "Areia grossa (teste m3)", "1800.00");
        JsonNode result = sendJson(put("/api/units/products/base"), c, """
                {"companyId":%s,"productId":%d,"unitCode":"M3"}""".formatted(c.companyId(), productId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString().transform(this::read);
        assertThat(result.get("baseUnit").get("code").asText()).isEqualTo("M3");

        // One truck load is 6 m³.
        sendJson(put("/api/units/products"), c, """
                {"companyId":%s,"productId":%d,"unitCode":"CARGA","factorToBase":6,
                 "sellable":true,"purchasable":false,"defaultSale":true,"defaultPurchase":false}"""
                .formatted(c.companyId(), productId))
                .andExpect(status().isOk());
        JsonNode conversion = getJson("/api/units/products/" + productId + "/convert?companyId=" + c.companyId()
                + "&unitCode=CARGA&quantity=2", c);
        assertThat(new BigDecimal(conversion.get("baseQuantity").asText())).isEqualByComparingTo("12");
    }

    // ─── SA-C03 ─────────────────────────────────────────────────────────────────
    @Test
    void tabelaDoClienteEntraNaFaturaESemTabelaFicaOPrecoDeSempre() throws Exception {
        Ctx c = context();
        long productId = createProduct(c, "Varão 12mm (teste preços)", "1000.00");
        addStock(c, productId, "100");

        JsonNode list = sendJson(put("/api/pricing/lists"), c, """
                {"companyId":%s,"code":"EMP-%s","name":"Empreiteiros","active":true}"""
                .formatted(c.companyId(), shortId()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString().transform(this::read);
        long listId = list.get("id").asLong();

        sendJson(put("/api/pricing/items"), c, """
                {"companyId":%s,"priceListId":%d,"productId":%d,"minQuantity":0,"price":900}"""
                .formatted(c.companyId(), listId, productId)).andExpect(status().isOk());
        sendJson(put("/api/pricing/items"), c, """
                {"companyId":%s,"priceListId":%d,"productId":%d,"minQuantity":10,"price":850}"""
                .formatted(c.companyId(), listId, productId)).andExpect(status().isOk());

        // Before the assignment the client pays the normal price.
        assertThat(invoiceUnitPrice(c, c.clientId(), productId, "2")).isEqualByComparingTo("1000.00");

        sendJson(put("/api/pricing/clients"), c, """
                {"companyId":%s,"clientId":%d,"priceListId":%d}""".formatted(c.companyId(), c.clientId(), listId))
                .andExpect(status().isOk());

        JsonNode preview = getJson("/api/pricing/resolve?companyId=" + c.companyId() + "&clientId=" + c.clientId()
                + "&productId=" + productId + "&quantity=12", c);
        assertThat(preview.get("source").asText()).isEqualTo("PRICE_LIST");
        assertThat(new BigDecimal(preview.get("unitPrice").asText())).isEqualByComparingTo("850");

        assertThat(invoiceUnitPrice(c, c.clientId(), productId, "2")).isEqualByComparingTo("900");
        assertThat(invoiceUnitPrice(c, c.clientId(), productId, "10")).isEqualByComparingTo("850");
        // A sale without a client (walk-in) keeps the normal price.
        assertThat(invoiceUnitPrice(c, null, productId, "2")).isEqualByComparingTo("1000.00");
        // The walk-in client cannot receive a list because it would change every counter sale.
        long walkInId = -1;
        for (JsonNode client : getJson("/api/comercial/clients", c)) {
            if ("000000000".equals(client.path("taxId").asText())) walkInId = client.get("id").asLong();
        }
        if (walkInId > 0) {
            sendJson(put("/api/pricing/clients"), c, """
                    {"companyId":%s,"clientId":%d,"priceListId":%d}""".formatted(c.companyId(), walkInId, listId))
                    .andExpect(status().isBadRequest());
        }

        // An inactive list no longer applies.
        sendJson(put("/api/pricing/lists"), c, """
                {"id":%d,"companyId":%s,"code":"%s","name":"Empreiteiros","active":false}"""
                .formatted(listId, c.companyId(), list.get("code").asText())).andExpect(status().isOk());
        assertThat(invoiceUnitPrice(c, c.clientId(), productId, "2")).isEqualByComparingTo("1000.00");

        // An assigned list cannot be deleted; remove the assignment first.
        mockMvc.perform(auth(delete("/api/pricing/lists/" + listId + "?companyId=" + c.companyId()), c))
                .andExpect(status().isBadRequest());
        sendJson(put("/api/pricing/clients"), c, """
                {"companyId":%s,"clientId":%d,"priceListId":null}""".formatted(c.companyId(), c.clientId()))
                .andExpect(status().isOk());
        mockMvc.perform(auth(delete("/api/pricing/lists/" + listId + "?companyId=" + c.companyId()), c))
                .andExpect(status().isNoContent());
    }

    @Test
    void freteDaZonaEUmServicoFaturadoComIvaESemStock() throws Exception {
        Ctx c = context();
        long freight = read(sendJson(post("/api/comercial/products"), c, """
                {"sku":"FRT-MATOLA-%s","name":"Frete — Matola","unitPrice":1500,"purchasePrice":0,"minStock":0,
                 "unitsPerBox":1,"saleType":"SERVICE","stockTracked":false}""".formatted(shortId()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        assertThat(invoiceUnitPrice(c, c.clientId(), freight, "1")).isEqualByComparingTo("1500");
    }

    @Test
    void vendedorConsultaMasNaoAlteraTabelasNemUnidades() throws Exception {
        Ctx admin = context();
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            var company = companyRepository.findById(Long.valueOf(admin.companyId())).orElseThrow();
            AppUser seller = appUserRepository.findByUsername("vendedor-sectores").orElseGet(AppUser::new);
            seller.setUsername("vendedor-sectores");
            seller.setName("Vendedor Sectores");
            seller.setPassword(passwordEncoder.encode("password"));
            seller.setRole("SELLER");
            seller.setActive(true);
            seller.grantCompany(company, "SELLER");
            appUserRepository.save(seller);
        });

        Ctx c = new Ctx(login("vendedor-sectores"), admin.companyId(), admin.warehouseId(), admin.clientId());
        mockMvc.perform(auth(get("/api/pricing/lists?companyId=" + c.companyId()), c)).andExpect(status().isOk());
        mockMvc.perform(auth(get("/api/units?companyId=" + c.companyId()), c)).andExpect(status().isOk());
        mockMvc.perform(auth(get("/api/activity/company?companyId=" + c.companyId()), c)).andExpect(status().isOk());

        sendJson(put("/api/pricing/lists"), c, """
                {"companyId":%s,"code":"X%s","name":"Indevida","active":true}""".formatted(c.companyId(), shortId()))
                .andExpect(status().isForbidden());
        sendJson(put("/api/activity/company"), c, """
                {"companyId":%s,"profiles":["BAKERY"]}""".formatted(c.companyId()))
                .andExpect(status().isForbidden());
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private BigDecimal invoiceUnitPrice(Ctx c, Long clientId, long productId, String quantity) throws Exception {
        String client = clientId == null ? "" : "\"clientId\":" + clientId + ",";
        String body = sendJson(post("/api/comercial/invoices"), c, """
                {%s"companyId":%s,"warehouseId":%d,
                 "lines":[{"productId":%d,"quantity":%s,"taxRate":0.16,"discountPercentage":0}]}"""
                .formatted(client, c.companyId(), c.warehouseId(), productId, quantity))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode line = read(body).get("lines").get(0);
        return new BigDecimal(line.get("unitPrice").asText());
    }

    private long createProduct(Ctx c, String name, String price) throws Exception {
        String body = sendJson(post("/api/comercial/products"), c, """
                {"sku":"SA-%s","name":"%s","unitPrice":%s,"purchasePrice":1,"minStock":0,"unitsPerBox":1,
                 "saleType":"UNIT","stockTracked":true}""".formatted(shortId(), name, price))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return read(body).get("id").asLong();
    }

    private void addStock(Ctx c, long productId, String quantity) throws Exception {
        sendJson(post("/api/inventory/movements"), c, """
                {"productId":%d,"warehouseId":%d,"quantity":%s,"movementType":"ENTRY"}"""
                .formatted(productId, c.warehouseId(), quantity)).andExpect(status().isOk());
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
            JsonNode clients = getJson("/api/comercial/clients", probe);
            Long namedClient = null;
            for (JsonNode client : clients) {
                if (!"000000000".equals(client.path("taxId").asText())) { namedClient = client.get("id").asLong(); break; }
            }
            if (warehouses.isEmpty() || namedClient == null) continue;
            return new Ctx(token, companyId, warehouses.get(0).get("id").asLong(), namedClient);
        }
        throw new IllegalStateException("Nenhuma empresa com armazém de venda e cliente na seed.");
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
        return read(mockMvc.perform(auth(get(path), c)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private ResultActions sendJson(MockHttpServletRequestBuilder req, Ctx c, String json) throws Exception {
        return mockMvc.perform(auth(req, c).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private JsonNode read(String body) {
        try {
            return om.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException(body, e);
        }
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static List<String> codes(JsonNode array) {
        return texts(array, "code");
    }

    private static List<String> texts(JsonNode array, String field) {
        List<String> out = new ArrayList<>();
        array.forEach(n -> out.add(n.get(field).asText()));
        return out;
    }

    private static List<String> texts(JsonNode array) {
        List<String> out = new ArrayList<>();
        array.forEach(n -> out.add(n.asText()));
        return out;
    }
}
