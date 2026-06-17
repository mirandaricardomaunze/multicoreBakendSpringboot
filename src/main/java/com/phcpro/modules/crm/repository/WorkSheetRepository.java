package com.phcpro.modules.crm.repository;

import com.phcpro.modules.crm.model.WorkSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkSheetRepository extends JpaRepository<WorkSheet, Long> {
    List<WorkSheet> findBySupportTicketId(Long ticketId);
    List<WorkSheet> findBySupportTicketCompanyIdOrderByCreatedAtDesc(Long companyId);
    java.util.Optional<WorkSheet> findByIdAndSupportTicketCompanyId(Long id, Long companyId);
}
