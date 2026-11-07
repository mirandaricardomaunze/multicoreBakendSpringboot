package mz.multicore.erp.modules.pos.repository;

import mz.multicore.erp.modules.pos.model.PaymentEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface PaymentEntryRepository extends JpaRepository<PaymentEntry, Long> {
    List<PaymentEntry> findByInvoiceIdOrderByPaidAtAsc(Long invoiceId);

    List<PaymentEntry> findByInvoiceCompanyIdAndPaidAtBetween(
            Long companyId, LocalDateTime from, LocalDateTime to);

    Optional<PaymentEntry> findByInvoiceIdAndIdempotencyKey(Long invoiceId, String idempotencyKey);

    List<PaymentEntry> findByInvoiceCompanyIdAndInvoiceIdIn(Long companyId, java.util.Collection<Long> invoiceIds);
}
