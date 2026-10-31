package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.accounting.dto.AccountDTO;
import mz.multicore.erp.modules.accounting.dto.SaveAccountRequest;
import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.model.AccountClass;
import mz.multicore.erp.modules.accounting.model.AccountNature;
import mz.multicore.erp.modules.accounting.repository.AccountRepository;
import mz.multicore.erp.modules.company.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChartOfAccountsService {

    private final ChartOfAccountsMutationService mutationService;
    private final ChartOfAccountsQueryService queryService;

    @Autowired
    public ChartOfAccountsService(ChartOfAccountsMutationService mutationService,
                                  ChartOfAccountsQueryService queryService) {
        this.mutationService = mutationService;
        this.queryService = queryService;
    }

    public ChartOfAccountsService(AccountRepository accountRepository, CompanyService companyService) {
        this(new ChartOfAccountsMutationService(accountRepository, companyService),
                new ChartOfAccountsQueryService(accountRepository));
    }

    @Transactional(readOnly = true)
    public List<AccountDTO> listAccounts() {
        return queryService.listAccounts();
    }

    @Transactional
    public int seedDefaultChart() {
        PermissionGuard.requireManagerOrAdmin("semear o plano de contas");
        return mutationService.seedDefaultChart();
    }

    @Transactional
    public AccountDTO createAccount(SaveAccountRequest request) {
        PermissionGuard.requireManagerOrAdmin("criar conta do plano");
        return mutationService.createAccount(request);
    }

    @Transactional(readOnly = true)
    public Account requirePostableAccount(Long companyId, String code) {
        return queryService.requirePostableAccount(companyId, code);
    }

    @Transactional(readOnly = true)
    public boolean hasChart(Long companyId) {
        return queryService.hasChart(companyId);
    }

    @Transactional(readOnly = true)
    public boolean hasAccounts(Long companyId, String... codes) {
        return queryService.hasAccounts(companyId, codes);
    }

    static AccountDTO toDTO(Account account) {
        return new AccountDTO(
                account.getId(), account.getCode(), account.getName(),
                account.getAccountClass(), account.getAccountClass().label(),
                account.getNature(), account.isPostable(), account.isActive(), account.getParentCode());
    }

    public static AccountNature defaultNatureFor(String code) {
        return AccountClass.ofCode(code).defaultNature();
    }
}
