package mz.multicore.erp.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentPackageQuantityCoverageHarnessTest {

    @Test
    void allCurrentProductLineEditorsUseCanonicalPackageEditor() throws IOException {
        assertInlinePackageGrid("CommercialInvoicesView.java");
        assertInlinePackageGrid("CommercialOrdersView.java");
        assertOccurrences("ComprasPanel.java", "new PackageQuantityEditor()", 1);
        assertOccurrences("PurchaseOrdersPanel.java", "new PackageQuantityEditor()", 1);
    }

    private static void assertInlinePackageGrid(String file) throws IOException {
        String source = Files.readString(Path.of("src/main/java/mz/multicore/erp/gui", file));
        assertTrue(source.contains("\"Emb.\"") && source.contains("\"Cx.\"")
                        && source.contains("sync") && source.contains("Grid"),
                () -> "Grelha canónica de embalagem ausente em " + file);
    }

    private static void assertOccurrences(String file, String token, int minimum) throws IOException {
        String source = Files.readString(Path.of("src/main/java/mz/multicore/erp/gui", file));
        int count = source.split(java.util.regex.Pattern.quote(token), -1).length - 1;
        assertTrue(count >= minimum, () -> "Editor canónico ausente em " + file);
    }
}
