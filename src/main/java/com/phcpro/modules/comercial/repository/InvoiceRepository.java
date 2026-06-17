package com.phcpro.modules.comercial.repository;

import com.phcpro.modules.comercial.model.Invoice;
import com.phcpro.modules.comercial.model.SalesChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByCompanyId(Long companyId);

    /** Vendas por canal (ex.: POS), ordenadas pela mais recente. */
    List<Invoice> findByCompanyIdAndSalesChannelOrderByCreatedAtDesc(
            Long companyId, SalesChannel salesChannel);
}
