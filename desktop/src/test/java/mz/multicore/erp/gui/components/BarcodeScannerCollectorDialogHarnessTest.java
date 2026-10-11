package mz.multicore.erp.gui.components;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.ProductionApiClient;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class BarcodeScannerCollectorDialogHarnessTest {

    @Test
    void instantiateCollectorDialog_bothModes_noExceptions() {
        ComercialApiClient comercial = mock(ComercialApiClient.class);
        ProductionApiClient production = mock(ProductionApiClient.class);
        InventoryApiClient inventory = mock(InventoryApiClient.class);

        JFrame frame = new JFrame();

        var mfgDialog = new BarcodeScannerCollectorDialog(
                frame,
                BarcodeScannerCollectorDialog.CollectorMode.MANUFACTURING,
                comercial,
                production,
                inventory,
                () -> {}
        );
        assertNotNull(mfgDialog);

        var invDialog = new BarcodeScannerCollectorDialog(
                frame,
                BarcodeScannerCollectorDialog.CollectorMode.INVENTORY,
                comercial,
                null,
                inventory,
                () -> {}
        );
        assertNotNull(invDialog);
    }
}
