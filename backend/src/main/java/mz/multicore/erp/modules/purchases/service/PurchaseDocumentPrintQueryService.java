package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.model.CurrencyCode;
import mz.multicore.erp.modules.purchases.dto.PurchaseDocumentPrintDTO;
import mz.multicore.erp.modules.purchases.model.CurrencyDocument;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.PurchaseOrder;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.purchases.repository.PurchaseOrderRepository;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Read side of the purchase-order and purchase PDFs (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.3.1): only documents
 * of the active company; another company's document is refused as not found.
 */
@Service
public class PurchaseDocumentPrintQueryService {

    private final PurchaseOrderRepository orderRepository;
    private final PurchaseRepository purchaseRepository;

    public PurchaseDocumentPrintQueryService(PurchaseOrderRepository orderRepository,
                                             PurchaseRepository purchaseRepository) {
        this.orderRepository = orderRepository;
        this.purchaseRepository = purchaseRepository;
    }

    @Transactional(readOnly = true)
    public PurchaseDocumentPrintDTO purchaseOrder(Long id) {
        PurchaseOrder order = orderRepository.findById(id)
                .filter(o -> ofActiveCompany(o.getCompany()))
                .orElseThrow(() -> new BusinessRuleException("Encomenda a fornecedor não encontrada."));
        List<PurchaseDocumentPrintDTO.Line> lines = order.getLines().stream()
                .map(l -> new PurchaseDocumentPrintDTO.Line(l.getProduct().getName(), l.getQuantity(),
                        l.getUnitPrice(), l.getTaxRate(), l.getLineTotal()))
                .toList();
        return snapshot(order, order.getId(), order.getCompany(), order.getOrderNumber(), order.getOrderDate(), order.getSupplier(),
                order.getWarehouse(), order.getTaxAmount(), lines);
    }

    @Transactional(readOnly = true)
    public PurchaseDocumentPrintDTO purchase(Long id) {
        Purchase purchase = purchaseRepository.findById(id)
                .filter(p -> ofActiveCompany(p.getCompany()))
                .orElseThrow(() -> new BusinessRuleException("Compra a fornecedor não encontrada."));
        List<PurchaseDocumentPrintDTO.Line> lines = purchase.getLines().stream()
                .map(l -> new PurchaseDocumentPrintDTO.Line(l.getProduct().getName(), l.getQuantity(),
                        l.getUnitPrice(), l.getTaxRate(), l.getLineTotal()))
                .toList();
        return snapshot(purchase, purchase.getId(), purchase.getCompany(), purchase.getPurchaseNumber(), purchase.getPurchaseDate(),
                purchase.getSupplier(), purchase.getWarehouse(), purchase.getTaxAmount(), lines);
    }

    private static boolean ofActiveCompany(Company company) {
        return company != null && Objects.equals(company.getId(), CurrentUserContext.requireCurrentCompanyId());
    }

    private static PurchaseDocumentPrintDTO snapshot(CurrencyDocument document, Long id, Company company, String number,
                                                     LocalDateTime date, Supplier supplier, Warehouse warehouse,
                                                     BigDecimal taxAmount, List<PurchaseDocumentPrintDTO.Line> lines) {
        BigDecimal total = document.getTotalAmount() == null ? BigDecimal.ZERO : document.getTotalAmount();
        BigDecimal tax = taxAmount == null ? BigDecimal.ZERO : taxAmount;
        String baseCurrency = company.getBaseCurrency() == null ? CurrencyCode.DEFAULT_CODE : company.getBaseCurrency();
        return new PurchaseDocumentPrintDTO(id, company.getId(), number, date,
                supplier != null ? supplier.getName() : null, supplier != null ? supplier.getTaxId() : null,
                warehouse != null ? warehouse.getName() : null, document.getCurrency(), baseCurrency,
                document.getExchangeRate(), total.subtract(tax), tax, total, document.baseTotal(), lines);
    }
}
