package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.purchases.dto.CreateOpeningPayableRequest;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.PurchaseDocumentKind;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cria o documento "Saldo Inicial Fornecedor" (série SIF) de uma factura por pagar trazida de outro ERP
 * (IMPORTACAO_DADOS_SPEC §5.3). Fica em {@code purchases} como {@link PurchaseDocumentKind#OPENING_BALANCE}, para
 * aparecer nas contas a pagar e se pagar como uma compra; não tem linhas, armazém nem IVA, não movimenta stock e não
 * publica evento de compra, por isso não gera lançamento contabilístico. Numeração própria por empresa
 * ({@code SIF-aaaa/n}); o número original fica guardado e, com o fornecedor, é a chave: a mesma dívida nunca entra
 * duas vezes.
 */
@Service
public class OpeningPayableCommandService {

    private final PurchaseRepository purchaseRepository;
    private final SupplierLookup supplierLookup;
    private final CompanyLookup companyLookup;
    private final DocumentNumberService documentNumberService;

    public OpeningPayableCommandService(PurchaseRepository purchaseRepository, SupplierLookup supplierLookup,
                                        CompanyLookup companyLookup, DocumentNumberService documentNumberService) {
        this.purchaseRepository = purchaseRepository;
        this.supplierLookup = supplierLookup;
        this.companyLookup = companyLookup;
        this.documentNumberService = documentNumberService;
    }

    /** Grava o Saldo Inicial Fornecedor na empresa activa e devolve o número atribuído ({@code SIF-aaaa/n}). */
    @Transactional
    public String create(CreateOpeningPayableRequest request) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        requireValidValues(request);
        Supplier supplier = supplierLookup.requireForCompany(request.supplierId(), companyId);
        if (purchaseRepository.existsByCompanyIdAndSupplierIdAndOriginalNumber(
                companyId, supplier.getId(), request.originalNumber())) {
            throw new BusinessRuleException("A dívida «" + request.originalNumber() + "» ao fornecedor "
                    + supplier.getName() + " já foi importada.");
        }
        Purchase document = new Purchase();
        document.setDocumentKind(PurchaseDocumentKind.OPENING_BALANCE);
        document.setCompany(companyLookup.require(companyId));
        document.setSupplier(supplier);
        document.setOriginalNumber(request.originalNumber());
        document.setPurchaseDate(request.documentDate().atStartOfDay());
        document.setDueDate(request.dueDate());
        document.setDescription(request.description());
        document.setTotalAmount(request.amount());
        document.setTaxAmount(BigDecimal.ZERO);
        document.setCreatedBy(CurrentUserContext.getUsername());
        document.setPurchaseNumber(documentNumberService.next(DocumentSeries.OPENING_PAYABLE));
        return purchaseRepository.save(document).getPurchaseNumber();
    }

    /** Valor &gt; 0, data presente e não futura, vencimento presente e ≥ data (§5.3 → §5.2). */
    private static void requireValidValues(CreateOpeningPayableRequest request) {
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new BusinessRuleException("O valor em dívida tem de ser maior que zero.");
        }
        if (request.documentDate() == null || request.dueDate() == null) {
            throw new BusinessRuleException("A data e o vencimento da factura são obrigatórios.");
        }
        if (request.documentDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("A data da factura não pode ser futura.");
        }
        if (request.dueDate().isBefore(request.documentDate())) {
            throw new BusinessRuleException("O vencimento não pode ser anterior à data da factura.");
        }
    }
}
