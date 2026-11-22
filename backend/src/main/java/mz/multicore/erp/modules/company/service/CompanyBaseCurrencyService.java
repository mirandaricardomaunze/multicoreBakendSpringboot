package mz.multicore.erp.modules.company.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.model.CurrencyCode;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Base currency of the active company (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.1): MZN by default and
 * frozen once the company has any document or movement, because stock, average cost and the ledger are kept in it.
 */
@Service
public class CompanyBaseCurrencyService {

    private final CompanyRepository companyRepository;
    private final List<CompanyMovementProbe> movementProbes;

    public CompanyBaseCurrencyService(CompanyRepository companyRepository,
                                      List<CompanyMovementProbe> movementProbes) {
        this.companyRepository = companyRepository;
        this.movementProbes = List.copyOf(movementProbes);
    }

    @Transactional(readOnly = true)
    public CurrencyCode baseCurrency(Long companyId) {
        return CurrencyCode.parse(activeCompany(companyId).getBaseCurrency());
    }

    /** True while the base currency can still change: no purchase, invoice or stock movement recorded yet. */
    @Transactional(readOnly = true)
    public boolean isBaseCurrencyEditable(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return !hasMovements(companyId);
    }

    @Transactional
    public CurrencyCode changeBaseCurrency(Long companyId, String currencyCode) {
        CurrencyCode requested = CurrencyCode.parse(currencyCode);
        Company company = activeCompany(companyId);
        if (requested.name().equals(company.getBaseCurrency())) {
            return requested;
        }
        if (hasMovements(companyId)) {
            throw new BusinessRuleException("A moeda base da empresa já não pode ser alterada: "
                    + "a empresa já tem documentos ou movimentos registados em " + company.getBaseCurrency() + ".");
        }
        company.setBaseCurrency(requested.name());
        companyRepository.save(company);
        return requested;
    }

    private boolean hasMovements(Long companyId) {
        return movementProbes.stream().anyMatch(probe -> probe.hasMovements(companyId));
    }

    private Company activeCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));
    }
}
