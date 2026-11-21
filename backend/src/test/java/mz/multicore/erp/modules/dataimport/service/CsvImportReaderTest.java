package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IMPORTACAO_DADOS_HARNESS — leitura do ficheiro CSV (SPEC §3.2).
 */
class CsvImportReaderTest {

    private static final String TEXT = "nome;nuit\nJoão Açúcar Lda;400000001\n";
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final CsvImportReader reader = new CsvImportReader();

    // IMP-01
    @Test
    void decodesUtf8WithoutBomKeepingAccents() {
        String decoded = reader.decode(TEXT.getBytes(StandardCharsets.UTF_8));

        assertThat(decoded).isEqualTo(TEXT);
    }

    // IMP-01
    @Test
    void decodesUtf8WithBomAndDropsTheBom() {
        byte[] body = TEXT.getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[UTF8_BOM.length + body.length];
        System.arraycopy(UTF8_BOM, 0, withBom, 0, UTF8_BOM.length);
        System.arraycopy(body, 0, withBom, UTF8_BOM.length, body.length);

        String decoded = reader.decode(withBom);

        assertThat(decoded).isEqualTo(TEXT);
        assertThat(decoded.charAt(0)).isEqualTo('n');
    }

    // IMP-01
    @Test
    void fallsBackToWindows1252WhenNotValidUtf8() {
        byte[] cp1252 = TEXT.getBytes(Charset.forName("windows-1252"));

        String decoded = reader.decode(cp1252);

        assertThat(decoded).isEqualTo(TEXT);
        assertThat(decoded).contains("João Açúcar");
    }

    // IMP-01
    @Test
    void emptyContentDecodesToEmptyText() {
        assertThat(reader.decode(new byte[0])).isEmpty();
    }

    // IMP-02
    @Test
    void detectsSemicolonSeparatorFromHeader() {
        assertThat(reader.detectSeparator("nome;nuit;email")).isEqualTo(';');
    }

    // IMP-02
    @Test
    void detectsCommaSeparatorFromHeader() {
        assertThat(reader.detectSeparator("nome,nuit,email")).isEqualTo(',');
    }

    // IMP-02
    @Test
    void separatorInsideQuotedHeaderDoesNotCount() {
        assertThat(reader.detectSeparator("\"nome, completo\";nuit")).isEqualTo(';');
    }

    // IMP-02
    @Test
    void splitsPlainFieldsBySeparator() {
        assertThat(reader.parseLine("João;400000001;", ';'))
                .containsExactly("João", "400000001", "");
    }

    // IMP-02
    @Test
    void quotedFieldMayContainTheSeparator() {
        assertThat(reader.parseLine("\"Açúcar, Lda\",400000001", ','))
                .containsExactly("Açúcar, Lda", "400000001");
    }

    // IMP-02
    @Test
    void doubledQuotesInsideQuotedFieldBecomeOneQuote() {
        assertThat(reader.parseLine("\"Loja \"\"Central\"\"; Maputo\";1", ';'))
                .containsExactly("Loja \"Central\"; Maputo", "1");
    }

    private static final List<String> CLIENT_REQUIRED = List.of("nome", "nuit");
    private static final List<String> CLIENT_OPTIONAL = List.of("email", "morada");

    // IMP-03
    @Test
    void recognisesColumnsIgnoringCaseAccentsAndOuterSpaces() {
        CsvHeader header = reader.readHeader(
                List.of(" NUIT ", "Nóme", "E-mail?", "MORADA"), CLIENT_REQUIRED, CLIENT_OPTIONAL);

        List<String> row = List.of("400000001", "João", "x", "Maputo");
        assertThat(header.value(row, "nome")).isEqualTo("João");
        assertThat(header.value(row, "nuit")).isEqualTo("400000001");
        assertThat(header.value(row, "morada")).isEqualTo("Maputo");
        assertThat(header.has("email")).isFalse();
        assertThat(header.value(row, "email")).isEmpty();
    }

    // IMP-03
    @Test
    void missingRequiredColumnRejectsTheWholeFile() {
        assertThatThrownBy(() -> reader.readHeader(List.of("nome", "email"), CLIENT_REQUIRED, CLIENT_OPTIONAL))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("nuit");
    }

    // IMP-03
    @Test
    void extraColumnsAreIgnoredAndAnnounced() {
        CsvHeader header = reader.readHeader(
                List.of("nome", "Telefone", "nuit", " Observações "), CLIENT_REQUIRED, CLIENT_OPTIONAL);

        assertThat(header.extraColumns()).containsExactly("Telefone", "Observações");
        assertThat(header.value(List.of("Ana", "84", "400000001", "obs"), "nuit")).isEqualTo("400000001");
    }

    // IMP-03
    @Test
    void shortRowYieldsEmptyValueAndValuesAreTrimmed() {
        CsvHeader header = reader.readHeader(List.of("nome", "nuit", "morada"), CLIENT_REQUIRED, CLIENT_OPTIONAL);

        assertThat(header.value(List.of("  Ana  ", "400000001"), "nome")).isEqualTo("Ana");
        assertThat(header.value(List.of("Ana", "400000001"), "morada")).isEmpty();
    }

    // IMP-05
    @Test
    void fileAboveTwoMegabytesIsRejected() {
        byte[] tooBig = new byte[CsvImportReader.MAX_FILE_BYTES + 1];
        java.util.Arrays.fill(tooBig, (byte) 'a');

        assertThatThrownBy(() -> reader.decode(tooBig))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("2 MB");
    }

    // IMP-05
    @Test
    void fileOfExactlyTwoMegabytesIsAccepted() {
        byte[] limit = new byte[CsvImportReader.MAX_FILE_BYTES];
        java.util.Arrays.fill(limit, (byte) 'a');

        assertThat(reader.decode(limit)).hasSize(CsvImportReader.MAX_FILE_BYTES);
    }

    // IMP-05
    @Test
    void emptyLinesAreIgnoredAndLineNumbersCountTheHeaderAsLineOne() {
        List<CsvLine> lines = reader.lines("nome;nuit\r\nAna;400000001\n\n   \r\nRui;400000002\n");

        assertThat(lines).containsExactly(
                new CsvLine(1, "nome;nuit"),
                new CsvLine(2, "Ana;400000001"),
                new CsvLine(5, "Rui;400000002"));
    }

    // IMP-05
    @Test
    void fiveThousandDataLinesAreAcceptedButOneMoreIsRejected() {
        assertThat(reader.lines(csvWithDataLines(CsvImportReader.MAX_DATA_LINES))).hasSize(5_001);

        assertThatThrownBy(() -> reader.lines(csvWithDataLines(CsvImportReader.MAX_DATA_LINES + 1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("5000");
    }

    // IMP-05
    @Test
    void emptyLinesDoNotCountTowardsTheLineLimit() {
        String csv = csvWithDataLines(CsvImportReader.MAX_DATA_LINES) + "\n\n\n";

        assertThat(reader.lines(csv)).hasSize(5_001);
    }

    private static String csvWithDataLines(int count) {
        StringBuilder csv = new StringBuilder("nome;nuit\n");
        for (int i = 0; i < count; i++) {
            csv.append("Cliente ").append(i).append(";400000001\n");
        }
        return csv.toString();
    }
}
