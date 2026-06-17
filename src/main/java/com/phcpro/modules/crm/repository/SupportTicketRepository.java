package com.phcpro.modules.crm.repository;

import com.phcpro.modules.crm.model.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByClientId(Long clientId);
    List<SupportTicket> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    java.util.Optional<SupportTicket> findByIdAndCompanyId(Long id, Long companyId);
}
