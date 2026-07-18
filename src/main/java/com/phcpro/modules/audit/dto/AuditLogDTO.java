package com.phcpro.modules.audit.dto;

import java.time.LocalDateTime;

/** Vista de leitura de um registo de auditoria para a UI. */
public record AuditLogDTO(
        LocalDateTime eventTime,
        String username,
        String action,
        String details
) {}
