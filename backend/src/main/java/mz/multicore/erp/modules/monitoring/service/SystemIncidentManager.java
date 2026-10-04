package mz.multicore.erp.modules.monitoring.service;

import mz.multicore.erp.modules.monitoring.dto.SystemAlertIncidentDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Gestor em memória de incidentes e alarmes técnicos do Multicore ERP.
 * Mantém o histórico dos últimos 50 incidentes operacionais de forma thread-safe.
 */
@Service
public class SystemIncidentManager {

    private static final Logger log = LoggerFactory.getLogger(SystemIncidentManager.class);
    private static final int MAX_INCIDENTS = 50;

    private final Deque<SystemAlertIncidentDTO> incidents = new ConcurrentLinkedDeque<>();

    public SystemAlertIncidentDTO recordIncident(String subsystem, String tenantName, String severity, String message) {
        String incidentId = "INC-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 900 + 100);
        SystemAlertIncidentDTO incident = new SystemAlertIncidentDTO(
                incidentId,
                subsystem != null ? subsystem : "Geral",
                tenantName != null ? tenantName : "Sistema",
                severity != null ? severity.toUpperCase() : "WARNING",
                message,
                Instant.now(),
                false
        );

        incidents.addFirst(incident);
        while (incidents.size() > MAX_INCIDENTS) {
            incidents.pollLast();
        }

        log.warn("Incidente operacional registado [{}]: {} - {} - {}",
                incident.incidentId(), incident.severity(), incident.subsystem(), incident.message());
        return incident;
    }

    public List<SystemAlertIncidentDTO> getRecentIncidents() {
        return List.copyOf(incidents);
    }

    public boolean resolveIncident(String incidentId) {
        if (incidentId == null) return false;
        List<SystemAlertIncidentDTO> updated = new ArrayList<>();
        boolean found = false;

        for (SystemAlertIncidentDTO inc : incidents) {
            if (inc.incidentId().equalsIgnoreCase(incidentId)) {
                updated.add(new SystemAlertIncidentDTO(
                        inc.incidentId(),
                        inc.subsystem(),
                        inc.tenantName(),
                        inc.severity(),
                        inc.message(),
                        inc.timestamp(),
                        true
                ));
                found = true;
            } else {
                updated.add(inc);
            }
        }

        if (found) {
            incidents.clear();
            incidents.addAll(updated);
        }
        return found;
    }

    public void clearAll() {
        incidents.clear();
    }
}
