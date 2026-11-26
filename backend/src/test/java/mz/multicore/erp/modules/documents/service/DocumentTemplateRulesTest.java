package mz.multicore.erp.modules.documents.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.printing.PdfTheme;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regras do modelo de documento (MODELOS_DOCUMENTO_HARNESS).
 * DT-01: cor {@code #RRGGBB} com contraste ≥ 4,5:1 com branco; inválida recusada; omissão do PdfTheme.
 */
class DocumentTemplateRulesTest {

    @Test
    void dt01_darkColorIsAcceptedAndNormalisedToUpperCase() {
        assertEquals("#0A3D62", DocumentAccentColorPolicy.normalize("  #0a3d62 "));
        assertEquals("#000000", DocumentAccentColorPolicy.normalize("#000000"));
    }

    @Test
    void dt01_contrastFollowsWcagFormula() {
        assertEquals(0, new BigDecimal("21").compareTo(
                DocumentAccentColorPolicy.contrastWithWhite("#000000").setScale(2, RoundingMode.HALF_UP)
                        .stripTrailingZeros()));
        assertEquals(0, BigDecimal.ONE.compareTo(
                DocumentAccentColorPolicy.contrastWithWhite("#FFFFFF").setScale(2, RoundingMode.HALF_UP)
                        .stripTrailingZeros()));
        // #767676 é o cinzento mais claro que ainda passa 4,5:1 com branco; #777777 já não passa.
        assertTrue(DocumentAccentColorPolicy.contrastWithWhite("#767676")
                .compareTo(DocumentAccentColorPolicy.MIN_CONTRAST_WITH_WHITE) >= 0);
        assertTrue(DocumentAccentColorPolicy.contrastWithWhite("#777777")
                .compareTo(DocumentAccentColorPolicy.MIN_CONTRAST_WITH_WHITE) < 0);
    }

    @Test
    void dt01_lowContrastColorIsRefusedInPortuguese() {
        BusinessRuleException yellow = assertThrows(BusinessRuleException.class,
                () -> DocumentAccentColorPolicy.normalize("#FFD700"));
        assertTrue(yellow.getMessage().contains("pouco contraste"), yellow.getMessage());
        assertTrue(yellow.getMessage().contains("4,5:1"), yellow.getMessage());
        assertThrows(BusinessRuleException.class, () -> DocumentAccentColorPolicy.normalize("#777777"));
        assertEquals("#767676", DocumentAccentColorPolicy.normalize("#767676"));
    }

    @Test
    void dt01_invalidFormatIsRefused() {
        for (String bad : new String[]{"1F2937", "#1F29", "#1F29371", "#GG2937", "azul", "rgb(0,0,0)"}) {
            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> DocumentAccentColorPolicy.normalize(bad), bad);
            assertTrue(ex.getMessage().contains("#RRGGBB"), ex.getMessage());
        }
    }

    @Test
    void dt01_blankMeansDefaultPdfThemeColour() {
        assertNull(DocumentAccentColorPolicy.normalize(null));
        assertNull(DocumentAccentColorPolicy.normalize("   "));
        assertEquals(PdfTheme.BRAND, PdfTheme.accentOrDefault(null));
        assertEquals(PdfTheme.BRAND, PdfTheme.accentOrDefault(""));
        assertEquals(new Color(0x0A, 0x3D, 0x62), PdfTheme.accentOrDefault("#0A3D62"));
    }

    @Test
    void dt01_defaultPdfThemeColourItselfPassesTheContrastRule() {
        String brandHex = String.format("#%06X", PdfTheme.BRAND.getRGB() & 0xFFFFFF);
        assertEquals(brandHex, DocumentAccentColorPolicy.normalize(brandHex));
    }
}
