package mz.multicore.erp.gui.accounting;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.ConfigPanel;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.modules.accounting.dto.CostCenterDTO;
import mz.multicore.erp.modules.accounting.dto.CostCenterResultDTO;
import mz.multicore.erp.modules.accounting.dto.CostCenterResultLineDTO;
import mz.multicore.erp.modules.accounting.model.CostCenterType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Harness de UI dos centros de custo (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.1–§3.3).
 * CC-UI-01: Configurações › Centros de Custo lista os centros da empresa activa, cria/edita os manuais,
 * apaga e escolhe o centro por omissão; centros de armazém só de leitura; gerir só ADMIN/MANAGER.
 * CC-UI-02: lançamento manual e despesa pedem o centro. CC-UI-03: Contabilidade › Resultados por centro.
 */
class CostCenterUiHarnessTest {

    private static final List<CostCenterDTO> CENTERS = List.of(
            new CostCenterDTO(1L, "ADM", "Administração", CostCenterType.MANUAL, null, true, true),
            new CostCenterDTO(2L, "A01", "Loja Central", CostCenterType.WAREHOUSE, 7L, true, false),
            new CostCenterDTO(3L, "FROTA", "Frota", CostCenterType.MANUAL, null, false, false));

    @AfterEach
    void clearSession() throws Exception {
        onEdt(CurrentUserContext::clear);
    }

    @Test
    @DisplayName("CC-UI-01: Configurações ganham o separador Centros de Custo no fim, sem mudar a ordem das outras abas")
    void ccUi01_configuracoesTemSeparadorDeCentros() throws Exception {
        onEdt(() -> {
            ConfigPanel config = new ConfigPanel(mock(UserApiClient.class), mock(AuditApiClient.class),
                    mock(BackupApiClient.class), mock(DocumentConfigApiClient.class), mock(SupportApiClient.class),
                    mock(MySubscriptionApiClient.class), mock(SystemMonitoringApiClient.class), null, null,
                    mock(ExchangeRateApiClient.class), mock(AccountingApiClient.class));

            JTabbedPane tabs = firstOf(config, JTabbedPane.class);
            int index = tabs.indexOfTab("Centros de Custo");
            assertThat(index).as("separador \"Centros de Custo\"").isEqualTo(tabs.getTabCount() - 1);
            assertThat(tabs.getComponentAt(index)).isInstanceOf(CostCentersPanel.class);
            assertThat(tabs.getIconAt(index)).as("ícone pelo UIHelper.icon").isNotNull();
            assertThat(tabs.getTitleAt(0)).isEqualTo("Log de Auditoria Geral");
            assertThat(tabs.indexOfTab("Câmbios e Moeda Base")).isEqualTo(index - 1);
        });
    }

    @Test
    @DisplayName("CC-UI-01: A tabela mostra código, nome, tipo, estado e o centro por omissão, dentro de um card")
    void ccUi01_tabelaDeCentros() throws Exception {
        onEdt(() -> {
            CostCentersPanel panel = new CostCentersPanel(mock(AccountingApiClient.class));
            panel.applyCenters(CENTERS);

            JTable table = panel.centersTable();
            assertThat(table.getColumnCount()).isEqualTo(5);
            assertThat(table.getColumnName(0)).isEqualTo("Código");
            assertThat(table.getColumnName(1)).isEqualTo("Nome");
            assertThat(table.getColumnName(2)).isEqualTo("Tipo");
            assertThat(table.getColumnName(3)).isEqualTo("Estado");
            assertThat(table.getColumnName(4)).isEqualTo("Por omissão");
            javax.swing.table.TableModel model = table.getModel();
            assertThat(model.getRowCount()).isEqualTo(3);
            assertThat(model.getValueAt(0, 0)).isEqualTo("ADM");
            assertThat(model.getValueAt(0, 2)).isEqualTo("Manual");
            assertThat(model.getValueAt(0, 4)).isEqualTo("Sim");
            assertThat(model.getValueAt(1, 2)).isEqualTo("Armazém");
            assertThat(model.getValueAt(1, 4)).isEqualTo("");
            assertThat(model.getValueAt(2, 3)).isEqualTo("Inactivo");
            assertThat(model.isCellEditable(0, 1)).as("editar só pelo diálogo").isFalse();
            assertThat(panel.defaultCenterLabel().getText()).contains("ADM").contains("Administração");
            assertThat(SwingUtilities.getAncestorOfClass(ModernPanel.class, table)).as("tabela num card").isNotNull();
        });
    }

    @Test
    @DisplayName("CC-UI-01: Centro de armazém não se edita nem apaga; o por omissão não se apaga nem se volta a escolher")
    void ccUi01_accoesSeguemOCentroSeleccionado() throws Exception {
        CostCentersPanel panel = loadedPanel("MANAGER");
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("gestor", "MANAGER");
            JTable table = panel.centersTable();

            assertThat(panel.newButton().isEnabled()).as("MANAGER gere centros").isTrue();
            assertThat(panel.editButton().isEnabled()).as("sem selecção").isFalse();

            table.setRowSelectionInterval(0, 0);
            assertThat(panel.editButton().isEnabled()).isTrue();
            assertThat(panel.deleteButton().isEnabled()).as("o por omissão não se apaga").isFalse();
            assertThat(panel.defaultButton().isEnabled()).as("já é o por omissão").isFalse();

            table.setRowSelectionInterval(1, 1);
            assertThat(panel.editButton().isEnabled()).as("centro de armazém acompanha o armazém").isFalse();
            assertThat(panel.deleteButton().isEnabled()).isFalse();
            assertThat(panel.defaultButton().isEnabled()).as("centro activo pode ser o por omissão").isTrue();

            table.setRowSelectionInterval(2, 2);
            assertThat(panel.editButton().isEnabled()).isTrue();
            assertThat(panel.deleteButton().isEnabled()).isTrue();
            assertThat(panel.defaultButton().isEnabled()).as("centro inactivo não é por omissão").isFalse();
        });
    }

    @Test
    @DisplayName("CC-UI-01: Gerir centros só para ADMIN e MANAGER (SPEC §3.1)")
    void ccUi01_soAdminEGestorGerem() throws Exception {
        CostCentersPanel panel = loadedPanel("EMPLOYEE");
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("caixa", "EMPLOYEE");
            panel.centersTable().setRowSelectionInterval(2, 2);

            assertThat(panel.newButton().isEnabled()).isFalse();
            assertThat(panel.editButton().isEnabled()).isFalse();
            assertThat(panel.deleteButton().isEnabled()).isFalse();
            assertThat(panel.defaultButton().isEnabled()).isFalse();
        });
    }

    @Test
    @DisplayName("CC-UI-01: Ícones pelo UIHelper.icon, diálogo canónico e endpoints de centros no cliente")
    void ccUi01_iconesDialogoECliente() throws Exception {
        String panel = Files.readString(gui("accounting", "CostCentersPanel.java"));
        String dialog = Files.readString(gui("accounting", "CostCenterFormDialog.java"));
        assertThat(panel).contains("UIHelper.icon(").contains("UIHelper.confirm(")
                .contains("accountingApiClient.setDefaultCostCenter(").contains("accountingApiClient.deleteCostCenter(")
                .doesNotContain("JOptionPane");
        assertThat(dialog).contains("new ModernFormDialog(").contains("setOnSaveAsync(")
                .contains("accountingApiClient.createCostCenter(").contains("accountingApiClient.updateCostCenter(")
                .doesNotContain("JOptionPane");
        Path client = Path.of("src", "main", "java", "mz", "multicore", "erp", "desktop", "client", "AccountingApiClient.java");
        if (!Files.exists(client)) client = Path.of("desktop").resolve(client);
        assertThat(Files.readString(client)).contains("/api/accounting/cost-centers");
    }

    @Test
    @DisplayName("CC-UI-02: O seletor de centro lista só os activos com o centro por omissão pré-seleccionado")
    void ccUi02_seletorPreSeleccionaOPorOmissao() throws Exception {
        onEdt(() -> {
            CostCenterCombo combo = new CostCenterCombo();
            assertThat(combo.selectedCostCenterId()).as("antes de carregar: por omissão do backend").isNull();

            combo.applyCenters(List.of(
                    new CostCenterDTO(2L, "A01", "Loja Central", CostCenterType.WAREHOUSE, 7L, true, false),
                    new CostCenterDTO(3L, "FROTA", "Frota", CostCenterType.MANUAL, null, false, false),
                    new CostCenterDTO(1L, "ADM", "Administração", CostCenterType.MANUAL, null, true, true)));

            assertThat(combo.getItemCount()).as("inactivo fica de fora").isEqualTo(2);
            assertThat(combo.selectedCostCenterId()).as("por omissão pré-seleccionado").isEqualTo(1L);
            combo.setSelectedIndex(0);
            assertThat(combo.selectedCostCenterId()).isEqualTo(2L);

            combo.applyCenters(List.of());
            assertThat(combo.getItemCount()).as("sem centros visíveis: fica a opção por omissão").isEqualTo(1);
            assertThat(combo.selectedCostCenterId()).isNull();
        });
    }

    @Test
    @DisplayName("CC-UI-02: O centro só segue nas partidas das classes 6 e 7")
    void ccUi02_centroSoNasClasses6e7() {
        assertThat(CostCenterCombo.centerForAccount("6201", 5L)).isEqualTo(5L);
        assertThat(CostCenterCombo.centerForAccount(" 7101", 5L)).isEqualTo(5L);
        assertThat(CostCenterCombo.centerForAccount("1101", 5L)).as("classes 1–5 recusam centro").isNull();
        assertThat(CostCenterCombo.centerForAccount("4421", 5L)).isNull();
        assertThat(CostCenterCombo.centerForAccount("6201", null)).isNull();
        assertThat(CostCenterCombo.centerForAccount(null, 5L)).isNull();
    }

    @Test
    @DisplayName("CC-UI-02: Lançamento manual e despesa pedem o centro e enviam-no no pedido")
    void ccUi02_lancamentoEDespesaPedemCentro() throws Exception {
        String journal = Files.readString(gui("accounting", "AccountingJournalTab.java"));
        assertThat(journal).contains("new CostCenterCombo()").contains("\"Centro de custo\"")
                .contains(".load(accountingApiClient)").contains("CostCenterCombo.centerForAccount(");
        Path expense = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "HrExpenseClaimDialog.java");
        if (!Files.exists(expense)) expense = Path.of("desktop").resolve(expense);
        assertThat(Files.readString(expense)).contains("new CostCenterCombo()").contains("\"Centro de custo:\"")
                .contains("selectedCostCenterId()");
        String combo = Files.readString(gui("accounting", "CostCenterCombo.java"));
        assertThat(combo).contains("accountingApiClient.listCostCenters()").doesNotContain("JOptionPane");
    }

    @Test
    @DisplayName("CC-UI-03: Contabilidade ganha a aba Resultados por centro no fim, com ícone")
    void ccUi03_contabilidadeTemAbaDeResultadosPorCentro() throws Exception {
        onEdt(() -> {
            AccountingPanel accounting = new AccountingPanel(mock(AccountingApiClient.class), mock(PrintApiClient.class));
            JTabbedPane tabs = firstOf(accounting, JTabbedPane.class);
            int index = tabs.indexOfTab("Resultados por centro");
            assertThat(index).as("aba \"Resultados por centro\"").isEqualTo(tabs.getTabCount() - 1);
            assertThat(tabs.getComponentAt(index)).isInstanceOf(AccountingCostCenterResultTab.class);
            assertThat(tabs.getIconAt(index)).as("ícone pelo UIHelper.icon").isNotNull();
            assertThat(tabs.indexOfTab("Demonstrações")).isEqualTo(index - 1);
        });
    }

    @Test
    @DisplayName("CC-UI-03: A tabela mostra uma linha por centro, \"Sem centro\" e a linha Total igual à demonstração")
    void ccUi03_tabelaDeResultadosPorCentro() throws Exception {
        onEdt(() -> {
            AccountingCostCenterResultTab tab = new AccountingCostCenterResultTab(mock(AccountingApiClient.class),
                    mock(PrintApiClient.class), new mz.multicore.erp.gui.components.InlineFeedbackPanel());
            tab.applyResult(new CostCenterResultDTO(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), List.of(
                    new CostCenterResultLineDTO(2L, "A01", "Loja Central", money("1000.00"), money("600.00"), money("400.00")),
                    new CostCenterResultLineDTO(1L, "ADM", "Administração", money("0.00"), money("150.00"), money("-150.00")),
                    new CostCenterResultLineDTO(null, null, null, money("50.00"), money("0.00"), money("50.00"))),
                    money("1050.00"), money("750.00"), money("300.00")));

            JTable table = tab.resultTable();
            assertThat(table.getColumnCount()).isEqualTo(5);
            assertThat(table.getColumnName(0)).isEqualTo("Código");
            assertThat(table.getColumnName(1)).isEqualTo("Centro de custo");
            assertThat(table.getColumnName(2)).isEqualTo("Proveitos");
            assertThat(table.getColumnName(3)).isEqualTo("Custos");
            assertThat(table.getColumnName(4)).isEqualTo("Resultado");
            javax.swing.table.TableModel model = table.getModel();
            assertThat(model.getRowCount()).as("três linhas e o Total").isEqualTo(4);
            assertThat(model.getValueAt(0, 0)).isEqualTo("A01");
            assertThat(model.getValueAt(0, 4)).isEqualTo(money("400.00"));
            assertThat(model.getValueAt(2, 1)).isEqualTo("Sem centro");
            assertThat(model.getValueAt(3, 1)).isEqualTo("Total");
            assertThat(model.getValueAt(3, 2)).isEqualTo(money("1050.00"));
            assertThat(model.getValueAt(3, 3)).isEqualTo(money("750.00"));
            assertThat(model.getValueAt(3, 4)).as("igual à demonstração de resultados").isEqualTo(money("300.00"));
            assertThat(model.isCellEditable(0, 1)).isFalse();
            assertThat(tab.summaryLabel().getText()).contains("01/11/2026").contains("30/11/2026").contains("300,00");
            assertThat(SwingUtilities.getAncestorOfClass(ModernPanel.class, table)).as("tabela num card").isNotNull();
        });
    }

    @Test
    @DisplayName("CC-UI-03: Período, cliente do relatório e exportação pelos meios normais (PDF/Excel/CSV)")
    void ccUi03_periodoEExportacao() throws Exception {
        String tab = Files.readString(gui("accounting", "AccountingCostCenterResultTab.java"));
        assertThat(tab).contains("new DateField()").contains("accountingApiClient.getCostCenterResult(")
                .contains("UIHelper.createExportButton(").contains("TableExportAction.export(")
                .contains("UIHelper.tableCardTop(").contains("UIHelper.icon(").doesNotContain("JOptionPane");
        Path client = Path.of("src", "main", "java", "mz", "multicore", "erp", "desktop", "client", "AccountingApiClient.java");
        if (!Files.exists(client)) client = Path.of("desktop").resolve(client);
        assertThat(Files.readString(client)).contains("/api/accounting/cost-center-result");
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    /** Builds and fills the panel in one EDT turn; the table pagination applies its rows in the next one. */
    private static CostCentersPanel loadedPanel(String role) throws Exception {
        AtomicReference<CostCentersPanel> panel = new AtomicReference<>();
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("utilizador", role);
            panel.set(new CostCentersPanel(mock(AccountingApiClient.class)));
            panel.get().applyCenters(CENTERS);
        });
        return panel.get();
    }

    private static Path gui(String pkg, String file) {
        Path nested = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", pkg, file);
        return Files.exists(nested) ? nested : Path.of("desktop").resolve(nested);
    }

    private static <T extends Component> T firstOf(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container container) {
                T found = firstOf(container, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void onEdt(Runnable action) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                failure.set(t);
            }
        });
        if (failure.get() instanceof Error error) throw error;
        if (failure.get() instanceof Exception exception) throw exception;
    }
}
