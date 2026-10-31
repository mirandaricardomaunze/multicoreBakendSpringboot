package mz.multicore.erp.modules.backup.service;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseBackupServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/backup/service");

    @Test
    void commandBuildingAndProcessExecutionStayExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("DatabaseBackupService.java"));
        String cmdBuilder = Files.readString(DIR.resolve("PostgresBackupCommandBuilder.java"));
        String runner = Files.readString(DIR.resolve("PostgresBackupProcessRunner.java"));

        assertTrue(facade.contains("processRunner.runProcess("));
        assertTrue(facade.contains("commandBuilder.parsePgConnection("));
        assertFalse(facade.contains("new ProcessBuilder("));
        assertTrue(cmdBuilder.contains("pg_dump"));
        assertTrue(runner.contains("PGPASSWORD"));

        DatabaseBackupService svc = new DatabaseBackupService("jdbc:postgresql://localhost:5432/multicore", "postgres", "secret", "", "backups");
        DatabaseBackupService.PgConnection conn = svc.parsePgConnection("jdbc:postgresql://localhost:5432/multicore");
        assertEquals("localhost", conn.host());
        assertEquals(5432, conn.port());
        assertEquals("multicore", conn.database());

        assertTrue(Files.readAllLines(DIR.resolve("DatabaseBackupService.java")).size() <= 115);
        assertTrue(Files.readAllLines(DIR.resolve("PostgresBackupCommandBuilder.java")).size() <= 90);
        assertTrue(Files.readAllLines(DIR.resolve("PostgresBackupProcessRunner.java")).size() <= 55);
    }
}
