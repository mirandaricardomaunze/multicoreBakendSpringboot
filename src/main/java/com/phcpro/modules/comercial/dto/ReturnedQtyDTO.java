package com.phcpro.modules.comercial.dto;

import java.math.BigDecimal;

/** Quantidade já devolvida (por nota de crédito) de uma linha de fatura. */
public record ReturnedQtyDTO(Long invoiceLineId, BigDecimal quantity) {}
