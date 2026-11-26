package mz.multicore.erp.gui;

import mz.multicore.erp.gui.ConfigDocumentTemplateOperations.TemplateForm;
import mz.multicore.erp.modules.documents.dto.DocumentBankAccountDTO;
import mz.multicore.erp.modules.documents.dto.DocumentTemplateDTO;
import mz.multicore.erp.modules.documents.model.DocumentLogoPosition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** DT-UI-01 — Configurações › Documentos com cor, logótipo, notas, bancos, QR e pré-visualização (MODELOS_DOCUMENTO_SPEC §3.4). */
@DisplayName("DT-UI-01: ecrã Configurações › Documentos com o modelo de documento")
class DocumentTemplateUiHarnessTest {

    private static Path gui(String file) {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui").resolve(file);
        return Files.exists(path) ? path : Path.of("src/main/java/mz/multicore/erp/gui").resolve(file);
    }

    private static Path client() {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/desktop/client/DocumentConfigApiClient.java");
        return Files.exists(path) ? path : Path.of("src/main/java/mz/multicore/erp/desktop/client/DocumentConfigApiClient.java");
    }

    @Test
    @DisplayName("O formulário tem cor com amostra, logótipo, notas, 3 contas bancárias e QR")
    void formHasAllTemplateOptions() {
        TemplateForm form = ConfigDocumentTemplateOperations.newForm();
        assertNotNull(form.colorField());
        assertNotNull(form.swatch());
        assertEquals(3, form.logoCombo().getItemCount(), "LEFT, CENTER e RIGHT");
        assertEquals(DocumentLogoPosition.LEFT, form.logoCombo().getSelectedItem(), "omissão LEFT (§3.1)");
        assertNotNull(form.notesArea());
        assertEquals(3, form.banksModel().getRowCount(), "até 3 contas (§3.1)");
        assertEquals(3, form.banksModel().getColumnCount(), "banco, titular, NIB/IBAN");
        assertTrue(form.qrBox().isSelected(), "QR por omissão quando existir");
    }

    @Test
    @DisplayName("Aplicar e ler o modelo devolve o mesmo DTO, ignorando linhas de banco vazias")
    void applyAndReadRoundTrip() {
        TemplateForm form = ConfigDocumentTemplateOperations.newForm();
        DocumentTemplateDTO dto = new DocumentTemplateDTO("#1F4E79", DocumentLogoPosition.RIGHT,
                "Pagamento a 30 dias.",
                List.of(new DocumentBankAccountDTO("BCI", "Loja Central, Lda", "000800001234567890123")),
                false);

        ConfigDocumentTemplateOperations.apply(form, dto);

        assertEquals("#1F4E79", form.colorField().getText());
        assertEquals(DocumentLogoPosition.RIGHT, form.logoCombo().getSelectedItem());
        assertEquals(dto, ConfigDocumentTemplateOperations.toDto(form));
    }

    @Test
    @DisplayName("Campos vazios viram omissões: sem cor própria, sem notas, sem contas")
    void blankFieldsBecomeDefaults() {
        TemplateForm form = ConfigDocumentTemplateOperations.newForm();
        form.colorField().setText("  ");
        form.notesArea().setText("   ");
        form.banksModel().setValueAt("  ", 0, 0);

        DocumentTemplateDTO dto = ConfigDocumentTemplateOperations.toDto(form);

        assertNull(dto.accentColor());
        assertNull(dto.notes());
        assertTrue(dto.bankAccounts().isEmpty());
        assertEquals(DocumentLogoPosition.LEFT, dto.logoPosition());
    }

    @Test
    @DisplayName("A amostra mostra a cor escrita; cor inválida não pinta amostra")
    void swatchFollowsTypedColour() {
        assertEquals(Color.decode("#1F4E79"), ConfigDocumentTemplateOperations.swatchColor("#1f4e79"));
        assertNull(ConfigDocumentTemplateOperations.swatchColor("azul"));
        assertNull(ConfigDocumentTemplateOperations.swatchColor("#12345"));
        assertNull(ConfigDocumentTemplateOperations.swatchColor(null));
    }

    @Test
    @DisplayName("O separador de documentos inclui o modelo e a pré-visualização vem do backend")
    void screenWiresTemplateAndBackendPreview() throws IOException {
        String columns = Files.readString(gui("ConfigDocumentColumnsPanel.java"));
        assertTrue(columns.contains("ConfigDocumentTemplatePanel"), "o ecrã de colunas acolhe o modelo (§3.4)");

        String panel = Files.readString(gui("ConfigDocumentTemplatePanel.java"));
        for (String label : List.of("Cor de destaque", "Posição do logótipo", "Notas", "Dados bancários",
                "QR", "Pré-visualizar", "Guardar modelo")) {
            assertTrue(panel.contains(label), "falta \"" + label + "\" no ecrã do modelo");
        }
        assertTrue(panel.contains("UIHelper.icon("), "ícones via UIHelper.icon");
        assertFalse(panel.contains("JOptionPane"), "sem JOptionPane");

        String ops = Files.readString(gui("ConfigDocumentTemplateOperations.java"));
        assertTrue(ops.contains("PrintPreviewDialog.show"), "PDF de exemplo no visualizador canónico");
        assertFalse(ops.contains("JOptionPane"), "sem JOptionPane");

        String api = Files.readString(client());
        assertTrue(api.contains("/api/documents/template/preview"), "pré-visualização gerada pelo backend");
        assertTrue(api.contains("\"/api/documents/template?companyId="), "GET/PUT do modelo");
    }
}
