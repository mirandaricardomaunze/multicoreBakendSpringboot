package mz.multicore.erp.modules.monitoring.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SystemDiagnosticsExportDTO(
        SystemHealthDTO healthSnapshot,
        Map<String, String> runtimeEnvironment,
        List<String> recentAuditHighlights,
        Instant generatedAt,
        String reportFormattedText
) {}
