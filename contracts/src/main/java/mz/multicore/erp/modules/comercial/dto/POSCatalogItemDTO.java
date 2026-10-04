package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;

/**
 * Produto do catálogo POS acompanhado do estado vendável e saldo em stock calculado pelo servidor
 * para o armazém selecionado ou para a empresa inteira.
 */
public record POSCatalogItemDTO(ProductDTO product, boolean sellable, BigDecimal stockQuantity) {

    public POSCatalogItemDTO(ProductDTO product, boolean sellable) {
        this(product, sellable, null);
    }
}
