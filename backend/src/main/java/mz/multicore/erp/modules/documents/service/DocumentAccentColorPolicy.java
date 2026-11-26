package mz.multicore.erp.modules.documents.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.format.DisplayNumbers;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Regra da cor de destaque do modelo de documento (MODELOS_DOCUMENTO_SPEC §3.1, DT-01):
 * hexadecimal {@code #RRGGBB} com contraste mínimo de 4,5:1 com branco (WCAG), para que o texto
 * branco impresso em cima da cor se leia. Vazio = sem cor própria (o PDF usa a cor do {@code PdfTheme}).
 */
public final class DocumentAccentColorPolicy {

    public static final BigDecimal MIN_CONTRAST_WITH_WHITE = new BigDecimal("4.5");

    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9A-Fa-f]{6}");
    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal CHANNEL_MAX = BigDecimal.valueOf(255);
    private static final BigDecimal LINEAR_THRESHOLD = new BigDecimal("0.04045");
    private static final BigDecimal LINEAR_DIVISOR = new BigDecimal("12.92");
    private static final BigDecimal GAMMA_OFFSET = new BigDecimal("0.055");
    private static final BigDecimal GAMMA_DIVISOR = new BigDecimal("1.055");
    private static final BigDecimal FLARE = new BigDecimal("0.05");
    private static final BigDecimal WEIGHT_RED = new BigDecimal("0.2126");
    private static final BigDecimal WEIGHT_GREEN = new BigDecimal("0.7152");
    private static final BigDecimal WEIGHT_BLUE = new BigDecimal("0.0722");

    private DocumentAccentColorPolicy() {}

    /**
     * Valida e normaliza a cor escolhida.
     *
     * @return {@code #RRGGBB} em maiúsculas, ou {@code null} quando vazia (usa a cor por omissão)
     * @throws BusinessRuleException formato inválido ou contraste com branco abaixo de 4,5:1
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String hex = raw.trim().toUpperCase(Locale.ROOT);
        if (!HEX_COLOR.matcher(hex).matches()) {
            throw new BusinessRuleException(
                    "A cor de destaque tem de estar no formato #RRGGBB (ex.: #1F2937).");
        }
        BigDecimal contrast = contrastWithWhite(hex);
        if (contrast.compareTo(MIN_CONTRAST_WITH_WHITE) < 0) {
            throw new BusinessRuleException("A cor " + hex + " tem pouco contraste com branco ("
                    + DisplayNumbers.qty(contrast.setScale(1, RoundingMode.DOWN))
                    + ":1). Escolha uma cor mais escura (mínimo 4,5:1).");
        }
        return hex;
    }

    /** Razão de contraste WCAG entre a cor {@code #RRGGBB} e branco (1:1 a 21:1). */
    public static BigDecimal contrastWithWhite(String hex) {
        int rgb = Integer.parseInt(hex.substring(1), 16);
        BigDecimal luminance = WEIGHT_RED.multiply(linear((rgb >> 16) & 0xFF), MC)
                .add(WEIGHT_GREEN.multiply(linear((rgb >> 8) & 0xFF), MC), MC)
                .add(WEIGHT_BLUE.multiply(linear(rgb & 0xFF), MC), MC);
        return BigDecimal.ONE.add(FLARE).divide(luminance.add(FLARE), MC);
    }

    /** Canal sRGB (0..255) para luminância linear (0..1). */
    private static BigDecimal linear(int channel) {
        BigDecimal c = BigDecimal.valueOf(channel).divide(CHANNEL_MAX, MC);
        if (c.compareTo(LINEAR_THRESHOLD) <= 0) {
            return c.divide(LINEAR_DIVISOR, MC);
        }
        BigDecimal base = c.add(GAMMA_OFFSET).divide(GAMMA_DIVISOR, MC);
        return new BigDecimal(Math.pow(base.doubleValue(), 2.4), MC);
    }
}
