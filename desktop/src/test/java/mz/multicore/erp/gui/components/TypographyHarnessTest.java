package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Harness of the single type scale and the themed dialogs (docs/TIPOGRAFIA_ESCALA_SPEC.md). */
class TypographyHarnessTest {

    private static final Path SRC = Path.of("src/main/java/mz/multicore/erp");

    /**
     * Files allowed to build fonts by hand, each with its reason. Anything else uses {@link Typography}.
     */
    private static final Map<String, String> FONT_EXCEPTIONS = Map.of(
            "Typography.java", "the scale itself",
            "ShelfLabelCardRenderer.java", "printed shelf label, sized for paper",
            "PosThermalReceiptPrinter.java", "printed thermal receipt, sized for paper",
            "StockTransferVolumeLabelsDialog.java", "printed volume label, sized for paper",
            "CircularAvatar.java", "initials scale with the avatar diameter",
            "SidebarNavItem.java", "fallback glyph drawn with the Segoe UI Symbol font",
            "KpiTextFit.java", "shrinks a KPI value until it fits its card");

    private static final Pattern NEW_FONT = Pattern.compile("new\\s+(?:java\\.awt\\.)?Font\\s*\\(");
    private static final Pattern SIZED_DERIVE = Pattern.compile("deriveFont\\s*\\([^)]*\\b\\d+(?:\\.\\d+)?f\\s*\\)");
    private static final Pattern NATIVE_DIALOG = Pattern.compile("\\bJOptionPane\\b");

    @Test
    @DisplayName("TY-01: fora da escala ninguém cria fontes com new Font(...)")
    void ty01_noAdHocFonts() {
        assertThat(offenders(NEW_FONT, FONT_EXCEPTIONS.keySet()))
                .as("use Typography.<PAPEL> em vez de new Font(...)")
                .isEmpty();
    }

    @Test
    @DisplayName("TY-02: nenhum deriveFont com tamanho fixo (15f, 13f…) fora das excepções")
    void ty02_noSizedDeriveFont() {
        assertThat(offenders(SIZED_DERIVE, FONT_EXCEPTIONS.keySet()))
                .as("deriveFont só muda o estilo; o tamanho vem da escala")
                .isEmpty();
    }

    @Test
    @DisplayName("TY-03: a escala tem um número fixo de tamanhos, todos da família da aplicação")
    void ty03_scaleIsClosed() throws IllegalAccessException {
        Set<Integer> sizes = new TreeSet<>();
        for (Field field : Typography.class.getDeclaredFields()) {
            if (field.getType() != Font.class || !Modifier.isPublic(field.getModifiers())) continue;
            Font font = (Font) field.get(null);
            sizes.add(font.getSize());
            boolean mono = field.getName().equals("MONO") || field.getName().equals("KEYCAP");
            assertThat(font.getName()).as(field.getName())
                    .isEqualTo(mono ? Font.MONOSPACED : UIHelper.FONT);
        }
        assertThat(sizes).containsExactly(9, 10, 11, 12, 13, 16, 20, 30);
    }

    @Test
    @DisplayName("TY-04: papéis de texto mantêm a hierarquia legenda < corpo < ênfase < título < métrica < destaque")
    void ty04_hierarchyIsMonotonic() {
        assertThat(List.of(Typography.CAPTION.getSize(), Typography.BODY.getSize(), Typography.EMPHASIS.getSize(),
                Typography.TITLE.getSize(), Typography.METRIC.getSize(), Typography.HERO.getSize()))
                .isSorted()
                .doesNotHaveDuplicates();
        assertThat(Typography.BODY_BOLD.isBold()).isTrue();
        assertThat(Typography.BODY.isBold()).isFalse();
        assertThat(Typography.TITLE.isBold()).isTrue();
    }

    @Test
    @DisplayName("TY-05: nenhum JOptionPane fora do ModernMessageDialog (só a saída de último recurso)")
    void ty05_noNativeDialogs() {
        assertThat(offenders(NATIVE_DIALOG, Set.of("ModernMessageDialog.java")))
                .as("use ModernMessageDialog.show/confirm/choose/prompt/showContent/confirmContent")
                .isEmpty();
        assertThat(read(SRC.resolve("gui/components/ModernMessageDialog.java")))
                .as("o JOptionPane só aparece dentro de showSafely, quando o tema falha")
                .containsOnlyOnce("JOptionPane.showMessageDialog");
    }

    @Test
    @DisplayName("TY-06: o erro global da aplicação usa o diálogo temático com recurso seguro")
    void ty06_globalErrorHandlerIsThemed() {
        assertThat(read(SRC.resolve("desktop/DesktopApplication.java"))).contains("ModernMessageDialog.showSafely(");
    }

    @Test
    @DisplayName("TY-07: escolher entre opções exige pelo menos uma opção")
    void ty07_chooseNeedsOptions() {
        assertThatThrownBy(() -> ModernMessageDialog.choose(null, FeedbackType.INFO, "T", "M"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static List<String> offenders(Pattern pattern, Set<String> allowed) {
        List<String> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SRC)) {
            files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !allowed.contains(p.getFileName().toString()))
                    .filter(p -> !p.toString().replace('\\', '/').contains("/gui/tv/"))
                    .forEach(p -> {
                        String code = stripComments(read(p));
                        Matcher m = pattern.matcher(code);
                        while (m.find()) {
                            int line = (int) code.substring(0, m.start()).chars().filter(c -> c == '\n').count() + 1;
                            found.add(SRC.relativize(p).toString().replace('\\', '/') + ":" + line);
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return found;
    }

    /** Removes comments but keeps line breaks, so reported line numbers stay right. */
    private static String stripComments(String code) {
        Matcher m = Pattern.compile("//[^\\n]*|/\\*.*?\\*/", Pattern.DOTALL).matcher(code);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(out, Matcher.quoteReplacement(m.group().replaceAll("[^\\n]", "")));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String read(Path p) {
        try {
            return Files.readString(p);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
