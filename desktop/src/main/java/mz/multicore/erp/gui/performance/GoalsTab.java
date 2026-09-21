package mz.multicore.erp.gui.performance;

import mz.multicore.erp.desktop.client.PerformanceApiClient;
import mz.multicore.erp.desktop.session.DesktopSession;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.performance.dto.CreateSalesGoalRequest;
import mz.multicore.erp.modules.performance.dto.SalesGoalDTO;
import mz.multicore.erp.modules.performance.model.BonusType;
import mz.multicore.erp.modules.performance.model.GoalPeriod;
import mz.multicore.erp.modules.performance.model.GoalScope;
import mz.multicore.erp.modules.performance.model.GoalStatus;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GoalsTab extends JPanel {

    private final PerformanceApiClient apiClient;
    private final DesktopSession session;
    private final Runnable onGoalChanged;

    private JTable table;
    private DefaultTableModel tableModel;
    private List<SalesGoalDTO> loadedGoals = new ArrayList<>();

    private JComboBox<String> statusFilter;
    private JComboBox<String> periodFilter;
    private InlineFeedbackPanel feedbackPanel;

    private static final DecimalFormat CURRENCY_FMT;

    static {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(new Locale("pt", "MZ"));
        sym.setGroupingSeparator(' ');
        sym.setDecimalSeparator(',');
        CURRENCY_FMT = new DecimalFormat("#,##0.00", sym);
    }

    public GoalsTab(PerformanceApiClient apiClient, DesktopSession session, Runnable onGoalChanged) {
        this.apiClient = apiClient;
        this.session = session;
        this.onGoalChanged = onGoalChanged;

        setLayout(new BorderLayout(0, 8));
        setBackground(UIHelper.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        buildUI();
    }

    private void buildUI() {
        feedbackPanel = new InlineFeedbackPanel();
        add(feedbackPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
        contentPanel.setBackground(UIHelper.BG_DARK);

        // Header / Filter toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbar.setBackground(UIHelper.BG_DARK);

        statusFilter = PerformanceControls.createSelect(
                new String[]{"Todas", "Ativas", "Atingidas", "Não Atingidas", "Canceladas"},
                PerformanceControls.FILTER_SELECT_WIDTH
        );
        statusFilter.addActionListener(e -> reload());
        toolbar.add(PerformanceControls.createFilterGroup("Estado:", statusFilter));

        periodFilter = PerformanceControls.createSelect(
                new String[]{"Todos", "Mensal", "Trimestral", "Semestral", "Anual"},
                PerformanceControls.FILTER_SELECT_WIDTH
        );
        periodFilter.addActionListener(e -> reload());
        toolbar.add(PerformanceControls.createFilterGroup("Período:", periodFilter));

        ModernButton btnRefresh = UIHelper.createSecondaryButton("Recarregar");
        btnRefresh.setIcon(UIHelper.icon("fas-sync-alt", 14));
        btnRefresh.addActionListener(e -> reload());
        toolbar.add(btnRefresh);

        ModernButton btnNew = UIHelper.createPrimaryButton("Nova Meta");
        btnNew.setIcon(UIHelper.icon("fas-plus", 14));
        btnNew.addActionListener(e -> openCreateGoalDialog());
        toolbar.add(btnNew);

        ModernButton btnCancel = UIHelper.createDangerButton("Cancelar Meta");
        btnCancel.setIcon(UIHelper.icon("fas-ban", 14));
        btnCancel.addActionListener(e -> cancelSelectedGoal());
        toolbar.add(btnCancel);

        contentPanel.add(toolbar, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Nome da Meta", "Período", "Início", "Fim", "Escopo", "Alvo Receita (MZN)", "Alvo Margem (MZN)", "Prémio", "Estado"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(tableModel);
        UIHelper.styleTable(table);

        JScrollPane scrollPane = new JScrollPane(table);
        UIHelper.styleScrollPane(scrollPane);
        TableContextMenu.install(scrollPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER));
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        add(contentPanel, BorderLayout.CENTER);
    }

    public void reload() {
        feedbackPanel.clear();
        Long companyId = session.activeCompanyId();
        if (companyId == null) {
            return;
        }

        GoalStatus status = switch (statusFilter.getSelectedIndex()) {
            case 1 -> GoalStatus.ACTIVE;
            case 2 -> GoalStatus.ACHIEVED;
            case 3 -> GoalStatus.MISSED;
            case 4 -> GoalStatus.CANCELLED;
            default -> null;
        };

        GoalPeriod period = switch (periodFilter.getSelectedIndex()) {
            case 1 -> GoalPeriod.MONTHLY;
            case 2 -> GoalPeriod.QUARTERLY;
            case 3 -> GoalPeriod.SEMIANNUAL;
            case 4 -> GoalPeriod.ANNUAL;
            default -> null;
        };

        UIHelper.loadAsync(
                this,
                () -> apiClient.getGoals(companyId, status, period),
                goals -> {
                    loadedGoals = goals;
                    tableModel.setRowCount(0);
                    for (SalesGoalDTO g : goals) {
                        tableModel.addRow(new Object[]{
                                g.id(),
                                g.name(),
                                humanPeriod(g.period()),
                                g.periodStart(),
                                g.periodEnd(),
                                g.scopeLabel() != null ? g.scopeLabel() : g.scope().name(),
                                g.targetRevenue() != null ? CURRENCY_FMT.format(g.targetRevenue()) : "-",
                                g.targetMargin() != null ? CURRENCY_FMT.format(g.targetMargin()) : "-",
                                formatBonus(g.bonusType(), g.bonusValue(), g.bonusCap()),
                                humanStatus(g.status())
                        });
                    }
                },
                ex -> feedbackPanel.show(FeedbackType.ERROR, "Erro ao carregar metas: " + ex.getMessage())
        );
    }

    private void openCreateGoalDialog() {
        Window window = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(window, "Nova Meta Comercial", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 560);
        dialog.setLocationRelativeTo(window);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIHelper.BG_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1.0;

        JTextField txtName = PerformanceControls.createTextInput("", "Ex: Meta Vendas Supermercado", 0);
        form.add(new FormField("Nome da Meta", txtName, true, "Ex: Meta Vendas Supermercado"), gc);

        gc.gridy++;
        JComboBox<GoalPeriod> cbPeriod = PerformanceControls.createSelect(GoalPeriod.values(), 0, PerformanceControls::humanPeriod);
        form.add(new FormField("Periodicidade", cbPeriod, true, null), gc);

        gc.gridy++;
        JPanel dateRow = new JPanel(new GridLayout(1, 2, 8, 0));
        dateRow.setOpaque(false);
        DateField txtStart = PerformanceControls.createDateField(LocalDate.now().withDayOfMonth(1), 0);
        DateField txtEnd = PerformanceControls.createDateField(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()), 0);
        dateRow.add(new FormField("Data Início (AAAA-MM-DD)", txtStart, true, null));
        dateRow.add(new FormField("Data Fim (AAAA-MM-DD)", txtEnd, true, null));
        form.add(dateRow, gc);

        gc.gridy++;
        JComboBox<GoalScope> cbScope = PerformanceControls.createSelect(GoalScope.values(), 0, PerformanceControls::humanScope);
        form.add(new FormField("Escopo da Meta", cbScope, true, null), gc);

        gc.gridy++;
        JPanel targetsRow = new JPanel(new GridLayout(1, 2, 8, 0));
        targetsRow.setOpaque(false);
        MoneyField txtRevenue = PerformanceControls.createMoneyField("", 0);
        MoneyField txtMargin = PerformanceControls.createMoneyField("", 0);
        targetsRow.add(new FormField("Alvo Receita (MZN)", txtRevenue, false, null));
        targetsRow.add(new FormField("Alvo Margem Bruta (MZN)", txtMargin, false, null));
        form.add(targetsRow, gc);

        gc.gridy++;
        JComboBox<BonusType> cbBonusType = PerformanceControls.createSelect(BonusType.values(), 0, PerformanceControls::humanBonusType);
        form.add(new FormField("Tipo de Bónus", cbBonusType, true, null), gc);

        gc.gridy++;
        JPanel bonusRow = new JPanel(new GridLayout(1, 2, 8, 0));
        bonusRow.setOpaque(false);
        MoneyField txtBonusValue = PerformanceControls.createMoneyField("", 0);
        MoneyField txtBonusCap = PerformanceControls.createMoneyField("", 0);
        bonusRow.add(new FormField("Valor do Bónus (MZN ou %)", txtBonusValue, true, null));
        bonusRow.add(new FormField("Teto Máximo Bónus (MZN)", txtBonusCap, false, null));
        form.add(bonusRow, gc);

        gc.gridy++;
        JCheckBox chkAuto = new JCheckBox("Gerar bónus automaticamente ao atingir", true);
        chkAuto.setOpaque(false);
        form.add(chkAuto, gc);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(UIHelper.BG_DARK);

        ModernButton btnCancel = UIHelper.createSecondaryButton("Cancelar");
        btnCancel.addActionListener(e -> dialog.dispose());
        footer.add(btnCancel);

        ModernButton btnSave = UIHelper.createPrimaryButton("Criar Meta");
        btnSave.setIcon(UIHelper.icon("fas-check", 14));
        btnSave.addActionListener(e -> {
            try {
                String name = txtName.getText().trim();
                LocalDate start = txtStart.value();
                LocalDate end = txtEnd.value();

                BigDecimal rev = txtRevenue.optionalValue();
                BigDecimal marg = txtMargin.optionalValue();
                BigDecimal bonusVal = txtBonusValue.optionalValue() != null ? txtBonusValue.optionalValue() : BigDecimal.ZERO;
                BigDecimal bonusCap = txtBonusCap.optionalValue();

                CreateSalesGoalRequest req = new CreateSalesGoalRequest(
                        session.activeCompanyId(),
                        name,
                        (GoalPeriod) cbPeriod.getSelectedItem(),
                        start,
                        end,
                        (GoalScope) cbScope.getSelectedItem(),
                        null,
                        null,
                        rev,
                        marg,
                        null,
                        (BonusType) cbBonusType.getSelectedItem(),
                        bonusVal,
                        bonusCap,
                        chkAuto.isSelected()
                );

                UIHelper.loadAsync(
                        (JComponent) dialog.getContentPane(),
                        () -> apiClient.createGoal(req),
                        created -> {
                            dialog.dispose();
                            ToastManager.success(window, "Meta criada com sucesso!");
                            reload();
                            if (onGoalChanged != null) onGoalChanged.run();
                        },
                        ex -> ModernMessageDialog.show(dialog, FeedbackType.ERROR, "Erro ao criar meta", ex.getMessage())
                );
            } catch (Exception ex) {
                ModernMessageDialog.show(dialog, FeedbackType.ERROR, "Validação", "Verifique os campos inseridos: " + ex.getMessage());
            }
        });
        footer.add(btnSave);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(footer, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void cancelSelectedGoal() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= loadedGoals.size()) {
            feedbackPanel.show(FeedbackType.WARNING, "Selecione uma meta na tabela para cancelar.");
            return;
        }
        SalesGoalDTO goal = loadedGoals.get(row);
        if (goal.status() == GoalStatus.CANCELLED) {
            feedbackPanel.show(FeedbackType.INFO, "A meta selecionada já está cancelada.");
            return;
        }

        Window win = SwingUtilities.getWindowAncestor(this);
        boolean confirmed = ModernMessageDialog.confirm(
                win,
                FeedbackType.WARNING,
                "Cancelar Meta Comercial",
                "Tem certeza que deseja cancelar a meta '" + goal.name() + "'?",
                "Cancelar Meta"
        );
        if (!confirmed) return;

        UIHelper.loadAsync(
                this,
                () -> {
                    apiClient.cancelGoal(goal.id());
                    return null;
                },
                ok -> {
                    ToastManager.success(win, "Meta cancelada com sucesso.");
                    reload();
                    if (onGoalChanged != null) onGoalChanged.run();
                },
                ex -> feedbackPanel.show(FeedbackType.ERROR, "Erro ao cancelar: " + ex.getMessage())
        );
    }

    private String humanPeriod(GoalPeriod period) {
        if (period == null) return "-";
        return switch (period) {
            case MONTHLY -> "Mensal";
            case QUARTERLY -> "Trimestral";
            case SEMIANNUAL -> "Semestral";
            case ANNUAL -> "Anual";
        };
    }

    private String humanStatus(GoalStatus status) {
        if (status == null) return "-";
        return switch (status) {
            case ACTIVE -> "Ativa";
            case ACHIEVED -> "Atingida";
            case MISSED -> "Não Atingida";
            case CANCELLED -> "Cancelada";
        };
    }

    private String formatBonus(BonusType type, BigDecimal value, BigDecimal cap) {
        if (type == null || value == null) return "-";
        String valStr = switch (type) {
            case FIXED -> CURRENCY_FMT.format(value) + " MZN";
            case PERCENTAGE_OF_REVENUE -> value + "% da Receita";
            case PERCENTAGE_OF_MARGIN -> value + "% da Margem";
        };
        if (cap != null && cap.compareTo(BigDecimal.ZERO) > 0) {
            valStr += " (teto " + CURRENCY_FMT.format(cap) + " MZN)";
        }
        return valStr;
    }
}
