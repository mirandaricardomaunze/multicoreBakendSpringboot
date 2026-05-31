package com.phcpro.modules.pos.repository;

import com.phcpro.modules.pos.model.PaymentEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentEntryRepository extends JpaRepository<PaymentEntry, Long> {
    List<PaymentEntry> findByInvoiceIdOrderByPaidAtAsc(Long invoiceId);
}
