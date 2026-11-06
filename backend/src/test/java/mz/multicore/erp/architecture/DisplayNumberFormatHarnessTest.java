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

    /** Documentos impressos não escrevem "1460.00" nem "0.25": passam por MoneyFormat. */
    @Test
    void fn07_documentosImpressosNaoEscrevemDecimaisComPonto() throws IOException {
        Path printing = sources().resolve("mz/multicore/erp/modules/printing");
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(printing)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (line.contains(".toPlainString()") && !line.contains(".toPlainString().replace('.', ',')")) {
                        violations.add(file.getFileName() + ":" + (i + 1));
                    }
                }
            }
        }
        assertThat(violations).as("usar MoneyFormat.formatPlain / formatQty").isEmpty();
    }

    /** Mensagens de regra de negócio falam com pessoas: "1.460,00 MT", não "1460.00". */
    @Test
    void fn11_mensagensDeNegocioNaoEscrevemDecimaisComPonto() throws IOException {
        Pattern exception = Pattern.compile("new BusinessRuleException\\((.*?)\\);", Pattern.DOTALL);
        Pattern raw = Pattern.compile("\\.toPlainString\\(\\)|%\\.\\d+f");
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(sources())) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                var matcher = exception.matcher(source);
                while (matcher.find()) {
                    if (raw.matcher(matcher.group(1)).find()) {
                        int line = source.substring(0, matcher.start()).split("\n", -1).length;
                        violations.add(file.getFileName() + ":" + line);
                    }
                }
            }
        }
        assertThat(violations).as("usar DisplayNumbers.money / qty").isEmpty();
    }

    @Test
    void fn11_formatadorPartilhado() {
        assertThat(mz.multicore.erp.architecture.format.DisplayNumbers.money(new java.math.BigDecimal("1460")))
                .isEqualTo("1.460,00");
        assertThat(mz.multicore.erp.architecture.format.DisplayNumbers.qty(new java.math.BigDecimal("2.500")))
                .isEqualTo("2,5");
        assertThat(mz.multicore.erp.architecture.format.DisplayNumbers.qty(new java.math.BigDecimal("1000")))
                .isEqualTo("1.000");
    }
}
