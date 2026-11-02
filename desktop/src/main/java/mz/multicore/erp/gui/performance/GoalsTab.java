package mz.multicore.erp.gui.performance;

import mz.multicore.erp.desktop.client.PerformanceApiClient;
import mz.multicore.erp.desktop.session.DesktopSession;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.performance.dto.SalesGoalDTO;
import mz.multicore.erp.modules.performance.model.BonusType;
import mz.multicore.erp.modules.performance.model.GoalPeriod;
import mz.multicore.erp.modules.performance.model.GoalStatus;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class GoalsTab extends JPanel {

    public record BusinessUnitOption(Long id, String name) {
        @Override public String toString() { return name; }
    }

    private final PerformanceApiClient apiClient;
    private final DesktopSession session;
    private final Runnable onGoalChanged;

    private JTable table;
    private DefaultTableModel tableModel;
    private List<SalesGoalDTO> loadedGoals = new ArrayList<>();
    private JComboBox<String> statusFilter, periodFilter;
    private InlineFeedbackPanel feedbackPanel;

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

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel filterBar = new JPanel(new BorderLayout(10, 0));
        filterBar.setOpaque(false);
        JPanel filtersLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filtersLeft.setOpaque(false);

        statusFilter = PerformanceControls.createSelect(new String[]{"Todas", "Ativas", "Atingidas", "Não Atingidas", "Canceladas"}, PerformanceControls.FILTER_SELECT_WIDTH);
        statusFilter.addActionListener(e -> reload());
        filtersLeft.add(PerformanceControls.createFilterGroup("Estado:", statusFilter));

        periodFilter = PerformanceControls.createSelect(new String[]{"Todos", "Mensal", "Trimestral", "Semestral", "Anual"}, PerformanceControls.FILTER_SELECT_WIDTH);
        periodFilter.addActionListener(e -> reload());
        filtersLeft.add(PerformanceControls.createFilterGroup("Período:", periodFilter));

        ModernButton btnRefresh = UIHelper.createSecondaryButton("Actualizar");
        btnRefresh.setIcon(UIHelper.icon("fas-sync-alt", 14));
        btnRefresh.addActionListener(e -> reload());

        ModernButton btnCancel = UIHelper.createDangerButton("Cancelar Meta");
        btnCancel.setIcon(UIHelper.icon("fas-ban", 14));
        btnCancel.addActionListener(e -> cancelSelectedGoal());

        ModernButton btnNew = UIHelper.createSuccessButton("Nova Meta");
        btnNew.setIcon(UIHelper.icon("fas-plus", 14));
        btnNew.addActionListener(e -> GoalCreateDialog.show(SwingUtilities.getWindowAncestor(this), apiClient, session, () -> {
            reload();
            if (onGoalChanged != null) onGoalChanged.run();
        }));

        filterBar.add(filtersLeft, BorderLayout.WEST);
        filterBar.add(UIHelper.actionsBar(btnRefresh, btnCancel, btnNew), BorderLayout.EAST);
        card.add(filterBar, BorderLayout.NORTH);

        String[] columns = {"ID", "Nome da Meta", "Período", "Início", "Fim", "Escopo", "Alvo Receita (MT)", "Alvo Margem (MT)", "Prémio", "Estado"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(tableModel);
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);

        TableQuickPeekController.install(table, card, (peek, mr) -> {
            int row = table.convertRowIndexToModel(mr);
            peek.setHeaderIcon("fas-bullseye", UIHelper.ACCENT_BLUE);
            peek.setTitle(String.valueOf(tableModel.getValueAt(row, 1)));
            peek.setSubtitle("Colaborador: " + tableModel.getValueAt(row, 2));
            peek.setStatus(String.valueOf(tableModel.getValueAt(row, 8)), UIHelper.ACCENT_BLUE);
            List<QuickPeekPanel.PeekItem> items = new ArrayList<>();
            items.add(new QuickPeekPanel.PeekItem("Alvo", String.valueOf(tableModel.getValueAt(row, 3)), false));
            items.add(new QuickPeekPanel.PeekItem("Atingido", String.valueOf(tableModel.getValueAt(row, 4)), false));
            items.add(new QuickPeekPanel.PeekItem("Progresso", String.valueOf(tableModel.getValueAt(row, 5)), true));
            peek.setItems(items);
        });

        JScrollPane scrollPane = new JScrollPane(table);
        UIHelper.styleScrollPane(scrollPane);
        TableContextMenu.install(scrollPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER));
        card.add(scrollPane, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
    }

    public void reload() {
        feedbackPanel.clear();
        Long companyId = session.activeCompanyId();
        if (companyId == null) return;

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

        UIHelper.loadAsync(this, () -> apiClient.getGoals(companyId, status, period), goals -> {
            loadedGoals = goals;
            tableModel.setRowCount(0);
            for (SalesGoalDTO g : goals) {
                tableModel.addRow(new Object[]{
                        g.id(), g.name(), PerformanceControls.humanPeriod(g.period()), g.periodStart(), g.periodEnd(),
                        g.scopeLabel() != null ? g.scopeLabel() : g.scope().name(),
                        g.targetRevenue() != null ? UIHelper.formatMzn(g.targetRevenue()) : "-",
                        g.targetMargin() != null ? UIHelper.formatMzn(g.targetMargin()) : "-",
                        formatBonus(g.bonusType(), g.bonusValue(), g.bonusCap()), humanStatus(g.status())
                });
            }
        }, ex -> feedbackPanel.show(FeedbackType.ERROR, "Erro ao carregar metas: " + ex.getMessage()));
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
                win, FeedbackType.WARNING, "Cancelar Meta Comercial",
                "Tem certeza que deseja cancelar a meta '" + goal.name() + "'?", "Cancelar Meta"
        );
        if (!confirmed) return;

        UIHelper.loadAsync(this, () -> { apiClient.cancelGoal(goal.id()); return null; }, ok -> {
            ToastManager.success(win, "Meta cancelada com sucesso.");
            reload();
            if (onGoalChanged != null) onGoalChanged.run();
        }, ex -> feedbackPanel.show(FeedbackType.ERROR, "Erro ao cancelar: " + ex.getMessage()));
    }

    private String humanStatus(GoalStatus status) {
        if (status == null) return "-";
        return switch (status) {
            case ACTIVE -> "Ativa"; case ACHIEVED -> "Atingida";
            case MISSED -> "Não Atingida"; case CANCELLED -> "Cancelada";
        };
    }

    private String formatBonus(BonusType type, BigDecimal value, BigDecimal cap) {
        if (type == null || value == null) return "-";
        String valStr = switch (type) {
            case FIXED -> UIHelper.formatMzn(value) + " MZN";
            case PERCENTAGE_OF_REVENUE -> value + "% da Receita";
            case PERCENTAGE_OF_MARGIN -> value + "% da Margem";
        };
        if (cap != null && cap.compareTo(BigDecimal.ZERO) > 0) valStr += " (teto " + UIHelper.formatMzn(cap) + " MZN)";
        return valStr;
    }
}
