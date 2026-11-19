package mz.multicore.erp.modules.dataimport.service;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
    private static final char QUOTE = '"';
    private static final char SEMICOLON = ';';
    private static final char COMMA = ',';

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

    /**
     * Detecta o separador na linha de cabeçalho: {@code ;} ou {@code ,}, o que aparecer mais vezes
     * fora de aspas. Em empate (ou nenhum) vale {@code ;}, o separador do modelo.
     */
    public char detectSeparator(String headerLine) {
        int semicolons = 0;
        int commas = 0;
        boolean inQuotes = false;
        for (int i = 0; i < headerLine.length(); i++) {
            char c = headerLine.charAt(i);
            if (c == QUOTE) {
                inQuotes = !inQuotes;
            } else if (!inQuotes && c == SEMICOLON) {
                semicolons++;
            } else if (!inQuotes && c == COMMA) {
                commas++;
            }
        }
        return commas > semicolons ? COMMA : SEMICOLON;
    }

    /**
     * Parte uma linha nos seus campos. Campos entre aspas podem conter o separador;
     * {@code ""} dentro de aspas vale uma aspa.
     */
    public List<String> parseLine(String line, char separator) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == QUOTE && i + 1 < line.length() && line.charAt(i + 1) == QUOTE) {
                    field.append(QUOTE);
                    i++;
                } else if (c == QUOTE) {
                    inQuotes = false;
                } else {
                    field.append(c);
                }
            } else if (c == QUOTE) {
                inQuotes = true;
            } else if (c == separator) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        return fields;
    }
}
