package com.phcpro.desktop.client;

import com.phcpro.modules.audit.dto.AuditLogDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/** Cliente HTTP para os registos de auditoria ({@code /api/audit}). Empresa vem do X-Company-Id. */
@Component
@Profile("desktop")
public class AuditApiClient {

    private final DesktopClientFactory clientFactory;

    public AuditApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    /** {@code companyId} é ignorado — o servidor deriva-o do X-Company-Id. Mantido para o call site. */
    public List<AuditLogDTO> getLogsByCompany(Long companyId) {
        return clientFactory.authenticatedClient().getList("/api/audit/logs", AuditLogDTO.class);
    }
}
