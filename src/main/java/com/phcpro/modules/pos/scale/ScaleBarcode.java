package com.phcpro.modules.pos.scale;

/**
 * Resultado da leitura sintáctica de uma etiqueta de balança (código de barras de medida
 * variável): o <b>código do artigo</b> (PLU, tal como impresso pela balança) e a <b>medida</b>
 * embutida em bruto ({@code measure}) — gramas ou cêntimos, conforme a configuração. A conversão
 * para quilos/meticais é feita pelo {@link ScaleBarcodeParser}.
 */
public record ScaleBarcode(String itemCode, long measure) {}
