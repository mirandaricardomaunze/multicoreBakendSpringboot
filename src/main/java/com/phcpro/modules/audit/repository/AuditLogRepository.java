package com.phcpro.modules.audit.repository;

import com.phcpro.modules.audit.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByOrderByEventTimeDesc();
    List<AuditLog> findByCompanyIdOrderByEventTimeDesc(Long companyId);
}
