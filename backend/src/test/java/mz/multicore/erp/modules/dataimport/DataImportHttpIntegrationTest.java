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
        JsonNode login = om.readTree(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        return new Ctx(login.get("token").asText(), login.get("companies").get(0).get("id").asText());
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req, Ctx c) {
        return req.header("Authorization", "Bearer " + c.token()).header("X-Company-Id", c.companyId());
    }
}
