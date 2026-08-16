package mz.multicore.erp.modules.purchases.repository;

import mz.multicore.erp.modules.purchases.model.GoodsReceiptDiscrepancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GoodsReceiptDiscrepancyRepository extends JpaRepository<GoodsReceiptDiscrepancy, Long> {

    /** Divergências de um período — a base do relatório por fornecedor. */
    List<GoodsReceiptDiscrepancy> findByCompanyIdAndOccurredOnBetweenOrderByOccurredOnDesc(
            Long companyId, LocalDate from, LocalDate to);

    /** Por resolver — o que ainda há a reclamar ao fornecedor. */
    List<GoodsReceiptDiscrepancy> findByCompanyIdAndResolvedFalseOrderByOccurredOnDesc(Long companyId);

    List<GoodsReceiptDiscrepancy> findByPurchaseOrderIdOrderByIdAsc(Long purchaseOrderId);
}
