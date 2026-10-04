package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.monitoring.dto.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Cliente HTTP desacoplado para monitoramento de saúde do sistema e diagnóstico de runtime.
 * Consome apenas os DTOs canónicos definidos no módulo contracts.
 */
@Component
@Profile("desktop")
public class SystemMonitoringApiClient {

    private final DesktopClientFactory clientFactory;

    public SystemMonitoringApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public boolean isSuperAdmin() {
        return clientFactory.isSuperAdmin();
    }

    public SystemHealthDTO getSystemHealth() {
        return clientFactory.authenticatedClient().get("/api/monitoring/system-health", SystemHealthDTO.class);
    }

    public SystemDiagnosticsExportDTO getDiagnosticsExport() {
        return clientFactory.authenticatedClient().get("/api/monitoring/diagnostics-export", SystemDiagnosticsExportDTO.class);
    }

    public List<TenantHealthDTO> getTenantsHealth() {
        return clientFactory.authenticatedClient().getList("/api/monitoring/tenants-health", TenantHealthDTO.class);
    }

    public List<SystemAlertIncidentDTO> getIncidents() {
        return clientFactory.authenticatedClient().getList("/api/monitoring/incidents", SystemAlertIncidentDTO.class);
    }

    public SystemAlertTestResultDTO testEmailAlert(String recipientEmail) {
        String path = "/api/monitoring/test-email-alert";
        if (recipientEmail != null && !recipientEmail.isBlank()) {
            path += "?recipientEmail=" + URLEncoder.encode(recipientEmail.trim(), StandardCharsets.UTF_8);
        }
        return clientFactory.authenticatedClient().post(path, null, SystemAlertTestResultDTO.class);
    }
}
