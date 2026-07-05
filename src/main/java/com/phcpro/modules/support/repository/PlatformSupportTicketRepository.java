package com.phcpro.modules.support.repository;

import com.phcpro.modules.support.model.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Tickets de assistência empresa→plataforma. Nome com prefixo {@code Platform} para não colidir
 * (nome de bean) com {@code crm.repository.SupportTicketRepository}, que é outra coisa (suporte ao
 * cliente da empresa).
 */
@Repository
public interface PlatformSupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<SupportTicket> findAllByOrderByCreatedAtDesc();
}
