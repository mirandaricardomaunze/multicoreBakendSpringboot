package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.format.DisplayLocale;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/** Valores e datas iguais em todas as tabelas (docs/TABELAS_CONSISTENCIA_SPEC.md, regra 12, TC-14). */
class TableValuesFormatHarnessTest {

    @BeforeAll
    static void displayLocale() {
        Locale.setDefault(Locale.Category.FORMAT, DisplayLocale.NUMBERS);
    }

    @Test
    @DisplayName("TC-14: dinheiro 1.234,50 MT com sinal, unidades à direita, datas dd/MM/yyyy mesmo em ISO")
    void tc14_valuesAndDatesLookTheSameEverywhere() {
        assertThat(UiTableCellStyler.formatValue(new BigDecimal("1234.5"))).isEqualTo("1.234,50");
        assertThat(UiTableCellStyler.formatValue("1234.50 MT")).isEqualTo("1.234,50 MT");
        assertThat(UiTableCellStyler.formatValue(String.format("%.2f MT", 1234.5))).isEqualTo("1.234,50 MT");
        assertThat(UiTableCellStyler.formatValue(String.format("%,.2f MT", 1234567.5))).isEqualTo("1.234.567,50 MT");
        assertThat(UiTableCellStyler.formatValue("-1234.50 MT")).as("negativo também formatado").isEqualTo("-1.234,50 MT");
        assertThat(UiTableCellStyler.formatValue("+250.00 MT")).isEqualTo("+250,00 MT");

        assertThat(UiTableCellStyler.looksNumeric("+250,00 MT")).as("com sinal fica à direita").isTrue();
        assertThat(UiTableCellStyler.looksNumeric("12,50 un")).as("quantidade fica à direita").isTrue();
        assertThat(UiTableCellStyler.looksNumeric("3,5 kg")).isTrue();
        assertThat(UiTableCellStyler.looksNumeric("Armazém 2")).isFalse();

        assertThat(UiTableCellStyler.formatValue(LocalDate.of(2026, 11, 14))).isEqualTo("14/11/2026");
        assertThat(UiTableCellStyler.formatValue(LocalDateTime.of(2026, 11, 14, 10, 20, 30))).isEqualTo("14/11/2026 10:20");
        assertThat(UiTableCellStyler.formatValue("2026-11-14")).isEqualTo("14/11/2026");
        assertThat(UiTableCellStyler.formatValue("2026-11-14T10:20:30.123")).isEqualTo("14/11/2026 10:20");
        assertThat(UiTableCellStyler.formatValue("2026-13-45")).as("data inválida fica como está").isEqualTo("2026-13-45");
        assertThat(UiTableCellStyler.formatValue("FT 2026/0012")).isEqualTo("FT 2026/0012");
    }
}
