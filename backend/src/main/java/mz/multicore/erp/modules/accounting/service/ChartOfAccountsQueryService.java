package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.accounting.dto.AccountDTO;
import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.repository.AccountRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChartOfAccountsQueryService {

    private final AccountRepository accountRepository;

    public ChartOfAccountsQueryService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public List<AccountDTO> listAccounts() {
        return accountRepository.findByCompanyIdOrderByCode(CurrentUserContext.getCurrentCompanyId())
                .stream().map(ChartOfAccountsService::toDTO).toList();
    }

    public Account requirePostableAccount(Long companyId, String code) {
        Account account = accountRepository.findByCompanyIdAndCode(companyId, code)
                .orElseThrow(() -> new BusinessRuleException(
                        "Conta " + code + " não existe no plano desta empresa. "
                                + "Semeie o plano de contas em Contabilidade antes de continuar."));
        if (!account.isPostable() || !account.isActive()) {
            throw new BusinessRuleException("A conta " + code + " não aceita lançamentos.");
        }
        return account;
    }

    public boolean hasChart(Long companyId) {
        return accountRepository.existsByCompanyId(companyId);
    }

    public boolean hasAccounts(Long companyId, String... codes) {
        for (String code : codes) {
            boolean usable = accountRepository.findByCompanyIdAndCode(companyId, code)
                    .filter(Account::isPostable).filter(Account::isActive).isPresent();
            if (!usable) {
                return false;
            }
        }
        return true;
    }
}
