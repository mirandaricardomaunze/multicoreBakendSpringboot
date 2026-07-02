package com.phcpro.modules.backup.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.PermissionGuard;
import com.phcpro.modules.backup.dto.PhysicalBackupResultDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Backup físico restaurável da base de dados PostgreSQL via {@code pg_dump}/{@code pg_restore}.
 *
 * <p>Ao contrário do {@link BackupService} (dump JSON lógico, lossy, só para verificação), este
 * serviço produz um backup de fidelidade total da instância — o caminho de recuperação de desastres
 * real. Ver {@code docs/BACKUP_RESTORE_SPEC.md}.
 */
@Service
public class DatabaseBackupService {

    private static final long PROCESS_TIMEOUT_MINUTES = 10;
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final String jdbcUrl;
    private final String dbUser;
    private final String dbPassword;
    private final String pgBinDir;
    private final String backupDir;

    public DatabaseBackupService(
            @Value("${spring.datasource.url:}") String jdbcUrl,
            @Value("${spring.datasource.username:}") String dbUser,
            @Value("${spring.datasource.password:}") String dbPassword,
            @Value("${backup.pg-bin-dir:}") String pgBinDir,
            @Value("${backup.dir:backups}") String backupDir
    ) {
        this.jdbcUrl = jdbcUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.pgBinDir = pgBinDir;
        this.backupDir = backupDir;
    }

    /**
     * Gera um backup físico restaurável (pg_dump, formato custom) da base de dados inteira.
     *
     * @return caminho, base de dados e tamanho do ficheiro {@code .dump} gerado.
     */
    public PhysicalBackupResultDTO executePhysicalBackup() {
        PermissionGuard.requireAdmin("gerar backup físico da base de dados");
        PgConnection conn = parsePgConnection(jdbcUrl);

        File dir = new File(backupDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BusinessRuleException("Não foi possível criar a pasta de backups: " + backupDir);
        }
        String fileName = "multicore_" + conn.database() + "_"
                + LocalDateTime.now().format(TIMESTAMP) + ".dump";
        File target = new File(dir, fileName);

        List<String> command = buildDumpCommand(conn, dbUser, target.getAbsolutePath());
        runProcess(command, "pg_dump");

        if (!target.exists() || target.length() == 0) {
            throw new BusinessRuleException("O backup físico não produziu um ficheiro válido.");
        }
        return new PhysicalBackupResultDTO(target.getAbsolutePath(), conn.database(), target.length());
    }

    /**
     * Restaura um backup físico para a base de dados configurada. Operação <b>destrutiva</b>: usa
     * {@code --clean}, substituindo o conteúdo do alvo.
     *
     * @param path             caminho do ficheiro {@code .dump}.
     * @param confirmOverwrite tem de ser {@code true} — guarda contra restore acidental.
     */
    public void restorePhysicalBackup(String path, boolean confirmOverwrite) {
        PermissionGuard.requireAdmin("restaurar backup físico da base de dados");
        if (!confirmOverwrite) {
            throw new BusinessRuleException(
                    "Restaurar substitui os dados actuais. Confirme a operação para continuar.");
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
        runProcess(command, "pg_restore");
    }

    // ─── Helpers puros (testáveis sem ambiente) ──────────────────────────────

    /** Liga a um JDBC URL de PostgreSQL os componentes host/porta/base de dados. */
    PgConnection parsePgConnection(String url) {
        if (url == null || !url.startsWith("jdbc:postgresql://")) {
            throw new BusinessRuleException(
                    "Backup físico só é suportado em PostgreSQL. Base de dados actual não é compatível.");
        }
        String rest = url.substring("jdbc:postgresql://".length());
        int queryIdx = rest.indexOf('?');
        if (queryIdx >= 0) {
            rest = rest.substring(0, queryIdx);
        }
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
        return new PgConnection(host, port, database);
    }

    List<String> buildDumpCommand(PgConnection conn, String user, String targetPath) {
        List<String> cmd = new ArrayList<>();
        cmd.add(binary("pg_dump"));
        cmd.add("-h");
        cmd.add(conn.host());
        cmd.add("-p");
        cmd.add(String.valueOf(conn.port()));
        cmd.add("-U");
        cmd.add(user);
        cmd.add("-F");
        cmd.add("c");
        cmd.add("-f");
        cmd.add(targetPath);
        cmd.add(conn.database());
        return cmd;
    }

    List<String> buildRestoreCommand(PgConnection conn, String user, String sourcePath) {
        List<String> cmd = new ArrayList<>();
        cmd.add(binary("pg_restore"));
        cmd.add("-h");
        cmd.add(conn.host());
        cmd.add("-p");
        cmd.add(String.valueOf(conn.port()));
        cmd.add("-U");
        cmd.add(user);
        cmd.add("--clean");
        cmd.add("--if-exists");
        cmd.add("--no-owner");
        cmd.add("-d");
        cmd.add(conn.database());
        cmd.add(sourcePath);
        return cmd;
    }

    private String binary(String name) {
        if (pgBinDir == null || pgBinDir.isBlank()) {
            return name;
        }
        return new File(pgBinDir, name).getPath();
    }

    private void runProcess(List<String> command, String tool) {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        // Password só via ambiente do subprocesso, nunca na linha de comando.
        if (dbPassword != null && !dbPassword.isBlank()) {
            pb.environment().put("PGPASSWORD", dbPassword);
        }
        try {
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = process.waitFor(PROCESS_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessRuleException(tool + " excedeu o tempo limite e foi cancelado.");
            }
            if (process.exitValue() != 0) {
                throw new BusinessRuleException(tool + " falhou: " + output.trim());
            }
        } catch (IOException e) {
            throw new BusinessRuleException(
                    "Não foi possível executar " + tool + ". Verifique se está instalado e no PATH "
                            + "(ou configure backup.pg-bin-dir). Detalhe: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessRuleException(tool + " foi interrompido.");
        }
    }

    /** Componentes de ligação extraídos do JDBC URL. */
    record PgConnection(String host, int port, String database) {
    }
}
