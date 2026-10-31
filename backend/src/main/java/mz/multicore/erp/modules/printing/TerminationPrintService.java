package mz.multicore.erp.modules.printing;

import mz.multicore.erp.modules.hr.model.Termination;
import mz.multicore.erp.modules.hr.service.TerminationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PDF do acerto final e do certificado de trabalho.
 */
@Service
public class TerminationPrintService {

    private final TerminationService terminationService;
    private final TerminationDocumentRenderer documentRenderer;

    @Autowired
    public TerminationPrintService(
            TerminationService terminationService,
            TerminationDocumentRenderer documentRenderer
    ) {
        this.terminationService = terminationService;
        this.documentRenderer = documentRenderer;
    }

    public TerminationPrintService(TerminationService terminationService) {
        this(terminationService, new TerminationDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] renderSettlement(Long terminationId) {
        Termination t = terminationService.loadForPrint(terminationId);
        return documentRenderer.renderSettlement(t);
    }

    @Transactional(readOnly = true)
    public byte[] renderCertificate(Long terminationId) {
        Termination t = terminationService.loadForPrint(terminationId);
        return documentRenderer.renderCertificate(t);
    }
}
