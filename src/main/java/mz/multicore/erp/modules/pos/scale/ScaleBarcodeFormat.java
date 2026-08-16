package mz.multicore.erp.modules.pos.scale;

/**
 * Formato do código de barras de <b>medida variável</b> impresso pelas balanças de loja (EAN-13).
 * As balanças de mercearia (carne, queijo, fruta pesada ao balcão) imprimem uma etiqueta cujo
 * código não é um EAN de fabricante — traz embutido o <b>artigo (PLU)</b> e a <b>medida</b> (peso
 * ou preço). O layout assumido é, da esquerda para a direita:
 *
 * <pre>{@code
 *   [ prefixo ][ artigo (PLU) ][ medida ][ dígito de controlo ]   → 13 dígitos (EAN-13)
 * }</pre>
 *
 * Tudo é configurável via {@code retail.scale.*} (ver {@link ScaleConfig}); os valores por omissão
 * ({@link #defaults()}) cobrem o caso comum {@code 2 + 5 + 6 + 1 = 13} com peso em gramas.
 */
public class ScaleBarcodeFormat {

    /** Comprimento fixo de um EAN-13. */
    public static final int EAN13_LENGTH = 13;

    private final boolean enabled;
    private final String prefix;
    private final int itemDigits;
    private final int measureDigits;
    private final EmbeddedMeasure embedded;
    private final int weightDivisor;
    private final int priceDivisor;

    public ScaleBarcodeFormat(boolean enabled, String prefix, int itemDigits, int measureDigits,
                              EmbeddedMeasure embedded, int weightDivisor, int priceDivisor) {
        this.enabled = enabled;
        this.prefix = prefix == null ? "" : prefix.trim();
        this.itemDigits = itemDigits;
        this.measureDigits = measureDigits;
        this.embedded = embedded == null ? EmbeddedMeasure.WEIGHT : embedded;
        this.weightDivisor = weightDivisor <= 0 ? 1000 : weightDivisor;
        this.priceDivisor = priceDivisor <= 0 ? 100 : priceDivisor;
    }

    /** Caso comum: prefixo {@code 2}, PLU de 5 dígitos, peso em gramas de 6 dígitos, dígito de controlo. */
    public static ScaleBarcodeFormat defaults() {
        return new ScaleBarcodeFormat(true, "2", 5, 6, EmbeddedMeasure.WEIGHT, 1000, 100);
    }

    /**
     * O formato só é utilizável se estiver activo, o prefixo for de dígitos, os campos forem
     * positivos e a soma {@code prefixo + PLU + medida + 1 (controlo)} der exactamente 13.
     * Se não for válido, o parser ignora todos os códigos (falha segura → tudo é tratado como
     * código de barras normal).
     */
    public boolean isValid() {
        return enabled
                && !prefix.isEmpty()
                && prefix.chars().allMatch(Character::isDigit)
                && itemDigits > 0
                && measureDigits > 0
                && prefix.length() + itemDigits + measureDigits + 1 == EAN13_LENGTH;
    }

    public boolean isEnabled()          { return enabled; }
    public String getPrefix()           { return prefix; }
    public int getItemDigits()          { return itemDigits; }
    public int getMeasureDigits()       { return measureDigits; }
    public EmbeddedMeasure getEmbedded() { return embedded; }
    public int getWeightDivisor()       { return weightDivisor; }
    public int getPriceDivisor()        { return priceDivisor; }
}
