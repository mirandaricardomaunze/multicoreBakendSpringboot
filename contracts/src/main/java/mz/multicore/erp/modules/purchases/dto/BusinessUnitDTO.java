package mz.multicore.erp.modules.purchases.dto;

public record BusinessUnitDTO(
        Long id,
        String code,
        String name,
        Long supplierId,
        String supplierName,
        String description,
        boolean active,
        long productCount
) {
    public BusinessUnitDTO(Long id, String code, String name, Long supplierId, String supplierName, String description, boolean active) {
        this(id, code, name, supplierId, supplierName, description, active, 0L);
    }
}
