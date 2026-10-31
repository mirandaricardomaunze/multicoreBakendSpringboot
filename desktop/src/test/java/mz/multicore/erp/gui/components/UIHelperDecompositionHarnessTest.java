package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class UIHelperDecompositionHarnessTest {

    @Test
    void delegatesStaticHelpersToExtractedCollaborators() {
        assertEquals("1 250,00 MT", UIHelper.formatMzn(new BigDecimal("1250")));
        assertEquals("Administrador", UIHelper.humanRole("ADMIN"));
        assertEquals("Paga", UIHelper.humanStatus("PAID"));
        assertTrue(UIHelper.looksNumeric("1.234,56 MT"));
        assertFalse(UIHelper.looksNumeric("ABC"));
        assertEquals("fas-file-invoice", UIHelper.iconForTitle("Nova Fatura"));
        assertNotNull(UIHelper.icon("fas-users", 16));
        assertNotNull(UIHelper.semanticColorFor("fas-boxes"));
        assertNotNull(UIHelper.readableTextOn(Color.WHITE));
        assertTrue(UIHelper.contrastRatio(Color.BLACK, Color.WHITE) > 15.0);
    }

    @Test
    void enforcesPhysicalLineCountLimitsOnUiHelperAndCollaborators() throws IOException {
        Path base = resolveDesktopDir().resolve("src/main/java/mz/multicore/erp/gui/components");
        long facadeLines = Files.readAllLines(base.resolve("UIHelper.java")).size();
        long themeLines = Files.readAllLines(base.resolve("UiThemeAndWindowSupport.java")).size();
        long iconLines = Files.readAllLines(base.resolve("UiIconAndImageSupport.java")).size();
        long asyncLines = Files.readAllLines(base.resolve("UiAsyncTaskSupport.java")).size();
        long tableLines = Files.readAllLines(base.resolve("UiTabbedAndTableStyler.java")).size();
        long formLines = Files.readAllLines(base.resolve("UiFormControlAndFormatSupport.java")).size();
        long formStylerLines = Files.readAllLines(base.resolve("UiFormComponentStyler.java")).size();
        long textInputLines = Files.readAllLines(base.resolve("UiTextInputSupport.java")).size();

        assertTrue(facadeLines <= 320, "UIHelper.java deve ter <= 320 linhas, mas tem " + facadeLines);
        assertTrue(themeLines <= 380, "UiThemeAndWindowSupport.java deve ter <= 380 linhas, mas tem " + themeLines);
        assertTrue(iconLines <= 310, "UiIconAndImageSupport.java deve ter <= 310 linhas, mas tem " + iconLines);
        assertTrue(asyncLines <= 190, "UiAsyncTaskSupport.java deve ter <= 190 linhas, mas tem " + asyncLines);
        assertTrue(tableLines <= 110, "UiTabbedAndTableStyler.java deve ter <= 110 linhas, mas tem " + tableLines);
        assertTrue(formLines <= 100, "UiFormControlAndFormatSupport.java deve ter <= 100 linhas, mas tem " + formLines);
        assertTrue(formStylerLines <= 500, "UiFormComponentStyler.java deve ter <= 500 linhas, mas tem " + formStylerLines);
        assertTrue(textInputLines <= 100, "UiTextInputSupport.java deve ter <= 100 linhas, mas tem " + textInputLines);
    }

    private static Path resolveDesktopDir() {
        Path cwd = Path.of("").toAbsolutePath();
        if (cwd.getFileName() != null && "desktop".equalsIgnoreCase(cwd.getFileName().toString())) {
            return cwd;
        }
        return cwd.resolve("desktop");
    }
}
