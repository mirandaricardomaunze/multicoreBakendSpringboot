package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.crm.model.WorkSheet;
import mz.multicore.erp.modules.crm.service.WorkSheetLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PDF da Folha de Obra de assistência técnica — o papel que o técnico deixa assinado no cliente.
 *
 * <p><b>Não é documento fiscal.</b> Os valores saem sem IVA e o rodapé di-lo — o que o cliente
 * paga sai na factura emitida a partir desta folha ("Faturar Folha de Obra").
 */
@Service
public class WorkSheetPrintService {

    private final WorkSheetLookup workSheetLookup;
    private final WorkSheetDocumentRenderer documentRenderer;

    @Autowired
    public WorkSheetPrintService(WorkSheetLookup workSheetLookup,
                                 WorkSheetDocumentRenderer documentRenderer) {
        this.workSheetLookup = workSheetLookup;
        this.documentRenderer = documentRenderer;
    }

    public WorkSheetPrintService(WorkSheetLookup workSheetLookup) {
        this(workSheetLookup, new WorkSheetDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] render(Long workSheetId) {
        WorkSheet ws = workSheetLookup.requireForCompany(workSheetId, CurrentUserContext.getCurrentCompanyId());
        return documentRenderer.renderA4(ws);
    }
}
