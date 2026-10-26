package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.gui.components.QuickPeekPanel;
import mz.multicore.erp.modules.comercial.dto.QuotationDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Coordenador de apresentação, formatação de validade e Quick Peek de Cotações. */
public final class QuotationViewCoordinator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private QuotationViewCoordinator() {}

    public static void applyQuotations(DefaultTableModel model, JComboBox<String> buCombo, List<QuotationDTO> quotations) {
        if (model == null) return;
        model.setRowCount(0);
        Set<String> bus = new TreeSet<>();
        for (QuotationDTO q : quotations) {
            String buName = q.businessUnitName() != null && !q.businessUnitName().isBlank() ? q.businessUnitName() : "Misto";
            if (!"Misto".equals(buName)) bus.add(buName);
            model.addRow(new Object[]{
                    q.id(), q.quotationNumber(),
                    q.quotationDate() == null ? "—" : q.quotationDate().format(DATE_TIME),
                    q.clientName(), blank(q.clientTaxId()), q.totalAmount(), q.statusLabel(),
                    validityLabel(q), blank(q.invoiceNumber() != null ? q.invoiceNumber() : q.orderNumber()), buName
            });
        }
        if (buCombo != null) {
            Object prev = buCombo.getSelectedItem();
            buCombo.removeAllItems();
            buCombo.addItem("Todos os negócios");
            buCombo.addItem("Misto");
            for (String b : bus) buCombo.addItem(b);
            if (prev != null) buCombo.setSelectedItem(prev);
        }
    }

    public static String validityLabel(QuotationDTO q) {
        String until = q.validUntil() == null ? "—" : q.validUntil().format(DATE);
        if (q.expired()) return "Caducada em " + until;
        if (q.daysUntilExpiry() == 0) return "Caduca hoje (" + until + ")";
        return until + " (faltam " + q.daysUntilExpiry() + " dias)";
    }

    public static void populateQuickPeek(QuickPeekPanel peek, DefaultTableModel model, int mr, Runnable onOpenFull) {
        if (peek == null || model == null || mr < 0 || mr >= model.getRowCount()) return;
        String num = String.valueOf(model.getValueAt(mr, 1));
        String date = String.valueOf(model.getValueAt(mr, 2));
        String client = String.valueOf(model.getValueAt(mr, 3));
        String nuit = String.valueOf(model.getValueAt(mr, 4));
        String total = String.valueOf(model.getValueAt(mr, 5));
        String state = String.valueOf(model.getValueAt(mr, 6));
        String validity = String.valueOf(model.getValueAt(mr, 7));
        String bu = String.valueOf(model.getValueAt(mr, 9));

        peek.setTitle("Cotação " + num);
        peek.setSubtitle(client + (nuit != null && !nuit.isBlank() ? " • NUIT: " + nuit : ""));
        peek.setItems(List.of(
                new QuickPeekPanel.PeekItem("Data de Emissão", date, false),
                new QuickPeekPanel.PeekItem("Cliente", client, false),
                new QuickPeekPanel.PeekItem("Linha de Negócio", bu, false),
                new QuickPeekPanel.PeekItem("NUIT", nuit, false),
                new QuickPeekPanel.PeekItem("Validade", validity, false),
                new QuickPeekPanel.PeekItem("Valor Total", total, true),
                new QuickPeekPanel.PeekItem("Estado", state, false)
        ));
        peek.getOpenFullButton().setText("Editar / Consultar");
        peek.setOnOpenFullAction(v -> onOpenFull.run());
    }

    public static String blank(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
