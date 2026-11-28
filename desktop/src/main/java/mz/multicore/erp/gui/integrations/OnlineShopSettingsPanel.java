package mz.multicore.erp.gui.integrations;

import mz.multicore.erp.desktop.client.OnlineShopApiClient;
import mz.multicore.erp.desktop.session.SignedInUser;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopDeliveryZoneDTO;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopSettingsDTO;
import mz.multicore.erp.modules.onlineshop.dto.SaveOnlineShopSettingsRequest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Configurações › Integrações › Loja online (docs/LOJA_ONLINE_SPEC.md §3.5, LO-UI-01): the ADMIN switches the online
 * shop on, picks its public code (address {@code /loja/{código}}), the pickup stores among the company's sales
 * warehouses, the delivery zones with their fee, the contact phone and the auto-cancel days (7 by default). The
 * backend validates (LO-01) and its messages are shown unchanged; another role requests nothing and sees the notice.
 */
public class OnlineShopSettingsPanel extends JPanel {

    static final String ADMIN_ONLY = "Só o administrador gere a loja online.";
    static final String ADDRESS_PREFIX = "Endereço: /loja/";
    static final int DEFAULT_AUTO_CANCEL_DAYS = 7;

    private final OnlineShopApiClient onlineShopApiClient;
    private final JCheckBox enabledBox = checkBox("Loja activa");
    private final JTextField codeField = new JTextField(20);
    private final JLabel addressLabel = caption(addressPreview(""));
    private final JPanel pickupsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, Spacing.MD, 0));
    private final Map<Long, JCheckBox> pickupBoxes = new LinkedHashMap<>();
    private final List<WarehouseDTO> warehouses = new ArrayList<>();
    private final DefaultTableModel zonesModel = new DefaultTableModel(new String[]{"Nome", "Taxa (MT)"}, 0);
    private final JTable zonesTable = new JTable(zonesModel);
    private final JTextField phoneField = new JTextField(15);
    private final JSpinner daysSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_AUTO_CANCEL_DAYS, 1, 365, 1));
    private final JLabel statusLabel = caption("");
    private final ModernButton addZoneBtn = UIHelper.createSecondaryButton("Adicionar zona");
    private final ModernButton removeZoneBtn = UIHelper.createSecondaryButton("Remover zona");
    private final ModernButton saveBtn = UIHelper.createSuccessButton("Guardar");

    public OnlineShopSettingsPanel(OnlineShopApiClient onlineShopApiClient) {
        this.onlineShopApiClient = onlineShopApiClient;
        setLayout(new BorderLayout());
        setOpaque(false);
        add(buildCard(), BorderLayout.CENTER);
        UIHelper.onTextChange(codeField, () -> addressLabel.setText(addressPreview(codeField.getText())));
        zonesTable.getSelectionModel().addListSelectionListener(e -> updateActions());
        updateActions();
    }

    private JComponent buildCard() {
        saveBtn.setIcon(UIHelper.icon("fas-save", 14));
        saveBtn.addActionListener(e -> save());
        addZoneBtn.setIcon(UIHelper.icon("fas-plus", 14));
        addZoneBtn.addActionListener(e -> addZone());
        removeZoneBtn.setIcon(UIHelper.icon("fas-trash-alt", 14));
        removeZoneBtn.addActionListener(e -> removeSelectedZone());
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refresh);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, Spacing.SM));
        card.setBorder(new EmptyBorder(Spacing.XL, Spacing.XL, Spacing.XL, Spacing.XL));
        card.add(UIHelper.tableCardTop("Loja online", statusLabel, saveBtn, refreshBtn), BorderLayout.NORTH);
        card.add(buildForm(), BorderLayout.CENTER);
        JLabel hint = caption("Os clientes encomendam em /loja/{código}, levantam numa loja ou recebem em casa "
                + "e pagam ao receber.");
        hint.setIcon(UIHelper.icon("fas-store", 14, UIHelper.TEXT_MUTED));
        card.add(hint, BorderLayout.SOUTH);
        return card;
    }

    private JComponent buildForm() {
        UIHelper.styleTextField(codeField);
        UIHelper.styleTextField(phoneField);
        UIHelper.styleSpinner(daysSpinner);
        UIHelper.styleTable(zonesTable);
        zonesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        zonesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        pickupsPanel.setOpaque(false);
        JScrollPane zonesScroll = new JScrollPane(zonesTable);
        UIHelper.styleScrollPane(zonesScroll);
        zonesScroll.setPreferredSize(new Dimension(360, 110));
        JPanel zoneActions = new JPanel(new FlowLayout(FlowLayout.LEFT, Spacing.SM, 0));
        zoneActions.setOpaque(false);
        zoneActions.add(addZoneBtn);
        zoneActions.add(removeZoneBtn);
        JPanel zones = new JPanel(new BorderLayout(0, Spacing.XS));
        zones.setOpaque(false);
        zones.add(zonesScroll, BorderLayout.CENTER);
        zones.add(zoneActions, BorderLayout.SOUTH);
        JPanel code = new JPanel(new FlowLayout(FlowLayout.LEFT, Spacing.SM, 0));
        code.setOpaque(false);
        code.add(codeField);
        code.add(addressLabel);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        int row = 0;
        addRow(form, row++, null, enabledBox);
        addRow(form, row++, "Código público", code);
        addRow(form, row++, "Lojas de levantamento", pickupsPanel);
        addRow(form, row++, "Zonas de entrega", zones);
        addRow(form, row++, "Telefone de contacto", phoneField);
        addRow(form, row, "Prazo de cancelamento (dias)", daysSpinner);
        return form;
    }

    private static void addRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(Spacing.XS, 0, Spacing.XS, Spacing.MD);
        c.anchor = GridBagConstraints.NORTHWEST;
        if (label != null) {
            c.gridx = 0;
            JLabel text = new JLabel(label);
            text.setFont(Typography.BODY);
            text.setForeground(UIHelper.TEXT_LIGHT);
            form.add(text, c);
        }
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, c);
    }

    /** Reloads the settings and the pickup candidates; for a non-ADMIN nothing is requested (§3.5). */
    public void refresh() {
        if (!SignedInUser.isAdmin()) {
            showAdminOnly();
            return;
        }
        UIHelper.loadAsync(this, () -> new Loaded(onlineShopApiClient.salesWarehouses(), onlineShopApiClient.settings()),
                loaded -> apply(loaded.warehouses(), loaded.settings()),
                error -> ToastManager.show(this, FeedbackType.ERROR, error.getMessage()));
    }

    private record Loaded(List<WarehouseDTO> warehouses, OnlineShopSettingsDTO settings) {
    }

    private void showAdminOnly() {
        apply(List.of(), null);
        statusLabel.setText(ADMIN_ONLY);
    }

    void apply(List<WarehouseDTO> salesWarehouses, OnlineShopSettingsDTO settings) {
        List<Long> chosen = settings == null || settings.pickupWarehouseIds() == null
                ? List.of() : settings.pickupWarehouseIds();
        List<WarehouseDTO> candidates = salesWarehouses == null ? List.of() : List.copyOf(salesWarehouses);
        warehouses.clear();
        warehouses.addAll(candidates);
        pickupBoxes.clear();
        pickupsPanel.removeAll();
        for (WarehouseDTO warehouse : warehouses) {
            JCheckBox box = checkBox(String.valueOf(warehouse));
            box.setSelected(chosen.contains(warehouse.id()));
            pickupBoxes.put(warehouse.id(), box);
            pickupsPanel.add(box);
        }
        pickupsPanel.revalidate();
        pickupsPanel.repaint();
        enabledBox.setSelected(settings != null && settings.enabled());
        codeField.setText(settings == null || settings.publicCode() == null ? "" : settings.publicCode());
        phoneField.setText(settings == null || settings.contactPhone() == null ? "" : settings.contactPhone());
        daysSpinner.setValue(settings == null || settings.autoCancelDays() < 1
                ? DEFAULT_AUTO_CANCEL_DAYS : settings.autoCancelDays());
        zonesModel.setRowCount(0);
        if (settings != null && settings.deliveryZones() != null) {
            settings.deliveryZones().forEach(zone -> zonesModel.addRow(new Object[]{zone.name(),
                    zone.fee() == null ? "" : zone.fee().toPlainString()}));
        }
        statusLabel.setText(settings == null ? "" : settings.enabled()
                ? "Activa em /loja/" + settings.publicCode() : "Desactivada");
        updateActions();
    }

    /** The request built from the form; the backend validates it (LO-01). */
    SaveOnlineShopSettingsRequest request() {
        if (zonesTable.isEditing()) zonesTable.getCellEditor().stopCellEditing();
        List<Long> pickups = new ArrayList<>();
        pickupBoxes.forEach((id, box) -> {
            if (box.isSelected()) pickups.add(id);
        });
        List<OnlineShopDeliveryZoneDTO> zones = new ArrayList<>();
        for (int row = 0; row < zonesModel.getRowCount(); row++) {
            Object name = zonesModel.getValueAt(row, 0);
            zones.add(new OnlineShopDeliveryZoneDTO(name == null ? "" : name.toString().trim(),
                    parseFee(zonesModel.getValueAt(row, 1))));
        }
        return new SaveOnlineShopSettingsRequest(enabledBox.isSelected(), codeField.getText().trim(), pickups, zones,
                phoneField.getText().trim(), ((Number) daysSpinner.getValue()).intValue());
    }

    /** "150", "150,50" or "1.460,00" in MT; unreadable means no fee, which the backend refuses with its message. */
    static BigDecimal parseFee(Object raw) {
        String text = raw == null ? "" : raw.toString().replace(" ", "").replace("MT", "").trim();
        if (text.contains(",")) text = text.replace(".", "").replace(',', '.');
        try {
            return text.isEmpty() ? null : new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String addressPreview(String code) {
        String trimmed = code == null ? "" : code.trim().toLowerCase(Locale.ROOT);
        return ADDRESS_PREFIX + (trimmed.isEmpty() ? "{código}" : trimmed);
    }

    private void save() {
        if (!SignedInUser.isAdmin()) {
            ToastManager.show(this, FeedbackType.WARNING, ADMIN_ONLY);
            return;
        }
        SaveOnlineShopSettingsRequest request = request();
        UIHelper.submitAsync(saveBtn, () -> onlineShopApiClient.saveSettings(request), saved -> {
            apply(warehouses, saved);
            ToastManager.show(this, FeedbackType.SUCCESS, "Definições da loja online gravadas.");
        }, error -> ToastManager.show(this, FeedbackType.ERROR, error.getMessage()));
    }

    private void addZone() {
        zonesModel.addRow(new Object[]{"", ""});
        int added = zonesModel.getRowCount() - 1;
        // The table's sorter/pagination may refresh after this event: select and edit once the view caught up.
        SwingUtilities.invokeLater(() -> {
            int row = added < zonesModel.getRowCount() ? zonesTable.convertRowIndexToView(added) : -1;
            if (row < 0 || row >= zonesTable.getRowCount()) return;
            zonesTable.setRowSelectionInterval(row, row);
            zonesTable.editCellAt(row, 0);
        });
    }

    private void removeSelectedZone() {
        int row = zonesTable.getSelectedRow();
        if (row < 0) return;
        if (zonesTable.isEditing()) zonesTable.getCellEditor().cancelCellEditing();
        zonesModel.removeRow(zonesTable.convertRowIndexToModel(row));
    }

    private void updateActions() {
        boolean admin = SignedInUser.isAdmin();
        for (JComponent field : List.of(enabledBox, codeField, phoneField, daysSpinner, zonesTable, addZoneBtn, saveBtn)) {
            field.setEnabled(admin);
        }
        pickupBoxes.values().forEach(box -> box.setEnabled(admin));
        removeZoneBtn.setEnabled(admin && zonesTable.getSelectedRow() >= 0);
    }

    private static JCheckBox checkBox(String text) {
        JCheckBox box = new JCheckBox(text);
        box.setOpaque(false);
        box.setFont(Typography.BODY);
        box.setForeground(UIHelper.TEXT_LIGHT);
        return box;
    }

    private static JLabel caption(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Typography.CAPTION);
        label.setForeground(UIHelper.TEXT_MUTED);
        return label;
    }

    JCheckBox enabledBox() { return enabledBox; }
    JTextField codeField() { return codeField; }
    JLabel addressLabel() { return addressLabel; }
    Map<Long, JCheckBox> pickupBoxes() { return pickupBoxes; }
    JTable zonesTable() { return zonesTable; }
    JTextField phoneField() { return phoneField; }
    JSpinner daysSpinner() { return daysSpinner; }
    JLabel statusLabel() { return statusLabel; }
    ModernButton addZoneButton() { return addZoneBtn; }
    ModernButton removeZoneButton() { return removeZoneBtn; }
    ModernButton saveButton() { return saveBtn; }
}
