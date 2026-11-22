package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.purchases.dto.PurchaseDocumentPrintDTO;
import mz.multicore.erp.modules.purchases.service.PurchaseDocumentPrintQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PDFs of the purchase order and of the supplier purchase, always in the active company (SPEC §3.3.1). */
@Service
public class PurchaseDocumentPrintService {

    static final String PURCHASE_ORDER_TITLE = "Encomenda a Fornecedor";
    static final String PURCHASE_TITLE = "Compra a Fornecedor";

    private final PurchaseDocumentPrintQueryService queryService;
    private final CompanyService companyService;
    private final PurchaseDocumentRenderer renderer;

    public PurchaseDocumentPrintService(PurchaseDocumentPrintQueryService queryService, CompanyService companyService,
                                        PurchaseDocumentRenderer renderer) {
        this.queryService = queryService;
        this.companyService = companyService;
        this.renderer = renderer;
    }

    @Transactional(readOnly = true)
    public byte[] renderPurchaseOrder(Long orderId) {
        return render(PURCHASE_ORDER_TITLE, queryService.purchaseOrder(orderId));
    }

    @Transactional(readOnly = true)
    public byte[] renderPurchase(Long purchaseId) {
        return render(PURCHASE_TITLE, queryService.purchase(purchaseId));
    }

    private byte[] render(String title, PurchaseDocumentPrintDTO document) {
        Company company = companyService.getCompanyById(document.companyId());
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada: " + document.companyId());
        }
        return renderer.render(company, title, document);
    }
}
