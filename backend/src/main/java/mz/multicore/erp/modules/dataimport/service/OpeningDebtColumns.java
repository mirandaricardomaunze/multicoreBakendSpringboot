package mz.multicore.erp.modules.dataimport.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

/**
 * Colunas comuns das dívidas em aberto importadas, de clientes (SI, IMPORTACAO_DADOS_SPEC §5.2) e a fornecedores (SIF,
 * §5.3), e as regras que as duas partilham: {@code numero_original} obrigatório, {@code data} não futura,
 * {@code vencimento} ≥ {@code data}, {@code valor_em_divida} &gt; 0. Lê pela ordem das colunas do modelo: o primeiro
 * problema da linha é o que se reporta, na coluna certa.
 */
final class OpeningDebtColumns {

    static final String ORIGINAL_NUMBER = "numero_original";
    static final String DATE = "data";
    static final String DUE_DATE = "vencimento";
    static final String AMOUNT = "valor_em_divida";
    static final String DESCRIPTION = "descricao";

    /** Obrigatórias depois da coluna do NUIT da contraparte, pela ordem do modelo. */
    static final List<String> REQUIRED_AFTER_PARTY = List.of(ORIGINAL_NUMBER, DATE, DUE_DATE, AMOUNT);
    static final List<String> OPTIONAL_COLUMNS = List.of(DESCRIPTION);

    /** Valores de uma dívida já validados. */
    record Values(String originalNumber, LocalDate date, LocalDate dueDate, BigDecimal amount, String description) {
    }

    private final ImportValueParser parser = new ImportValueParser();

    /** Colunas obrigatórias do modelo: o NUIT da contraparte seguido das comuns. */
    static List<String> requiredColumns(String partyTaxIdColumn) {
        return Stream.concat(Stream.of(partyTaxIdColumn), REQUIRED_AFTER_PARTY.stream()).toList();
    }

    /** Lê e valida as colunas comuns; lança {@link ImportValueException} na primeira inválida. */
    Values read(CsvTable table, CsvTable.Row row) {
        int line = row.number();
        String originalNumber = parser.requireText(table.value(row, ORIGINAL_NUMBER), line, ORIGINAL_NUMBER);
        LocalDate date = requireDate(table.value(row, DATE), line, DATE);
        if (date.isAfter(LocalDate.now())) {
            throw new ImportValueException(line, DATE, "a data da factura não pode ser futura.");
        }
        LocalDate dueDate = requireDate(table.value(row, DUE_DATE), line, DUE_DATE);
        if (dueDate.isBefore(date)) {
            throw new ImportValueException(line, DUE_DATE, "o vencimento não pode ser anterior à data da factura.");
        }
        BigDecimal amount = parser.requireDecimal(table.value(row, AMOUNT), line, AMOUNT);
        if (amount.signum() <= 0) {
            throw new ImportValueException(line, AMOUNT, "o valor em dívida tem de ser maior que zero.");
        }
        return new Values(originalNumber, date, dueDate, amount, parser.parseText(table.value(row, DESCRIPTION)));
    }

    private LocalDate requireDate(String raw, int line, String column) {
        return parser.parseDate(parser.requireText(raw, line, column), line, column);
    }
}
