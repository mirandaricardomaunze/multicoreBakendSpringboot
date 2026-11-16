package mz.multicore.erp.modules.pickup.repository;

import mz.multicore.erp.modules.comercial.model.NoteStatus;
import mz.multicore.erp.modules.pickup.model.PickupAccount;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Read-only view of approved credit notes, so credited material is never handed over. */
@org.springframework.stereotype.Repository
public interface PickupCreditQueryRepository extends Repository<PickupAccount, Long> {

    @Query("select l.invoiceLine.id, sum(l.quantity) from CreditNoteLine l "
            + "where l.invoiceLine.invoice.id = :invoiceId and l.creditNote.status = :status group by l.invoiceLine.id")
    List<Object[]> creditedByInvoiceLine(@Param("invoiceId") Long invoiceId, @Param("status") NoteStatus status);
}
