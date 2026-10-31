package mz.multicore.erp.modules.comercial.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebitNoteServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/comercial/service/DebitNoteService.java");
    private static final Path LIFECYCLE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/comercial/service/DebitNoteLifecycleService.java");
    private static final Path QUERY_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/comercial/service/DebitNoteQueryService.java");

    @Test
    void debitNoteServiceDelegatesToLifecycleAndQueryCollaborators() throws Exception {
        String serviceSource = Files.readString(SERVICE_FILE);
        String lifecycleSource = Files.readString(LIFECYCLE_FILE);
        String querySource = Files.readString(QUERY_FILE);

        assertTrue(serviceSource.contains("lifecycleService.create("));
        assertTrue(serviceSource.contains("lifecycleService.approve("));
        assertTrue(serviceSource.contains("queryService.findByCompany("));
        assertFalse(serviceSource.contains("new DebitNote()"));

        assertTrue(lifecycleSource.contains("new DebitNoteApprovedEvent("));
        assertTrue(querySource.contains("new DebitNoteDTO("));

        int serviceLines = Files.readAllLines(SERVICE_FILE).size();
        int lifecycleLines = Files.readAllLines(LIFECYCLE_FILE).size();
        int queryLines = Files.readAllLines(QUERY_FILE).size();

        assertTrue(serviceLines <= 100, "DebitNoteService deve ter <= 100 linhas, mas tem " + serviceLines);
        assertTrue(lifecycleLines <= 150, "DebitNoteLifecycleService deve ter <= 150 linhas, mas tem " + lifecycleLines);
        assertTrue(queryLines <= 85, "DebitNoteQueryService deve ter <= 85 linhas, mas tem " + queryLines);
    }
}
