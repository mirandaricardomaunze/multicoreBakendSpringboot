package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness of the semantic chrome colours (docs/CORES_TEMA_SPEC.md). */
class ThemeColorsHarnessTest {

    private static final Path GUI = Path.of("src/main/java/mz/multicore/erp/gui");

    /** Files allowed to keep a light/dark pair of literal colours, each with its reason. */
    private static final Map<String, String> EXCEPTIONS = Map.of(
            "ThemeColors.java", "the token table itself",
            "KpiCardPalette.java", "accent families tuned per theme for KPI cards",
            "ProfitAnalyticsWidget.java", "healthy/at-risk status tones of the profit widget");

    private Theme original;

    @BeforeEach
    void remember() {
        original = UIHelper.currentTheme();
    }

    @AfterEach
    void restore() {
        UIHelper.applyTheme(original != null ? original : Theme.LIGHT);
    }

    @Test
    @DisplayName("CT-01: no Alto Contraste o cromo é preto, as bordas fortes são brancas e o texto lê-se a 7:1")
    void ct01_highContrastIsReallyHighContrast() {
        UIHelper.applyTheme(Theme.HIGH_CONTRAST);
        Color bg = ThemeColors.chromeBg();
        assertThat(bg).isEqualTo(Color.BLACK);
        assertThat(ThemeColors.chromeBorder()).isEqualTo(Color.WHITE);
        assertThat(ThemeColors.outline()).isEqualTo(Color.WHITE);
        assertThat(contrast(ThemeColors.control(), bg)).isGreaterThanOrEqualTo(7.0);
        assertThat(contrast(ThemeColors.textSoft(), bg)).isGreaterThanOrEqualTo(7.0);
        assertThat(contrast(ThemeColors.controlSubtle(), bg)).isGreaterThanOrEqualTo(4.5);
        assertThat(contrast(ThemeColors.hairline(), bg)).isGreaterThanOrEqualTo(3.0);
    }

    @Test
    @DisplayName("CT-02: cada token muda com o tema (claro, escuro e alto contraste têm valores próprios)")
    void ct02_tokensFollowTheTheme() {
        List<Supplier<Color>> tokens = List.of(ThemeColors::chromeBg, ThemeColors::chromeBorder, ThemeColors::hairline,
                ThemeColors::outline, ThemeColors::textSoft, ThemeColors::control, ThemeColors::controlSubtle,
                ThemeColors::subtleFill, ThemeColors::hoverOverlay, ThemeColors::restOverlay);
        for (Supplier<Color> token : tokens) {
            UIHelper.applyTheme(Theme.LIGHT);
            Color light = token.get();
            UIHelper.applyTheme(Theme.DARK);
            Color dark = token.get();
            UIHelper.applyTheme(Theme.HIGH_CONTRAST);
            Color high = token.get();
            assertThat(light).isNotEqualTo(dark);
            assertThat(high).isNotEqualTo(dark);
        }
    }

    @Test
    @DisplayName("CT-03: no texto do cromo há contraste de leitura em todos os temas (≥ 4.5:1)")
    void ct03_chromeTextIsReadable() {
        for (Theme theme : List.of(Theme.LIGHT, Theme.DARK, Theme.HIGH_CONTRAST)) {
            UIHelper.applyTheme(theme);
            assertThat(contrast(ThemeColors.textSoft(), ThemeColors.chromeBg())).as(theme.id).isGreaterThanOrEqualTo(4.5);
            assertThat(contrast(ThemeColors.control(), ThemeColors.chromeBg())).as(theme.id).isGreaterThanOrEqualTo(4.5);
        }
    }

    @Test
    @DisplayName("CT-04: ninguém escreve isLight() ? new Color(..) : new Color(..) — usa ThemeColors ou os tokens do tema")
    void ct04_noTwoBranchLiteralColours() {
        Pattern twoBranch = Pattern.compile(
                "(?:isLight\\(\\)|\\blight\\b|\\bisLight\\b)\\s*\\)?\\s*\\?\\s*new Color\\(\\s*\\d"
                        + "|(?:isLight\\(\\)|\\blight\\b|\\bisLight\\b)\\s*\\)?\\s*\\?[^;:]*:\\s*new Color\\(\\s*\\d");
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(GUI)) {
            files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !EXCEPTIONS.containsKey(p.getFileName().toString()))
                    .filter(p -> !p.toString().replace('\\', '/').contains("/tv/"))
                    .forEach(p -> {
                        Matcher m = twoBranch.matcher(read(p));
                        while (m.find()) offenders.add(GUI.relativize(p).toString().replace('\\', '/'));
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assertThat(offenders).isEmpty();
    }

    private static double contrast(Color a, Color b) {
        double la = luminance(a), lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(Color c) {
        return 0.2126 * channel(c.getRed()) + 0.7152 * channel(c.getGreen()) + 0.0722 * channel(c.getBlue());
    }

    private static double channel(int v) {
        double s = v / 255.0;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    private static String read(Path p) {
        try {
            return Files.readString(p);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
