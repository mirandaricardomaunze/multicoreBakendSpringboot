package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.financeira.model.TreasuryAccount;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Porta de leitura das contas de tesouraria para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2).
 * A abertura de contas passa pelo {@link TreasuryAccountProvisioningService} e os movimentos pelo
 * {@link FinanceService}. As entidades devolvidas servem só para uso dentro do backend.
 */
@Service
@Transactional(readOnly = true)
public class TreasuryAccountLookup {

    private static final String NOT_FOUND = "Conta de tesouraria não encontrada.";

    private final TreasuryAccountRepository accountRepository;

    public TreasuryAccountLookup(TreasuryAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /** Conta da empresa indicada; falha se não existir ou for de outra empresa. */
    public TreasuryAccount requireForCompany(Long id, Long companyId) {
        if (id == null || companyId == null) {
            throw new BusinessRuleException(NOT_FOUND);
        }
        return accountRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException(NOT_FOUND));
    }

    /** Contas de tesouraria da empresa, ordenadas por nome. */
    public List<TreasuryAccount> listForCompany(Long companyId) {
        if (companyId == null) {
            return List.of();
        }
        return accountRepository.findByCompanyIdOrderByName(companyId);
    }
}
