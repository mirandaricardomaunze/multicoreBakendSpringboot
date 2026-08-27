package mz.multicore.erp.modules.pos.scale;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Lê etiquetas de balança (código de barras de medida variável) segundo um {@link ScaleBarcodeFormat}.
 * Lógica <b>pura</b> (sem Spring, sem IO) — testável directamente; o bean é construído em
 * {@link ScaleConfig} a partir de {@code retail.scale.*}.
 *
 * <p>Fluxo no POS: {@link #parse(String)} → se presente, é uma etiqueta de balança; resolve-se o
 * artigo pelo {@code itemCode} (PLU) e calcula-se a quantidade em quilos com {@link #weightKg} ou,
 * quando a balança embute o preço, com {@link #priceMt} (o POS deriva o peso = preço ÷ preço/kg).
 */
public class ScaleBarcodeParser {

    private final ScaleBarcodeFormat format;

    public ScaleBarcodeParser(ScaleBarcodeFormat format) {
        this.format = format == null ? ScaleBarcodeFormat.defaults() : format;
    }

    public ScaleBarcodeFormat format() {
        return format;
    }

    /** true quando a balança imprime o <b>preço</b> já calculado (em vez do peso). */
    public boolean embedsPrice() {
        return format.getEmbedded() == EmbeddedMeasure.PRICE;
    }

    /**
     * Interpreta {@code raw} como etiqueta de balança. Devolve vazio quando <b>não</b> é uma etiqueta
     * de medida variável (comprimento ≠ 13, não-numérico, prefixo diferente, ou formato inválido) —
     * nesse caso o chamador deve seguir o caminho normal de código de barras.
     */
    public Optional<ScaleBarcode> parse(String raw) {
        if (!format.isValid() || raw == null) {
            return Optional.empty();
        }
        String code = raw.trim();
        if (code.length() != ScaleBarcodeFormat.EAN13_LENGTH) {
            return Optional.empty();
        }
        if (!code.chars().allMatch(Character::isDigit)) {
            return Optional.empty();
        }
        if (!code.startsWith(format.getPrefix())) {
            return Optional.empty();
        }
        int start = format.getPrefix().length();
        int measureStart = start + format.getItemDigits();
        String itemCode = code.substring(start, measureStart);
        String measureStr = code.substring(measureStart, measureStart + format.getMeasureDigits());
        long measure;
        try {
            measure = Long.parseLong(measureStr);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        return Optional.of(new ScaleBarcode(itemCode, measure));
    }

    /** Peso em quilos embutido na etiqueta (medida ÷ divisor de peso; ex.: gramas ÷ 1000). */
    public BigDecimal weightKg(ScaleBarcode barcode) {
        return BigDecimal.valueOf(barcode.measure())
                .divide(BigDecimal.valueOf(format.getWeightDivisor()), 3, RoundingMode.HALF_UP);
    }

    /** Preço total (MT) embutido na etiqueta (medida ÷ divisor de preço; ex.: cêntimos ÷ 100). */
    public BigDecimal priceMt(ScaleBarcode barcode) {
        return BigDecimal.valueOf(barcode.measure())
                .divide(BigDecimal.valueOf(format.getPriceDivisor()), 2, RoundingMode.HALF_UP);
    }
}
