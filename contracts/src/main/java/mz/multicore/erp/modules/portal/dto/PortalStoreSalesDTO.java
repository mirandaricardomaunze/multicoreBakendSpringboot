package mz.multicore.erp.modules.portal.dto;

import java.math.BigDecimal;

/** Vendas realizadas de uma loja/armazém hoje e no mês corrente. */
public record PortalStoreSalesDTO(String store, BigDecimal today, BigDecimal month) {}
