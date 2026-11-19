package mz.multicore.erp.modules.dataimport.service;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Lê ficheiros CSV de importação de outro ERP (IMPORTACAO_DADOS_SPEC §3.2).
 *
 * <p>Classe pura, sem Spring: recebe os bytes do ficheiro e devolve o texto.
 * UTF-8 (com ou sem BOM) é a codificação preferida; se os bytes não forem UTF-8
 * válido, o ficheiro é lido como Windows-1252 (exportações do Excel e do Primavera).</p>
 */
public class CsvImportReader {

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
    private static final char BOM = '﻿';

    /** Converte os bytes do ficheiro em texto, sem BOM, com os acentos intactos. */
    public String decode(byte[] content) {
        if (content == null || content.length == 0) {
            return "";
        }
        String text = decodeStrictUtf8(content);
        if (text == null) {
            text = new String(content, WINDOWS_1252);
        }
        return stripBom(text);
    }

    private static String decodeStrictUtf8(byte[] content) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(content)).toString();
        } catch (CharacterCodingException notUtf8) {
            return null;
        }
    }

    private static String stripBom(String text) {
        return !text.isEmpty() && text.charAt(0) == BOM ? text.substring(1) : text;
    }
}
