package mz.multicore.erp.modules.printing;

import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.inventory.model.ProductBatch;
import mz.multicore.erp.modules.inventory.repository.ProductBatchRepository;
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

    private static final java.time.format.DateTimeFormatter DATE_FMT = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

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

        String effectiveBatch = (batchNumber != null && !batchNumber.isBlank() && !"-".equals(batchNumber))
                ? batchNumber.trim()
                : resolveActiveBatch(product);

        LocalDate expiry = isDisplayableBatch(effectiveBatch) ? resolveExpiry(product, effectiveBatch) : null;

        if (isDisplayableBatch(effectiveBatch)) {
            String batchTag = "\n[Lote: " + effectiveBatch + "]";
            description = (description != null ? description : "") + batchTag;
        }

        return new LineItemsTableRenderer.Row(
                barcode,
                reference,
                description,
                expiry,
                quantity,
                product == null ? null : product.getPackagesPerBox(),
                product == null ? null : product.getUnitsPerPackage(),
                unitPrice,
                taxRate,
                discountPercentage,
                lineTotal
        );
    }

    public static boolean isDisplayableBatch(String batch) {
        if (batch == null || batch.isBlank()) return false;
        String b = batch.trim().toUpperCase();
        return !b.startsWith("LEGACY") && !b.startsWith("DEF") && !b.startsWith("PADR")
                && !b.startsWith("SEM-LOTE") && !"-".equals(b) && !"—".equals(b)
                && !"N/A".equals(b) && !"NONE".equals(b);
    }

    private String resolveActiveBatch(Product product) {
        if (product == null || product.getId() == null) return null;
        try {
            return batchRepository.findFirstByProductIdOrderByExpirationDateAsc(product.getId())
                    .map(ProductBatch::getBatchNumber)
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
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
