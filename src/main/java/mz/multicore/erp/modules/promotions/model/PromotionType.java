package mz.multicore.erp.modules.promotions.model;

/**
 * Tipo de promoção de loja.
 *
 * <ul>
 *   <li>{@link #PERCENT} — desconto percentual sobre o preço (sobre um produto ou uma categoria).</li>
 *   <li>{@link #BUY_X_GET_Y} — "leve X, pague Y" (ex.: leve 3, pague 2) sobre um produto.</li>
 * </ul>
 */
public enum PromotionType {
    PERCENT,
    BUY_X_GET_Y
}
