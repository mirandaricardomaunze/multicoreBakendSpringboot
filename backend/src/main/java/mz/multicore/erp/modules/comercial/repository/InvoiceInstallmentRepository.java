package mz.multicore.erp.modules.comercial.repository;

import mz.multicore.erp.modules.comercial.model.InvoiceInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceInstallmentRepository extends JpaRepository<InvoiceInstallment, Long> {

    @Query("SELECT i FROM InvoiceInstallment i WHERE i.invoice.id = :invoiceId AND i.company.id = :companyId ORDER BY i.installmentNumber ASC")
    List<InvoiceInstallment> findByInvoiceIdAndCompanyIdOrderByInstallmentNumberAsc(
            @Param("invoiceId") Long invoiceId, @Param("companyId") Long companyId);

    @Query("SELECT i FROM InvoiceInstallment i WHERE i.company.id = :companyId AND i.status <> 'PAID' ORDER BY i.dueDate ASC")
    List<InvoiceInstallment> findPendingByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT i FROM InvoiceInstallment i WHERE i.company.id = :companyId AND i.dueDate BETWEEN :startDate AND :endDate ORDER BY i.dueDate ASC")
    List<InvoiceInstallment> findByCompanyIdAndDueDateBetween(
            @Param("companyId") Long companyId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    void deleteByInvoiceIdAndCompanyId(Long invoiceId, Long companyId);

    Optional<InvoiceInstallment> findByIdAndCompanyId(Long id, Long companyId);
}
