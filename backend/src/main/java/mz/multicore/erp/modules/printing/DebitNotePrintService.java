package mz.multicore.erp.modules.printing;

import mz.multicore.erp.modules.comercial.model.DebitNote;
import mz.multicore.erp.modules.comercial.service.DebitNoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Generates the PDF for a debit note (Nota de Débito). */
@Service
public class DebitNotePrintService {

    public static final List<String> DEBIT_NOTE_COPIES = List.of(
            "1.ª VIA — ORIGINAL (Cliente)",
            "2.ª VIA — DUPLICADO (Contabilidade / Fisco)",
            "3.ª VIA — TRIPLICADO (Arquivo / Cobrança)",
            "4.ª VIA — QUADRUPLICADO (Arquivo Geral)"
    );

    private final DebitNoteService service;
    private final DebitNoteDocumentRenderer documentRenderer;

    @Autowired
    public DebitNotePrintService(DebitNoteService service, DebitNoteDocumentRenderer documentRenderer) {
        this.service = service;
        this.documentRenderer = documentRenderer;
    }

    public DebitNotePrintService(DebitNoteService service) {
        this(service, new DebitNoteDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] render(Long noteId) {
        DebitNote note = service.loadForPrint(noteId);
        return documentRenderer.render(note, DEBIT_NOTE_COPIES);
    }
}
