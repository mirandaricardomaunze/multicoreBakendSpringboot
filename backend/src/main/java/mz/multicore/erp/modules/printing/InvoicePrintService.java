package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Fachada tenant-scoped para emissão de facturas. */
@Service
public class InvoicePrintService {
    public static final List<String> INVOICE_COPIES = List.of(
            "1.ª VIA — ORIGINAL (Cliente)",
            "2.ª VIA — DUPLICADO (Contabilidade / Fisco)",
            "3.ª VIA — TRIPLICADO (Arquivo / Cobrança)",
            "4.ª VIA — QUADRUPLICADO (Arquivo Geral)"
    );

    private final InvoiceLookup invoiceLookup;
    private final InvoiceA4Renderer a4Renderer;
    private final InvoiceThermalRenderer thermalRenderer;

    @Autowired
    public InvoicePrintService(InvoiceLookup invoiceLookup, InvoiceA4Renderer a4Renderer,
                               InvoiceThermalRenderer thermalRenderer) {
        this.invoiceLookup = invoiceLookup;
        this.a4Renderer = a4Renderer;
        this.thermalRenderer = thermalRenderer;
    }

    /** Preserva a construção usada pelos testes e integrações anteriores à decomposição. */
    public InvoicePrintService(InvoiceLookup invoiceLookup, LineRowMapper lineRowMapper,
                               DocumentConfigService ignoredDocumentConfigService) {
        this(invoiceLookup, new InvoiceA4Renderer(lineRowMapper), new InvoiceThermalRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] render(Long invoiceId) {
        return a4Renderer.render(loadOwned(invoiceId), INVOICE_COPIES);
    }

    @Transactional(readOnly = true)
    public byte[] renderThermal(Long invoiceId) {
        return thermalRenderer.render(loadOwned(invoiceId));
    }

    private Invoice loadOwned(Long invoiceId) {
        Invoice invoice = invoiceLookup.require(invoiceId);
        CurrentUserContext.requireCompany(invoice.getCompany().getId());
        return invoice;
    }
}
