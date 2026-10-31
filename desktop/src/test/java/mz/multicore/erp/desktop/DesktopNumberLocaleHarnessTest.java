package mz.multicore.erp.desktop;

import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.UIHelper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Ver FORMATO_NUMERICO_SPEC: os valores do desktop não dependem do idioma do Windows. */
class DesktopNumberLocaleHarnessTest {
    private static final String MAIN_SOURCES = "src/main/java/mz/multicore/erp";

    private static Path sources() {
        Path fromModule = Path.of(MAIN_SOURCES);
        return Files.isDirectory(fromModule) ? fromModule : Path.of("desktop").resolve(fromModule);
    }

    @Test
    void fn03_arranqueFixaFormatoNumericoAntesDaInterface() throws IOException {
        String source = Files.readString(sources().resolve("desktop/DesktopApplication.java"));

        int locale = source.indexOf("Locale.setDefault(Locale.Category.FORMAT, DisplayLocale.NUMBERS)");
        assertThat(locale).as("o arranque deve fixar o formato numérico").isPositive();
        assertThat(locale).isLessThan(source.indexOf("setDefaultLookAndFeelDecorated"));
    }

    @Test
    void fn04_valorMostradoVoltaAoMesmoNumero() {
        BigDecimal value = new BigDecimal("1234567.89");
        String shown = UIHelper.formatMzn(value);

        assertThat(shown).isEqualTo("1.234.567,89 MT");
        assertThat(new BigDecimal(MoneyField.normalize(shown.replace(" MT", "")))).isEqualByComparingTo(value);
    }

    @Test
    void fn05_leitorAceitaOsFormatosDeEntrada() {
        assertThat(MoneyField.normalize("1.500,00")).isEqualTo("1500.00");
        assertThat(MoneyField.normalize("1 500,00")).isEqualTo("1500.00");
        assertThat(MoneyField.normalize("1500,5")).isEqualTo("1500.5");
        assertThat(MoneyField.normalize("1.234.567")).isEqualTo("1234567");
        assertThat(MoneyField.normalize("1.5")).as("ponto isolado continua decimal").isEqualTo("1.5");
    }

    @Test
    void fn06_desktopNaoAgrupaComEspaco() throws IOException {
        try (Stream<Path> files = Files.walk(sources().resolve("gui"))) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                assertThat(Files.readString(file)).as(file.toString()).doesNotContain("setGroupingSeparator(' ')");
            }
        }
    }
}
