package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO;
import mz.multicore.erp.modules.documents.dto.DocumentTemplateDTO;
import mz.multicore.erp.modules.documents.model.DocumentType;
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
    private final DocumentConfigService documentConfigService;

    @Autowired
    public InvoicePrintService(InvoiceLookup invoiceLookup, InvoiceA4Renderer a4Renderer,
                               InvoiceThermalRenderer thermalRenderer, DocumentConfigService documentConfigService) {
        this.invoiceLookup = invoiceLookup;
        this.a4Renderer = a4Renderer;
        this.thermalRenderer = thermalRenderer;
        this.documentConfigService = documentConfigService;
    }

    /** Preserva a construção usada pelos testes e integrações anteriores à decomposição. */
    public InvoicePrintService(InvoiceLookup invoiceLookup, LineRowMapper lineRowMapper,
                               DocumentConfigService documentConfigService) {
        this(invoiceLookup, new InvoiceA4Renderer(lineRowMapper), new InvoiceThermalRenderer(), documentConfigService);
    }

    /** A4 com o modelo actual da empresa, também nas 2.ª vias de documentos antigos (SPEC §3.3). */
    @Transactional(readOnly = true)
    public byte[] render(Long invoiceId) {
        Invoice invoice = loadOwned(invoiceId);
        return a4Renderer.render(invoice, INVOICE_COPIES, template(invoice));
    }

    /** Talão térmico: do modelo só usa logótipo, notas e rodapé (SPEC §3.3). */
    @Transactional(readOnly = true)
    public byte[] renderThermal(Long invoiceId) {
        Invoice invoice = loadOwned(invoiceId);
        DocumentColumnsDTO columns = documentConfigService.getColumns(invoice.getCompany().getId(), DocumentType.COMMERCIAL);
        return thermalRenderer.render(invoice, template(invoice), columns == null ? null : columns.footer());
    }

    private DocumentTemplateDTO template(Invoice invoice) {
        return documentConfigService.getTemplate(invoice.getCompany().getId(), DocumentType.COMMERCIAL);
    }

    private Invoice loadOwned(Long invoiceId) {
        Invoice invoice = invoiceLookup.require(invoiceId);
        CurrentUserContext.requireCompany(invoice.getCompany().getId());
        return invoice;
    }
}
