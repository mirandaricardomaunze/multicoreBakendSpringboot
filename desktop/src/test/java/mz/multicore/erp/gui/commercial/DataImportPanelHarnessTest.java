package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.ConfigPanel;
import mz.multicore.erp.gui.dataimport.DataImportPanel;
import mz.multicore.erp.gui.dataimport.ImportEntityOption;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowStatus;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Ecrã de importação e formulários que ela torna coerentes
 * (docs/IMPORTACAO_DADOS_HARNESS.md IMP-UI-01, IMP-UI-02).
 */
class DataImportPanelHarnessTest {

    private static final String VALID_NUIT = "400123452";

    @Test
    void impUi02_formularioDeCliente_aceitaEmailVazioEEnviaNull() {
        ClientFormFields fields = new ClientFormFields(null);
        fields.nameField.setText("Mercearia Sol");
        fields.taxIdField.setText(VALID_NUIT);
        fields.emailField.setText("   ");

        assertTrue(fields.validateAll(), "cliente sem email tem de ser aceite");
        assertNull(fields.email(), "email vazio segue como null para o backend");
        fields.emailField.setText("");
        assertTrue(fields.validateAll(), "campo email totalmente vazio também é aceite");
    }

    @Test
    void impUi02_formularioDeCliente_emailPreenchidoContinuaValidado() {
        ClientFormFields fields = new ClientFormFields(null);
        fields.nameField.setText("Mercearia Sol");
        fields.taxIdField.setText(VALID_NUIT);

        fields.emailField.setText("sem-arroba");
        assertFalse(fields.validateAll(), "email mal formado continua a ser recusado");

        fields.emailField.setText("  geral@sol.co.mz ");
        assertTrue(fields.validateAll());
        assertEquals("geral@sol.co.mz", fields.email());
    }

    @Test
    void impUi02_edicaoDeClienteSemEmail_abreComCampoVazio() {
        ClientDTO existing = new ClientDTO(7L, "Mercearia Sol", VALID_NUIT, null, null, 0, BigDecimal.ZERO);

        ClientFormFields fields = new ClientFormFields(existing);

        assertEquals("", fields.emailField.getText());
        assertTrue(fields.validateAll());
        assertNull(fields.email());
    }

    @Test
    void impUi02_nomeENuitContinuamObrigatorios() {
        ClientFormFields fields = new ClientFormFields(null);
        assertFalse(fields.validateAll(), "sem nome nem NUIT o formulário é recusado");
    }

    // ── IMP-UI-01 ────────────────────────────────────────────────────────────────────────────

    private static final byte[] CSV = "nome;nuit\nSol;400123452\n".getBytes(StandardCharsets.UTF_8);

    @Test
    void impUi01_configuracoesTemEcraImportarDados() throws Exception {
        onEdt(() -> {
            ConfigPanel config = new ConfigPanel(mock(UserApiClient.class), mock(AuditApiClient.class),
                    mock(BackupApiClient.class), mock(DocumentConfigApiClient.class), mock(SupportApiClient.class),
                    mock(MySubscriptionApiClient.class), mock(SystemMonitoringApiClient.class), null,
                    mock(DataImportApiClient.class));

            JTabbedPane tabs = firstTabbedPane(config);
            int index = tabs.indexOfTab("Importar dados");
            assertTrue(index >= 0, "Configurações têm o separador \"Importar dados\"");
            assertInstanceOf(DataImportPanel.class, tabs.getComponentAt(index));
            assertNotNull(tabs.getIconAt(index), "ícone pelo UIHelper.icon");
            assertEquals("Log de Auditoria Geral", tabs.getTitleAt(0), "as outras abas mantêm a ordem");
        });
    }

    @Test
    void impUi01_ecraOfereceEntidadesModeloEFicheiro() throws Exception {
        onEdt(() -> {
            DataImportPanel panel = new DataImportPanel(mock(DataImportApiClient.class));

            JComboBox<ImportEntityOption> entity = panel.entityCombo();
            assertEquals(3, entity.getItemCount());
            assertEquals("Clientes", entity.getItemAt(0).toString());
            assertEquals("Fornecedores", entity.getItemAt(1).toString());
            assertEquals("Artigos", entity.getItemAt(2).toString());
            assertEquals("clients", ImportEntityOption.CLIENTS.path());
            assertEquals("suppliers", ImportEntityOption.SUPPLIERS.path());
            assertEquals("products", ImportEntityOption.PRODUCTS.path());
            assertEquals("modelo-artigos.csv", ImportEntityOption.PRODUCTS.templateFileName());

            for (AbstractButton button : List.of(panel.templateButton(), panel.chooseFileButton(), panel.confirmButton())) {
                assertNotNull(button.getIcon(), button.getText() + " tem ícone (UIHelper.icon, nunca emoji)");
            }
            assertEquals(List.of("Linha", "Estado", "Coluna", "Mensagem"), columnNames(panel.previewTableModel()));
        });
    }

    @Test
    void impUi01_confirmarSoFicaActivoSemErros() throws Exception {
        onEdt(() -> {
            DataImportPanel panel = new DataImportPanel(mock(DataImportApiClient.class));
            assertFalse(panel.confirmButton().isEnabled(), "sem pré-visualização não há o que confirmar");

            panel.showPreview("clientes.csv", CSV, new ImportPreviewDTO(List.of(
                    ImportRowDTO.of(2, ImportRowStatus.NOVO),
                    ImportRowDTO.error(3, "nuit", "NUIT inválido.")), List.of()));
            assertFalse(panel.confirmButton().isEnabled(), "com erros o Confirmar fica inactivo");
            DefaultTableModel model = panel.previewTableModel();
            assertEquals(2, model.getRowCount());
            assertEquals(List.of(3, "ERRO", "nuit", "NUIT inválido."), rowValues(model, 1));

            panel.showPreview("clientes.csv", CSV, new ImportPreviewDTO(List.of(
                    ImportRowDTO.of(2, ImportRowStatus.NOVO),
                    ImportRowDTO.of(3, ImportRowStatus.JA_EXISTE)), List.of("obs")));
            assertTrue(panel.confirmButton().isEnabled(), "sem erros o Confirmar fica activo");
            assertEquals("JÁ EXISTE", model.getValueAt(1, 1));
            assertTrue(panel.summaryLabel().getText().contains("obs"), "colunas a mais são anunciadas");

            panel.entityCombo().setSelectedItem(ImportEntityOption.SUPPLIERS);
            assertFalse(panel.confirmButton().isEnabled(), "mudar de entidade descarta a pré-visualização");
            assertEquals(0, model.getRowCount());
        });
    }

    @Test
    void impUi01_resumoMostraTotaisECategoriasACriar() throws Exception {
        onEdt(() -> {
            DataImportPanel panel = new DataImportPanel(mock(DataImportApiClient.class));
            panel.showPreview("artigos.csv", CSV, new ImportPreviewDTO(List.of(
                    ImportRowDTO.of(2, ImportRowStatus.NOVO),
                    ImportRowDTO.of(3, ImportRowStatus.NOVO),
                    ImportRowDTO.of(4, ImportRowStatus.JA_EXISTE)), List.of(), List.of("Bebidas")));

            String summary = panel.summaryLabel().getText();
            assertTrue(summary.contains("artigos.csv"), summary);
            assertTrue(summary.contains("2 novos"), summary);
            assertTrue(summary.contains("1 já existente"), summary);
            assertTrue(summary.contains("Bebidas"), summary);
        });
    }

    /**
     * Corre o corpo na EDT: os instaladores do UIHelper.styleTable reagem aos eventos do modelo na EDT,
     * e mexer na tabela fora dela dá corridas intermitentes no RowSorter.
     */
    private static void onEdt(Runnable body) throws Exception {
        try {
            SwingUtilities.invokeAndWait(body);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Error error) throw error;
            if (e.getCause() instanceof RuntimeException runtime) throw runtime;
            throw e;
        }
    }

    private static JTabbedPane firstTabbedPane(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JTabbedPane tabs) return tabs;
        }
        throw new AssertionError("sem JTabbedPane");
    }

    private static List<String> columnNames(DefaultTableModel model) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < model.getColumnCount(); i++) names.add(model.getColumnName(i));
        return names;
    }

    private static List<Object> rowValues(DefaultTableModel model, int row) {
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < model.getColumnCount(); i++) values.add(model.getValueAt(row, i));
        return values;
    }
}
