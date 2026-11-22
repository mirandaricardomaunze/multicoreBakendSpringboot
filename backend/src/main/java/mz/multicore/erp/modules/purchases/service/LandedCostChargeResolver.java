package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.fiscal.service.TaxRateLookup;
import mz.multicore.erp.modules.purchases.dto.LandedCostChargeRequest;
import mz.multicore.erp.modules.purchases.model.DocumentCurrency;
import mz.multicore.erp.modules.purchases.model.LandedCostCharge;
import mz.multicore.erp.modules.purchases.model.LandedCostDocumentCharge;
import mz.multicore.erp.modules.purchases.model.LandedCostType;
import mz.multicore.erp.modules.purchases.model.Supplier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Turns the cost lines typed on a "Custos de Importação" document into validated lines
 * (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.1): supplier of the company, currency and exchange rate by the §3
 * rules, value &gt; 0 and the VAT rate of the company, as a percentage. Nothing is saved here.
 */
@Component
public class LandedCostChargeResolver {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final SupplierLookup supplierLookup;
    private final PurchaseCurrencyResolver currencyResolver;
    private final TaxRateLookup taxRateLookup;

    public LandedCostChargeResolver(SupplierLookup supplierLookup, PurchaseCurrencyResolver currencyResolver,
                                    TaxRateLookup taxRateLookup) {
        this.supplierLookup = supplierLookup;
        this.currencyResolver = currencyResolver;
        this.taxRateLookup = taxRateLookup;
    }

    public List<LandedCostDocumentCharge> resolve(Company company, LocalDate documentDate,
                                                  List<LandedCostChargeRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessRuleException("Indique pelo menos uma linha de custo (frete, direitos, despachante…).");
        }
        return requests.stream().map(request -> resolve(company, documentDate, request)).toList();
    }

    private LandedCostDocumentCharge resolve(Company company, LocalDate documentDate, LandedCostChargeRequest request) {
        if (request == null) throw new BusinessRuleException("Linha de custo de importação vazia.");
        DocumentCurrency currency = currencyResolver.resolve(company, request.currency(), documentDate,
                request.exchangeRate());
        LandedCostCharge charge = new LandedCostCharge(request.type(), currency, request.amount(),
                vatRatePercent(request, company.getId()));
        if (request.supplierId() == null) throw new BusinessRuleException("Indique o fornecedor da linha de custo.");
        Supplier supplier = supplierLookup.requireForCompany(request.supplierId(), company.getId());
        return LandedCostDocumentCharge.of(charge, supplier);
    }

    /** Taxa de IVA da empresa em percentagem; o IVA aduaneiro é o próprio imposto e não leva taxa (§4.2). */
    private BigDecimal vatRatePercent(LandedCostChargeRequest request, Long companyId) {
        if (request.taxRateId() == null) return null;
        if (request.type() == LandedCostType.IVA_ADUANEIRO) {
            throw new BusinessRuleException("O IVA aduaneiro é o próprio imposto: não leva taxa de IVA.");
        }
        return taxRateLookup.requireForCompany(request.taxRateId(), companyId).getRate().multiply(HUNDRED);
    }
}
