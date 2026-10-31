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

    @Autowired
    public DatabaseBackupService(
            @Value("${spring.datasource.url:}") String jdbcUrl,
            @Value("${spring.datasource.username:}") String dbUser,
            @Value("${backup.dir:backups}") String backupDir,
            PostgresBackupCommandBuilder commandBuilder,
            PostgresBackupProcessRunner processRunner
    ) {
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
        this(jdbcUrl, dbUser, backupDir, new PostgresBackupCommandBuilder(pgBinDir), new PostgresBackupProcessRunner(dbPassword));
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
        return new PhysicalBackupResultDTO(target.getAbsolutePath(), conn.database(), target.length());
    }

    public void restorePhysicalBackup(String path, boolean confirmOverwrite) {
        PermissionGuard.requireAdmin("restaurar backup físico da base de dados");
        if (!confirmOverwrite) {
            throw new BusinessRuleException("Restaurar substitui os dados actuais. Confirme a operação para continuar.");
        }
        if (path == null || path.isBlank()) {
            throw new BusinessRuleException("Selecione um ficheiro de backup para restaurar.");
        }
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            throw new BusinessRuleException("Ficheiro de backup não encontrado.");
        }
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".dump")) {
            throw new BusinessRuleException("O ficheiro selecionado não é um backup físico (.dump) válido.");
        }
        PgConnection conn = parsePgConnection(jdbcUrl);
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
