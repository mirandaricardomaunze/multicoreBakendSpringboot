package com.phcpro.modules.comercial.dto;

/** Produto do catálogo POS acompanhado do estado vendável calculado pelo servidor. */
public record POSCatalogItemDTO(ProductDTO product, boolean sellable) {}
