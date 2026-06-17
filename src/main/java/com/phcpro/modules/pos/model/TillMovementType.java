package com.phcpro.modules.pos.model;

/**
 * Tipo de movimento de caixa (gaveta).
 * SALE/SUPRIMENTO = entradas; SANGRIA/REFUND = saidas.
 */
public enum TillMovementType {
    SALE,
    SUPRIMENTO,
    SANGRIA,
    REFUND
}
