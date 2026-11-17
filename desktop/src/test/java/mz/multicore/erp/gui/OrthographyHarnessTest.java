package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The text the operator reads follows one spelling, the one before the 1990 Agreement, as used in
 * Mozambique, and has no English left (docs/ORTOGRAFIA_SPEC.md). "Fatura" stays: it is the term of the
 * system and of the fiscal documents.
 */
class OrthographyHarnessTest {

    private static final Path DESKTOP = Path.of("src/main/java/mz/multicore/erp");
    private static final Path BACKEND = Path.of("../backend/src/main/java/mz/multicore/erp");
    private static final Path CONTRACTS = Path.of("../contracts/src/main/java/mz/multicore/erp");

    /** 1990 spellings that the operator must not see; each word start is checked, any case. */
    private static final Pattern AGREEMENT_1990 = Pattern.compile(
            // Built from pieces so that a spelling sweep over string literals cannot rewrite the list itself.
            "(?<![\\p{L}])(?:" + String.join("|",
                    "a" + "tiv", "ina" + "tiv", "desa" + "tiv", "rea" + "tiv", "a" + "tualiz", "a" + "tual",
                    "dire" + "t", "indire" + "t", "dire" + "ç", "sele" + "cion", "sele" + "ç", "efe" + "tu", "efe" + "tiv",
                    "cole" + "tiv", "obje" + "tiv", "proje" + "t", "proje" + "ç", "conta" + "t", "exa" + "t", "óti" + "m",
                    "transa" + "ç", "prote" + "ç", "exce" + "ç", "exce" + "to", "inspe" + "ç", "corre" + "ç", "reda" + "ç",
                    "rece" + "ç", "notur" + "n", "ele" + "tr", "elé" + "tr", "se" + "tor", "fa" + "tor", "dete" + "ç",
                    "dete" + "t", "a" + "ção(?![\\p{L}])", "a" + "ções(?![\\p{L}])", "corre" + "t[oa]s?(?![\\p{L}])") + ")",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern ENGLISH = Pattern.compile(
            "\\b(?:Blind Drop|Picking List|Cash Flow|Push USSD|Workflow|Aging|Backups?|Dashboard|Checkout)\\b");

    private static final Pattern LITERAL = Pattern.compile("\"(?:[^\"\\\\\\n]|\\\\.)*\"");
    private static final Pattern COMMENT = Pattern.compile("//[^\\n]*|/\\*.*?\\*/", Pattern.DOTALL);

    @Test
    @DisplayName("OR-01: nenhum texto visível usa a grafia do Acordo de 1990 (Atu" + "alizar, Sele" + "cione, Ati" + "vo…)")
    void or01_preAgreementSpelling() {
        List<String> offenders = new ArrayList<>(scan(DESKTOP, AGREEMENT_1990));
        if (Files.isDirectory(BACKEND)) offenders.addAll(scan(BACKEND, AGREEMENT_1990));
        if (Files.isDirectory(CONTRACTS)) offenders.addAll(scan(CONTRACTS, AGREEMENT_1990));
        assertThat(offenders).isEmpty();
    }

    @Test
    @DisplayName("OR-02: nenhum termo inglês nos textos do desktop (Backup, Workflow, Aging, Push USSD…)")
    void or02_noEnglish() {
        assertThat(scan(DESKTOP, ENGLISH)).isEmpty();
    }

    private static List<String> scan(Path root, Pattern forbidden) {
        List<String> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                String code = COMMENT.matcher(read(p)).replaceAll("");
                Matcher literal = LITERAL.matcher(code);
                while (literal.find()) {
                    String body = literal.group().substring(1, literal.group().length() - 1);
                    if (isCodeLike(body)) continue;
                    Matcher bad = forbidden.matcher(body);
                    if (bad.find()) found.add(root.relativize(p).toString().replace('\\', '/') + ": \"" + body + "\"");
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return found;
    }

    /** Status codes ("ATIVO"), keys ("atividade"), URLs and SQL are data, not text for people. */
    private static boolean isCodeLike(String body) {
        if (!body.contains(" ") && body.equals(body.toUpperCase())) return true;
        if (body.matches("[a-z][a-zA-Z0-9_.]*")) return true;
        return body.contains("://") || body.contains("/api/") || body.matches("(?s).*\\b(SELECT|FROM|WHERE|UPDATE|INSERT)\\b.*");
    }

    private static String read(Path p) {
        try {
            return Files.readString(p);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
