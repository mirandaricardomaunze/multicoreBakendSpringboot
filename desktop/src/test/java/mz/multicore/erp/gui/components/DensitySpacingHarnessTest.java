package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness of the interface density and the spacing scale (docs/DENSIDADE_ESPACAMENTO_SPEC.md). */
class DensitySpacingHarnessTest {

    private static final Path GUI = Path.of("src/main/java/mz/multicore/erp/gui");
    private static final Set<String> PAPER = Set.of(
            "ShelfLabelCardRenderer.java", "PosThermalReceiptPrinter.java", "StockTransferVolumeLabelsDialog.java");

    @AfterEach
    void standard() {
        UiDensityManager.getInstance().applyDensityToSystem(UiDensity.STANDARD);
    }

    @Test
    @DisplayName("DE-01: Confortável aumenta letra, campos e espaços; Compacto reduz")
    void de01_densityScalesEverything() {
        UiDensityManager manager = UiDensityManager.getInstance();

        manager.applyDensityToSystem(UiDensity.COMFORTABLE);
        int comfortableBody = Typography.BODY.getSize();
        int comfortableField = UIHelper.FORM_CONTROL_HEIGHT;
        int comfortableCard = Spacing.LG;

        manager.applyDensityToSystem(UiDensity.STANDARD);
        assertThat(Typography.BODY.getSize()).isEqualTo(12);
        assertThat(UIHelper.FORM_CONTROL_HEIGHT).isEqualTo(38);
        assertThat(Spacing.LG).isEqualTo(16);

        manager.applyDensityToSystem(UiDensity.COMPACT);
        assertThat(Typography.BODY.getSize()).isLessThan(12);
        assertThat(UIHelper.FORM_CONTROL_HEIGHT).isLessThan(38);
        assertThat(Spacing.LG).isLessThan(16);

        assertThat(comfortableBody).isGreaterThan(12);
        assertThat(comfortableField).isGreaterThan(38);
        assertThat(comfortableCard).isGreaterThan(16);
    }

    @Test
    @DisplayName("DE-02: a escala mantém a hierarquia em todas as densidades")
    void de02_hierarchySurvivesScaling() {
        for (UiDensity density : UiDensity.values()) {
            UiDensityManager.getInstance().applyDensityToSystem(density);
            assertThat(List.of(Typography.CAPTION.getSize(), Typography.BODY.getSize(), Typography.EMPHASIS.getSize(),
                    Typography.TITLE.getSize(), Typography.METRIC.getSize(), Typography.HERO.getSize()))
                    .as(density.name()).isSorted().doesNotHaveDuplicates();
            assertThat(List.of(Spacing.XXS, Spacing.XS, Spacing.SM, Spacing.MD, Spacing.LG, Spacing.XL, Spacing.XXL))
                    .as(density.name()).isSorted().doesNotHaveDuplicates();
        }
    }

    @Test
    @DisplayName("DE-03: a altura dos campos não é uma constante de compilação (senão o compilador fixava 38 em todo o lado)")
    void de03_controlHeightIsNotACompileTimeConstant() throws NoSuchFieldException {
        assertThat(Modifier.isFinal(UIHelper.class.getField("FORM_CONTROL_HEIGHT").getModifiers())).isFalse();
        assertThat(Modifier.isFinal(Typography.class.getField("BODY").getModifiers())).isFalse();
        assertThat(Modifier.isFinal(Spacing.class.getField("LG").getModifiers())).isFalse();
    }

    @Test
    @DisplayName("DE-04: mudar a densidade reconstrói a janela, e a densidade é carregada antes do primeiro ecrã")
    void de04_rebuildAndStartupOrder() {
        assertThat(read(GUI.resolve("components/UiDensityManager.java")))
                .contains("Typography.applyScale(density.getFontScale())")
                .contains("UIHelper.FORM_CONTROL_HEIGHT = density.getFormControlHeight()")
                .contains("Spacing.applyScale(density.getFontScale())")
                .contains("UIHelper.onThemeChanged.run()");
        String helper = read(GUI.resolve("components/UIHelper.java"));
        assertThat(helper).contains("UiDensityManager.getInstance(); UiThemeAndWindowSupport.loadAndApplySavedTheme();");
    }

    @Test
    @DisplayName("DE-05: margens usam a escala Spacing; ninguém escreve createEmptyBorder(15, 14, …)")
    void de05_noHandPickedMargins() {
        Pattern raw = Pattern.compile("(?:createEmptyBorder|new EmptyBorder)\\(\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*\\)");
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(GUI)) {
            files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !PAPER.contains(p.getFileName().toString()))
                    .filter(p -> !p.toString().replace('\\', '/').contains("/tv/"))
                    .forEach(p -> {
                        Matcher m = raw.matcher(read(p));
                        while (m.find()) {
                            if (!m.group().matches(".*\\(\\s*0\\s*,\\s*0\\s*,\\s*0\\s*,\\s*0\\s*\\)")) {
                                offenders.add(GUI.relativize(p).toString().replace('\\', '/') + ": " + m.group());
                            }
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assertThat(offenders).isEmpty();
    }

    private static String read(Path p) {
        try {
            return Files.readString(p);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
