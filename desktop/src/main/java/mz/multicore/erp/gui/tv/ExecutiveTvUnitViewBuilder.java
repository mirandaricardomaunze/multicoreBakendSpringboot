package mz.multicore.erp.gui.tv;

import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.List;

/**
 * Visão focada numa unidade de negócio: o mesmo resumo da visão consolidada, aplicado à unidade,
 * com o ranking de artigos e as últimas faturas dessa linha.
 */
public final class ExecutiveTvUnitViewBuilder {

    private ExecutiveTvUnitViewBuilder() {}

    public static JPanel build(BusinessUnitDTO bu, List<InvoiceDTO> filteredInvoices,
                               ExecutiveTvDashboardDialog.Period currentPeriod, boolean lightMode) {
        return build(bu, filteredInvoices, currentPeriod, lightMode, null);
    }

    public static JPanel build(BusinessUnitDTO bu, List<InvoiceDTO> filteredInvoices,
                               ExecutiveTvDashboardDialog.Period currentPeriod, boolean lightMode, Runnable onEditGoals) {
        int gap = ExecutiveTvComponents.GAP;
        JPanel panel = ExecutiveTvComponents.transparent(new BorderLayout(0, gap));
        ExecutiveTvMetrics m = ExecutiveTvMetrics.of(filteredInvoices, bu.id(),
                BusinessUnitGoalManager.getGoalForUnit(bu.id()), currentPeriod, LocalDate.now());
        String supplier = bu.supplierName() != null && !bu.supplierName().isBlank() ? " · " + bu.supplierName() : "";
        panel.add(ExecutiveTvSummaryBuilder.build(bu.name() + supplier + " · " + currentPeriod.getLabel(),
                m, lightMode, onEditGoals), BorderLayout.NORTH);

        JPanel lists = ExecutiveTvComponents.transparent(new GridLayout(1, 2, gap, 0));
        lists.add(ExecutiveTvListCards.topProducts(filteredInvoices, bu.id(), lightMode));
        lists.add(ExecutiveTvListCards.recentInvoices(filteredInvoices, bu.id(), lightMode));
        panel.add(lists, BorderLayout.CENTER);
        return panel;
    }
}
