package mz.multicore.erp.modules.purchases.dto;

import java.math.BigDecimal;

/**
 * Sugestão de reposição de um produto abaixo do stock mínimo. A quantidade sugerida vem arredondada
 * a caixas inteiras (a loja compra ao grosso). Leitura pura — não cria nada.
 */
public record ReorderSuggestionDTO(
        Long productId,
        String sku,
        String name,
        BigDecimal currentStock,
        BigDecimal minStock,
        int unitsPerBox,
        BigDecimal suggestedBoxes,
        BigDecimal suggestedUnits
) {}
