package mz.multicore.erp.modules.dataimport.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Converte o texto das células de um CSV de importação em valores (IMPORTACAO_DADOS_SPEC §3.2).
 *
 * <p>Classe pura, sem Spring. Célula vazia devolve {@code null} (a obrigatoriedade é verificada por
 * quem valida a entidade); formato não aceite lança {@link ImportValueException} com linha e coluna.</p>
 */
public class ImportValueParser {

    /** Com vírgula decimal: pontos só como separadores de milhar em grupos de 3 ({@code 1.234,56}). */
    private static final Pattern DECIMAL_COMMA = Pattern.compile("-?(\\d{1,3}(\\.\\d{3})+|\\d+),\\d+");
    /** Sem vírgula: o ponto é decimal ({@code 1234.56}). */
    private static final Pattern DECIMAL_POINT = Pattern.compile("-?\\d+(\\.\\d+)?");

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT));

    /** Texto da célula sem espaços nas pontas; vazio devolve {@code null}. */
    public String parseText(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.strip();
        return value.isEmpty() ? null : value;
    }

    /**
     * Número em {@code 1.234,56} (vírgula decimal, pontos de milhar) ou {@code 1234.56} (ponto decimal).
     */
    public BigDecimal parseDecimal(String raw, int lineNumber, String column) {
        String value = parseText(raw);
        if (value == null) {
            return null;
        }
        if (DECIMAL_COMMA.matcher(value).matches()) {
            return new BigDecimal(value.replace(".", "").replace(',', '.'));
        }
        if (DECIMAL_POINT.matcher(value).matches()) {
            return new BigDecimal(value);
        }
        throw new ImportValueException(lineNumber, column,
                "valor não numérico «" + value + "» (use 1.234,56 ou 1234.56).");
    }

    /** Data em {@code dd/mm/aaaa} ou {@code aaaa-mm-dd}; datas inexistentes (31/02) são erro. */
    public LocalDate parseDate(String raw, int lineNumber, String column) {
        String value = parseText(raw);
        if (value == null) {
            return null;
        }
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, format);
            } catch (DateTimeParseException tryNextFormat) {
                // segue para o formato seguinte
            }
        }
        throw new ImportValueException(lineNumber, column,
                "data inválida «" + value + "» (use dd/mm/aaaa ou aaaa-mm-dd).");
    }
}
