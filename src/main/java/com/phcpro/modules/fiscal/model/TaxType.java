package com.phcpro.modules.fiscal.model;

/**
 * Categorias fiscais usadas em Moçambique.
 * IVA (normal/reduzida/zero/isento), Retenção na Fonte, IRPC e ICE.
 */
public enum TaxType {
    IVA_STANDARD,      // 16% — taxa normal
    IVA_REDUCED,       // 5%  — alimentos, livros, transportes
    IVA_ZERO,          // 0%  — exportações
    IVA_EXEMPT,        // Isento — saúde, educação, finanças
    WITHHOLDING,       // Retenção na Fonte (10% serviços, 14% rendas, 20% não-residentes)
    CORPORATE_INCOME,  // IRPC — 32%
    EXCISE             // ICE — álcool, tabaco, combustíveis
}
