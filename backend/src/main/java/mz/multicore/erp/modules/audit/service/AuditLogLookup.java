package mz.multicore.erp.modules.audit.service;

import mz.multicore.erp.modules.audit.model.AuditLog;
import mz.multicore.erp.modules.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Porta de leitura do registo de auditoria para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2).
 * O registo de eventos passa pelo {@link AuditLogService}.
 */
@Service
@Transactional(readOnly = true)
public class AuditLogLookup {

    private final AuditLogRepository auditLogRepository;

    public AuditLogLookup(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /** Eventos da empresa, do mais recente para o mais antigo. */
    public List<AuditLog> listForCompany(Long companyId) {
        if (companyId == null) {
            return List.of();
        }
        return auditLogRepository.findByCompanyIdOrderByEventTimeDesc(companyId);
    }
}
