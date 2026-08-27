package mz.multicore.erp.modules.comercial.repository;

import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.model.SalesChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
}
