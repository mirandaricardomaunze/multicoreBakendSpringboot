package mz.multicore.erp.modules.manufacturing.service;

import mz.multicore.erp.modules.manufacturing.dto.ProductionOrderDTO;
import mz.multicore.erp.modules.manufacturing.model.ProductionOrder;

/**
 * Mapeador determinístico para Ordens de Produção (ProductionOrder -> ProductionOrderDTO).
 */
public final class ProductionOrderMapper {

    private ProductionOrderMapper() {}

    public static ProductionOrderDTO toOrderDTO(ProductionOrder o) {
        if (o == null) return null;
        return new ProductionOrderDTO(
                o.getId(),
                o.getCompany().getId(),
                o.getOrderNumber(),
                o.getBom() != null ? o.getBom().getId() : null,
                o.getBom() != null ? o.getBom().getBomCode() : "—",
                o.getTargetProduct().getId(),
                o.getTargetProduct().getSku(),
                o.getTargetProduct().getName(),
                o.getPlannedQuantity(),
                o.getProducedQuantity(),
                o.getStatus(),
                o.getPlannedStartDate(),
                o.getPlannedEndDate(),
                o.getCompletionDate(),
                o.getSourceWarehouse() != null ? o.getSourceWarehouse().getId() : null,
                o.getTargetWarehouse() != null ? o.getTargetWarehouse().getId() : null,
                o.getTotalProductionCost(),
                o.getUnitProductionCost()
        );
    }
}
