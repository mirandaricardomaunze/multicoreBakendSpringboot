package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PackageQuantityEditorTest {

    @Test
    void boxesAndLooseUnitsUpdateTotalBidirectionally() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PackageQuantityEditor editor = new PackageQuantityEditor();
            editor.setUnitsPerBox(12);
            editor.boxesField().setText("2");
            editor.looseUnitsField().setText("5");
            assertEquals("29", editor.totalField().getText());

            editor.totalField().setText("41");
            assertEquals("3", editor.boxesField().getText());
            assertEquals("5", editor.looseUnitsField().getText());
        });
    }

    @Test
    void resetRemovesResidualLooseUnits() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PackageQuantityEditor editor = new PackageQuantityEditor();
            editor.setUnitsPerBox(12);
            editor.totalField().setText("1");
            editor.reset();
            editor.boxesField().setText("2");
            assertEquals("24", editor.totalField().getText());
            assertEquals("0", editor.looseUnitsField().getText());
        });
    }
}
