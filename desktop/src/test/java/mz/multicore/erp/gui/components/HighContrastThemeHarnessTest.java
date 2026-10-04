package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Harness automatizado de validação do Modo de Alto Contraste Acessível (HARNESS-HCON-001).
 * Verifica conformidade estrita com WCAG 2.1 AAA, integridade da paleta canónica,
 * transições de ciclo e limites de arquitetura Swing.
 */
class HighContrastThemeHarnessTest {

    private Theme originalTheme;

    @BeforeEach
    void setUp() {
        originalTheme = UIHelper.currentTheme();
    }

    @AfterEach
    void tearDown() {
        UIHelper.applyTheme(originalTheme != null ? originalTheme : Theme.DARK);
    }

    @Test
    @DisplayName("HCON-01: Integridade da Paleta Theme.HIGH_CONTRAST")
    void testPaletteIntegrity() {
        Theme hc = Theme.HIGH_CONTRAST;
        assertNotNull(hc, "Theme.HIGH_CONTRAST não pode ser nulo");
        assertEquals("high_contrast", hc.id);
        assertTrue(hc.isHighContrast());
        assertFalse(hc.isLight());
        assertFalse(hc.isDark());

        Color[] palette = hc.palette();
        assertEquals(10, palette.length, "A paleta canónica de Theme deve conter exatamente 10 slots");
        for (int i = 0; i < palette.length; i++) {
            assertNotNull(palette[i], "Slot " + i + " da paleta não pode ser nulo");
        }

        assertEquals(new Color(0, 0, 0), hc.bg);
        assertEquals(new Color(10, 10, 10), hc.card);
        assertEquals(new Color(255, 255, 255), hc.textPrimary);
        assertEquals(new Color(255, 255, 255), hc.border);
    }

    @Test
    @DisplayName("HCON-02: Resolução por Identificadores no Theme.byId")
    void testThemeByIdResolution() {
        assertSame(Theme.HIGH_CONTRAST, Theme.byId("high_contrast"));
        assertSame(Theme.HIGH_CONTRAST, Theme.byId("highcontrast"));
        assertSame(Theme.HIGH_CONTRAST, Theme.byId("contrast"));
        assertSame(Theme.HIGH_CONTRAST, Theme.byId("HIGH_CONTRAST"));
        assertSame(Theme.LIGHT, Theme.byId("light"));
        assertSame(Theme.LIGHT, Theme.byId("LIGHT"));
        assertSame(Theme.DARK, Theme.byId("dark"));
        assertSame(Theme.DARK, Theme.byId("unknown_theme"));
        assertSame(Theme.DARK, Theme.byId(null));
    }

    @Test
    @DisplayName("HCON-03: WCAG 2.1 AAA em Texto Primário (>= 7.0:1)")
    void testWcagAaaTextPrimary() {
        Theme hc = Theme.HIGH_CONTRAST;
        double ratioBg = UIHelper.contrastRatio(hc.textPrimary, hc.bg);
        double ratioCard = UIHelper.contrastRatio(hc.textPrimary, hc.card);

        assertTrue(ratioBg >= 7.0, "Texto primário sobre bg deve cumprir WCAG AAA (>= 7.0). Atual: " + ratioBg);
        assertTrue(ratioCard >= 7.0, "Texto primário sobre card deve cumprir WCAG AAA (>= 7.0). Atual: " + ratioCard);
        assertTrue(ratioBg >= 20.0, "Texto branco sobre fundo preto puro atinge contraste quase máximo (~21:1). Atual: " + ratioBg);

        assertTrue(UIHelper.meetsWcagAaa(hc.textPrimary, hc.bg));
        assertTrue(UIHelper.meetsWcagAaa(hc.textPrimary, hc.card));
    }

    @Test
    @DisplayName("HCON-04: WCAG 2.1 AAA em Texto Muted (>= 7.0:1)")
    void testWcagAaaTextMuted() {
        Theme hc = Theme.HIGH_CONTRAST;
        double ratioBg = UIHelper.contrastRatio(hc.textMuted, hc.bg);
        double ratioCard = UIHelper.contrastRatio(hc.textMuted, hc.card);

        assertTrue(ratioBg >= 7.0, "Texto secundário sobre bg deve cumprir WCAG AAA (>= 7.0). Atual: " + ratioBg);
        assertTrue(ratioCard >= 7.0, "Texto secundário sobre card deve cumprir WCAG AAA (>= 7.0). Atual: " + ratioCard);
        assertTrue(ratioBg >= 15.0, "Texto secundário prateado (#E0E0E0) oferece visibilidade ultra-nítida (>15:1). Atual: " + ratioBg);

        assertTrue(UIHelper.meetsWcagAaa(hc.textMuted, hc.bg));
        assertTrue(UIHelper.meetsWcagAaa(hc.textMuted, hc.card));
    }

    @Test
    @DisplayName("HCON-05: Legibilidade Dinâmica com readableTextOn em Superfícies e Bordas")
    void testReadableTextOnSurfaces() {
        Theme hc = Theme.HIGH_CONTRAST;
        for (Color surface : new Color[]{hc.bg, hc.card, hc.fieldBg, hc.border, hc.selectionBg}) {
            Color fg = UIHelper.readableTextOn(surface);
            assertNotNull(fg);
            double ratio = UIHelper.contrastRatio(surface, fg);
            assertTrue(ratio >= 4.5, "Superfície " + surface + " deve ter contraste legível (>= 4.5). Atual: " + ratio);
        }
    }

    @Test
    @DisplayName("HCON-06: Ciclo Circular de Temas (DARK -> LIGHT -> HIGH_CONTRAST -> DARK)")
    void testThemeCycling() {
        UIHelper.applyTheme(Theme.DARK);
        assertEquals(Theme.DARK, UIHelper.currentTheme());

        Theme t1 = UIHelper.cycleTheme();
        assertSame(Theme.LIGHT, t1);
        assertSame(Theme.LIGHT, UIHelper.currentTheme());
        assertTrue(UIHelper.isLight());
        assertFalse(UIHelper.isHighContrast());

        Theme t2 = UIHelper.cycleTheme();
        assertSame(Theme.HIGH_CONTRAST, t2);
        assertSame(Theme.HIGH_CONTRAST, UIHelper.currentTheme());
        assertTrue(UIHelper.isHighContrast());
        assertFalse(UIHelper.isLight());

        Theme t3 = UIHelper.cycleTheme();
        assertSame(Theme.DARK, t3);
        assertSame(Theme.DARK, UIHelper.currentTheme());
        assertFalse(UIHelper.isHighContrast());
        assertFalse(UIHelper.isLight());
    }

    @Test
    @DisplayName("HCON-07: Aplicação de Slots em UIHelper ao ativar HIGH_CONTRAST")
    void testApplyThemeSlots() {
        UIHelper.applyTheme(Theme.HIGH_CONTRAST);
        assertTrue(UIHelper.isHighContrast());
        assertEquals(Theme.HIGH_CONTRAST.bg, UIHelper.BG_DARK);
        assertEquals(Theme.HIGH_CONTRAST.card, UIHelper.BG_CARD);
        assertEquals(Theme.HIGH_CONTRAST.textPrimary, UIHelper.TEXT_LIGHT);
        assertEquals(Theme.HIGH_CONTRAST.border, UIHelper.BORDER);
        assertEquals(Color.WHITE, UIHelper.BORDER);
    }

    @Test
    @DisplayName("HCON-08: Limite Estrito de Linhas de Código (<= 1000)")
    void testCodeLinesConstraint() throws Exception {
        List<String> filesToCheck = List.of(
                "desktop/src/main/java/mz/multicore/erp/gui/MainFrame.java",
                "desktop/src/main/java/mz/multicore/erp/gui/ConfigPanel.java",
                "desktop/src/main/java/mz/multicore/erp/gui/components/Theme.java"
        );

        for (String relPath : filesToCheck) {
            File f = new File(relPath);
            if (!f.exists()) {
                f = new File("../" + relPath);
            }
            assertTrue(f.exists(), "Ficheiro deve existir: " + relPath);
            long lineCount = Files.lines(f.toPath()).count();
            assertTrue(lineCount <= 1000,
                    "A classe " + f.getName() + " ultrapassou o limite estrito de 1000 linhas! Atual: " + lineCount);
        }
    }
}
