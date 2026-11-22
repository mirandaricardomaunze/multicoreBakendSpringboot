package mz.multicore.erp.modules.accounting.dto;

import mz.multicore.erp.modules.accounting.model.CostCenterType;

/**
 * Cost centre of the active company (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.1).
 *
 * @param warehouseId linked warehouse, only for {@link CostCenterType#WAREHOUSE} centres
 */
public record CostCenterDTO(
        Long id,
        String code,
        String name,
        CostCenterType type,
        Long warehouseId,
        boolean active
) {}
