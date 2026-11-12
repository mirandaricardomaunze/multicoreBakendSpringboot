package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Fachada tenant-scoped para emissão da Guia de Remessa derivada de uma factura.
 */
@Service
public class GuideRemittancePrintService {

    public static final List<String> GUIDE_REMITTANCE_COPIES = List.of(
            "1.ª VIA — ORIGINAL (Destinatário / Cliente)",
            "2.ª VIA — DUPLICADO (Trânsito e Fiscalização / Viatura)",
            "3.ª VIA — TRIPLICADO (Expedição / Arquivo de Origem)",
            "4.ª VIA — QUADRUPLICADO (Comprovativo de Recepção e Devolução)"
    );

    private final InvoiceLookup invoiceLookup;
    private final GuideRemittanceDocumentRenderer documentRenderer;

    @Autowired
    public GuideRemittancePrintService(InvoiceLookup invoiceLookup,
                                       GuideRemittanceDocumentRenderer documentRenderer) {
        this.invoiceLookup = invoiceLookup;
        this.documentRenderer = documentRenderer;
    }

    public GuideRemittancePrintService(InvoiceLookup invoiceLookup, LineRowMapper lineRowMapper,
                                       DocumentConfigService documentConfigService) {
        this(invoiceLookup, new GuideRemittanceDocumentRenderer(lineRowMapper, documentConfigService, null));
    }

    public GuideRemittancePrintService(InvoiceLookup invoiceLookup, LineRowMapper lineRowMapper,
                                       DocumentConfigService documentConfigService,
                                       OrderRepository orderRepository) {
        this(invoiceLookup, new GuideRemittanceDocumentRenderer(lineRowMapper, documentConfigService, orderRepository));
    }

    @Transactional(readOnly = true)
    public byte[] render(Long invoiceId) {
        Invoice invoice = invoiceLookup.require(invoiceId);
        CurrentUserContext.requireCompany(invoice.getCompany().getId());
        return documentRenderer.render(invoice);
    }
}
