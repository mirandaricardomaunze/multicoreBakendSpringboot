package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.QuickPeekPanel;
import mz.multicore.erp.modules.promotions.dto.PromotionDTO;

import javax.swing.table.DefaultTableModel;
import java.util.List;

/** Coordenador de operações visuais e benefícios de promoções. */
final class PromotionsOperationsCoordinator {

    private PromotionsOperationsCoordinator() {}

    static String benefit(PromotionDTO p) {
        if ("PERCENT".equals(p.type())) {
            return p.percentValue() == null ? "—" : "-" + p.percentValue().stripTrailingZeros().toPlainString() + "%";
        }
        return "Leve " + p.buyQuantity() + ", pague " + p.payQuantity();
    }

    static void populatePeek(QuickPeekPanel peek, DefaultTableModel model, int mr, Runnable onToggle) {
        if (mr < 0 || mr >= model.getRowCount()) return;
        String name = String.valueOf(model.getValueAt(mr, 0));
        String tipo = String.valueOf(model.getValueAt(mr, 1));
        String scope = String.valueOf(model.getValueAt(mr, 2));
        String benefit = String.valueOf(model.getValueAt(mr, 3));
        String start = String.valueOf(model.getValueAt(mr, 4));
        String end = String.valueOf(model.getValueAt(mr, 5));
        String status = String.valueOf(model.getValueAt(mr, 6));

        peek.setTitle(name);
        peek.setSubtitle(tipo + " • " + scope);
        String bu = mr < model.getRowCount() ? String.valueOf(model.getValueAt(mr, 7)) : "—";
        peek.setItems(List.of(
                new QuickPeekPanel.PeekItem("Benefício", benefit, true),
                new QuickPeekPanel.PeekItem("Data Início", start, false),
                new QuickPeekPanel.PeekItem("Data Fim", end, false),
                new QuickPeekPanel.PeekItem("Estado", status, false),
                new QuickPeekPanel.PeekItem("Linha de Negócio", bu, false)
        ));
        peek.getOpenFullButton().setText("Activar / Desactivar");
        peek.setOnOpenFullAction(v -> onToggle.run());
    }
}
