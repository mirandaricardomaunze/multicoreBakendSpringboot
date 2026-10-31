package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.format.DisplayLocale;
import mz.multicore.erp.modules.purchases.dto.ReorderSuggestionDTO;

import javax.swing.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;

/** Coordenador de povoamento e atualização analítica de dados de reposição inteligente. */
public final class PurchaseReorderDataCoordinator {

    private PurchaseReorderDataCoordinator() {}

    public static void applyReorderSuggestions(
            List<ReorderSuggestionDTO> loaded,
            ComprasPanel owner,
            JComboBox<String> businessUnitFilterCombo,
            JComboBox<String> supplierFilterCombo,
            JLabel kpiEsgotados,
            JLabel kpiCriticos,
            JLabel kpiTotalRepor,
            JLabel kpiCustoTotal,
            JLabel reorderFooter
    ) {
        owner.reorderList = loaded != null ? loaded : List.of();
        owner.reorderModel.setRowCount(0);

        int esgotadosCount = 0;
        int criticosCount = 0;
        BigDecimal totalCostSum = BigDecimal.ZERO;
        Set<String> uniqueSuppliers = new TreeSet<>();
        Set<String> uniqueBusinessUnits = new TreeSet<>();

        for (ReorderSuggestionDTO s : owner.reorderList) {
            String status = s.urgencyStatus() != null ? s.urgencyStatus() : (s.currentStock().signum() <= 0 ? "ESGOTADO" : "BAIXO");
            if ("ESGOTADO".equalsIgnoreCase(status)) {
                esgotadosCount++;
            } else if ("CRÍTICO".equalsIgnoreCase(status) || "CRITICO".equalsIgnoreCase(status)) {
                criticosCount++;
            }

            BigDecimal estCost = s.estimatedTotalCost() != null ? s.estimatedTotalCost() : BigDecimal.ZERO;
            totalCostSum = totalCostSum.add(estCost);

            String supName = (s.supplierName() != null && !s.supplierName().isBlank()) ? s.supplierName() : "—";
            if (!"—".equals(supName)) uniqueSuppliers.add(supName);

            String buName = (s.businessUnitName() != null && !s.businessUnitName().isBlank()) ? s.businessUnitName() : "—";
            if (!"—".equals(buName)) uniqueBusinessUnits.add(buName);

            String daysStr = s.daysRemaining() != null ? (s.daysRemaining() + (s.daysRemaining() == 1 ? " dia" : " dias")) : "—";
            BigDecimal salesRate = s.dailySalesRate() != null ? s.dailySalesRate() : BigDecimal.ZERO;

            owner.reorderModel.addRow(new Object[]{
                    s.name(), s.sku(),
                    String.format(Locale.ROOT, "%,.2f", s.currentStock()),
                    String.format(Locale.ROOT, "%,.2f", s.minStock()),
                    String.format(Locale.ROOT, "%,.2f /dia", salesRate),
                    daysStr, s.unitsPerBox(),
                    String.format(Locale.ROOT, "%.0f", s.suggestedBoxes()),
                    String.format(Locale.ROOT, "%.0f", s.suggestedUnits()),
                    supName,
                    s.estimatedUnitPrice() != null ? s.estimatedUnitPrice() : BigDecimal.ZERO,
                    estCost, status, buName
            });
        }

        if (businessUnitFilterCombo != null) {
            Object prevBu = businessUnitFilterCombo.getSelectedItem();
            businessUnitFilterCombo.removeAllItems();
            businessUnitFilterCombo.addItem("Todos os negócios");
            businessUnitFilterCombo.addItem("—");
            for (String bu : uniqueBusinessUnits) businessUnitFilterCombo.addItem(bu);
            if (prevBu != null) businessUnitFilterCombo.setSelectedItem(prevBu);
        }

        if (kpiEsgotados != null) kpiEsgotados.setText(String.valueOf(esgotadosCount));
        if (kpiCriticos != null) kpiCriticos.setText(String.valueOf(criticosCount));
        if (kpiTotalRepor != null) kpiTotalRepor.setText(String.valueOf(owner.reorderList.size()));

        NumberFormat fmt = NumberFormat.getNumberInstance(DisplayLocale.NUMBERS);
        fmt.setMinimumFractionDigits(2);
        fmt.setMaximumFractionDigits(2);
        if (kpiCustoTotal != null) kpiCustoTotal.setText(fmt.format(totalCostSum) + " MT");

        if (supplierFilterCombo != null) {
            String prevSel = (String) supplierFilterCombo.getSelectedItem();
            supplierFilterCombo.removeAllItems();
            supplierFilterCombo.addItem("Todos os fornecedores");
            for (String sup : uniqueSuppliers) supplierFilterCombo.addItem(sup);
            if (prevSel != null && uniqueSuppliers.contains(prevSel)) supplierFilterCombo.setSelectedItem(prevSel);
        }

        if (reorderFooter != null) {
            reorderFooter.setText(owner.reorderList.isEmpty()
                    ? "Sem reposições pendentes — todo o stock está em níveis adequados acima do mínimo."
                    : String.format("%d produto(s) a repor · %d esgotado(s) · %d crítico(s) · Investimento estimado: %s MT",
                    owner.reorderList.size(), esgotadosCount, criticosCount, fmt.format(totalCostSum)));
        }
    }
}
