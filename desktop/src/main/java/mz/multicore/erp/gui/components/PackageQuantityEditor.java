package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.quantity.PackageQuantity;

import javax.swing.*;
import java.awt.*;

/** Editor reutilizável de quantidade total, caixas completas e unidades soltas. */
public final class PackageQuantityEditor extends JPanel {
    private final QuantityField totalField = new QuantityField("0", true);
    private final QuantityField boxesField = new QuantityField("0", false);
    private final QuantityField looseUnitsField = new QuantityField("0", false);
    private int unitsPerBox = 1;
    private boolean updating;

    public PackageQuantityEditor() {
        super(new GridLayout(1, 3, 6, 0));
        setOpaque(false);
        add(field("Total", totalField));
        add(field("Caixas", boxesField));
        add(field("Soltas", looseUnitsField));
        UIHelper.onTextChange(totalField, this::totalChanged);
        UIHelper.onTextChange(boxesField, this::packagesChanged);
        UIHelper.onTextChange(looseUnitsField, this::packagesChanged);
        updateTooltips();
    }

    public QuantityField totalField() { return totalField; }
    public QuantityField boxesField() { return boxesField; }
    public QuantityField looseUnitsField() { return looseUnitsField; }
    public int unitsPerBox() { return unitsPerBox; }

    public void setUnitsPerBox(int unitsPerBox) {
        this.unitsPerBox = Math.max(1, unitsPerBox);
        updateTooltips();
        totalChanged();
    }

    public void reset() {
        updating = true;
        try {
            totalField.setText("0");
            boxesField.setText("0");
            looseUnitsField.setText("0");
        } finally {
            updating = false;
        }
    }

    private JPanel field(String label, QuantityField input) {
        JPanel panel = new JPanel(new BorderLayout(4, 0));
        panel.setOpaque(false);
        JLabel caption = new JLabel(label + ":");
        caption.setForeground(UIHelper.TEXT_MUTED);
        panel.add(caption, BorderLayout.WEST);
        panel.add(input, BorderLayout.CENTER);
        return panel;
    }

    private void packagesChanged() {
        if (updating) return;
        try {
            updating = true;
            PackageQuantity value = PackageQuantity.fromPackages(value(boxesField), value(looseUnitsField), unitsPerBox);
            totalField.setText(String.valueOf(value.totalUnits()));
            clearValidation();
        } catch (RuntimeException exception) {
            UIHelper.markFieldInvalid(looseUnitsField, exception.getMessage());
        } finally {
            updating = false;
        }
    }

    private void totalChanged() {
        if (updating) return;
        try {
            updating = true;
            PackageQuantity value = PackageQuantity.fromTotal(value(totalField), unitsPerBox);
            boxesField.setText(String.valueOf(value.boxes()));
            looseUnitsField.setText(String.valueOf(value.looseUnits()));
            clearValidation();
        } catch (RuntimeException exception) {
            UIHelper.markFieldInvalid(totalField, exception.getMessage());
        } finally {
            updating = false;
        }
    }

    private long value(JTextField field) {
        String text = field.getText();
        return text == null || text.isBlank() ? 0 : Long.parseLong(text.trim());
    }

    private void updateTooltips() {
        totalField.setToolTipText("Quantidade total em unidades; " + unitsPerBox + " unidade(s) por caixa.");
        boxesField.setToolTipText("Caixas completas de " + unitsPerBox + " unidade(s).");
        looseUnitsField.setToolTipText("Unidades soltas adicionais, entre 0 e " + (unitsPerBox - 1) + ".");
        getAccessibleContext().setAccessibleName("Quantidade: total, caixas e unidades soltas");
    }

    private void clearValidation() {
        UIHelper.clearFieldInvalid(totalField);
        UIHelper.clearFieldInvalid(boxesField);
        UIHelper.clearFieldInvalid(looseUnitsField);
    }
}
