package mz.multicore.erp.modules.purchases.dto;

public record SupplierDTO(
        Long id,
        String name,
        String taxId,
        String email,
        String address,
        String phone,
        String contactPerson,
        boolean active,
        Long companyId
) {}
