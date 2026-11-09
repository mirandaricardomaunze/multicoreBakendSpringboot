package mz.multicore.erp.modules.exceptions.repository;

import mz.multicore.erp.modules.audit.model.AuditLog;
import mz.multicore.erp.modules.bulkstock.model.BulkMeasurement;
import mz.multicore.erp.modules.butchery.model.CarcassBreakdown;
import mz.multicore.erp.modules.comercial.model.DeliveryGuide;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.consignment.model.ConsignmentDelivery;
import mz.multicore.erp.modules.inventory.model.StockWaste;
import mz.multicore.erp.modules.pickup.model.PickupAccount;
import mz.multicore.erp.modules.pos.model.TillSession;
import mz.multicore.erp.modules.productionplan.model.ProductionPlanLine;
import mz.multicore.erp.modules.weighbridge.model.WeighingTicket;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Read-only queries behind the exception report. It reads records of several modules on purpose: a control
 * crosses modules (an operator, a period), and nothing here writes.
 */
public interface ExceptionQueryRepository extends org.springframework.data.repository.Repository<AuditLog, Long> {

    @Query("select a from AuditLog a where a.companyId = :companyId and a.eventTime >= :from and a.eventTime < :to and a.action in :actions")
    List<AuditLog> audit(@Param("companyId") Long companyId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                         @Param("actions") Collection<String> actions);

    @Query("select t from TillSession t where t.company.id = :companyId and t.closeDate >= :from and t.closeDate < :to "
            + "and t.difference is not null")
    List<TillSession> closedTills(@Param("companyId") Long companyId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select w from StockWaste w join fetch w.product where w.company.id = :companyId and w.createdAt >= :from and w.createdAt < :to")
    List<StockWaste> wastes(@Param("companyId") Long companyId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select b from CarcassBreakdown b join fetch b.sourceProduct where b.company.id = :companyId and b.createdAt >= :from "
            + "and b.createdAt < :to and b.status = mz.multicore.erp.modules.butchery.model.CarcassBreakdownStatus.POSTED")
    List<CarcassBreakdown> breakdowns(@Param("companyId") Long companyId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select l from ProductionPlanLine l join fetch l.plan p join fetch l.product where p.company.id = :companyId "
            + "and p.planDate >= :from and p.planDate <= :to "
            + "and p.status = mz.multicore.erp.modules.productionplan.model.ProductionPlanStatus.CLOSED")
    List<ProductionPlanLine> closedPlanLines(@Param("companyId") Long companyId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select d from ConsignmentDelivery d join fetch d.client where d.company.id = :companyId "
            + "and d.status = mz.multicore.erp.modules.consignment.model.ConsignmentStatus.OPEN and d.deliveryDate < :before")
    List<ConsignmentDelivery> unsettledConsignments(@Param("companyId") Long companyId, @Param("before") LocalDate before);

    @Query("select g from DeliveryGuide g where g.company.id = :companyId "
            + "and g.status = mz.multicore.erp.modules.comercial.model.DeliveryGuideStatus.APPROVED and g.invoiceId is null "
            + "and g.guideDate < :before")
    List<DeliveryGuide> guidesWithoutInvoice(@Param("companyId") Long companyId, @Param("before") LocalDateTime before);

    @Query("select a from PickupAccount a join fetch a.invoice i left join fetch i.client where a.company.id = :companyId "
            + "and a.status = mz.multicore.erp.modules.pickup.model.PickupAccountStatus.OPEN and a.deadline < :today")
    List<PickupAccount> overduePickups(@Param("companyId") Long companyId, @Param("today") LocalDate today);

    @Query("select m from BulkMeasurement m join fetch m.product where m.company.id = :companyId and m.createdAt >= :from and m.createdAt < :to")
    List<BulkMeasurement> bulkMeasurements(@Param("companyId") Long companyId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select t from WeighingTicket t where t.company.id = :companyId and t.manual = true and t.firstAt >= :from and t.firstAt < :to")
    List<WeighingTicket> manualWeighings(@Param("companyId") Long companyId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select l from InvoiceLine l join fetch l.invoice i join fetch l.product where i.company.id = :companyId "
            + "and i.createdAt >= :from and i.createdAt < :to and l.discountPercentage > :min and i.status in :statuses")
    List<InvoiceLine> discountedLines(@Param("companyId") Long companyId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                      @Param("min") BigDecimal min, @Param("statuses") Collection<InvoiceStatus> statuses);
}
