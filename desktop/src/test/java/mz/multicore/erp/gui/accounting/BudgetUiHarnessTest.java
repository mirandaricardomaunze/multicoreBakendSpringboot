package mz.multicore.erp.gui.accounting;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.AccountingApiClient;
import mz.multicore.erp.desktop.client.ApprovalApiClient;
import mz.multicore.erp.desktop.client.HRApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.MySubscriptionApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.gui.NotificationFeed;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.StatusTone;
import mz.multicore.erp.modules.accounting.dto.AccountDTO;
import mz.multicore.erp.modules.accounting.dto.BudgetAlertDTO;
import mz.multicore.erp.modules.accounting.dto.BudgetDTO;
import mz.multicore.erp.modules.accounting.dto.BudgetLineDTO;
import mz.multicore.erp.modules.accounting.dto.BudgetVarianceDTO;
import mz.multicore.erp.modules.accounting.dto.BudgetVarianceLineDTO;
import mz.multicore.erp.modules.accounting.dto.BudgetVarianceQuery;
import mz.multicore.erp.modules.accounting.dto.CostCenterDTO;
import mz.multicore.erp.modules.accounting.dto.SaveBudgetRequest;
import mz.multicore.erp.modules.accounting.model.AccountClass;
import mz.multicore.erp.modules.accounting.model.AccountNature;
import mz.multicore.erp.modules.accounting.model.BudgetVarianceDirection;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Harness de UI do orçamento (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.3, §4.4).
 * OR-UI-01: Contabilidade › Orçamento — grelha anual conta × centro × 12 meses, copiar do ano anterior, gravar.
 * OR-UI-02: Contabilidade › Real vs Orçado com desvios em {@code StatusTone}, alertas no sino e aviso em toast
 * (o relay do cabeçalho X-Operation-Warning está em {@code OperationWarningRelayTest}).
 */
class BudgetUiHarnessTest {

    private static final BudgetDTO BUDGET = new BudgetDTO(9L, 2026, List.of(
            new BudgetLineDTO("6221", "Rendas", 1L, "ADM", "Administração", 1, money("1000.00")),
            new BudgetLineDTO("6221", "Rendas", 1L, "ADM", "Administração", 3, money("0.00")),
            new BudgetLineDTO("7111", "Vendas", 2L, "A01", "Loja Central", 12, money("5000.00")),
            new BudgetLineDTO("6221", "Rendas", 2L, "A01", "Loja Central", 2, money("400.00"))),
            money("1400.00"), money("5000.00"));

    @AfterEach
    void clearSession() throws Exception {
        onEdt(CurrentUserContext::clear);
    }

    @Test
    @DisplayName("OR-UI-01: Contabilidade ganha a aba Orçamento a seguir ao Razão, com ícone, sem mexer nas últimas")
    void orUi01_contabilidadeTemAbaDeOrcamento() throws Exception {
        onEdt(() -> {
            AccountingPanel accounting = new AccountingPanel(mock(AccountingApiClient.class), mock(PrintApiClient.class));
            JTabbedPane tabs = firstOf(accounting, JTabbedPane.class);
            int index = tabs.indexOfTab("Orçamento");
            assertThat(index).as("aba \"Orçamento\" a seguir ao Razão").isEqualTo(tabs.indexOfTab("Razão") + 1);
            assertThat(tabs.getComponentAt(index)).isInstanceOf(AccountingBudgetTab.class);
            assertThat(tabs.getIconAt(index)).as("ícone pelo UIHelper.icon").isNotNull();
            assertThat(tabs.indexOfTab("Resultados por centro")).isEqualTo(tabs.getTabCount() - 1);
        });
    }

    @Test
    @DisplayName("OR-UI-01: A grelha tem uma linha por conta × centro com os 12 meses e o Total, dentro de um card")
    void orUi01_grelhaContaCentroMeses() throws Exception {
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("gestor", "MANAGER");
            AccountingBudgetTab tab = newTab();
            tab.applyBudget(BUDGET);

            JTable table = tab.budgetTable();
            assertThat(table.getColumnCount()).isEqualTo(15);
            assertThat(table.getColumnName(0)).isEqualTo("Conta");
            assertThat(table.getColumnName(1)).isEqualTo("Centro");
            assertThat(table.getColumnName(2)).isEqualTo("Jan");
            assertThat(table.getColumnName(13)).isEqualTo("Dez");
            assertThat(table.getColumnName(14)).isEqualTo("Total");

            javax.swing.table.TableModel model = table.getModel();
            assertThat(model.getRowCount()).as("três pares conta × centro").isEqualTo(3);
            assertThat(model.getValueAt(0, 0)).isEqualTo("6221 — Rendas");
            assertThat(model.getValueAt(0, 1)).isEqualTo("ADM — Administração");
            assertThat(model.getValueAt(0, 2)).isEqualTo(money("1000.00"));
            assertThat(model.getValueAt(0, 3)).as("célula vazia").isNull();
            assertThat(model.getValueAt(0, 4)).as("zero é célula preenchida").isEqualTo(money("0.00"));
            assertThat(model.getValueAt(0, 14)).isEqualTo(money("1000.00"));
            assertThat(model.getValueAt(1, 1)).isEqualTo("A01 — Loja Central");
            assertThat(model.getValueAt(2, 0)).isEqualTo("7111 — Vendas");
            assertThat(model.getValueAt(2, 13)).isEqualTo(money("5000.00"));

            assertThat(model.isCellEditable(0, 0)).isFalse();
            assertThat(model.isCellEditable(0, 1)).isFalse();
            assertThat(model.isCellEditable(0, 2)).as("meses editáveis").isTrue();
            assertThat(model.isCellEditable(0, 14)).as("Total só leitura").isFalse();
            assertThat(tab.summaryLabel().getText()).contains("1.400,00").contains("5.000,00");
            assertThat(SwingUtilities.getAncestorOfClass(ModernPanel.class, table)).as("tabela num card").isNotNull();
        });
    }

    @Test
    @DisplayName("OR-UI-01: Editar meses: vazio tira a célula, zero fica, negativo ou texto recusado; o Total acompanha")
    void orUi01_edicaoDasCelulas() throws Exception {
        onEdt(() -> {
            BudgetGridModel model = new BudgetGridModel();
            model.applyBudget(BUDGET);

            model.setValueAt("1.500,50", 0, 3);
            assertThat(model.getValueAt(0, 3)).isEqualTo(money("1500.50"));
            assertThat(model.getValueAt(0, 14)).isEqualTo(money("2500.50"));
            model.setValueAt("", 0, 2);
            assertThat(model.getValueAt(0, 2)).isNull();
            assertThat(model.getValueAt(0, 14)).isEqualTo(money("1500.50"));

            assertThatThrownBy(() -> BudgetGridModel.parseAmount("-5"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("negativo");
            assertThatThrownBy(() -> BudgetGridModel.parseAmount("abc"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Valor inválido");
            assertThat(BudgetGridModel.parseAmount("  ")).isNull();
            assertThat(BudgetGridModel.parseAmount("0")).isEqualByComparingTo("0");
        });
    }

    @Test
    @DisplayName("OR-UI-01: Gravar envia o ano inteiro só com as células preenchidas (zero incluído)")
    void orUi01_gravarEnviaCelulasPreenchidas() throws Exception {
        onEdt(() -> {
            BudgetGridModel model = new BudgetGridModel();
            model.applyBudget(BUDGET);
            model.setValueAt(null, 1, 3);

            SaveBudgetRequest request = model.toRequest(2026);
            assertThat(request.year()).isEqualTo(2026);
            assertThat(request.lines()).containsExactly(
                    new SaveBudgetRequest.Line("6221", 1L, 1, money("1000.00")),
                    new SaveBudgetRequest.Line("6221", 1L, 3, money("0.00")),
                    new SaveBudgetRequest.Line("7111", 2L, 12, money("5000.00")));
        });
    }

    @Test
    @DisplayName("OR-UI-01: Adicionar linha recusa conta × centro repetida; remover tira a linha do pedido")
    void orUi01_adicionarERemoverLinhas() throws Exception {
        onEdt(() -> {
            BudgetGridModel model = new BudgetGridModel();
            model.applyBudget(BUDGET);

            model.addLine("6241", "Electricidade", 1L, "ADM — Administração");
            assertThat(model.getRowCount()).isEqualTo(4);
            assertThat(model.getValueAt(3, 0)).isEqualTo("6241 — Electricidade");
            assertThat(model.getValueAt(3, 2)).as("linha nova vazia").isNull();
            assertThatThrownBy(() -> model.addLine("6221", "Rendas", 1L, "ADM — Administração"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("já está no orçamento");

            model.removeLine(0);
            assertThat(model.getRowCount()).isEqualTo(3);
            assertThat(model.toRequest(2026).lines()).containsExactly(
                    new SaveBudgetRequest.Line("6221", 2L, 2, money("400.00")),
                    new SaveBudgetRequest.Line("7111", 2L, 12, money("5000.00")));
        });
    }

    @Test
    @DisplayName("OR-UI-01: Só contas movimentáveis e activas das classes 6 e 7 entram no orçamento")
    void orUi01_contasOrcamentaveis() {
        List<AccountDTO> accounts = List.of(
                account("6221", AccountClass.CUSTOS_E_PERDAS, true, true),
                account("62", AccountClass.CUSTOS_E_PERDAS, false, true),
                account("7111", AccountClass.PROVEITOS_E_GANHOS, true, true),
                account("7999", AccountClass.PROVEITOS_E_GANHOS, true, false),
                account("1101", AccountClass.MEIOS_CIRCULANTES_FINANCEIROS, true, true));
        assertThat(BudgetLineDialog.budgetableAccounts(accounts)).extracting(AccountDTO::code)
                .containsExactly("6221", "7111");
    }

    @Test
    @DisplayName("OR-UI-01: Alterar o orçamento só para ADMIN e MANAGER (SPEC §4.1)")
    void orUi01_soAdminEGestorAlteram() throws Exception {
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("gestor", "MANAGER");
            AccountingBudgetTab manager = newTab();
            assertThat(manager.saveButton().isEnabled()).isTrue();
            assertThat(manager.copyButton().isEnabled()).isTrue();
            assertThat(manager.addButton().isEnabled()).isTrue();

            CurrentUserContext.setCurrentUser("caixa", "EMPLOYEE");
            AccountingBudgetTab employee = newTab();
            employee.applyBudget(BUDGET);
            employee.budgetTable().setRowSelectionInterval(0, 0);
            assertThat(employee.saveButton().isEnabled()).isFalse();
            assertThat(employee.copyButton().isEnabled()).isFalse();
            assertThat(employee.addButton().isEnabled()).isFalse();
            assertThat(employee.removeButton().isEnabled()).isFalse();
            assertThat(employee.budgetTable().isCellEditable(0, 2)).as("sem perfil, grelha só leitura").isFalse();
        });
    }

    @Test
    @DisplayName("OR-UI-01: Ano, copiar com ajuste e confirmação, gravar pelo cliente; ícones e diálogos canónicos")
    void orUi01_anoCopiarGravarECliente() throws Exception {
        String tab = Files.readString(gui("AccountingBudgetTab.java"));
        assertThat(tab).contains("UIHelper.createYearSpinner(").contains("accountingApiClient.getBudget(")
                .contains("accountingApiClient.saveBudget(").contains("accountingApiClient.copyBudgetFromPreviousYear(")
                .contains("UIHelper.promptAmount(").contains("UIHelper.confirm(").contains("UIHelper.tableCardTop(")
                .contains("UIHelper.icon(").contains("SignedInUser.isManagerOrAdmin()").doesNotContain("JOptionPane");
        String dialog = Files.readString(gui("BudgetLineDialog.java"));
        assertThat(dialog).contains("new ModernFormDialog(").contains("new CostCenterCombo()")
                .contains("accountingApiClient.getAccounts()").doesNotContain("JOptionPane");
        Path client = Path.of("src", "main", "java", "mz", "multicore", "erp", "desktop", "client", "AccountingApiClient.java");
        if (!Files.exists(client)) client = Path.of("desktop").resolve(client);
        assertThat(Files.readString(client)).contains("/api/accounting/budgets").contains("/copy-previous-year");
    }

    private static final BudgetVarianceDTO VARIANCE = new BudgetVarianceDTO(2026, 3, false, null, List.of(
            varianceLine("6221", "Rendas", 6, 1L, "ADM", "Administração", "1000.00", "1200.00", "200.00", "20.00",
                    BudgetVarianceDirection.UNFAVORABLE),
            varianceLine("7111", "Vendas", 7, 2L, "A01", "Loja Central", "5000.00", "5500.00", "500.00", "10.00",
                    BudgetVarianceDirection.FAVORABLE),
            varianceLine("6241", "Electricidade", 6, 2L, "A01", "Loja Central", "0.00", "300.00", "300.00", null,
                    BudgetVarianceDirection.UNFAVORABLE),
            varianceLine("6231", "Comunicações", 6, 1L, "ADM", "Administração", "250.00", "250.00", "0.00", "0.00",
                    BudgetVarianceDirection.ON_BUDGET)));

    @Test
    @DisplayName("OR-UI-02: Contabilidade ganha a aba Real vs Orçado a seguir ao Orçamento, com ícone, sem mexer nas últimas")
    void orUi02_contabilidadeTemAbaRealVsOrcado() throws Exception {
        onEdt(() -> {
            AccountingPanel accounting = new AccountingPanel(mock(AccountingApiClient.class), mock(PrintApiClient.class));
            JTabbedPane tabs = firstOf(accounting, JTabbedPane.class);
            int index = tabs.indexOfTab("Real vs Orçado");
            assertThat(index).as("aba a seguir ao Orçamento").isEqualTo(tabs.indexOfTab("Orçamento") + 1);
            assertThat(tabs.getComponentAt(index)).isInstanceOf(AccountingBudgetVarianceTab.class);
            assertThat(tabs.getIconAt(index)).as("ícone pelo UIHelper.icon").isNotNull();
            assertThat(tabs.indexOfTab("Demonstrações")).isEqualTo(tabs.getTabCount() - 2);
            assertThat(tabs.indexOfTab("Resultados por centro")).isEqualTo(tabs.getTabCount() - 1);
        });
    }

    @Test
    @DisplayName("OR-UI-02: Uma linha por conta × centro com orçado, real, desvio, desvio % e situação em StatusTone")
    void orUi02_desviosColoridosPeloStatusTone() throws Exception {
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("gestor", "MANAGER");
            AccountingBudgetVarianceTab tab = newVarianceTab(mock(AccountingApiClient.class));
            tab.applyReport(VARIANCE);

            JTable table = tab.varianceTable();
            assertThat(table.getColumnCount()).isEqualTo(7);
            assertThat(List.of(table.getColumnName(0), table.getColumnName(1), table.getColumnName(2),
                    table.getColumnName(3), table.getColumnName(4), table.getColumnName(5), table.getColumnName(6)))
                    .containsExactly("Conta", "Centro", "Orçado", "Real", "Desvio", "Desvio %", "Situação");
            javax.swing.table.TableModel model = table.getModel();
            assertThat(model.getRowCount()).isEqualTo(4);
            assertThat(model.getValueAt(0, 0)).isEqualTo("6221 — Rendas");
            assertThat(model.getValueAt(0, 1)).isEqualTo("ADM — Administração");
            assertThat(model.getValueAt(0, 4)).isEqualTo(money("200.00"));
            assertThat(model.getValueAt(0, 5)).isEqualTo(money("20.00"));
            assertThat(model.getValueAt(2, 5)).as("orçado zero: desvio % vazio").isNull();
            assertThat(model.getValueAt(0, 6)).isEqualTo("Desfavorável");
            assertThat(model.getValueAt(1, 6)).isEqualTo("Favorável");
            assertThat(model.getValueAt(3, 6)).isEqualTo("No orçamento");

            assertThat(StatusTone.toneOf("Desfavorável")).isEqualTo(StatusTone.Tone.BAD);
            assertThat(StatusTone.toneOf("Favorável")).isEqualTo(StatusTone.Tone.GOOD);
            assertThat(StatusTone.toneOf("No orçamento")).isEqualTo(StatusTone.Tone.INFO);
            assertThat(foreground(table, 0, AccountingBudgetVarianceTab.VARIANCE_COLUMN))
                    .isEqualTo(StatusTone.colorOf(StatusTone.Tone.BAD));
            assertThat(foreground(table, 1, AccountingBudgetVarianceTab.VARIANCE_PERCENT_COLUMN))
                    .isEqualTo(StatusTone.colorOf(StatusTone.Tone.GOOD));
            assertThat(foreground(table, 3, AccountingBudgetVarianceTab.DIRECTION_COLUMN))
                    .isEqualTo(StatusTone.colorOf(StatusTone.Tone.INFO));
            assertThat(tab.summaryLabel().getText()).contains("Março de 2026").contains("4 linha(s)")
                    .contains("2 desfavorável");
            assertThat(SwingUtilities.getAncestorOfClass(ModernPanel.class, table)).as("tabela num card").isNotNull();
        });
    }

    @Test
    @DisplayName("OR-UI-02: Mês ou acumulado e filtro por centro (inactivos incluídos) vão no pedido; só ADMIN/MANAGER consultam")
    void orUi02_periodoCentroEPerfil() throws Exception {
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("gestor", "MANAGER");
            AccountingBudgetVarianceTab tab = newVarianceTab(mock(AccountingApiClient.class));
            tab.applyCenters(List.of(new CostCenterDTO(1L, "ADM", "Administração", CostCenterType.MANUAL, null,
                            true, true),
                    new CostCenterDTO(5L, "OLD", "Loja fechada", CostCenterType.MANUAL, null, false, false)));
            assertThat(tab.centerCombo().getItemCount()).as("Todos + todos os centros").isEqualTo(3);
            assertThat(tab.currentQuery().costCenterId()).as("por omissão todos os centros").isNull();

            tab.yearSpinner().setValue(2025);
            tab.monthCombo().setSelectedItem(4);
            tab.periodCombo().setSelectedIndex(1);
            tab.centerCombo().setSelectedIndex(2);
            assertThat(tab.currentQuery()).isEqualTo(new BudgetVarianceQuery(2025, 4, true, 5L));
            assertThat(tab.calculateButton().isEnabled()).isTrue();

            CurrentUserContext.setCurrentUser("caixa", "EMPLOYEE");
            AccountingApiClient client = mock(AccountingApiClient.class);
            AccountingBudgetVarianceTab employee = newVarianceTab(client);
            employee.onPanelSelected();
            assertThat(employee.calculateButton().isEnabled()).isFalse();
            verifyNoInteractions(client);
        });
        String source = Files.readString(gui("AccountingBudgetVarianceTab.java"));
        assertThat(source).contains("accountingApiClient.getBudgetVariance(").contains("StatusTone.")
                .contains("UIHelper.createYearSpinner(").contains("UIHelper.tableCardTop(").contains("UIHelper.icon(")
                .doesNotContain("JOptionPane").doesNotContain("new JComboBox");
    }

    @Test
    @DisplayName("OR-UI-02: Os alertas 90%/100% do orçamento entram no sino de ADMIN/MANAGER e abrem a Contabilidade")
    void orUi02_alertasNoSino() throws Exception {
        AccountingApiClient client = mock(AccountingApiClient.class);
        when(client.getBudgetAlerts(anyInt())).thenReturn(List.of(
                new BudgetAlertDTO(1L, 1L, "ADM", "Administração", 2026, 3, 100, money("1000.00"), money("1000.00"),
                        "Orçamento: o centro ADM atingiu 100% dos custos de Março/2026", LocalDateTime.now()),
                new BudgetAlertDTO(2L, 1L, "ADM", "Administração", 2026, 3, 90, money("1000.00"), money("900.00"),
                        "Orçamento: o centro ADM atingiu 90% dos custos de Março/2026", LocalDateTime.now())));
        onEdt(() -> {
            CurrentUserContext.setCurrentUser("gestor", "MANAGER");
            List<NotificationFeed.NotificationItem> items = newFeed(client).load(1L);
            assertThat(items).filteredOn(item -> "Orçamento".equals(item.type())).hasSize(2)
                    .allSatisfy(item -> assertThat(item.moduleCard()).isEqualTo("contabilidade"))
                    .extracting(NotificationFeed.NotificationItem::title)
                    .containsExactly("Orçamento: o centro ADM atingiu 100% dos custos de Março/2026",
                            "Orçamento: o centro ADM atingiu 90% dos custos de Março/2026");
            verify(client).getBudgetAlerts(LocalDate.now().getYear());

            AccountingApiClient employeeClient = mock(AccountingApiClient.class);
            CurrentUserContext.setCurrentUser("caixa", "EMPLOYEE");
            assertThat(newFeed(employeeClient).load(1L)).noneMatch(item -> "Orçamento".equals(item.type()));
            verify(employeeClient, never()).getBudgetAlerts(anyInt());

            AccountingApiClient failing = mock(AccountingApiClient.class);
            CurrentUserContext.setCurrentUser("gestor", "MANAGER");
            when(failing.getBudgetAlerts(anyInt())).thenThrow(new IllegalStateException("offline"));
            assertThat(newFeed(failing).load(1L)).as("o sino não falha por um fornecedor")
                    .noneMatch(item -> "Orçamento".equals(item.type()));
        });
    }

    @Test
    @DisplayName("OR-UI-02: O aviso X-Operation-Warning vira toast de aviso e o sino volta a contar")
    void orUi02_avisoEmToast() throws Exception {
        Path coordinator = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui",
                "MainFrameNotificationCoordinator.java");
        if (!Files.exists(coordinator)) coordinator = Path.of("desktop").resolve(coordinator);
        assertThat(Files.readString(coordinator)).contains("OperationWarningRelay.setListener(")
                .contains("ToastManager.show(").contains("FeedbackType.WARNING")
                .contains("refreshNotificationBadgeAsync");
        Path mainFrame = coordinator.resolveSibling("MainFrame.java");
        assertThat(Files.readString(mainFrame)).contains(".withBudgetAlerts(accountingApiClient)");
    }

    private static NotificationFeed newFeed(AccountingApiClient client) {
        return new NotificationFeed(mock(ApprovalApiClient.class), mock(InventoryApiClient.class),
                mock(MySubscriptionApiClient.class), mock(HRApiClient.class)).withBudgetAlerts(client);
    }

    private static AccountingBudgetVarianceTab newVarianceTab(AccountingApiClient client) {
        return new AccountingBudgetVarianceTab(client, new InlineFeedbackPanel());
    }

    private static Color foreground(JTable table, int row, int column) {
        return table.prepareRenderer(table.getCellRenderer(row, column), row, column).getForeground();
    }

    private static BudgetVarianceLineDTO varianceLine(String accountCode, String accountName, int accountClass,
                                                      Long centerId, String centerCode, String centerName,
                                                      String budgeted, String actual, String variance, String percent,
                                                      BudgetVarianceDirection direction) {
        return new BudgetVarianceLineDTO(accountCode, accountName, accountClass, centerId, centerCode, centerName,
                money(budgeted), money(actual), money(variance), percent == null ? null : money(percent), direction);
    }

    private static AccountingBudgetTab newTab() {
        return new AccountingBudgetTab(mock(AccountingApiClient.class), new InlineFeedbackPanel());
    }

    private static AccountDTO account(String code, AccountClass accountClass, boolean postable, boolean active) {
        return new AccountDTO((long) code.hashCode(), code, "Conta " + code, accountClass, accountClass.name(),
                AccountNature.DEVEDORA, postable, active, null);
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private static Path gui(String file) {
        Path nested = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "accounting", file);
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
