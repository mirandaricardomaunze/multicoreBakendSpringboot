package mz.multicore.erp.modules.documents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Harness DT-06 (docs/MODELOS_DOCUMENTO_SPEC.md §3.3, §3.4): alterar o modelo de documento é só do ADMIN e
 * fica auditado como {@code DOCUMENT_TEMPLATE_CHANGE}; a pré-visualização devolve uma factura de exemplo em
 * PDF com o modelo pedido, sem o gravar nem auditar.
 * Utilizadores semeados: ana=ADMIN, joao=MANAGER, maria=EMPLOYEE. Password "password".
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:document-template-http;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentTemplateHttpIntegrationTest {

    private static final String TEMPLATE = "/api/documents/template";
    private static final String PREVIEW = "/api/documents/template/preview";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    private record Session(String token, String companyId) {}

    @Test
    void dt06_adminGravaOModeloComAuditoriaEOsDocumentosLeemNo() throws Exception {
        Session admin = admin();
        String notes = "Condições " + tag();

        JsonNode saved = json(send(put(TEMPLATE + query(admin)), admin, template("#1F4E79", "RIGHT", notes, true)));
        assertThat(saved.get("accentColor").asText()).isEqualTo("#1F4E79");
        assertThat(saved.get("logoPosition").asText()).isEqualTo("RIGHT");
        assertThat(saved.get("notes").asText()).isEqualTo(notes);
        assertThat(saved.get("bankAccounts").size()).isEqualTo(1);
        assertThat(saved.get("showQrCode").asBoolean()).isFalse();

        JsonNode read = getJson(TEMPLATE + query(admin), admin);
        assertThat(read.get("notes").asText()).isEqualTo(notes);
        assertThat(read.get("bankAccounts").get(0).get("accountNumber").asText()).isEqualTo("MZ59000100000012345678901");

        assertThat(templateChanges(admin, notes)).isEqualTo(1);
    }

    @Test
    void dt06_regrasDoModeloRecusadasComMensagemESemAuditoria() throws Exception {
        Session admin = admin();
        String notes = "Recusado " + tag();

        send(put(TEMPLATE + query(admin)), admin, template("#FFFF00", "LEFT", notes, false))
                .andExpect(status().isBadRequest());
        send(post(PREVIEW + query(admin)), admin, template("#FFFF00", "LEFT", notes, false))
                .andExpect(status().isBadRequest());
        assertThat(templateChanges(admin, notes)).isZero();
    }

    @Test
    void dt06_soAdminAlteraOuPreVisualiza() throws Exception {
        Session admin = admin();
        String notes = "Intruso " + tag();

        for (String user : new String[]{"joao", "maria"}) {
            Session other = login(user, admin.companyId());
            send(put(TEMPLATE + query(other)), other, template("#1F4E79", "LEFT", notes, false))
                    .andExpect(status().isForbidden());
            send(post(PREVIEW + query(other)), other, template("#1F4E79", "LEFT", notes, false))
                    .andExpect(status().isForbidden());
        }
        assertThat(getJson(TEMPLATE + query(admin), admin).path("notes").asText()).isNotEqualTo(notes);
        assertThat(templateChanges(admin, notes)).isZero();
    }

    @Test
    void dt06_preVisualizacaoGeraFacturaDeExemploSemGravar() throws Exception {
        Session admin = admin();
        String before = getJson(TEMPLATE + query(admin), admin).toString();
        String notes = "Exemplo " + tag();

        byte[] pdf = send(post(PREVIEW + query(admin)), admin, template("#1F4E79", "CENTER", notes, true))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();

        String text = text(pdf);
        assertThat(text).contains("FATURA", "EXEMPLO", notes, "MZ59000100000012345678901", "Consumidor Final");
        assertThat(getJson(TEMPLATE + query(admin), admin).toString()).isEqualTo(before);
        assertThat(templateChanges(admin, notes)).isZero();
    }

    // --- cenário ---

    private static String tag() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String query(Session s) {
        return "?companyId=" + s.companyId();
    }

    private static String template(String color, String logo, String notes, boolean withBank) {
        String bank = withBank
                ? "{\"bankName\":\"BCI\",\"holder\":\"Empresa Lda\",\"accountNumber\":\"MZ59000100000012345678901\"}"
                : "";
        return """
                {"accentColor":"%s","logoPosition":"%s","notes":"%s","bankAccounts":[%s],"showQrCode":false}"""
                .formatted(color, logo, notes, bank);
    }

    private long templateChanges(Session s, String marker) throws Exception {
        long count = 0;
        for (JsonNode log : getJson("/api/audit/logs", s)) {
            if ("DOCUMENT_TEMPLATE_CHANGE".equals(log.get("action").asText())
                    && log.get("details").asText().contains(marker)) count++;
        }
        return count;
    }

    private static String text(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            StringBuilder text = new StringBuilder();
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page)).append('\n');
            }
            return text.toString().replaceAll("\\s+", " ");
        } finally {
            reader.close();
        }
    }

    // --- HTTP ---

    /** ADMIN na empresa do gestor semeado, onde os outros perfis também entram. */
    private Session admin() throws Exception {
        return login("ana", login("joao", null).companyId());
    }

    private Session login(String username, String companyId) throws Exception {
        JsonNode login = read(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"password\"}".formatted(username)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String active = companyId != null ? companyId : login.get("companies").get(0).get("id").asText();
        return new Session(login.get("token").asText(), active);
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req, Session s) {
        return req.header("Authorization", "Bearer " + s.token()).header("X-Company-Id", s.companyId());
    }

    private JsonNode json(ResultActions result) throws Exception {
        return read(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
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
