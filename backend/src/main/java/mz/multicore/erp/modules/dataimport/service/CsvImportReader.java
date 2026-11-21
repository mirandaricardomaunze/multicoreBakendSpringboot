package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lê ficheiros CSV de importação de outro ERP (IMPORTACAO_DADOS_SPEC §3.2).
 *
 * <p>Classe pura, sem Spring: recebe os bytes do ficheiro e devolve o texto.
 * UTF-8 (com ou sem BOM) é a codificação preferida; se os bytes não forem UTF-8
 * válido, o ficheiro é lido como Windows-1252 (exportações do Excel e do Primavera).</p>
 */
public class CsvImportReader {

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
    private static final char BOM = (char) 0xFEFF;
    private static final char QUOTE = '"';
    private static final char SEMICOLON = ';';
    private static final char COMMA = ',';

    /** Tamanho máximo do ficheiro: 2 MB. */
    public static final int MAX_FILE_BYTES = 2 * 1024 * 1024;
    /** Número máximo de linhas de dados (sem cabeçalho nem linhas vazias). */
    public static final int MAX_DATA_LINES = 5_000;

    /**
     * Converte os bytes do ficheiro em texto, sem BOM, com os acentos intactos.
     * Ficheiro acima de {@link #MAX_FILE_BYTES} é recusado.
     */
    public String decode(byte[] content) {
        if (content == null || content.length == 0) {
            return "";
        }
        if (content.length > MAX_FILE_BYTES) {
            throw new BusinessRuleException("O ficheiro excede o limite de 2 MB.");
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
     * Parte o texto nas linhas não vazias (a primeira é o cabeçalho), cada uma com o seu número no
     * ficheiro, contando o cabeçalho como linha 1. Linhas vazias ou só com espaços são ignoradas.
     * Mais de {@link #MAX_DATA_LINES} linhas de dados recusa o ficheiro.
     */
    public List<CsvLine> lines(String text) {
        String[] physical = text.split("\\r?\\n|\\r", -1);
        List<CsvLine> lines = new ArrayList<>();
        for (int i = 0; i < physical.length; i++) {
            if (!physical[i].isBlank()) {
                lines.add(new CsvLine(i + 1, physical[i]));
            }
        }
        if (lines.size() - 1 > MAX_DATA_LINES) {
            throw new BusinessRuleException("O ficheiro excede o limite de " + MAX_DATA_LINES + " linhas de dados.");
        }
        return lines;
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

    /**
     * Reconhece as colunas do cabeçalho pelo nome do modelo, sem distinguir maiúsculas, acentos nem
     * espaços nas pontas. Coluna obrigatória em falta recusa o ficheiro inteiro; colunas a mais são
     * ignoradas e devolvidas em {@link CsvHeader#extraColumns()} para serem anunciadas.
     */
    public CsvHeader readHeader(List<String> headerFields, List<String> requiredColumns, List<String> optionalColumns) {
        Map<String, String> known = new LinkedHashMap<>();
        requiredColumns.forEach(column -> known.put(ImportText.comparisonKey(column), column));
        optionalColumns.forEach(column -> known.put(ImportText.comparisonKey(column), column));

        Map<String, Integer> columnIndex = new LinkedHashMap<>();
        List<String> extraColumns = new ArrayList<>();
        for (int i = 0; i < headerFields.size(); i++) {
            String original = headerFields.get(i) == null ? "" : headerFields.get(i).strip();
            String column = known.get(ImportText.comparisonKey(original));
            if (column == null) {
                if (!original.isEmpty()) {
                    extraColumns.add(original);
                }
            } else {
                columnIndex.putIfAbsent(column, i);
            }
        }

        List<String> missing = requiredColumns.stream().filter(column -> !columnIndex.containsKey(column)).toList();
        if (!missing.isEmpty()) {
            throw new BusinessRuleException((missing.size() == 1
                    ? "Coluna obrigatória em falta no ficheiro: "
                    : "Colunas obrigatórias em falta no ficheiro: ") + String.join(", ", missing) + ".");
        }
        return new CsvHeader(columnIndex, extraColumns);
    }

    /**
     * Lê o ficheiro inteiro: descodifica, detecta o separador, reconhece o cabeçalho e separa os campos
     * de cada linha de dados. Ficheiro vazio (sem cabeçalho) é recusado.
     */
    public CsvTable read(byte[] content, List<String> requiredColumns, List<String> optionalColumns) {
        List<CsvLine> lines = lines(decode(content));
        if (lines.isEmpty()) {
            throw new BusinessRuleException("O ficheiro está vazio: falta a linha de cabeçalho.");
        }
        char separator = detectSeparator(lines.get(0).text());
        CsvHeader header = readHeader(parseLine(lines.get(0).text(), separator), requiredColumns, optionalColumns);
        List<CsvTable.Row> rows = lines.subList(1, lines.size()).stream()
                .map(line -> new CsvTable.Row(line.number(), parseLine(line.text(), separator)))
                .toList();
        return new CsvTable(header, rows);
    }
}
