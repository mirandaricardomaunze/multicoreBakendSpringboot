package mz.multicore.erp.modules.monitoring.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.backup.service.ScheduledBackupService;
import mz.multicore.erp.modules.monitoring.dto.SystemDiagnosticsExportDTO;
import mz.multicore.erp.modules.monitoring.dto.SystemHealthDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço de observabilidade e diagnóstico em tempo real do Multicore ERP.
 * Orquestrador desacoplado delegando a recolha de telemetria e geração de relatórios.
 */
@Service
public class SystemMonitoringService {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final SystemTelemetryCollector telemetryCollector;
    private final SystemDiagnosticsReportBuilder reportBuilder;
    private final AuditLogService auditLogService;
    private final String serverVersion;

    @Autowired
    public SystemMonitoringService(
            SystemTelemetryCollector telemetryCollector,
            SystemDiagnosticsReportBuilder reportBuilder,
            AuditLogService auditLogService,
            @Value("${app.version:1.0.0}") String serverVersion) {
        this.telemetryCollector = telemetryCollector;
        this.reportBuilder = reportBuilder;
        this.auditLogService = auditLogService;
        this.serverVersion = serverVersion;
    }

    /** Construtor retrocompatível para testes legados e configurações manuais. */
    public SystemMonitoringService(
            DataSource dataSource,
            ScheduledBackupService scheduledBackupService,
            AuditLogService auditLogService,
            @Value("${app.version:1.0.0}") String serverVersion,
            @Value("${backup.dir:backups}") String backupDir) {
        this(new SystemTelemetryCollector(dataSource, scheduledBackupService),
                new SystemDiagnosticsReportBuilder(),
                auditLogService,
                serverVersion);
    }

    public SystemHealthDTO getSystemHealth() {
        PermissionGuard.requireMonitoringAdmin("consultar telemetria e diagnósticos do sistema");
        return telemetryCollector.collectSystemHealth(serverVersion);
    }

    public SystemDiagnosticsExportDTO getDiagnosticsExport() {
        return exportDiagnostics();
    }

    public SystemDiagnosticsExportDTO exportDiagnostics() {
        SystemHealthDTO snapshot = getSystemHealth();
        Map<String, String> runtimeEnv = collectRuntimeEnv();
        List<String> auditHighlights = collectAuditHighlights();
        return reportBuilder.buildExport(snapshot, runtimeEnv, auditHighlights);
    }

    private Map<String, String> collectRuntimeEnv() {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("java.version", System.getProperty("java.version"));
        env.put("java.vendor", System.getProperty("java.vendor"));
        env.put("os.name", System.getProperty("os.name"));
        env.put("os.version", System.getProperty("os.version"));
        env.put("user.timezone", System.getProperty("user.timezone", "UTC"));
        env.put("server.version", serverVersion);
        env.put("available.processors", String.valueOf(Runtime.getRuntime().availableProcessors()));
        return env;
    }

    private List<String> collectAuditHighlights() {
        List<String> highlights = new ArrayList<>();
        if (auditLogService == null) {
            highlights.add("Aviso: Serviço de auditoria não configurado.");
            return highlights;
        }
        try {
            var logs = (PermissionGuard.SUPERADMIN_ROLE.equalsIgnoreCase(CurrentUserContext.getRole())
                    ? auditLogService.getAllLogs()
                    : auditLogService.getLogsByCompany(CurrentUserContext.requireCurrentCompanyId()))
                    .stream().limit(20).toList();
            for (var l : logs) {
                highlights.add(String.format("[%s] %s: %s (%s)",
                        l.getEventTime() != null ? l.getEventTime().format(DATE_TIME_FMT) : "—",
                        l.getUsername() != null ? l.getUsername() : "sistema",
                        l.getAction(),
                        l.getDetails() != null ? l.getDetails() : ""));
            }
        } catch (Exception e) {
            highlights.add("Aviso: Histórico de auditoria geral restrito ou não disponível.");
        }
        return highlights;
    }
}
