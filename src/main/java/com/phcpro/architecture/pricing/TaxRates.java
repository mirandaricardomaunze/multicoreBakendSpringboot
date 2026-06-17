package com.phcpro.architecture.pricing;

import java.math.BigDecimal;

/**
 * Fonte única de verdade para as taxas de IVA do sistema.
 *
 * Em Moçambique a taxa-padrão de IVA é 16% (desde 2023). Aplicar uma taxa fixa
 * a todas as linhas — a taxa NÃO depende do NUIT do cliente/fornecedor.
 *
 * Quando for necessário suportar produtos isentos ou de taxa reduzida, mover a
 * taxa para o {@code Product} e ler daí; esta constante passa a ser apenas o
 * valor por omissão.
 */
public final class TaxRates {

    /** Taxa-padrão de IVA aplicada por omissão (16%). */
    public static final BigDecimal STANDARD_VAT = new BigDecimal("0.16");

    private TaxRates() {}
}
