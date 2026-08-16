package mz.multicore.erp.modules.pos.scale;

/**
 * O que a etiqueta da balança traz embutido no código de barras de medida variável:
 * o <b>peso</b> (o POS calcula o preço = peso × preço/kg) ou o <b>preço total</b> já
 * calculado pela balança (o POS deriva o peso = preço ÷ preço/kg).
 */
public enum EmbeddedMeasure {
    WEIGHT,
    PRICE
}
