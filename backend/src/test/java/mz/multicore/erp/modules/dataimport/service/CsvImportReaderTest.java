package mz.multicore.erp.modules.dataimport.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

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
}
