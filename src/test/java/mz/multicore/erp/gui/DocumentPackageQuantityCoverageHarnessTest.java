package mz.multicore.erp.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentPackageQuantityCoverageHarnessTest {

    @Test
    void allCurrentProductLineEditorsUseCanonicalPackageEditor() throws IOException {
        assertOccurrences("CommercialInvoicesView.java", "new PackageQuantityEditor()", 1);
        assertOccurrences("CommercialOrdersView.java", "new PackageQuantityEditor()", 1);
        assertOccurrences("ComprasPanel.java", "new PackageQuantityEditor()", 1);
        assertOccurrences("PurchaseOrdersPanel.java", "new PackageQuantityEditor()", 1);
    }

    private static void assertOccurrences(String file, String token, int minimum) throws IOException {
        String source = Files.readString(Path.of("src/main/java/mz/multicore/erp/gui", file));
        int count = source.split(java.util.regex.Pattern.quote(token), -1).length - 1;
        assertTrue(count >= minimum, () -> "Editor canónico ausente em " + file);
    }
}
