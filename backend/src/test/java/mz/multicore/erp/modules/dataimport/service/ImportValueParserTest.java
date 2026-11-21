package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IMPORTACAO_DADOS_HARNESS — IMP-04: números e datas (SPEC §3.2).
 */
class ImportValueParserTest {

    private final ImportValueParser parser = new ImportValueParser();

    // IMP-04
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "1.234,56   | 1234.56",
            "1234,56    | 1234.56",
            "1.234.567,8| 1234567.8",
            "0,5        | 0.5",
            "1234.56    | 1234.56",
            "1234       | 1234",
            "-12,30     | -12.30",
            "' 17,5 '   | 17.5"
    })
    void parsesNumbersInBothAcceptedFormats(String raw, String expected) {
        BigDecimal value = parser.parseDecimal(raw, 4, "preco");

        assertThat(value).isEqualByComparingTo(expected);
    }

    // IMP-04
    @Test
    void blankNumberIsAbsentNotAnError() {
        assertThat(parser.parseDecimal("   ", 2, "preco")).isNull();
        assertThat(parser.parseDecimal(null, 2, "preco")).isNull();
    }

    // IMP-04
    @ParameterizedTest
    @ValueSource(strings = {"abc", "12,3,4", "1.23,4", "12 345,00", "1,234.56", "12.", ",5", "MT 10"})
    void otherNumberFormatIsARowErrorWithLineAndColumn(String raw) {
        assertThatThrownBy(() -> parser.parseDecimal(raw, 7, "preco"))
                .isInstanceOf(ImportValueException.class)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Linha 7")
                .hasMessageContaining("preco")
                .satisfies(error -> {
                    ImportValueException importError = (ImportValueException) error;
                    assertThat(importError.getLineNumber()).isEqualTo(7);
                    assertThat(importError.getColumn()).isEqualTo("preco");
                });
    }

    // IMP-04
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "21/03/2026  | 2026-03-21",
            "2026-03-21  | 2026-03-21",
            "' 01/12/2025 ' | 2025-12-01",
            "29/02/2024  | 2024-02-29"
    })
    void parsesDatesInBothAcceptedFormats(String raw, String expected) {
        LocalDate value = parser.parseDate(raw, 3, "data");

        assertThat(value).isEqualTo(LocalDate.parse(expected));
    }

    // IMP-04
    @Test
    void blankDateIsAbsentNotAnError() {
        assertThat(parser.parseDate("", 3, "data")).isNull();
    }

    // IMP-04
    @ParameterizedTest
    @ValueSource(strings = {"03/21/2026", "2026/03/21", "21-03-2026", "21/3/2026", "31/02/2026", "29/02/2025", "ontem", "20260321"})
    void otherDateFormatIsARowErrorWithLineAndColumn(String raw) {
        assertThatThrownBy(() -> parser.parseDate(raw, 9, "vencimento"))
                .isInstanceOf(ImportValueException.class)
                .hasMessageContaining("Linha 9")
                .hasMessageContaining("vencimento")
                .hasMessageContaining("dd/mm/aaaa");
    }

    // IMP-04 (SPEC §3.2 — texto)
    @Test
    void textIsStrippedAndBlankIsAbsent() {
        assertThat(parser.parseText("  Loja do Bairro  ")).isEqualTo("Loja do Bairro");
        assertThat(parser.parseText("   ")).isNull();
        assertThat(parser.parseText(null)).isNull();
    }
}
