package mz.multicore.erp.modules.backup.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.backup.dto.PhysicalBackupResultDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Backup físico restaurável da base de dados PostgreSQL via {@code pg_dump}/{@code pg_restore}.
 */
@Service
public class DatabaseBackupService {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final String jdbcUrl;
    private final String dbUser;
    private final String backupDir;
    private final PostgresBackupCommandBuilder commandBuilder;
    private final PostgresBackupProcessRunner processRunner;
    private final BackupEncryption encryption;

    @Autowired
    public DatabaseBackupService(
            @Value("${spring.datasource.url:}") String jdbcUrl,
            @Value("${spring.datasource.username:}") String dbUser,
            @Value("${backup.dir:backups}") String backupDir,
            PostgresBackupCommandBuilder commandBuilder,
            PostgresBackupProcessRunner processRunner,
            BackupEncryption encryption
    ) {
        this.encryption = encryption;
        this.jdbcUrl = jdbcUrl;
        this.dbUser = dbUser;
        this.backupDir = backupDir;
        this.commandBuilder = commandBuilder;
        this.processRunner = processRunner;
    }

    public DatabaseBackupService(
            String jdbcUrl,
            String dbUser,
            String dbPassword,
            String pgBinDir,
            String backupDir
    ) {
        this(jdbcUrl, dbUser, backupDir, new PostgresBackupCommandBuilder(pgBinDir), new PostgresBackupProcessRunner(dbPassword), null);
    }

    public PhysicalBackupResultDTO executePhysicalBackup() {
        PermissionGuard.requireAdmin("gerar backup físico da base de dados");
        return runPhysicalBackup();
    }

    public boolean supportsPhysicalBackup() {
        return jdbcUrl != null && jdbcUrl.startsWith("jdbc:postgresql://");
    }

    public PhysicalBackupResultDTO runPhysicalBackup() {
        PgConnection conn = parsePgConnection(jdbcUrl);
        File dir = new File(backupDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BusinessRuleException("Não foi possível criar a pasta de backups: " + backupDir);
        }
        String fileName = "multicore_" + conn.database() + "_" + LocalDateTime.now().format(TIMESTAMP) + ".dump";
        File target = new File(dir, fileName);

        List<String> command = buildDumpCommand(conn, dbUser, target.getAbsolutePath());
        processRunner.runProcess(command, "pg_dump");

        if (!target.exists() || target.length() == 0) {
            throw new BusinessRuleException("O backup físico não produziu um ficheiro válido.");
        }
        File stored = encryption != null ? encryption.seal(target) : target;
        return new PhysicalBackupResultDTO(stored.getAbsolutePath(), conn.database(), stored.length());
    }

    public void restorePhysicalBackup(String path, boolean confirmOverwrite) {
        PermissionGuard.requireAdmin("restaurar backup físico da base de dados");
        File file = BackupEncryption.requireRestorableFile(path, confirmOverwrite);
        boolean encrypted = file.getName().toLowerCase(Locale.ROOT).endsWith(BackupFileCipher.ENCRYPTED_SUFFIX);
        PgConnection conn = parsePgConnection(jdbcUrl);
        if (encrypted) {
            encryption.withDecrypted(file, plain -> processRunner.runProcess(
                    buildRestoreCommand(conn, dbUser, plain.getAbsolutePath()), "pg_restore"));
            return;
        }
        List<String> command = buildRestoreCommand(conn, dbUser, file.getAbsolutePath());
        processRunner.runProcess(command, "pg_restore");
    }

    PgConnection parsePgConnection(String url) {
        return commandBuilder.parsePgConnection(url);
    }

    List<String> buildDumpCommand(PgConnection conn, String user, String targetPath) {
        return commandBuilder.buildDumpCommand(conn, user, targetPath);
    }

    List<String> buildRestoreCommand(PgConnection conn, String user, String sourcePath) {
        return commandBuilder.buildRestoreCommand(conn, user, sourcePath);
    }

    record PgConnection(String host, int port, String database) {}
}
