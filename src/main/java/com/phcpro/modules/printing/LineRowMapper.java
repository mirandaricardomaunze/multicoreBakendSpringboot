package com.phcpro.modules.printing;

import com.phcpro.modules.comercial.model.Product;
import com.phcpro.modules.inventory.model.ProductBatch;
import com.phcpro.modules.inventory.repository.ProductBatchRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Traduz uma linha de documento de domínio (fatura, encomenda, nota de crédito) numa
 * {@link LineItemsTableRenderer.Row} — resolvendo código de barras, referência, descrição do artigo e a
 * validade do lote. Centralizado num só sítio para todos os documentos partilharem a mesma lógica (DRY).
 */
@Component
public class LineRowMapper {

    private final ProductBatchRepository batchRepository;

    public LineRowMapper(ProductBatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    public LineItemsTableRenderer.Row map(
            Product product,
            String batchNumber,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal taxRate,
            BigDecimal discountPercentage,
            BigDecimal lineTotal
    ) {
        String barcode = product == null ? null : product.getBarcode();
        String reference = product == null ? null : product.getReference();
        String description = product == null ? null : product.getName();
        return new LineItemsTableRenderer.Row(
                barcode,
                reference,
                description,
                resolveExpiry(product, batchNumber),
                quantity,
                unitPrice,
                taxRate,
                discountPercentage,
                lineTotal
        );
    }

    private LocalDate resolveExpiry(Product product, String batchNumber) {
        if (product == null || batchNumber == null || batchNumber.isBlank()) {
            return null;
        }
        return batchRepository
                .findFirstByProductIdAndBatchNumberOrderByExpirationDateAsc(product.getId(), batchNumber)
                .map(ProductBatch::getExpirationDate)
                .orElse(null);
    }
}
