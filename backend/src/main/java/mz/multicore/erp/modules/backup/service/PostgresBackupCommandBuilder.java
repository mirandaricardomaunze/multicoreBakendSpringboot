package mz.multicore.erp.modules.backup.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class PostgresBackupCommandBuilder {

    private final String pgBinDir;

    public PostgresBackupCommandBuilder(@Value("${backup.pg-bin-dir:}") String pgBinDir) {
        this.pgBinDir = pgBinDir;
    }

    public DatabaseBackupService.PgConnection parsePgConnection(String url) {
        if (url == null || !url.startsWith("jdbc:postgresql://")) {
            throw new BusinessRuleException("Backup físico só é suportado em PostgreSQL. Base de dados actual não é compatível.");
        }
        String rest = url.substring("jdbc:postgresql://".length());
        int queryIdx = rest.indexOf('?');
        if (queryIdx >= 0) rest = rest.substring(0, queryIdx);
        int slashIdx = rest.indexOf('/');
        if (slashIdx < 0 || slashIdx == rest.length() - 1) {
            throw new BusinessRuleException("URL da base de dados inválido: nome da base de dados ausente.");
        }
        String authority = rest.substring(0, slashIdx);
        String database = rest.substring(slashIdx + 1);
        String host = authority;
        int port = 5432;
        int colonIdx = authority.indexOf(':');
        if (colonIdx >= 0) {
            host = authority.substring(0, colonIdx);
            String portText = authority.substring(colonIdx + 1);
            try {
                port = Integer.parseInt(portText);
            } catch (NumberFormatException e) {
                throw new BusinessRuleException("URL da base de dados inválido: porta '" + portText + "'.");
            }
        }
        if (host.isBlank() || database.isBlank()) {
            throw new BusinessRuleException("URL da base de dados inválido.");
        }
        return new DatabaseBackupService.PgConnection(host, port, database);
    }

    public List<String> buildDumpCommand(DatabaseBackupService.PgConnection conn, String user, String targetPath) {
        return new ArrayList<>(List.of(
                binary("pg_dump"), "-h", conn.host(), "-p", String.valueOf(conn.port()),
                "-U", user, "-F", "c", "-f", targetPath, conn.database()
        ));
    }

    public List<String> buildListCommand(String sourcePath) {
        return List.of(binary("pg_restore"), "--list", sourcePath);
    }

    public List<String> buildRestoreCommand(DatabaseBackupService.PgConnection conn, String user, String sourcePath) {
        return new ArrayList<>(List.of(
                binary("pg_restore"), "-h", conn.host(), "-p", String.valueOf(conn.port()),
                "-U", user, "--clean", "--if-exists", "--no-owner", "-d", conn.database(), sourcePath
        ));
    }

    private String binary(String name) {
        return (pgBinDir == null || pgBinDir.isBlank()) ? name : new File(pgBinDir, name).getPath();
    }
}
