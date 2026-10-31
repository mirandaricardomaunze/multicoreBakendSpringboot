package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.modules.purchases.dto.PurchaseDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseLineDTO;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DirectPurchaseQueryService {

    private final PurchaseRepository purchaseRepository;

    public DirectPurchaseQueryService(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    public List<Purchase> getPurchasesByCompany(Long companyId) {
        return purchaseRepository.findByCompanyId(companyId);
    }

    public List<PurchaseDTO> findPurchasesByCompany(Long companyId) {
        return purchaseRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    public PurchaseDTO toDTO(Purchase p) {
        List<PurchaseLineDTO> lines = p.getLines().stream().map(l -> new PurchaseLineDTO(
                l.getId(), l.getProduct().getId(), l.getProduct().getName(), l.getProduct().getSku(),
                l.getQuantity(), l.getUnitPrice(), l.getTaxRate(), l.getLineTotal(),
                l.getBatchNumber(), l.getExpirationDate(), l.getSerialNumber()
        )).toList();

        Set<String> bus = p.getLines().stream()
                .map(l -> l.getProduct() != null && l.getProduct().getBusinessUnit() != null ? l.getProduct().getBusinessUnit().getName() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        String buName = bus.isEmpty() ? null : (bus.size() == 1 ? bus.iterator().next() : "Misto");
        Long buId = bus.size() == 1 ? p.getLines().stream()
                .map(l -> l.getProduct() != null && l.getProduct().getBusinessUnit() != null ? l.getProduct().getBusinessUnit().getId() : null)
                .filter(Objects::nonNull).findFirst().orElse(null) : null;

        return new PurchaseDTO(
                p.getId(), p.getPurchaseNumber(),
                p.getSupplier().getId(), p.getSupplier().getName(),
                p.getWarehouse() != null ? p.getWarehouse().getId() : null,
                p.getCompany() != null ? p.getCompany().getId() : null,
                p.getTotalAmount(), p.getTaxAmount(), p.getAmountPaid(),
                p.getStatus(), p.getPurchaseDate(), lines, buId, buName
        );
    }
}
