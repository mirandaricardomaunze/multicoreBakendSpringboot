package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness de Decomposição de ProductLookupDialog (Fase 84)")
class ProductLookupDialogDecompositionHarnessTest {

    private static final Path SOURCE_DIR = Path.of("src/main/java/mz/multicore/erp/gui/components");

    @Test
    @DisplayName("Garante limites físicos estritos para ProductLookupDialog e colaboradores")
    void testPhysicalLineLimits() throws IOException {
        Path dialogFile = SOURCE_DIR.resolve("ProductLookupDialog.java");
        Path helperFile = SOURCE_DIR.resolve("ProductLookupTableHelper.java");
        Path localFile = SOURCE_DIR.resolve("ProductLookupLocalDialog.java");
        Path remoteFile = SOURCE_DIR.resolve("ProductLookupRemoteDialog.java");

        assertTrue(Files.exists(dialogFile), "ProductLookupDialog.java deve existir");
        assertTrue(Files.exists(helperFile), "ProductLookupTableHelper.java deve existir");
        assertTrue(Files.exists(localFile), "ProductLookupLocalDialog.java deve existir");
        assertTrue(Files.exists(remoteFile), "ProductLookupRemoteDialog.java deve existir");

        List<String> dialogLines = Files.readAllLines(dialogFile);
        List<String> helperLines = Files.readAllLines(helperFile);
        List<String> localLines = Files.readAllLines(localFile);
        List<String> remoteLines = Files.readAllLines(remoteFile);

        assertTrue(dialogLines.size() <= 120, "ProductLookupDialog deve ter <= 120 linhas (atual: " + dialogLines.size() + ")");
        assertTrue(helperLines.size() <= 110, "ProductLookupTableHelper deve ter <= 110 linhas (atual: " + helperLines.size() + ")");
        assertTrue(localLines.size() <= 190, "ProductLookupLocalDialog deve ter <= 190 linhas (atual: " + localLines.size() + ")");
        assertTrue(remoteLines.size() <= 240, "ProductLookupRemoteDialog deve ter <= 240 linhas (atual: " + remoteLines.size() + ")");
    }

    @Test
    @DisplayName("Proíbe o uso de JOptionPane nos componentes de lookup de produtos")
    void testNoJOptionPaneUsage() throws IOException {
        Path dialogFile = SOURCE_DIR.resolve("ProductLookupDialog.java");
        Path helperFile = SOURCE_DIR.resolve("ProductLookupTableHelper.java");
        Path localFile = SOURCE_DIR.resolve("ProductLookupLocalDialog.java");
        Path remoteFile = SOURCE_DIR.resolve("ProductLookupRemoteDialog.java");

        for (Path p : List.of(dialogFile, helperFile, localFile, remoteFile)) {
            String content = Files.readString(p);
            assertFalse(content.contains("JOptionPane"), "Ficheiro " + p.getFileName() + " não pode conter JOptionPane");
        }
    }

    @Test
    @DisplayName("Garante que contratos públicos essenciais de ProductLookupDialog estão preservados")
    void testPublicContractsPreserved() {
        assertDoesNotThrow(() -> {
            Class<?> providerIface = ProductLookupDialog.ProductPageProvider.class;
            assertTrue(providerIface.isInterface());
            Class<?> optionClass = ProductLookupDialog.CategoryOption.class;
            assertTrue(optionClass.isRecord());

            ProductLookupDialog.class.getMethod("show", java.awt.Component.class, List.class);
            ProductLookupDialog.class.getMethod("showMulti", java.awt.Component.class, List.class);
            ProductLookupDialog.class.getMethod("showRemote", java.awt.Component.class, ProductLookupDialog.ProductPageProvider.class);
            ProductLookupDialog.class.getMethod("showMultiRemote", java.awt.Component.class, ProductLookupDialog.ProductPageProvider.class);
        });
    }

    @Test
    @DisplayName("Garante formatação segura de embalagens em ProductLookupTableHelper")
    void testPackagingFormatting() {
        assertEquals("—", ProductLookupTableHelper.formatPackaging(null));
        assertEquals("comercial", ProductLookupTableHelper.normalize("COMÉRCIAL"));
    }
}
