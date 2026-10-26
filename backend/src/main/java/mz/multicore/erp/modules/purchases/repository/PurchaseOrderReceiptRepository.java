package mz.multicore.erp.modules.purchases.repository;

import mz.multicore.erp.modules.purchases.model.PurchaseOrderReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseOrderReceiptRepository extends JpaRepository<PurchaseOrderReceipt, Long> {
    Optional<PurchaseOrderReceipt> findByPurchaseOrderIdAndIdempotencyKey(Long purchaseOrderId, String idempotencyKey);
}
