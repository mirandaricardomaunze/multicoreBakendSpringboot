package com.phcpro.modules.backup.dto;

/**
 * Resultado de um backup físico (pg_dump) da base de dados.
 */
public record PhysicalBackupResultDTO(
        String filePath,
        String database,
        long sizeBytes
) {
}
