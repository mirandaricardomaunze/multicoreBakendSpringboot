package mz.multicore.erp.modules.monitoring.service;

import mz.multicore.erp.modules.monitoring.dto.SystemDiagnosticsExportDTO;
import mz.multicore.erp.modules.monitoring.dto.SystemHealthDTO;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Construtor e formatador de relatórios textuais de diagnóstico e telemetria do sistema.
 */
@Service
public class SystemDiagnosticsReportBuilder {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public SystemDiagnosticsExportDTO buildExport(SystemHealthDTO snapshot, Map<String, String> runtimeEnv, List<String> auditHighlights) {
        String reportText = buildFormattedDiagnosticReport(snapshot, runtimeEnv, auditHighlights);
        return new SystemDiagnosticsExportDTO(snapshot, runtimeEnv, auditHighlights, Instant.now(), reportText);
    }

    public String buildFormattedDiagnosticReport(SystemHealthDTO s, Map<String, String> env, List<String> audits) {
        StringBuilder sb = new StringBuilder();
        sb.append("=================================================================\n");
        sb.append("      MULTICORE ERP - RELATÓRIO DE DIAGNÓSTICO DO SISTEMA\n");
        sb.append("=================================================================\n");
        sb.append("Data de Emissão : ").append(LocalDateTime.ofInstant(s.timestamp(), ZoneId.systemDefault()).format(DATE_TIME_FMT)).append("\n");
        sb.append("Estado Geral    : ").append(s.overallStatus()).append("\n");
        sb.append("Versão Servidor : ").append(s.serverVersion()).append("\n");
        sb.append("Uptime Total    : ").append(formatUptime(s.uptimeSeconds())).append("\n");
        sb.append("Ambiente JVM    : Java ").append(s.javaVersion()).append(" (").append(s.osName()).append(")\n\n");

        sb.append("--- [1. RECURSOS DE RUNTIME] ---\n");
        sb.append(String.format("Memória Heap     : %d MB usados de %d MB (%.1f%%)\n",
                s.memory().heapUsedBytes() / (1024 * 1024),
                s.memory().heapMaxBytes() / (1024 * 1024),
                s.memory().heapUsedPercent()));
        sb.append(String.format("Memória Non-Heap : %d MB\n", s.memory().nonHeapUsedBytes() / (1024 * 1024)));
        sb.append(String.format("Disco Principal  : %d GB livres de %d GB (%.1f%% ocupado)\n",
                s.storage().freeBytes() / (1024 * 1024 * 1024),
                s.storage().totalBytes() / (1024 * 1024 * 1024),
                s.storage().usedPercent()));
        sb.append(String.format("Threads JVM      : %d activas (Pico: %d, Daemon: %d)\n\n",
                s.threadPool().liveThreads(),
                s.threadPool().peakThreads(),
                s.threadPool().daemonThreads()));

        sb.append("--- [2. PERSISTÊNCIA & BASE DE DADOS] ---\n");
        sb.append("Estado da BD     : ").append(s.database().status()).append("\n");
        sb.append("Motor / Engine   : ").append(s.database().databaseEngine()).append("\n");
        sb.append("Latência de Ping : ").append(s.database().responseTimeMs()).append(" ms\n");
        sb.append(String.format("Conexões Pool    : %d activas / %d ociosas (Máx: %d)\n\n",
                s.database().activeConnections(),
                s.database().idleConnections(),
                s.database().maxConnections()));

        sb.append("--- [3. SUBSISTEMAS AUDITADOS] ---\n");
        for (var sub : s.subsystems()) {
            sb.append(String.format("• [%-8s] %-32s : %s (%d ms)\n",
                    sub.status(), sub.name(), sub.details(), sub.latencyMs()));
        }
        sb.append("\n");

        sb.append("--- [4. VARIÁVEIS DE AMBIENTE] ---\n");
        env.forEach((k, v) -> sb.append(String.format("• %-24s: %s\n", k, v)));
        sb.append("\n");

        sb.append("--- [5. REGISTOS RECENTES DE AUDITORIA] ---\n");
        for (String a : audits) {
            sb.append(a).append("\n");
        }
        sb.append("=================================================================\n");
        return sb.toString();
    }

    public String formatUptime(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (days > 0) return String.format("%d d, %d h, %d min", days, hours, minutes);
        if (hours > 0) return String.format("%d h, %d min, %d s", hours, minutes, seconds);
        return String.format("%d min, %d s", minutes, seconds);
    }
}
