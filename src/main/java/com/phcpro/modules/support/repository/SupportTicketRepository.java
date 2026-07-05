package com.phcpro.modules.support.repository;

import com.phcpro.modules.support.model.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<SupportTicket> findAllByOrderByCreatedAtDesc();
}
