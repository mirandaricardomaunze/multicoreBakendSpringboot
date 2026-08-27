package mz.multicore.erp.modules.backup.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.backup.service.DatabaseBackupService.PgConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobre os cenários BR-01..BR-12 do harness — parsing, construção de comando e guardas.
 * A execução real de pg_dump/pg_restore é validada manualmente (BR-50..BR-54).
 */
class DatabaseBackupServiceTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    private DatabaseBackupService service() {
        return new DatabaseBackupService(
                "jdbc:postgresql://localhost:5432/multicore", "multicore", "secret", "", "backups");
    }

    private DatabaseBackupService serviceWithBinDir(String binDir) {
        return new DatabaseBackupService(
                "jdbc:postgresql://localhost:5432/multicore", "multicore", "secret", binDir, "backups");
    }

    // ── BR-01..04: parsing do JDBC URL ──────────────────────────────────────

    @Test
    void br01_parseUrlCompleto() {
        PgConnection conn = service().parsePgConnection("jdbc:postgresql://localhost:5432/multicore");
        assertEquals("localhost", conn.host());
        assertEquals(5432, conn.port());
        assertEquals("multicore", conn.database());
    }

    @Test
    void br02_parseUrlSemPorta_usaDefault() {
        PgConnection conn = service().parsePgConnection("jdbc:postgresql://db/multicore");
        assertEquals("db", conn.host());
        assertEquals(5432, conn.port());
        assertEquals("multicore", conn.database());
    }

    @Test
    void br03_parseUrlComQuery_ignoraQuery() {
        PgConnection conn = service()
                .parsePgConnection("jdbc:postgresql://localhost:5432/multicore?sslmode=require");
        assertEquals("multicore", conn.database());
    }

    @Test
    void br04_parseUrlNaoPostgres_lanca() {
        assertThrows(BusinessRuleException.class,
                () -> service().parsePgConnection("jdbc:h2:mem:test"));
    }

    // ── BR-05..07: construção de comandos ───────────────────────────────────

    @Test
    void br05_buildDumpCommand() {
        PgConnection conn = new PgConnection("localhost", 5432, "multicore");
        List<String> cmd = service().buildDumpCommand(conn, "multicore", "/tmp/x.dump");
        assertEquals(
                List.of("pg_dump", "-h", "localhost", "-p", "5432", "-U", "multicore",
                        "-F", "c", "-f", "/tmp/x.dump", "multicore"),
                cmd);
    }

    @Test
    void br06_buildDumpCommand_comBinDir() {
        PgConnection conn = new PgConnection("localhost", 5432, "multicore");
        List<String> cmd = serviceWithBinDir("/usr/pgsql/bin")
                .buildDumpCommand(conn, "multicore", "/tmp/x.dump");
        assertTrue(cmd.get(0).contains("pg_dump"));
        assertTrue(cmd.get(0).contains("pgsql"));
    }

    @Test
    void br07_buildRestoreCommand() {
        PgConnection conn = new PgConnection("localhost", 5432, "multicore");
        List<String> cmd = service().buildRestoreCommand(conn, "multicore", "/tmp/x.dump");
        assertTrue(cmd.contains("--clean"));
        assertTrue(cmd.contains("--if-exists"));
        assertTrue(cmd.contains("--no-owner"));
        assertEquals("-d", cmd.get(cmd.size() - 3));
        assertEquals("multicore", cmd.get(cmd.size() - 2));
        assertEquals("/tmp/x.dump", cmd.get(cmd.size() - 1));
    }

    // ── BR-08..09: guardas de permissão ─────────────────────────────────────

    @Test
    void br08_backupComoEmployee_lanca() {
        CurrentUserContext.setCurrentUser("caixa", "EMPLOYEE");
        assertThrows(BusinessRuleException.class, () -> service().executePhysicalBackup());
    }

    @Test
    void br09_restoreComoManager_lanca() {
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
        assertThrows(BusinessRuleException.class,
                () -> service().restorePhysicalBackup("/tmp/x.dump", true));
    }

    // ── BR-10..12: guardas do restore (ADMIN) ───────────────────────────────

    @Test
    void br10_restoreSemConfirmacao_lanca() {
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        assertThrows(BusinessRuleException.class,
                () -> service().restorePhysicalBackup("/tmp/x.dump", false));
    }

    @Test
    void br11_restoreFicheiroInexistente_lanca() {
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        assertThrows(BusinessRuleException.class,
                () -> service().restorePhysicalBackup(tempDir.resolve("nada.dump").toString(), true));
    }

    @Test
    void br12_restoreExtensaoErrada_lanca() throws Exception {
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        Path notDump = tempDir.resolve("backup.txt");
        Files.writeString(notDump, "x");
        assertThrows(BusinessRuleException.class,
                () -> service().restorePhysicalBackup(notDump.toString(), true));
    }
}
