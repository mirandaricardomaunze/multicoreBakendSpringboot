package mz.multicore.erp.modules.backup.dto;

import java.time.LocalDateTime;

/** Estado do backup automático para a UI (achatado; campos de última execução a null se nunca correu). */
public record BackupStatusDTO(
        boolean autoEnabled,
        LocalDateTime lastTime,
        Boolean lastSuccess,
        String lastMessage
) {}
