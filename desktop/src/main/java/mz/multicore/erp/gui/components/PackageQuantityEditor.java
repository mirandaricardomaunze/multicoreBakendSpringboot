package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.quantity.PackagingComposition;
import mz.multicore.erp.architecture.quantity.PackagingQuantity;

import javax.swing.*;
import java.awt.*;

/** Editor reutilizável de quantidade total, caixas completas e unidades soltas. */
public final class PackageQuantityEditor extends JPanel {
    private final QuantityField totalField = new QuantityField("0", true);
    private final QuantityField boxesField = new QuantityField("0", false);
    private final QuantityField packagesField = new QuantityField("0", false);
    private final QuantityField looseUnitsField = new QuantityField("0", false);
    private PackagingComposition composition = PackagingComposition.of(1, 1);
    private boolean updating;

    public PackageQuantityEditor() {
        super(new GridLayout(1, 4, 6, 0));
        setOpaque(false);
        add(field("Total", totalField));
        add(field("Caixas", boxesField));
        add(field("Embalagens", packagesField));
        add(field("Unidades", looseUnitsField));
        UIHelper.onTextChange(totalField, this::totalChanged);
        UIHelper.onTextChange(boxesField, this::packagesChanged);
        UIHelper.onTextChange(packagesField, this::packagesChanged);
        UIHelper.onTextChange(looseUnitsField, this::packagesChanged);
        updateTooltips();
    }

    public QuantityField totalField() { return totalField; }
    public QuantityField boxesField() { return boxesField; }
    public QuantityField packagesField() { return packagesField; }
    public QuantityField looseUnitsField() { return looseUnitsField; }
    public int unitsPerBox() { return composition.unitsPerBox(); }
    public int packagesPerBox() { return composition.packagesPerBox(); }
    public int unitsPerPackage() { return composition.unitsPerPackage(); }

    public void setUnitsPerBox(int unitsPerBox) {
        setPackaging(Math.max(1, unitsPerBox), 1);
    }

    public void setPackaging(int packagesPerBox, int unitsPerPackage) {
        this.composition = PackagingComposition.of(packagesPerBox, unitsPerPackage);
        updateTooltips();
        totalChanged();
    }

    public void reset() {
        updating = true;
        try {
            totalField.setText("0");
            boxesField.setText("0");
            packagesField.setText("0");
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
            PackagingQuantity value = PackagingQuantity.fromPackaging(
                    value(boxesField), value(packagesField), value(looseUnitsField), composition);
            totalField.setText(String.valueOf(value.totalUnits()));
            clearValidation();
        } catch (RuntimeException exception) {
            UIHelper.markFieldInvalid(packagesField, exception.getMessage());
            UIHelper.markFieldInvalid(looseUnitsField, exception.getMessage());
        } finally {
            updating = false;
        }
    }

    private void totalChanged() {
        if (updating) return;
        try {
            updating = true;
            PackagingQuantity value = PackagingQuantity.fromTotal(value(totalField), composition);
            boxesField.setText(String.valueOf(value.boxes()));
            packagesField.setText(String.valueOf(value.packages()));
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
        totalField.setToolTipText("Quantidade total em unidades; " + composition.unitsPerBox() + " unidade(s) por caixa.");
        boxesField.setToolTipText("Caixas completas com " + composition.packagesPerBox() + " embalagem(ns).");
        packagesField.setToolTipText("Embalagens soltas, entre 0 e " + (composition.packagesPerBox() - 1) + ".");
        looseUnitsField.setToolTipText("Unidades soltas, entre 0 e " + (composition.unitsPerPackage() - 1) + ".");
        getAccessibleContext().setAccessibleName("Quantidade: total, caixas, embalagens e unidades soltas");
    }

    private void clearValidation() {
        UIHelper.clearFieldInvalid(totalField);
        UIHelper.clearFieldInvalid(boxesField);
        UIHelper.clearFieldInvalid(packagesField);
        UIHelper.clearFieldInvalid(looseUnitsField);
    }
}
