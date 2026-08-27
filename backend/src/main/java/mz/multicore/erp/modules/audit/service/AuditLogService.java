package mz.multicore.erp.modules.audit.service;

import mz.multicore.erp.modules.audit.model.AuditLog;
import mz.multicore.erp.modules.audit.repository.AuditLogRepository;
import mz.multicore.erp.architecture.security.CurrentUserContext;
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
