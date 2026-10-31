package mz.multicore.erp.architecture;

import mz.multicore.erp.architecture.format.DisplayLocale;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Ver FORMATO_NUMERICO_SPEC: o servidor pode correr com qualquer locale. */
class DisplayNumberFormatHarnessTest {
    private static final Pattern UNLOCALISED_GROUPING = Pattern.compile("String\\.format\\(\"[^\"]*%,");

    private static Path sources() {
        Path fromModule = Path.of("src/main/java");
        return Files.isDirectory(fromModule) ? fromModule : Path.of("backend").resolve(fromModule);
    }

    @Test
    void fn01_valoresUsamPontoNoMilharEVirgulaNosDecimais() {
        assertThat(String.format(DisplayLocale.NUMBERS, "%,.2f MT", 1500.5)).isEqualTo("1.500,50 MT");
    }

    @Test
    void fn02_backendNaoFormataMilharesComLocaleDoServidor() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(sources())) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    if (UNLOCALISED_GROUPING.matcher(lines.get(i)).find()) {
                        violations.add(file.getFileName() + ":" + (i + 1));
                    }
                }
            }
        }
        assertThat(violations).isEmpty();
    }
}
