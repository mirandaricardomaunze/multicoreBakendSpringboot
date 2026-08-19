package mz.multicore.erp.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CommercialMultiuserRefreshHarnessTest {

    @Test
    void criticalCommercialViewsExposeCanonicalRefreshAction() throws IOException {
        assertContains("POSPanel.java", "createRefreshButton(this::refreshOperationalData)");
        assertContains("CommercialInvoicesView.java", "createRefreshButton(owner::loadInvoicesTable)");
        assertContains("CommercialOrdersView.java", "createRefreshButton(owner::loadOrdersTable)");
        assertContains("commercial/DeliveryGuidesPanel.java", "createRefreshButton(this::refresh)");
        assertContains("commercial/CommercialNotesPanel.java", "createRefreshButton(this::loadCredits)");
        assertContains("commercial/CommercialNotesPanel.java", "createRefreshButton(this::loadDebits)");
    }

    private static void assertContains(String relative, String expected) throws IOException {
        Path source = Path.of("src/main/java/mz/multicore/erp/gui").resolve(relative);
        assertTrue(Files.readString(source).contains(expected), () -> "Acção ausente em " + source);
    }
}
