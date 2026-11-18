package mz.multicore.erp.architecture;

import com.lowagie.text.FontFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Texto que o sistema escreve tem de sair igual no ecrã e no papel.
 *
 * <p>Os PDFs usam Helvetica com a codificação Cp1252: os acentos portugueses passam, mas um
 * carácter fora dessa tabela ("→", "≤", "−", "≠", emojis) desaparece sem aviso — "(−) Sangrias"
 * imprimia "() Sangrias" e a rota "Armazém A → Armazém B" imprimia "Armazém A  Armazém B".
 * Para setas e caminhos de menu usa-se "›"; para limites, "até"; para subtrair, "-".
 */
class PrintableTextHarnessTest {

    private static final Charset PDF_ENCODING = Charset.forName("windows-1252");

    /** Ficheiro → razão. Só texto que nunca chega a um PDF. */
    private static final Map<String, String> ALLOWED = Map.of(
            "SystemAlertMailDispatcher.java", "email HTML em UTF-8, não passa por PDF",
            "PosWorkspaceBuilder.java", "botão '−' do POS, só Swing (PosProfessionalErgonomicsHarnessTest)");

    private static final Set<String> MODULES = Set.of("backend", "contracts", "desktop");

    private static Path repoRoot() {
        return Files.isDirectory(Path.of("backend")) ? Path.of(".") : Path.of("..");
    }

    @Test
    void pt01_aFonteDosPdfsUsaCp1252() {
        assertThat(FontFactory.defaultEncoding).isEqualToIgnoringCase("Cp1252");
    }

    @Test
    void pt02_textoDoSistemaSoUsaCaracteresQueOPdfDesenha() throws IOException {
        List<String> violations = new ArrayList<>();
        for (String module : MODULES) {
            Path sources = repoRoot().resolve(module).resolve("src/main/java");
            try (Stream<Path> files = Files.walk(sources)) {
                for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                    if (ALLOWED.containsKey(file.getFileName().toString())) continue;
                    String source = Files.readString(file);
                    for (int[] literal : stringLiterals(source)) {
                        String text = source.substring(literal[0], literal[1]);
                        String bad = unprintable(text);
                        if (!bad.isEmpty()) {
                            int line = source.substring(0, literal[0]).split("\n", -1).length;
                            violations.add(module + "/" + file.getFileName() + ":" + line + " " + bad);
                        }
                    }
                }
            }
        }
        assertThat(violations)
                .as("caracteres que o PDF não desenha: usar '›' para setas, 'até' para ≤, '-' para −")
                .isEmpty();
    }

    @Test
    void pt03_osSubstitutosEscolhidosExistemNoPdf() {
        assertThat(unprintable("Armazém A › Armazém B · até 30 dias (-) Sangrias — Faturação € …")).isEmpty();
        assertThat(unprintable("A → B ≤ 30 − ≠")).isEqualTo("→≤−≠");
    }

    private static String unprintable(String text) {
        CharsetEncoder encoder = PDF_ENCODING.newEncoder();
        StringBuilder bad = new StringBuilder();
        text.codePoints()
                .filter(cp -> !encoder.canEncode(new String(Character.toChars(cp))))
                .distinct()
                .forEach(bad::appendCodePoint);
        return bad.toString();
    }

    /** Intervalos [início, fim) de literais de texto e de carácter, ignorando comentários. */
    private static List<int[]> stringLiterals(String src) {
        List<int[]> out = new ArrayList<>();
        int i = 0;
        int n = src.length();
        while (i < n) {
            char c = src.charAt(i);
            if (src.startsWith("//", i)) {
                int end = src.indexOf('\n', i);
                i = end < 0 ? n : end;
            } else if (src.startsWith("/*", i)) {
                int end = src.indexOf("*/", i + 2);
                i = end < 0 ? n : end + 2;
            } else if (src.startsWith("\"\"\"", i)) {
                int end = src.indexOf("\"\"\"", i + 3);
                end = end < 0 ? n : end + 3;
                out.add(new int[]{i, end});
                i = end;
            } else if (c == '"' || c == '\'') {
                int j = i + 1;
                while (j < n && src.charAt(j) != c && src.charAt(j) != '\n') {
                    if (src.charAt(j) == '\\') j++;
                    j++;
                }
                int end = Math.min(n, j + 1);
                out.add(new int[]{i, end});
                i = end;
            } else {
                i++;
            }
        }
        return out;
    }
}
