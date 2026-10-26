package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.QuantityField;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.QuotationDTO;
import mz.multicore.erp.modules.comercial.model.QuotationValidity;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Gestor do cabeçalho de cotação comercial (cliente, armazém, validade e prazos).
 */
final class QuotationEditorHeaderForm {

    private final JComboBox<String> clientCombo = new JComboBox<>();
    private final JTextField walkInField = new JTextField();
    private final JComboBox<String> warehouseCombo = new JComboBox<>();
    private final QuantityField validityField = new QuantityField(String.valueOf(QuotationValidity.DEFAULT_DAYS), true);
    private final JTextField paymentTermsField = new JTextField();
    private final JTextField deliveryTermsField = new JTextField();
    private final JTextField deliveryDaysField = new JTextField();
    private final JTextField notesField = new JTextField();

    private List<ClientDTO> clients = List.of();
    private List<WarehouseDTO> warehouses = List.of();
    private boolean loading;

    QuotationEditorHeaderForm() {
        UIHelper.styleComboBox(clientCombo);
        UIHelper.styleComboBox(warehouseCombo);
        for (JTextField f : List.of(walkInField, paymentTermsField, deliveryTermsField, deliveryDaysField, notesField)) {
            UIHelper.styleTextField(f);
        }
        walkInField.putClientProperty("JTextField.placeholderText", "Nome do comprador, se não for cliente registado");
        paymentTermsField.putClientProperty("JTextField.placeholderText", "Ex.: Pronto pagamento");
        deliveryTermsField.putClientProperty("JTextField.placeholderText", "Ex.: Entrega imediata");
        deliveryDaysField.putClientProperty("JTextField.placeholderText", "Dias");
        notesField.putClientProperty("JTextField.placeholderText", "Observações gerais da cotação...");
    }

    JPanel buildPanel() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        row1.setOpaque(false);
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);
        row1.add(fieldGroup("Cliente:", clientCombo, 260));
        row1.add(fieldGroup("Comprador (opcional):", walkInField, 220));
        row1.add(fieldGroup("Armazém:", warehouseCombo, 180));
        row1.add(fieldGroup("Validade (dias):", validityField, 110));

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        row2.setOpaque(false);
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);
        row2.add(fieldGroup("Condições de pagamento:", paymentTermsField, 190));
        row2.add(fieldGroup("Prazo de entrega:", deliveryTermsField, 160));
        row2.add(fieldGroup("Dias:", deliveryDaysField, 80));
        row2.add(fieldGroup("Observações:", notesField, 320));

        card.add(row1);
        card.add(Box.createVerticalStrut(6));
        card.add(row2);
        return card;
    }

    private static JPanel fieldGroup(String label, JComponent comp, int width) {
        JPanel group = new JPanel(new BorderLayout(0, 4));
        group.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        lbl.setForeground(UIHelper.TEXT_MUTED);
        group.add(lbl, BorderLayout.NORTH);
        if (width > 0) comp.setPreferredSize(new Dimension(width, 36));
        group.add(comp, BorderLayout.CENTER);
        return group;
    }

    void setOptions(List<ClientDTO> clients, List<WarehouseDTO> warehouses) {
        this.clients = clients == null ? List.of() : clients;
        this.warehouses = warehouses == null ? List.of() : warehouses;
        loading = true;
        clientCombo.removeAllItems();
        clientCombo.addItem("Consumidor Final (sem registo)");
        this.clients.forEach(c -> clientCombo.addItem(c.name() + " (" + c.taxId() + ")"));
        warehouseCombo.removeAllItems();
        this.warehouses.forEach(w -> warehouseCombo.addItem(w.name()));
        loading = false;
    }

    void prepareCreate() {
        loading = true;
        if (clientCombo.getItemCount() > 0) clientCombo.setSelectedIndex(0);
        if (warehouseCombo.getItemCount() > 0) warehouseCombo.setSelectedIndex(0);
        walkInField.setText("");
        validityField.setText(String.valueOf(QuotationValidity.DEFAULT_DAYS));
        paymentTermsField.setText("");
        deliveryTermsField.setText("");
        deliveryDaysField.setText("");
        notesField.setText("");
        loading = false;
    }

    void load(QuotationDTO quotation) {
        loading = true;
        selectClient(quotation);
        selectWarehouse(quotation.warehouseId());
        walkInField.setText(nz(quotation.walkInName()));
        long days = quotation.validUntil() == null ? QuotationValidity.DEFAULT_DAYS
                : Math.max(1, ChronoUnit.DAYS.between(LocalDate.now(), quotation.validUntil()));
        validityField.setText(String.valueOf(days));
        paymentTermsField.setText(nz(quotation.paymentTerms()));
        deliveryTermsField.setText(nz(quotation.deliveryTerms()));
        deliveryDaysField.setText(quotation.deliveryDays() == null ? "" : quotation.deliveryDays().toString());
        notesField.setText(nz(quotation.notes()));
        loading = false;
    }

    HeaderValues validateHeader() {
        int wh = warehouseCombo.getSelectedIndex();
        if (wh < 0 || wh >= warehouses.size()) throw new RuntimeException("Selecione o armazém.");
        int validity;
        try {
            validity = validityField.value().intValueExact();
            if (validity <= 0) throw new NumberFormatException();
        } catch (RuntimeException e) {
            throw new RuntimeException("A validade deve ser um número inteiro superior a zero.");
        }
        Integer deliveryDays = null;
        if (!deliveryDaysField.getText().isBlank()) {
            try { deliveryDays = Integer.valueOf(deliveryDaysField.getText().trim()); }
            catch (NumberFormatException e) { throw new RuntimeException("Os dias de entrega devem ser um número inteiro."); }
            if (deliveryDays <= 0) throw new RuntimeException("O prazo de entrega deve ser de pelo menos um dia.");
        }
        int client = clientCombo.getSelectedIndex();
        Long clientId = client > 0 && client <= clients.size() ? clients.get(client - 1).id() : null;
        return new HeaderValues(clientId, blank(walkInField.getText()), warehouses.get(wh).id(), validity,
                blank(paymentTermsField.getText()), blank(deliveryTermsField.getText()), deliveryDays, blank(notesField.getText()));
    }

    void setEditable(boolean editable) {
        clientCombo.setEnabled(editable); walkInField.setEnabled(editable); warehouseCombo.setEnabled(editable);
        validityField.setEnabled(editable); paymentTermsField.setEnabled(editable);
        deliveryTermsField.setEnabled(editable); deliveryDaysField.setEnabled(editable); notesField.setEnabled(editable);
    }

    void installDirtyTracking(Runnable onDirty) {
        DocumentListener listener = new DocumentListener() {
            private void changed() { if (!loading) onDirty.run(); }
            @Override public void insertUpdate(DocumentEvent e) { changed(); }
            @Override public void removeUpdate(DocumentEvent e) { changed(); }
            @Override public void changedUpdate(DocumentEvent e) { changed(); }
        };
        for (javax.swing.text.JTextComponent field : List.of(walkInField, validityField,
                paymentTermsField, deliveryTermsField, deliveryDaysField, notesField)) {
            field.getDocument().addDocumentListener(listener);
        }
        clientCombo.addActionListener(e -> { if (!loading) onDirty.run(); });
        warehouseCombo.addActionListener(e -> { if (!loading) onDirty.run(); });
    }

    private void selectClient(QuotationDTO quotation) {
        int selected = 0;
        if (quotation.walkInName() == null || quotation.walkInName().isBlank()) {
            for (int i = 0; i < clients.size(); i++) {
                if (clients.get(i).id().equals(quotation.clientId())) { selected = i + 1; break; }
            }
        }
        if (clientCombo.getItemCount() > selected) clientCombo.setSelectedIndex(selected);
    }

    private void selectWarehouse(Long id) {
        for (int i = 0; i < warehouses.size(); i++) {
            if (warehouses.get(i).id().equals(id)) { warehouseCombo.setSelectedIndex(i); return; }
        }
    }

    private static String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static String nz(String value) { return value == null ? "" : value; }

    record HeaderValues(Long clientId, String walkInName, Long warehouseId, int validityDays,
                        String paymentTerms, String deliveryTerms, Integer deliveryDays, String notes) {}
}
