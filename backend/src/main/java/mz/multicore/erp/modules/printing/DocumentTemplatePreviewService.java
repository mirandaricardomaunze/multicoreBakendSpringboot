package mz.multicore.erp.modules.printing;

import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.documents.dto.DocumentTemplateDTO;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pré-visualização do modelo de documento (MODELOS_DOCUMENTO_SPEC §3.4, DT-06): uma factura de exemplo da
 * empresa, montada em memória e nunca gravada, impressa pelo mesmo renderer A4 das facturas reais com o
 * modelo pedido. Validação e permissão (só ADMIN) vêm do {@link DocumentConfigService}; nada é gravado nem
 * auditado.
 */
@Service
public class DocumentTemplatePreviewService {

    static final String SAMPLE_COPY = "EXEMPLO — PRÉ-VISUALIZAÇÃO (sem valor fiscal)";
    static final String SAMPLE_NUMBER = "FT EXEMPLO/0001";
    static final String SAMPLE_EXEMPTION = "Artigo 9.º do CIVA";

    private final DocumentConfigService documentConfigService;
    private final CompanyLookup companyLookup;
    private final InvoiceA4Renderer a4Renderer;

    public DocumentTemplatePreviewService(DocumentConfigService documentConfigService, CompanyLookup companyLookup,
                                          InvoiceA4Renderer a4Renderer) {
        this.documentConfigService = documentConfigService;
        this.companyLookup = companyLookup;
        this.a4Renderer = a4Renderer;
    }

    @Transactional(readOnly = true)
    public byte[] preview(Long companyId, DocumentTemplateDTO candidate) {
        DocumentTemplateDTO template = documentConfigService.validateTemplate(companyId, candidate);
        return a4Renderer.render(sampleInvoice(companyId), List.of(SAMPLE_COPY), template);
    }

    /** Consumidor final; 1000 MT a 16% (IVA 160) e 500 MT isentos com motivo legal: total 1660 MT. */
    private Invoice sampleInvoice(Long companyId) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(SAMPLE_NUMBER);
        invoice.setCompany(companyLookup.require(companyId));
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setTotalBeforeTax(new BigDecimal("1500.00"));
        invoice.setTaxAmount(new BigDecimal("160.00"));
        invoice.setTotalAmount(new BigDecimal("1660.00"));
        List<InvoiceLine> lines = new ArrayList<>();
        lines.add(sampleLine(invoice, "Artigo de exemplo A", "0.16", "10", "100.00", "1000.00", null));
        lines.add(sampleLine(invoice, "Artigo de exemplo B", "0", "5", "100.00", "500.00", SAMPLE_EXEMPTION));
        invoice.setLines(lines);
        return invoice;
    }

    private static InvoiceLine sampleLine(Invoice invoice, String name, String rate, String quantity, String price,
                                          String total, String exemption) {
        Product product = new Product();
        product.setName(name);
        InvoiceLine line = new InvoiceLine();
        line.setInvoice(invoice);
        line.setProduct(product);
        line.setQuantity(new BigDecimal(quantity));
        line.setUnitPrice(new BigDecimal(price));
        line.setTaxRate(new BigDecimal(rate));
        line.setDiscountPercentage(BigDecimal.ZERO);
        line.setLineTotal(new BigDecimal(total));
        line.setExemptionReason(exemption);
        return line;
    }
}
