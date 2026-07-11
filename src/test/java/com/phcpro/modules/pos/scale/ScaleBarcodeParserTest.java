package com.phcpro.modules.pos.scale;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes do {@link ScaleBarcodeParser} (SB-01..SB-10) — lógica pura, sem Spring.
 * Formato de referência (defaults): prefixo {@code 2} + PLU 5 + peso 6 (gramas) + dígito de controlo.
 */
class ScaleBarcodeParserTest {

    private final ScaleBarcodeParser weightParser = new ScaleBarcodeParser(ScaleBarcodeFormat.defaults());

    // SB-01 — etiqueta de peso: "2" + "00042" + "001500"(=1500 g) + "0"
    @Test
    void parsesWeightLabel() {
        Optional<ScaleBarcode> parsed = weightParser.parse("2000420015000");
        assertTrue(parsed.isPresent());
        assertEquals("00042", parsed.get().itemCode());
        assertEquals(1500L, parsed.get().measure());
        assertEquals(0, new BigDecimal("1.500").compareTo(weightParser.weightKg(parsed.get())));
        assertFalse(weightParser.embedsPrice());
    }

    // SB-02 — etiqueta de preço: balança embute o total já calculado (cêntimos ÷ 100)
    @Test
    void parsesPriceLabel() {
        ScaleBarcodeParser priceParser = new ScaleBarcodeParser(new ScaleBarcodeFormat(
                true, "2", 5, 6, EmbeddedMeasure.PRICE, 1000, 100));
        Optional<ScaleBarcode> parsed = priceParser.parse("2000420123500"); // 012350 -> 12350 cêntimos
        assertTrue(parsed.isPresent());
        assertTrue(priceParser.embedsPrice());
        assertEquals(0, new BigDecimal("123.50").compareTo(priceParser.priceMt(parsed.get())));
    }

    // SB-03 — EAN de fabricante (prefixo diferente) não é etiqueta de balança
    @Test
    void ignoresNonScaleEan() {
        assertTrue(weightParser.parse("5601234567890").isEmpty());
    }

    // SB-04 — comprimento diferente de 13 é ignorado
    @Test
    void ignoresWrongLength() {
        assertTrue(weightParser.parse("20004200150").isEmpty());
    }

    // SB-05 — código não-numérico é ignorado
    @Test
    void ignoresNonNumeric() {
        assertTrue(weightParser.parse("2000420ABC000").isEmpty());
    }

    // SB-06 — desactivado por configuração: tudo é tratado como código normal
    @Test
    void disabledFormatParsesNothing() {
        ScaleBarcodeParser off = new ScaleBarcodeParser(new ScaleBarcodeFormat(
                false, "2", 5, 6, EmbeddedMeasure.WEIGHT, 1000, 100));
        assertTrue(off.parse("2000420015000").isEmpty());
    }

    // SB-07 — configuração inválida (campos não somam 13) falha em segurança
    @Test
    void invalidFormatFailsSafe() {
        ScaleBarcodeParser bad = new ScaleBarcodeParser(new ScaleBarcodeFormat(
                true, "2", 5, 5, EmbeddedMeasure.WEIGHT, 1000, 100)); // 1+5+5+1 = 12 ≠ 13
        assertFalse(bad.format().isValid());
        assertTrue(bad.parse("2000420015000").isEmpty());
    }

    // SB-08 — prefixo multi-dígito ("20") + PLU 4 + peso 6 + controlo
    @Test
    void parsesMultiDigitPrefix() {
        ScaleBarcodeParser p = new ScaleBarcodeParser(new ScaleBarcodeFormat(
                true, "20", 4, 6, EmbeddedMeasure.WEIGHT, 1000, 100));
        Optional<ScaleBarcode> parsed = p.parse("2000420015000");
        assertTrue(parsed.isPresent());
        assertEquals("0042", parsed.get().itemCode());
        assertEquals(1500L, parsed.get().measure());
    }

    // SB-09 — espaços nas pontas são tolerados
    @Test
    void trimsWhitespace() {
        assertTrue(weightParser.parse("  2000420015000  ").isPresent());
    }

    // SB-10 — null / vazio devolvem vazio sem rebentar
    @Test
    void handlesNullAndEmpty() {
        assertTrue(weightParser.parse(null).isEmpty());
        assertTrue(weightParser.parse("").isEmpty());
    }
}
