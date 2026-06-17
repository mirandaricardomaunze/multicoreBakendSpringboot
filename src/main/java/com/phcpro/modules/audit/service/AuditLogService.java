package com.phcpro.modules.audit.service;

import com.phcpro.modules.audit.model.AuditLog;
import com.phcpro.modules.audit.repository.AuditLogRepository;
import com.phcpro.architecture.security.CurrentUserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void logEvent(String username, Long companyId, String action, String details) {
        AuditLog log = new AuditLog();
        log.setUsername(username != null ? username : "SYSTEM");
        log.setCompanyId(companyId);
        log.setAction(action);
        log.setDetails(details);
        log.setEventTime(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    @Transactional
    public void logCurrent(String action, String details) {
        logEvent(CurrentUserContext.getUsername(), CurrentUserContext.getCurrentCompanyId(), action, details);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByCompany(Long companyId) {
        if (companyId == null) {
            return auditLogRepository.findByOrderByEventTimeDesc();
        }
        return auditLogRepository.findByCompanyIdOrderByEventTimeDesc(companyId);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findByOrderByEventTimeDesc();
    }
}
