package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.QuickPeekPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ClientCreditRiskDTO;
import mz.multicore.erp.modules.comercial.dto.CreditRiskSummaryDTO;
import mz.multicore.erp.modules.comercial.model.CreditRiskLevel;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/** Coordenador de visualização, formatação de linhas, filtros e QuickPeek da matriz de risco de crédito. */
final class CreditRiskViewCoordinator {
    private CreditRiskViewCoordinator() {}

    static void populateKpis(JPanel kpiRow, JLabel rec, JLabel overdue, JLabel blocked, JLabel critical) {
        kpiRow.add(KpiCard.createMetricCard("TOTAL A RECEBER", rec, "Carteira global em aberto", "fas-wallet", UIHelper.ACCENT_BLUE));
        kpiRow.add(KpiCard.createMetricCard("SALDO EM MORA", overdue, "Valores vencidos em atraso", "fas-exclamation-triangle", UIHelper.PENDING_YELLOW));
        kpiRow.add(KpiCard.createMetricCard("CLIENTES BLOQUEADOS", blocked, "Vendas a crédito suspensas", "fas-ban", UIHelper.REJECTED_RED));
        kpiRow.add(KpiCard.createMetricCard("RISCO ELEVADO / CRÍTICO", critical, "Atrasos > 30d ou limite excedido", "fas-user-slash", UIHelper.REJECTED_RED));
    }

    static void updateKpis(CreditRiskPanel panel, CreditRiskSummaryDTO summary) {
        if (summary == null) return;
        panel.kpiTotalReceivable.setText(UIHelper.formatMzn(summary.totalReceivable()));
        panel.kpiTotalOverdue.setText(UIHelper.formatMzn(summary.totalOverdue()));
        panel.kpiBlockedCount.setText(String.valueOf(summary.blockedClientsCount()));
        panel.kpiCriticalCount.setText(String.valueOf(summary.criticalRiskCount() + summary.highRiskCount()));
    }

    static void configureTableColumns(JTable table) {
        if (table.getColumnModel().getColumnCount() <= 0) return;
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        int[] idxs = {1, 2, 3, 4, 5, 11, 12, 13, 14};
        int[] widths = {170, 90, 95, 100, 95, 100, 65, 95, 110};
        for (int i = 0; i < idxs.length; i++) table.getColumnModel().getColumn(idxs[i]).setPreferredWidth(widths[i]);
    }

    static void applyFilters(CreditRiskPanel panel) {
        String filterType = panel.riskFilterCombo != null ? (String) panel.riskFilterCombo.getSelectedItem() : "Todos os Riscos";
        String term = panel.searchField != null ? panel.searchField.getText().trim().toLowerCase() : "";

        panel.visibleClientsRisk = new ArrayList<>();
        panel.tableModel.setRowCount(0);

        for (ClientCreditRiskDTO c : panel.allClientsRisk) {
            if (!matchesRisk(c, filterType)) continue;
            if (!term.isEmpty()) {
                boolean matchName = c.clientName() != null && c.clientName().toLowerCase().contains(term);
                boolean matchTaxId = c.clientTaxId() != null && c.clientTaxId().toLowerCase().contains(term);
                boolean matchEmail = c.email() != null && c.email().toLowerCase().contains(term);
                if (!matchName && !matchTaxId && !matchEmail) continue;
            }

            panel.visibleClientsRisk.add(c);
            panel.tableModel.addRow(toRowData(c));
        }
        UIHelper.ensureHeadersFit(panel.table);
        panel.updateActionButtons();
        panel.table.clearSelection();
    }

    static boolean matchesRisk(ClientCreditRiskDTO c, String filter) {
        return switch (filter != null ? filter : "") {
            case "Apenas Bloqueados" -> c.isBlocked();
            case "Apenas Crítico / Alto" -> c.riskLevel() == CreditRiskLevel.CRITICAL || c.riskLevel() == CreditRiskLevel.HIGH;
            case "CRITICAL - Crítico" -> c.riskLevel() == CreditRiskLevel.CRITICAL;
            case "HIGH - Alto" -> c.riskLevel() == CreditRiskLevel.HIGH;
            case "MEDIUM - Médio" -> c.riskLevel() == CreditRiskLevel.MEDIUM;
            case "LOW - Baixo" -> c.riskLevel() == CreditRiskLevel.LOW;
            default -> true;
        };
    }

    static Object[] toRowData(ClientCreditRiskDTO c) {
        return new Object[]{
                c.clientId(), c.clientName(), c.clientTaxId() != null ? c.clientTaxId() : "-",
                c.creditLimit() != null ? UIHelper.formatMzn(c.creditLimit()) : "Livre",
                UIHelper.formatMzn(c.totalDebt()),
                c.availableCredit() != null ? UIHelper.formatMzn(c.availableCredit()) : "Livre",
                UIHelper.formatMzn(c.corrente()), UIHelper.formatMzn(c.ate30()), UIHelper.formatMzn(c.de31a60()),
                UIHelper.formatMzn(c.de61a90()), UIHelper.formatMzn(c.maisDe90()), UIHelper.formatMzn(c.totalOverdue()),
                c.maxDaysOverdue() > 0 ? c.maxDaysOverdue() + " d" : "-",
                c.riskLevel().label(),
                c.isBlocked() ? "BLOQUEADO" : (c.blockReason() != null ? "EXCEPÇÃO" : "NORMAL")
        };
    }

    static void populateQuickPeek(QuickPeekPanel peek, int mr, CreditRiskPanel panel, Runnable onOpenFull) {
        int row = mr; // o controlador já entrega a linha do modelo
        var m = panel.tableModel;
        peek.setHeaderIcon("fas-wallet", UIHelper.ACCENT_BLUE);
        peek.setTitle(String.valueOf(m.getValueAt(row, 1)));
        peek.setSubtitle("NUIT: " + m.getValueAt(row, 2) + " • " + m.getValueAt(row, 13));
        peek.setStatus(String.valueOf(m.getValueAt(row, 14)), UIHelper.ACCENT_BLUE);
        peek.setItems(List.of(
                new QuickPeekPanel.PeekItem("Limite Autorizado", peek.cell(m.getValueAt(row, 3), 3), false),
                new QuickPeekPanel.PeekItem("Dívida Total", peek.cell(m.getValueAt(row, 4), 4), true),
                new QuickPeekPanel.PeekItem("Crédito Disponível", peek.cell(m.getValueAt(row, 5), 5), false),
                new QuickPeekPanel.PeekItem("Total em Mora", peek.cell(m.getValueAt(row, 11), 11), true),
                new QuickPeekPanel.PeekItem("Maior Atraso", peek.cell(m.getValueAt(row, 12), 12), false)
        ));
        peek.getOpenFullButton().setText("Ficha de Cobrança");
        peek.setOnOpenFullAction(v -> onOpenFull.run());
    }
}
