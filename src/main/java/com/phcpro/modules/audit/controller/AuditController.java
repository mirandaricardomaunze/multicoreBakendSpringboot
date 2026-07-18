package com.phcpro.modules.audit.controller;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.audit.dto.AuditLogDTO;
import com.phcpro.modules.audit.model.AuditLog;
import com.phcpro.modules.audit.service.AuditLogService;
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
