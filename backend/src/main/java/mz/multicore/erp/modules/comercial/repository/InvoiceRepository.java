package mz.multicore.erp.modules.comercial.repository;

import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.model.SalesChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByCompanyId(Long companyId);

    /** Página de faturas da empresa, da mais recente para a mais antiga. */
    Page<Invoice> findByCompanyIdOrderByCreatedAtDesc(Long companyId, Pageable pageable);

    /** Vendas por canal (ex.: POS), ordenadas pela mais recente. */
    List<Invoice> findByCompanyIdAndSalesChannelOrderByCreatedAtDesc(
            Long companyId, SalesChannel salesChannel);

    /** Página de vendas por canal (histórico do POS, que cresce todos os dias). */
    Page<Invoice> findByCompanyIdAndSalesChannelOrderByCreatedAtDesc(
            Long companyId, SalesChannel salesChannel, Pageable pageable);

    /** Página de vendas por canal filtrada por intervalo de criação. */
    @Query("""
            select i from Invoice i
            where i.company.id = :companyId
              and i.salesChannel = :salesChannel
              and (:from is null or i.createdAt >= :from)
              and (:to is null or i.createdAt <= :to)
            order by i.createdAt desc
            """)
    Page<Invoice> findSalesPage(
            @Param("companyId") Long companyId,
            @Param("salesChannel") SalesChannel salesChannel,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("""
            select count(i) from Invoice i
            where i.company.id = :companyId
              and i.salesChannel = :salesChannel
              and (:from is null or i.createdAt >= :from)
              and (:to is null or i.createdAt <= :to)
            """)
    long countSales(
            @Param("companyId") Long companyId,
            @Param("salesChannel") SalesChannel salesChannel,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            select coalesce(sum(i.totalAmount), 0) from Invoice i
            where i.company.id = :companyId
              and i.salesChannel = :salesChannel
              and (:from is null or i.createdAt >= :from)
              and (:to is null or i.createdAt <= :to)
            """)
    BigDecimal sumSalesTotal(
            @Param("companyId") Long companyId,
            @Param("salesChannel") SalesChannel salesChannel,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * Documentos de um intervalo de datas. Existe para o dashboard e o relatório diário não
     * terem de carregar <b>todas</b> as faturas da empresa só para olhar para um dia.
     */
    List<Invoice> findByCompanyIdAndCreatedAtBetween(Long companyId, LocalDateTime from, LocalDateTime to);

    /**
     * Documentos nos estados indicados — usar com {@code InvoiceStatus.collectableStatuses()}
     * para que a pergunta "o que está por cobrar?" seja feita à base de dados.
     */
    List<Invoice> findByCompanyIdAndStatusIn(Long companyId, Collection<InvoiceStatus> statuses);

    /** Procura fatura já gravada para a mesma referência de contingência (idempotência offline). */
    java.util.Optional<Invoice> findByCompanyIdAndContingencyReference(Long companyId, String contingencyReference);

    /**
     * Quantidade total vendida por produto para documentos realizados desde determinada data.
     * Utilizado para cálculo de velocidade de rotação (Daily Sales Velocity).
     */
    @Query("""
            select l.product.id, coalesce(sum(l.quantity), 0)
            from InvoiceLine l
            where l.invoice.company.id = :companyId
              and l.invoice.status in :statuses
              and l.invoice.createdAt >= :since
            group by l.product.id
            """)
    List<Object[]> sumQuantitySoldByProductSince(
            @Param("companyId") Long companyId,
            @Param("statuses") Collection<InvoiceStatus> statuses,
            @Param("since") LocalDateTime since);
}
