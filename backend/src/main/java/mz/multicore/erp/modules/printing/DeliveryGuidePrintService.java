package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.DeliveryGuide;
import mz.multicore.erp.modules.comercial.service.OrderLookup;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gera o PDF da Guia de Remessa ao cliente a partir do documento persistido {@link DeliveryGuide}.
 */
@Service
public class DeliveryGuidePrintService {

    public static final List<String> DELIVERY_GUIDE_COPIES = List.of(
            "1.ª VIA — ORIGINAL (Destinatário / Cliente)",
            "2.ª VIA — DUPLICADO (Trânsito e Fiscalização / Viatura)",
            "3.ª VIA — TRIPLICADO (Expedição / Arquivo de Origem)",
            "4.ª VIA — QUADRUPLICADO (Comprovativo de Recepção e Devolução)"
    );

    private final OrderLookup orderLookup;
    private final DeliveryGuideDocumentRenderer documentRenderer;

    @Autowired
    public DeliveryGuidePrintService(
            OrderLookup orderLookup,
            DeliveryGuideDocumentRenderer documentRenderer
    ) {
        this.orderLookup = orderLookup;
        this.documentRenderer = documentRenderer;
    }

    public DeliveryGuidePrintService(
            OrderLookup orderLookup,
            LineRowMapper lineRowMapper,
            DocumentConfigService documentConfigService
    ) {
        this(orderLookup, new DeliveryGuideDocumentRenderer(lineRowMapper, documentConfigService));
    }

    @Transactional(readOnly = true)
    public byte[] render(Long guideId) {
        DeliveryGuide guide = orderLookup
                .findDeliveryGuideWithLines(guideId, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Guia de remessa não encontrada."));
        CurrentUserContext.requireCompany(guide.getCompany().getId());
        return documentRenderer.renderDocument(guide, DELIVERY_GUIDE_COPIES);
    }
}
