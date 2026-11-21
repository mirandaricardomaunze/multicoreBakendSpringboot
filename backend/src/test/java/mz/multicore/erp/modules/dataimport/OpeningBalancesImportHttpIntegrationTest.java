package mz.multicore.erp.modules.dataimport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IMP-12 — endpoints de stock inicial, dívidas de clientes (SI) e dívidas a fornecedores (SIF): modelo, pré-visualizar e
 * confirmar com tudo ou nada, na empresa activa e só para ADMIN ({@code docs/IMPORTACAO_DADOS_SPEC.md} §3 e §5).
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:opening-import;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpeningBalancesImportHttpIntegrationTest {

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private static final String CLIENT_HEADER = "nome;nuit;email;morada;prazo_pagamento_dias;limite_credito";
    private static final String SUPPLIER_HEADER = "nome;nuit;email;telefone;morada;contacto";
    private static final String PRODUCT_HEADER = "sku;nome;preco_venda;iva;categoria";
    private static final String STOCK_HEADER = "sku;armazem;quantidade;custo_unitario;lote;validade";
    private static final String RECEIVABLE_HEADER = "nuit_cliente;numero_original;data;vencimento;valor_em_divida;descricao";
    private static final String PAYABLE_HEADER = "nuit_fornecedor;numero_original;data;vencimento;valor_em_divida;descricao";

    private static final String PAST = "2026-01-10";
    private static final String DUE = "2026-02-10";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PlatformTransactionManager transactionManager;

    private record Ctx(String token, String companyId) {}

    // ---- modelos (§3.1 passo 1) ---------------------------------------------------------------------

    @Test
    @DisplayName("IMP-12: modelos de stock inicial, dívidas de clientes e a fornecedores com as colunas da SPEC §5")
    void templatesHaveSpecColumns() throws Exception {
        assertTemplate("opening-stock", "modelo-stock-inicial.csv", STOCK_HEADER);
        assertTemplate("opening-receivables", "modelo-dividas-clientes.csv", RECEIVABLE_HEADER);
        assertTemplate("opening-payables", "modelo-dividas-fornecedores.csv", PAYABLE_HEADER);
    }

    // ---- stock inicial (§5.1) -----------------------------------------------------------------------

    @Test
    @DisplayName("IMP-12: stock inicial — pré-visualizar não grava; confirmar dá entrada; reimportar é recusado")
    void openingStockPreviewCommitAndRetry() throws Exception {
        Ctx c = admin();
        upload("products", "commit", c, PRODUCT_HEADER, "IMP12-ST-A;Artigo Stock A;10;IVA16;Geral");
        String warehouse = firstWarehouseName(c);
        String line = "IMP12-ST-A;" + warehouse + ";40;7,50;L-IMP12;";

        JsonNode preview = upload("opening-stock", "preview", c, STOCK_HEADER, line);
        assertThat(preview.get("rows").get(0).get("status").asText()).isEqualTo("NOVO");
        assertThat(stockOf(c, "IMP12-ST-A")).isEmpty();

        JsonNode result = upload("opening-stock", "commit", c, STOCK_HEADER, line);
        assertThat(result.get("created").asInt()).isEqualTo(1);
        List<JsonNode> stock = stockOf(c, "IMP12-ST-A");
        assertThat(stock).hasSize(1);
        assertThat(stock.get(0).get("quantity").decimalValue()).isEqualByComparingTo("40");

        String body = uploadRequest("opening-stock", "commit", c, STOCK_HEADER, line)
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("nada foi gravado");
        assertThat(stockOf(c, "IMP12-ST-A").get(0).get("quantity").decimalValue()).isEqualByComparingTo("40");
    }

    // ---- dívidas de clientes (§5.2) -----------------------------------------------------------------

    @Test
    @DisplayName("IMP-12: SI — confirmar cria o Saldo Inicial por receber; repetir é JÁ EXISTE e não duplica")
    void openingReceivableCommitAndRetry() throws Exception {
        Ctx c = admin();
        upload("clients", "commit", c, CLIENT_HEADER, "Cliente Imp12 SI;416000101;;;;");
        String line = "416000101;FT-ANT-101;" + PAST + ";" + DUE + ";1.250,00;Factura do Primavera";

        JsonNode preview = upload("opening-receivables", "preview", c, RECEIVABLE_HEADER, line);
        assertThat(preview.get("rows").get(0).get("status").asText()).isEqualTo("NOVO");
        assertThat(openingReceivables(c, "416000101")).isEmpty();

        JsonNode result = upload("opening-receivables", "commit", c, RECEIVABLE_HEADER, line);
        assertThat(result.get("created").asInt()).isEqualTo(1);
        List<JsonNode> receivables = openingReceivables(c, "416000101");
        assertThat(receivables).hasSize(1);
        assertThat(receivables.get(0).get("totalAmount").decimalValue()).isEqualByComparingTo("1250");

        JsonNode again = upload("opening-receivables", "commit", c, RECEIVABLE_HEADER, line);
        assertThat(again.get("created").asInt()).isZero();
        assertThat(again.get("alreadyExisting").asInt()).isEqualTo(1);
        assertThat(openingReceivables(c, "416000101")).hasSize(1);
    }

    @Test
    @DisplayName("IMP-12: SI com uma linha em erro — confirmação recusada e nenhuma dívida gravada")
    void openingReceivableWithAnyErrorWritesNothing() throws Exception {
        Ctx c = admin();
        upload("clients", "commit", c, CLIENT_HEADER, "Cliente Imp12 Erro;416000102;;;;");
        String future = LocalDate.now().plusDays(5).toString();

        String body = uploadRequest("opening-receivables", "commit", c, RECEIVABLE_HEADER,
                        "416000102;FT-ANT-OK;" + PAST + ";" + DUE + ";100;",
                        "416000102;FT-ANT-FUT;" + future + ";" + future + ";100;")
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(body).contains("nada foi gravado");
        assertThat(openingReceivables(c, "416000102")).isEmpty();
    }

    // ---- dívidas a fornecedores (§5.3) --------------------------------------------------------------

    @Test
    @DisplayName("IMP-12: SIF — confirmar põe a dívida nas contas a pagar; repetir é JÁ EXISTE e não duplica")
    void openingPayableCommitAndRetry() throws Exception {
        Ctx c = admin();
        upload("suppliers", "commit", c, SUPPLIER_HEADER, "Fornecedor Imp12 SIF;416000103;;;;");
        String line = "416000103;FA-ANT-103;" + PAST + ";" + DUE + ";800;Compra antiga";

        JsonNode preview = upload("opening-payables", "preview", c, PAYABLE_HEADER, line);
        assertThat(preview.get("rows").get(0).get("status").asText()).isEqualTo("NOVO");
        assertThat(openingPayables(c, "Fornecedor Imp12 SIF")).isEmpty();

        JsonNode result = upload("opening-payables", "commit", c, PAYABLE_HEADER, line);
        assertThat(result.get("created").asInt()).isEqualTo(1);
        List<JsonNode> payables = openingPayables(c, "Fornecedor Imp12 SIF");
        assertThat(payables).hasSize(1);
        assertThat(payables.get(0).get("outstanding").decimalValue()).isEqualByComparingTo("800");

        JsonNode again = upload("opening-payables", "commit", c, PAYABLE_HEADER, line);
        assertThat(again.get("alreadyExisting").asInt()).isEqualTo(1);
        assertThat(openingPayables(c, "Fornecedor Imp12 SIF")).hasSize(1);
    }

    // ---- empresa activa (§3.3) ----------------------------------------------------------------------

    @Test
    @DisplayName("IMP-12: a dívida fica só na empresa activa; noutra empresa o fornecedor não existe e é ERRO")
    void openingBalancesStayInTheActiveCompany() throws Exception {
        Ctx active = adminIn(1);
        Ctx other = adminIn(0);
        upload("suppliers", "commit", active, SUPPLIER_HEADER, "Fornecedor Imp12 Tenant;416000104;;;;");
        String line = "416000104;FA-ANT-104;" + PAST + ";" + DUE + ";300;";

        upload("opening-payables", "commit", active, PAYABLE_HEADER, line);

        assertThat(openingPayables(active, "Fornecedor Imp12 Tenant")).hasSize(1);
        assertThat(openingPayables(other, "Fornecedor Imp12 Tenant")).isEmpty();
        JsonNode otherPreview = upload("opening-payables", "preview", other, PAYABLE_HEADER, line);
        JsonNode row = otherPreview.get("rows").get(0);
        assertThat(row.get("status").asText()).isEqualTo("ERRO");
        assertThat(row.get("message").asText()).contains("importe primeiro os fornecedores");
    }

    // ---- auditoria (§3.1 passo 4) -------------------------------------------------------------------

    @Test
    @DisplayName("IMP-12: confirmar dívidas de clientes deixa um DATA_IMPORT com a entidade e os totais")
    void commitWritesDataImportAudit() throws Exception {
        Ctx c = admin();
        upload("clients", "commit", c, CLIENT_HEADER, "Cliente Imp12 Aud;416000105;;;;");

        mockMvc.perform(auth(post("/api/data-import/opening-receivables/commit"), c)
                        .param("fileName", "dividas-imp12.csv")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(csv(RECEIVABLE_HEADER, "416000105;FT-ANT-105;" + PAST + ";" + DUE + ";50;")))
                .andExpect(status().isOk());

        List<JsonNode> entries = auditEntries(c, "dividas-imp12.csv");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).get("action").asText()).isEqualTo("DATA_IMPORT");
        assertThat(entries.get(0).get("details").asText()).contains("dívidas de clientes").contains("1 novo");
    }

    // ---- só ADMIN (§3.3) ----------------------------------------------------------------------------

    @Test
    @DisplayName("IMP-12: gestor e vendedor recebem 403 no modelo, pré-visualização e confirmação dos saldos iniciais")
    void onlyAdminMayImportOpeningBalances() throws Exception {
        Ctx admin = admin();
        upload("suppliers", "commit", admin, SUPPLIER_HEADER, "Fornecedor Imp12 Proibido;416000106;;;;");
        String payable = "416000106;FA-ANT-106;" + PAST + ";" + DUE + ";10;";
        for (String role : List.of("MANAGER", "SELLER")) {
            Ctx user = userWithRole(admin, role);
            for (String entity : List.of("opening-stock", "opening-receivables", "opening-payables")) {
                mockMvc.perform(auth(get("/api/data-import/" + entity + "/template"), user))
                        .andExpect(status().isForbidden());
            }
            uploadRequest("opening-stock", "preview", user, STOCK_HEADER, "X;Y;1;1;;")
                    .andExpect(status().isForbidden());
            uploadRequest("opening-receivables", "preview", user, RECEIVABLE_HEADER, payable)
                    .andExpect(status().isForbidden());
            uploadRequest("opening-payables", "commit", user, PAYABLE_HEADER, payable)
                    .andExpect(status().isForbidden());
        }
        assertThat(openingPayables(admin, "Fornecedor Imp12 Proibido")).isEmpty();
    }

    // ---- apoio --------------------------------------------------------------------------------------

    private String firstWarehouseName(Ctx c) throws Exception {
        JsonNode warehouses = getJson("/api/inventory/warehouses", c);
        assertThat(warehouses.size()).as("a empresa do arranque tem armazém").isPositive();
        return warehouses.get(0).get("name").asText();
    }

    private List<JsonNode> stockOf(Ctx c, String sku) throws Exception {
        return filter(getJson("/api/inventory/stocks", c), "sku", sku);
    }

    private List<JsonNode> openingReceivables(Ctx c, String clientTaxId) throws Exception {
        List<JsonNode> matches = new ArrayList<>();
        for (JsonNode invoice : filter(getJson("/api/comercial/invoices/outstanding", c), "clientTaxId", clientTaxId)) {
            if (invoice.get("invoiceNumber").asText().startsWith("SI-")) {
                matches.add(invoice);
            }
        }
        return matches;
    }

    private List<JsonNode> openingPayables(Ctx c, String supplierName) throws Exception {
        List<JsonNode> matches = new ArrayList<>();
        for (JsonNode payable : filter(getJson("/api/purchases/payables", c), "supplierName", supplierName)) {
            if (payable.get("purchaseNumber").asText().startsWith("SIF-")) {
                matches.add(payable);
            }
        }
        return matches;
    }

    private static List<JsonNode> filter(JsonNode array, String field, String value) {
        List<JsonNode> matches = new ArrayList<>();
        array.forEach(node -> {
            if (value.equals(node.path(field).asText())) {
                matches.add(node);
            }
        });
        return matches;
    }

    private JsonNode getJson(String path, Ctx c) throws Exception {
        return om.readTree(mockMvc.perform(auth(get(path).param("companyId", c.companyId()), c))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private List<JsonNode> auditEntries(Ctx c, String fileName) throws Exception {
        JsonNode logs = om.readTree(mockMvc.perform(auth(get("/api/audit/logs"), c))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        List<JsonNode> matches = new ArrayList<>();
        logs.forEach(log -> {
            if (log.path("details").asText().contains(fileName)) {
                matches.add(log);
            }
        });
        return matches;
    }

    private static byte[] csv(String header, String... lines) {
        return (header + "\r\n" + String.join("\r\n", lines) + "\r\n").getBytes(StandardCharsets.UTF_8);
    }

    private JsonNode upload(String entity, String action, Ctx c, String header, String... lines) throws Exception {
        return om.readTree(uploadRequest(entity, action, c, header, lines)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private ResultActions uploadRequest(String entity, String action, Ctx c, String header, String... lines)
            throws Exception {
        return mockMvc.perform(auth(post("/api/data-import/" + entity + "/" + action), c)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(csv(header, lines)));
    }

    private void assertTemplate(String entity, String fileName, String header) throws Exception {
        Ctx c = admin();
        MockHttpServletResponse response = mockMvc.perform(auth(get("/api/data-import/" + entity + "/template"), c))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertThat(response.getContentType()).startsWith("text/csv");
        assertThat(response.getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"" + fileName + "\"");
        byte[] bytes = response.getContentAsByteArray();
        assertThat(Arrays.copyOfRange(bytes, 0, 3)).as("começa com BOM UTF-8").isEqualTo(UTF8_BOM);
        assertThat(new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8)).isEqualTo(header + "\r\n");
    }

    /** Utilizador com o perfil dado na empresa activa do {@code admin}, criado para o teste. */
    private Ctx userWithRole(Ctx admin, String role) throws Exception {
        String username = "saldos-" + role.toLowerCase(Locale.ROOT);
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            var company = companyRepository.findById(Long.valueOf(admin.companyId())).orElseThrow();
            AppUser user = appUserRepository.findByUsername(username).orElseGet(AppUser::new);
            user.setUsername(username);
            user.setName("Teste " + role);
            user.setPassword(passwordEncoder.encode("password"));
            user.setRole(role);
            user.setActive(true);
            user.grantCompany(company, role);
            appUserRepository.save(user);
        });
        JsonNode login = om.readTree(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        return new Ctx(login.get("token").asText(), admin.companyId());
    }

    private Ctx admin() throws Exception {
        return adminIn(0);
    }

    /** A Ana é ADMIN nas duas empresas do arranque; {@code index} escolhe a empresa activa pela ordem do id. */
    private Ctx adminIn(int index) throws Exception {
        JsonNode login = om.readTree(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        List<Long> companyIds = new ArrayList<>();
        login.get("companies").forEach(company -> companyIds.add(company.get("id").asLong()));
        companyIds.sort(null);
        return new Ctx(login.get("token").asText(), String.valueOf(companyIds.get(index)));
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req, Ctx c) {
        return req.header("Authorization", "Bearer " + c.token()).header("X-Company-Id", c.companyId());
    }
}
