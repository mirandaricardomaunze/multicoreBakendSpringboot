package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.QuickPeekPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.purchases.dto.PayableDTO;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Coordenador de apresentação, filtros de negócio e Quick Peek para contas a pagar.
 */
public final class PurchasePayablesViewCoordinator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private PurchasePayablesViewCoordinator() {}

    public static void populatePeek(QuickPeekPanel peek, int modelRow, ComprasPanel owner, Runnable onPayAction) {
        if (owner.payablesList == null || modelRow < 0 || modelRow >= owner.payablesList.size()) return;
        PayableDTO pa = owner.payablesList.get(modelRow);
        boolean inDebt = pa.outstanding() != null && pa.outstanding().signum() > 0;
        peek.setHeaderIcon("fas-file-invoice-dollar", UIHelper.REJECTED_RED);
        peek.setTitle("Compra " + (pa.purchaseNumber() != null ? pa.purchaseNumber() : ""));
        peek.setSubtitle(pa.supplierName() != null ? pa.supplierName() : "-");
        peek.setStatus(inDebt ? "EM DÍVIDA" : "LIQUIDADO", inDebt ? UIHelper.REJECTED_RED : UIHelper.APPROVED_GREEN);

        List<QuickPeekPanel.PeekItem> items = new ArrayList<>();
        items.add(new QuickPeekPanel.PeekItem("Linha de Negócio", pa.businessUnitName() != null ? pa.businessUnitName() : "Geral", false));
        items.add(new QuickPeekPanel.PeekItem("Fornecedor", pa.supplierName() != null ? pa.supplierName() : "-", false));
        BigDecimal total = pa.totalAmount() != null ? pa.totalAmount() : BigDecimal.ZERO;
        items.add(new QuickPeekPanel.PeekItem("Total da Compra", String.format("%,.2f MT", total), false));
        BigDecimal paid = pa.amountPaid() != null ? pa.amountPaid() : BigDecimal.ZERO;
        items.add(new QuickPeekPanel.PeekItem("Total Já Pago", String.format("%,.2f MT", paid), false));
        items.add(new QuickPeekPanel.PeekItem("Data da Compra", pa.purchaseDate() != null ? pa.purchaseDate().format(DATE_FORMATTER) : "-", false));
        BigDecimal debt = pa.outstanding() != null ? pa.outstanding() : BigDecimal.ZERO;
        items.add(new QuickPeekPanel.PeekItem("Valor em Dívida", String.format("%,.2f MT", debt), true));
        peek.setItems(items);

        if (inDebt) {
            peek.getOpenFullButton().setText("Registar Pagamento");
            peek.setOnOpenFullAction(ignored -> onPayAction.run());
        } else {
            peek.setOnOpenFullAction(null);
        }
    }

    public static void applyPayables(List<PayableDTO> loaded, ComprasPanel owner,
                                     JComboBox<String> businessUnitCombo, JLabel payablesFooter) {
        owner.payablesList = loaded;
        BigDecimal totalDivida = BigDecimal.ZERO;
        owner.payablesModel.setRowCount(0);
        Set<String> bus = new TreeSet<>();
        for (PayableDTO pa : owner.payablesList) {
            String bu = (pa.businessUnitName() != null && !pa.businessUnitName().isBlank()) ? pa.businessUnitName() : "Geral";
            bus.add(bu);
            totalDivida = totalDivida.add(pa.outstanding() != null ? pa.outstanding() : BigDecimal.ZERO);
            owner.payablesModel.addRow(new Object[]{
                    pa.purchaseNumber(),
                    bu,
                    pa.supplierName(),
                    pa.totalAmount(), pa.amountPaid(), pa.outstanding(),
                    pa.purchaseDate() == null ? "-" : pa.purchaseDate().format(DATE_FORMATTER)});
        }
        Object sel = businessUnitCombo.getSelectedItem();
        businessUnitCombo.removeAllItems();
        businessUnitCombo.addItem("Todos os negócios");
        for (String b : bus) businessUnitCombo.addItem(b);
        if (sel != null) businessUnitCombo.setSelectedItem(sel);
        payablesFooter.setText(String.format("%d fatura(s) em dívida · Total a pagar: %,.2f MT",
                owner.payablesList.size(), totalDivida));
    }
}
