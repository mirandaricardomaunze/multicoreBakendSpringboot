package mz.multicore.erp.modules.monitoring.controller;

import lombok.RequiredArgsConstructor;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.monitoring.dto.*;
import mz.multicore.erp.modules.monitoring.service.SystemAlertEmailService;
import mz.multicore.erp.modules.monitoring.service.SystemIncidentManager;
import mz.multicore.erp.modules.monitoring.service.SystemMonitoringService;
import mz.multicore.erp.modules.monitoring.service.TenantMonitoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class SystemMonitoringController {

    private final SystemMonitoringService monitoringService;
    private final TenantMonitoringService tenantMonitoringService;
    private final SystemIncidentManager systemIncidentManager;
    private final SystemAlertEmailService systemAlertEmailService;

    @GetMapping("/system-health")
    public ResponseEntity<SystemHealthDTO> getSystemHealth() {
        return ResponseEntity.ok(monitoringService.getSystemHealth());
    }

    @GetMapping("/diagnostics-export")
    public ResponseEntity<SystemDiagnosticsExportDTO> getDiagnosticsExport() {
        return ResponseEntity.ok(monitoringService.getDiagnosticsExport());
    }

    @GetMapping("/tenants-health")
    public ResponseEntity<List<TenantHealthDTO>> getTenantsHealth() {
        return ResponseEntity.ok(tenantMonitoringService.getTenantsHealth());
    }

    @GetMapping("/incidents")
    public ResponseEntity<List<SystemAlertIncidentDTO>> getIncidents() {
        PermissionGuard.requireSuperAdmin("consultar incidentes globais");
        return ResponseEntity.ok(systemIncidentManager.getRecentIncidents());
    }

    @PostMapping("/test-email-alert")
    public ResponseEntity<SystemAlertTestResultDTO> testEmailAlert(
            @RequestParam(name = "recipientEmail", required = false) String recipientEmail
    ) {
        PermissionGuard.requireSuperAdmin("enviar email de teste de monitorização");
        return ResponseEntity.ok(systemAlertEmailService.testEmailAlert(recipientEmail));
    }
}
