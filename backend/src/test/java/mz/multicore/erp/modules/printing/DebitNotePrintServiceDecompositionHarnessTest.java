package mz.multicore.erp.modules.printing;

import mz.multicore.erp.modules.comercial.model.DebitNote;
import mz.multicore.erp.modules.comercial.service.DebitNoteService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DebitNotePrintServiceDecompositionHarnessTest {

    @Test
    void debitNotePrintServiceDelegatesToRendererAndRespectsLineLimits() throws Exception {
        DebitNoteService noteService = mock(DebitNoteService.class);
        DebitNoteDocumentRenderer renderer = mock(DebitNoteDocumentRenderer.class);

        DebitNotePrintService service = new DebitNotePrintService(noteService, renderer);

        DebitNote note = new DebitNote();
        byte[] expected = new byte[]{7, 8, 9};

        when(noteService.loadForPrint(12L)).thenReturn(note);
        when(renderer.render(note, DebitNotePrintService.DEBIT_NOTE_COPIES)).thenReturn(expected);

        assertSame(expected, service.render(12L));
        verify(renderer).render(note, DebitNotePrintService.DEBIT_NOTE_COPIES);

        Path base = findModuleRoot().resolve("src/main/java/mz/multicore/erp/modules/printing");
        int facadeLines = Files.readAllLines(base.resolve("DebitNotePrintService.java")).size();
        int rendererLines = Files.readAllLines(base.resolve("DebitNoteDocumentRenderer.java")).size();

        assertTrue(facadeLines <= 55, "DebitNotePrintService deve ter <= 55 linhas, mas tem " + facadeLines);
        assertTrue(rendererLines <= 155, "DebitNoteDocumentRenderer deve ter <= 155 linhas, mas tem " + rendererLines);
    }

    private Path findModuleRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (cwd.endsWith("backend")) {
            return cwd;
        }
        return cwd.resolve("backend");
    }
}
