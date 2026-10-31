package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Folha de <b>etiquetas de produto</b> (código de barras + nome + preço) em A4 ou bobina térmica 80mm.
 */
@Service
public class ProductLabelPrintService {

    private static final int MAX_COPIES = 200;

    private final ComercialService comercialService;
    private final CompanyService companyService;
    private final ProductLabelDocumentRenderer documentRenderer;

    @Autowired
    public ProductLabelPrintService(ComercialService comercialService,
                                    CompanyService companyService,
                                    ProductLabelDocumentRenderer documentRenderer) {
        this.comercialService = comercialService;
        this.companyService = companyService;
        this.documentRenderer = documentRenderer;
    }

    public ProductLabelPrintService(ComercialService comercialService, CompanyService companyService) {
        this(comercialService, companyService, new ProductLabelDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, List<Long> productIds, int copies) {
        return render(companyId, productIds, copies, null, null, "A4");
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, List<Long> productIds, int copies, String lotNumber, String expiryDate, String layoutFormat) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada.");
        }
        if (productIds == null || productIds.isEmpty()) {
            throw new BusinessRuleException("Selecione pelo menos um produto.");
        }
        int copiesClamped = Math.max(1, Math.min(copies, MAX_COPIES));
        Set<Long> idSet = new HashSet<>(productIds);
        List<ProductDTO> products = comercialService.getAllProducts().stream()
                .filter(p -> idSet.contains(p.id()))
                .toList();
        if (products.isEmpty()) {
            throw new BusinessRuleException("Nenhum dos produtos indicados foi encontrado.");
        }

        return documentRenderer.renderLabels(company, products, copiesClamped, lotNumber, expiryDate, layoutFormat);
    }
}
