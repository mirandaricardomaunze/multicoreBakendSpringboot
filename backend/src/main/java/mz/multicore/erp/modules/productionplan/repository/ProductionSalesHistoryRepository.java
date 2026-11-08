package mz.multicore.erp.modules.productionplan.repository;

import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/** Read-only sales history used to suggest the day plan (product, invoice time, quantity). */
public interface ProductionSalesHistoryRepository extends org.springframework.data.repository.Repository<InvoiceLine, Long> {

    @Query("select l.product.id, l.invoice.createdAt, l.quantity from InvoiceLine l where l.invoice.company.id = :companyId "
            + "and l.invoice.createdAt >= :from and l.invoice.createdAt < :to and l.invoice.status in :statuses "
            + "and l.product.id in :productIds")
    List<Object[]> soldLines(@Param("companyId") Long companyId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                             @Param("statuses") Collection<InvoiceStatus> statuses, @Param("productIds") Collection<Long> productIds);
}
