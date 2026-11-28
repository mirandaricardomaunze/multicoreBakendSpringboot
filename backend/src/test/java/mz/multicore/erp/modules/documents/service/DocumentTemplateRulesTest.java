package mz.multicore.erp.modules.documents.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.documents.dto.DocumentBankAccountDTO;
import mz.multicore.erp.modules.documents.dto.DocumentTemplateDTO;
import mz.multicore.erp.modules.documents.model.DocumentColumnConfig;
import mz.multicore.erp.modules.documents.model.DocumentLogoPosition;
import mz.multicore.erp.modules.printing.PdfTheme;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regras do modelo de documento (MODELOS_DOCUMENTO_HARNESS).
 * DT-01: cor {@code #RRGGBB} com contraste ≥ 4,5:1 com branco; inválida recusada; omissão do PdfTheme.
 * DT-02: posição do logótipo, notas (≤ 500), até 3 contas bancárias e QR.
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

    private static final DocumentBankAccountDTO BCI =
            new DocumentBankAccountDTO("BCI", "Multicore Lda", "0008 0000 1234 5678 101 13");

    private static DocumentTemplateDTO template(DocumentLogoPosition position, String notes,
                                                List<DocumentBankAccountDTO> accounts) {
        return new DocumentTemplateDTO(null, position, notes, accounts, true);
    }

    @Test
    void dt02_defaultsMatchSpec() {
        DocumentTemplateDTO defaults = DocumentTemplateDTO.defaults();
        assertNull(defaults.accentColor());
        assertEquals(DocumentLogoPosition.LEFT, defaults.logoPosition());
        assertNull(defaults.notes());
        assertTrue(defaults.bankAccounts().isEmpty());
        assertTrue(defaults.showQrCode());

        DocumentColumnConfig config = new DocumentColumnConfig();
        assertEquals(DocumentLogoPosition.LEFT, config.getLogoPosition());
        assertTrue(config.isShowQrCode());
        assertTrue(config.getBankAccounts().isEmpty());
    }

    @Test
    void dt02_logoPositionAcceptsTheThreeOptionsAndDefaultsToLeft() {
        for (DocumentLogoPosition position : DocumentLogoPosition.values()) {
            assertEquals(position, DocumentTemplatePolicy.normalize(template(position, null, null)).logoPosition());
        }
        assertEquals(DocumentLogoPosition.LEFT,
                DocumentTemplatePolicy.normalize(template(null, null, null)).logoPosition());
        assertEquals(List.of("LEFT", "CENTER", "RIGHT"),
                java.util.Arrays.stream(DocumentLogoPosition.values()).map(Enum::name).toList());
    }

    @Test
    void dt02_notesUpTo500CharactersAreTrimmedAndBlankMeansNone() {
        String max = "x".repeat(DocumentTemplatePolicy.MAX_NOTES_LENGTH);
        assertEquals(500, DocumentTemplatePolicy.MAX_NOTES_LENGTH);
        assertEquals(max, DocumentTemplatePolicy.normalize(template(null, "  " + max + " ", null)).notes());
        assertNull(DocumentTemplatePolicy.normalize(template(null, "   ", null)).notes());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> DocumentTemplatePolicy.normalize(template(null, max + "y", null)));
        assertTrue(ex.getMessage().contains("500"), ex.getMessage());
    }

    @Test
    void dt02_atMostThreeBankAccounts() {
        List<DocumentBankAccountDTO> three = List.of(BCI,
                new DocumentBankAccountDTO("Millennium BIM", "Multicore Lda", "MZ59 0001 0000 0000 0000 0000 0"),
                new DocumentBankAccountDTO("Standard Bank", "Multicore Lda", "0003 0000 9999 8888 777 66"));
        assertEquals(three, DocumentTemplatePolicy.normalize(template(null, null, three)).bankAccounts());

        List<DocumentBankAccountDTO> four = new java.util.ArrayList<>(three);
        four.add(BCI);
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> DocumentTemplatePolicy.normalize(template(null, null, four)));
        assertTrue(ex.getMessage().contains("3 contas"), ex.getMessage());
    }

    @Test
    void dt02_bankAccountNeedsBankHolderAndNumberAndIsTrimmed() {
        DocumentTemplateDTO trimmed = DocumentTemplatePolicy.normalize(template(null, null,
                List.of(new DocumentBankAccountDTO("  BCI ", " Multicore Lda ", " 0008 0000 1234 5678 101 13 "))));
        assertEquals(List.of(BCI), trimmed.bankAccounts());

        for (DocumentBankAccountDTO bad : List.of(
                new DocumentBankAccountDTO(" ", "Multicore Lda", "0008"),
                new DocumentBankAccountDTO("BCI", null, "0008"),
                new DocumentBankAccountDTO("BCI", "Multicore Lda", ""))) {
            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> DocumentTemplatePolicy.normalize(template(null, null, List.of(BCI, bad))));
            assertTrue(ex.getMessage().contains("conta bancária 2"), ex.getMessage());
        }
        BusinessRuleException tooLong = assertThrows(BusinessRuleException.class,
                () -> DocumentTemplatePolicy.normalize(template(null, null,
                        List.of(new DocumentBankAccountDTO("BCI", "Multicore Lda", "9".repeat(51))))));
        assertTrue(tooLong.getMessage().contains("50"), tooLong.getMessage());
    }

    @Test
    void dt02_qrFlagIsKeptAndAccentColourGoesThroughDt01Rule() {
        DocumentTemplateDTO noQr = new DocumentTemplateDTO(" #0a3d62", null, null, null, false);
        DocumentTemplateDTO normalized = DocumentTemplatePolicy.normalize(noQr);
        assertFalse(normalized.showQrCode());
        assertEquals("#0A3D62", normalized.accentColor());
        assertThrows(BusinessRuleException.class, () -> DocumentTemplatePolicy.normalize(
                new DocumentTemplateDTO("#FFD700", null, null, null, true)));
    }
}
