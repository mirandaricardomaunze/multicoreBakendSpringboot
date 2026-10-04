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
        Long companyId,
        Long version
) {
    public SupplierDTO(Long id, String name, String taxId, String email, String address, String phone, String contactPerson, boolean active, Long companyId) {
        this(id, name, taxId, email, address, phone, contactPerson, active, companyId, 0L);
    }
}
