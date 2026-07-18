package com.phcpro.desktop.client;

import com.phcpro.modules.backup.dto.BackupStatusDTO;
import com.phcpro.modules.backup.dto.BackupVerificationDTO;
import com.phcpro.modules.backup.dto.PhysicalBackupResultDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP para as cópias de segurança ({@code /api/backup}). As operações correm no servidor
 * (onde está a BD); a auditoria é registada server-side.
 */
@Component
@Profile("desktop")
public class BackupApiClient {

    private final DesktopClientFactory clientFactory;

    public BackupApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public BackupStatusDTO status() {
        return clientFactory.authenticatedClient().get("/api/backup/status", BackupStatusDTO.class);
    }

    public List<String> files() {
        return clientFactory.authenticatedClient().getList("/api/backup/files", String.class);
    }

    public AutoRunResult runAuto() {
        return clientFactory.authenticatedClient().post("/api/backup/auto-run", null, AutoRunResult.class);
    }

    public String executeBackup() {
        PathResult result = clientFactory.authenticatedClient().post("/api/backup/logical", null, PathResult.class);
        return result == null ? null : result.path();
    }

    public PhysicalBackupResultDTO executePhysical() {
        return clientFactory.authenticatedClient().post("/api/backup/physical", null, PhysicalBackupResultDTO.class);
    }

    public BackupVerificationDTO verify(String fileName) {
        return clientFactory.authenticatedClient()
                .post("/api/backup/verify", Map.of("fileName", fileName), BackupVerificationDTO.class);
    }

    public record AutoRunResult(LocalDateTime time, boolean success, String message) {}

    record PathResult(String path) {}
}
