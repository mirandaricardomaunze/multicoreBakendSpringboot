package mz.multicore.erp.gui.accounting;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.AccountingApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.modules.accounting.dto.AccountDTO;
import mz.multicore.erp.modules.accounting.dto.BudgetDTO;
import mz.multicore.erp.modules.accounting.dto.BudgetLineDTO;
import mz.multicore.erp.modules.accounting.dto.SaveBudgetRequest;
import mz.multicore.erp.modules.accounting.model.AccountClass;
import mz.multicore.erp.modules.accounting.model.AccountNature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Harness de UI do orçamento (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.4).
 * OR-UI-01: Contabilidade › Orçamento — grelha anual conta × centro × 12 meses, copiar do ano anterior, gravar.
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
