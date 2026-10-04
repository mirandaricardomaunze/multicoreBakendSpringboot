package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PackageQuantityEditorTest {

    @Test
    void boxesAndLooseUnitsUpdateTotalBidirectionally() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PackageQuantityEditor editor = new PackageQuantityEditor();
            editor.setPackaging(4, 3);
            editor.boxesField().setText("2");
            editor.packagesField().setText("1");
            editor.looseUnitsField().setText("2");
            assertEquals("29", editor.totalField().getText());

            editor.totalField().setText("41");
            assertEquals("3", editor.boxesField().getText());
            assertEquals("1", editor.packagesField().getText());
            assertEquals("2", editor.looseUnitsField().getText());
        });
    }

    @Test
    void resetRemovesResidualLooseUnits() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PackageQuantityEditor editor = new PackageQuantityEditor();
            editor.setPackaging(4, 3);
            editor.totalField().setText("1");
            editor.reset();
            editor.boxesField().setText("2");
            assertEquals("24", editor.totalField().getText());
            assertEquals("0", editor.packagesField().getText());
            assertEquals("0", editor.looseUnitsField().getText());
        });
    }
}
