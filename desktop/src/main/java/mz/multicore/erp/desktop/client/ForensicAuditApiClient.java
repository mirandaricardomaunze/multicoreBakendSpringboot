package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.audit.dto.ForensicAuditSummaryDTO;
import mz.multicore.erp.modules.audit.dto.ForensicCategory;
import mz.multicore.erp.modules.audit.dto.ForensicSeverity;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("desktop")
public class ForensicAuditApiClient {

    private final DesktopClientFactory clientFactory;

    public ForensicAuditApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public ForensicAuditSummaryDTO getSummary(
            LocalDate startDate,
            LocalDate endDate,
            ForensicSeverity severity,
            ForensicCategory category,
            String operator
    ) {
        String path = buildQueryString("/api/audit/forensic", startDate, endDate, severity, category, operator);
        return clientFactory.authenticatedClient().get(path, ForensicAuditSummaryDTO.class);
    }

    public byte[] getPdf(
            LocalDate startDate,
            LocalDate endDate,
            ForensicSeverity severity,
            ForensicCategory category,
            String operator
    ) {
        String path = buildQueryString("/api/audit/forensic/pdf", startDate, endDate, severity, category, operator);
        return clientFactory.authenticatedClient().getBytes(path);
    }

    private String buildQueryString(
            String basePath,
            LocalDate startDate,
            LocalDate endDate,
            ForensicSeverity severity,
            ForensicCategory category,
            String operator
    ) {
        List<String> params = new ArrayList<>();
        if (startDate != null) {
            params.add("startDate=" + startDate);
        }
        if (endDate != null) {
            params.add("endDate=" + endDate);
        }
        if (severity != null) {
            params.add("severity=" + severity.name());
        }
        if (category != null) {
            params.add("category=" + category.name());
        }
        if (operator != null && !operator.isBlank()) {
            params.add("operator=" + URLEncoder.encode(operator.trim(), StandardCharsets.UTF_8));
        }

        if (params.isEmpty()) {
            return basePath;
        }
        return basePath + "?" + String.join("&", params);
    }
}
