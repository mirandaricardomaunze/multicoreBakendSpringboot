package mz.multicore.erp.desktop;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Ver FORMATO_NUMERICO_SPEC: os valores do desktop não dependem do idioma do Windows. */
class DesktopNumberLocaleHarnessTest {

    @Test
    void fn03_arranqueFixaFormatoNumericoAntesDaInterface() throws IOException {
        Path fromModule = Path.of("src/main/java/mz/multicore/erp/desktop/DesktopApplication.java");
        Path file = Files.exists(fromModule) ? fromModule : Path.of("desktop").resolve(fromModule);
        String source = Files.readString(file);

        int locale = source.indexOf("Locale.setDefault(Locale.Category.FORMAT, DisplayLocale.NUMBERS)");
        assertThat(locale).as("o arranque deve fixar o formato numérico").isPositive();
        assertThat(locale).isLessThan(source.indexOf("setDefaultLookAndFeelDecorated"));
    }
}
