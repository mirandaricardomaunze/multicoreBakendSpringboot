package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.gui.components.QuickPeekPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.company.model.CurrencyCode;
import mz.multicore.erp.modules.purchases.dto.PayableDTO;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Coordenador de apresentação, filtros de negócio e Quick Peek para contas a pagar. Total, pago e dívida na moeda
 * do documento; a dívida em moeda base e o total a pagar somam em moeda base (SPEC moeda estrangeira §3.3).
 */
public final class PurchasePayablesViewCoordinator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private PurchasePayablesViewCoordinator() {}

    public static void populatePeek(QuickPeekPanel peek, int modelRow, ComprasPanel owner, Runnable onPayAction) {
        if (owner.payablesList == null || modelRow < 0 || modelRow >= owner.payablesList.size()) return;
        PayableDTO pa = owner.payablesList.get(modelRow);
        boolean inDebt = pa.outstanding() != null && pa.outstanding().signum() > 0;
        String currency = currency(pa);
        peek.setHeaderIcon("fas-file-invoice-dollar", UIHelper.REJECTED_RED);
        peek.setTitle("Compra " + (pa.purchaseNumber() != null ? pa.purchaseNumber() : ""));
        peek.setSubtitle(pa.supplierName() != null ? pa.supplierName() : "-");
        peek.setStatus(inDebt ? "EM DÍVIDA" : "LIQUIDADO", inDebt ? UIHelper.REJECTED_RED : UIHelper.APPROVED_GREEN);

        List<QuickPeekPanel.PeekItem> items = new ArrayList<>();
        items.add(new QuickPeekPanel.PeekItem("Linha de Negócio", pa.businessUnitName() != null ? pa.businessUnitName() : "Geral", false));
        items.add(new QuickPeekPanel.PeekItem("Fornecedor", pa.supplierName() != null ? pa.supplierName() : "-", false));
        items.add(new QuickPeekPanel.PeekItem("Moeda", pa.exchangeRate() == null ? currency
                : currency + " · câmbio " + DisplayNumbers.rate(pa.exchangeRate()), false));
        items.add(new QuickPeekPanel.PeekItem("Total da Compra", amount(pa.totalAmount(), currency), false));
        items.add(new QuickPeekPanel.PeekItem("Total Já Pago", amount(pa.amountPaid(), currency), false));
        items.add(new QuickPeekPanel.PeekItem("Data da Compra", pa.purchaseDate() != null ? pa.purchaseDate().format(DATE_FORMATTER) : "-", false));
        items.add(new QuickPeekPanel.PeekItem("Em Dívida (moeda base)", DisplayNumbers.money(baseOutstanding(pa)), false));
        items.add(new QuickPeekPanel.PeekItem("Valor em Dívida", amount(pa.outstanding(), currency), true));
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
            BigDecimal baseOutstanding = baseOutstanding(pa);
            totalDivida = totalDivida.add(baseOutstanding);
            owner.payablesModel.addRow(new Object[]{
                    pa.purchaseNumber(),
                    bu,
                    pa.supplierName(),
                    currency(pa),
                    pa.totalAmount(), pa.amountPaid(), pa.outstanding(), baseOutstanding,
                    pa.purchaseDate() == null ? "-" : pa.purchaseDate().format(DATE_FORMATTER)});
        }
        Object sel = businessUnitCombo.getSelectedItem();
        businessUnitCombo.removeAllItems();
        businessUnitCombo.addItem("Todos os negócios");
        for (String b : bus) businessUnitCombo.addItem(b);
        if (sel != null) businessUnitCombo.setSelectedItem(sel);
        payablesFooter.setText(String.format("%d fatura(s) em dívida · Total a pagar (moeda base): %s",
                owner.payablesList.size(), DisplayNumbers.money(totalDivida)));
    }

    /** Compra sem moeda (servidor anterior à moeda estrangeira) está na moeda base. */
    private static String currency(PayableDTO pa) {
        return pa.currency() == null || pa.currency().isBlank() ? CurrencyCode.DEFAULT_CODE : pa.currency();
    }

    private static BigDecimal baseOutstanding(PayableDTO pa) {
        if (pa.baseOutstanding() != null) return pa.baseOutstanding();
        return pa.outstanding() != null ? pa.outstanding() : BigDecimal.ZERO;
    }

    private static String amount(BigDecimal value, String currency) {
        return DisplayNumbers.money(value) + " " + currency;
    }
}
