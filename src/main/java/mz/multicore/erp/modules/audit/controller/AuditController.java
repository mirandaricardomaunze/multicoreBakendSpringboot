package mz.multicore.erp.modules.audit.controller;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.dto.AuditLogDTO;
import mz.multicore.erp.modules.audit.model.AuditLog;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Registos de auditoria da empresa activa. Escopo pela empresa do contexto (SecurityInterceptor). */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditLogService auditLogService;

    public AuditController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/logs")
    public List<AuditLogDTO> logs() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return auditLogService.getLogsByCompany(companyId).stream()
                .map(AuditController::toDto)
                .toList();
    }

    private static AuditLogDTO toDto(AuditLog l) {
        return new AuditLogDTO(l.getEventTime(), l.getUsername(), l.getAction(), l.getDetails());
    }
}
