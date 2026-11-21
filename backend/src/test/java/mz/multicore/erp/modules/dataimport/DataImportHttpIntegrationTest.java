package mz.multicore.erp.modules.dataimport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Importação de cadastros por HTTP ({@code docs/IMPORTACAO_DADOS_SPEC.md} §3.1). */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:data-import;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DataImportHttpIntegrationTest {

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    private record Ctx(String token, String companyId) {}

    @Test
    @DisplayName("IMP-09: modelo de clientes em UTF-8 com BOM, separador ; e só o cabeçalho")
    void clientTemplateIsUtf8BomSemicolonHeaderOnly() throws Exception {
        assertTemplate("clients", "modelo-clientes.csv",
                "nome;nuit;email;morada;prazo_pagamento_dias;limite_credito");
    }

    @Test
    @DisplayName("IMP-09: modelo de fornecedores com as colunas da SPEC §4.2")
    void supplierTemplateHasSpecColumns() throws Exception {
        assertTemplate("suppliers", "modelo-fornecedores.csv",
                "nome;nuit;email;telefone;morada;contacto");
    }

    @Test
    @DisplayName("IMP-09: modelo de artigos com as colunas da SPEC §4.3")
    void productTemplateHasSpecColumns() throws Exception {
        assertTemplate("products", "modelo-artigos.csv",
                "sku;nome;preco_venda;iva;categoria;codigo_barras;referencia;preco_grosso;unidade;descricao");
    }

    @Test
    @DisplayName("IMP-09: entidade desconhecida é recusada com mensagem PT-MZ")
    void unknownEntityIsRejected() throws Exception {
        Ctx c = admin();
        String body = mockMvc.perform(auth(get("/api/data-import/invoices/template"), c))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("Entidade de importação desconhecida");
    }

    // ---- IMP-06: pré-visualizar não grava; confirmar revalida, tudo ou nada, uma transacção ----------

    private static final String CLIENT_HEADER = "nome;nuit;email;morada;prazo_pagamento_dias;limite_credito";
    private static final String PRODUCT_HEADER = "sku;nome;preco_venda;iva;categoria;codigo_barras";

    @Test
    @DisplayName("IMP-06: pré-visualizar classifica as linhas e não grava nada")
    void previewWritesNothing() throws Exception {
        Ctx c = admin();
        JsonNode preview = upload("clients", "preview", c, CLIENT_HEADER,
                "Importado Prev A;416000001;;;;", "Importado Prev B;416000002;;;;");

        assertThat(preview.get("rows")).hasSize(2);
        assertThat(preview.get("rows").get(0).get("status").asText()).isEqualTo("NOVO");
        assertThat(preview.get("rows").get(1).get("status").asText()).isEqualTo("NOVO");
        assertThat(clientTaxIds(c)).doesNotContain("416000001", "416000002");
    }

    @Test
    @DisplayName("IMP-06: confirmar grava as linhas NOVO e devolve os totais; repetir não duplica")
    void commitWritesNewClientsAndIsNotDuplicatedOnRetry() throws Exception {
        Ctx c = admin();
        String[] lines = {"Importado Com A;416000011;a@importado.co.mz;Maputo;30;1000", "Importado Com B;416000012;;;;"};

        JsonNode result = upload("clients", "commit", c, CLIENT_HEADER, lines);
        assertThat(result.get("created").asInt()).isEqualTo(2);
        assertThat(result.get("associated").asInt()).isZero();
        assertThat(result.get("alreadyExisting").asInt()).isZero();
        assertThat(clientTaxIds(c)).contains("416000011", "416000012");

        JsonNode again = upload("clients", "commit", c, CLIENT_HEADER, lines);
        assertThat(again.get("created").asInt()).isZero();
        assertThat(again.get("alreadyExisting").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("IMP-06: confirmar fornecedores grava pelo serviço das compras")
    void commitWritesNewSuppliers() throws Exception {
        Ctx c = admin();
        JsonNode result = upload("suppliers", "commit", c, "nome;nuit;email;telefone;morada;contacto",
                "Fornecedor Importado;416000021;;;Beira;Sr. Mussa");

        assertThat(result.get("created").asInt()).isEqualTo(1);
        String suppliers = mockMvc.perform(auth(get("/api/purchases/suppliers").param("companyId", c.companyId()), c))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(suppliers).contains("416000021");
    }

    @Test
    @DisplayName("IMP-06: com uma linha em erro a confirmação é recusada e nada é gravado")
    void commitWithAnyErrorWritesNothing() throws Exception {
        Ctx c = admin();
        String body = uploadRequest("clients", "commit", c, CLIENT_HEADER,
                        "Importado Ok;416000031;;;;", "Importado Mau;12345;;;;")
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(body).contains("nada foi gravado");
        assertThat(clientTaxIds(c)).doesNotContain("416000031");
    }

    @Test
    @DisplayName("IMP-06: confirmar volta a validar — conflito surgido depois da pré-visualização recusa tudo")
    void commitRevalidatesInsteadOfTrustingThePreview() throws Exception {
        Ctx c = admin();
        String[] lines = {"IMP-REV-A;Artigo Rev A;10;IVA16;Geral;5600000000611", "IMP-REV-B;Artigo Rev B;20;IVA16;Geral;"};
        JsonNode preview = upload("products", "preview", c, PRODUCT_HEADER, lines);
        assertThat(preview.toString()).doesNotContain("ERRO");

        mockMvc.perform(auth(post("/api/comercial/products"), c).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"IMP-REV-OUTRO\",\"name\":\"Outro\",\"unitPrice\":5,\"unitsPerBox\":1,"
                                + "\"barcode\":\"5600000000611\",\"stockTracked\":true}"))
                .andExpect(status().isOk());

        String body = uploadRequest("products", "commit", c, PRODUCT_HEADER, lines)
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("nada foi gravado");
        assertThat(productSkus(c)).doesNotContain("IMP-REV-A", "IMP-REV-B");
    }

    @Test
    @DisplayName("IMP-06: confirmar cria as categorias novas e os artigos na mesma operação")
    void commitCreatesNewCategoriesAndProducts() throws Exception {
        Ctx c = admin();
        JsonNode result = upload("products", "commit", c, PRODUCT_HEADER,
                "IMP-CAT-A;Artigo Cat A;10;IVA16;Congelados Importados;", "IMP-CAT-B;Artigo Cat B;12;IVA16;congelados importados;");

        assertThat(result.get("created").asInt()).isEqualTo(2);
        assertThat(productSkus(c)).contains("IMP-CAT-A", "IMP-CAT-B");
        String categories = mockMvc.perform(auth(get("/api/product-categories"), c))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(categories).containsOnlyOnce("Congelados Importados");
    }

    @Test
    @DisplayName("IMP-06: falha a meio da escrita desfaz tudo o que a confirmação já tinha gravado")
    void failureDuringWriteRollsBackEverything() throws Exception {
        Ctx c = admin();
        // A categoria "Doces Importados" é criada primeiro; a seguinte colide no código com uma categoria da empresa.
        mockMvc.perform(auth(post("/api/product-categories"), c).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"IMPCOLIDE\",\"name\":\"Outra Categoria\"}"))
                .andExpect(status().isOk());

        uploadRequest("products", "commit", c, PRODUCT_HEADER,
                        "IMP-TX-A;Artigo Tx A;10;IVA16;Doces Importados;", "IMP-TX-B;Artigo Tx B;10;IVA16;Impcolide;")
                .andExpect(status().isBadRequest());

        assertThat(productSkus(c)).doesNotContain("IMP-TX-A", "IMP-TX-B");
        String categories = mockMvc.perform(auth(get("/api/product-categories"), c))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(categories).doesNotContain("Doces Importados");
    }

    // ---- IMP-07: tudo na empresa activa; a outra empresa não vê nem é alterada (§3.3) ----------------

    @Test
    @DisplayName("IMP-07: clientes, fornecedores, artigos e categorias importados ficam só na empresa activa")
    void commitWritesOnlyIntoTheActiveCompany() throws Exception {
        Ctx active = adminIn(1);
        Ctx other = adminIn(0);
        assertThat(active.companyId()).isNotEqualTo(other.companyId());

        upload("clients", "commit", active, CLIENT_HEADER, "Importado Tenant;416000041;;;;");
        upload("suppliers", "commit", active, "nome;nuit;email;telefone;morada;contacto",
                "Fornecedor Tenant;416000042;;;;");
        upload("products", "commit", active, PRODUCT_HEADER, "IMP-TEN-A;Artigo Tenant;10;IVA16;Tenant Importados;");

        assertThat(clientTaxIds(active)).contains("416000041");
        assertThat(clientTaxIds(other)).doesNotContain("416000041");
        assertThat(supplierTaxIds(active)).contains("416000042");
        assertThat(supplierTaxIds(other)).doesNotContain("416000042");
        assertThat(productSkus(active)).contains("IMP-TEN-A");
        assertThat(productSkus(other)).doesNotContain("IMP-TEN-A");
        assertThat(categoryNames(active)).contains("Tenant Importados");
        assertThat(categoryNames(other)).doesNotContain("Tenant Importados");
    }

    @Test
    @DisplayName("IMP-07: na outra empresa o mesmo ficheiro não é JÁ EXISTE — cliente ASSOCIADO, fornecedor NOVO")
    void otherCompanyClassifiesAgainstItsOwnData() throws Exception {
        Ctx active = adminIn(1);
        Ctx other = adminIn(0);
        upload("clients", "commit", active, CLIENT_HEADER, "Importado Assoc;416000051;;;;");
        upload("suppliers", "commit", active, "nome;nuit;email;telefone;morada;contacto",
                "Fornecedor Assoc;416000052;;;;");

        JsonNode clients = upload("clients", "preview", other, CLIENT_HEADER, "Importado Assoc;416000051;;;;");
        assertThat(clients.get("rows").get(0).get("status").asText()).isEqualTo("ASSOCIADO");
        JsonNode suppliers = upload("suppliers", "preview", other, "nome;nuit;email;telefone;morada;contacto",
                "Fornecedor Assoc;416000052;;;;");
        assertThat(suppliers.get("rows").get(0).get("status").asText()).isEqualTo("NOVO");

        JsonNode associated = upload("clients", "commit", other, CLIENT_HEADER, "Importado Assoc;416000051;;;;");
        assertThat(associated.get("associated").asInt()).isEqualTo(1);
        assertThat(clientTaxIds(other)).contains("416000051");
        assertThat(clientTaxIds(active)).contains("416000051");
    }

    private JsonNode upload(String entity, String action, Ctx c, String header, String... lines) throws Exception {
        return om.readTree(uploadRequest(entity, action, c, header, lines)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private org.springframework.test.web.servlet.ResultActions uploadRequest(
            String entity, String action, Ctx c, String header, String... lines) throws Exception {
        byte[] csv = (header + "\r\n" + String.join("\r\n", lines) + "\r\n").getBytes(StandardCharsets.UTF_8);
        return mockMvc.perform(auth(post("/api/data-import/" + entity + "/" + action), c)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(csv));
    }

    private String clientTaxIds(Ctx c) throws Exception {
        return mockMvc.perform(auth(get("/api/comercial/clients"), c))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String productSkus(Ctx c) throws Exception {
        return mockMvc.perform(auth(get("/api/comercial/products"), c))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String supplierTaxIds(Ctx c) throws Exception {
        return mockMvc.perform(auth(get("/api/purchases/suppliers").param("companyId", c.companyId()), c))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String categoryNames(Ctx c) throws Exception {
        return mockMvc.perform(auth(get("/api/product-categories"), c))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
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
        String text = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        assertThat(text).as("apenas a linha de cabeçalho").isEqualTo(header + "\r\n");
    }

    private Ctx admin() throws Exception {
        return adminIn(0);
    }

    /** A Ana é ADMIN nas duas empresas do arranque; {@code index} escolhe a empresa activa. */
    private Ctx adminIn(int index) throws Exception {
        JsonNode login = om.readTree(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        return new Ctx(login.get("token").asText(), login.get("companies").get(index).get("id").asText());
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req, Ctx c) {
        return req.header("Authorization", "Bearer " + c.token()).header("X-Company-Id", c.companyId());
    }
}
