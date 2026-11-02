package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.accounting.dto.AccountDTO;
import mz.multicore.erp.modules.accounting.dto.SaveAccountRequest;
import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.model.AccountClass;
import mz.multicore.erp.modules.accounting.repository.AccountRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ChartOfAccountsMutationService {

    private final AccountRepository accountRepository;
    private final CompanyService companyService;

    public ChartOfAccountsMutationService(AccountRepository accountRepository, CompanyService companyService) {
        this.accountRepository = accountRepository;
        this.companyService = companyService;
    }

    public int seedDefaultChart() {
        PermissionGuard.requireManagerOrAdmin("semear o plano de contas");
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return seed(companyId, companyService.getCurrentCompanyReference(companyId));
    }

    public int seedChartFor(Company company) {
        return seed(company.getId(), company);
    }

    private int seed(Long companyId, Company company) {
        var existingCodes = accountRepository.findByCompanyIdOrderByCode(companyId).stream()
                .map(Account::getCode).collect(Collectors.toSet());
        List<Account> accounts = PgcNirfChart.accounts().stream()
                .filter(seed -> !existingCodes.contains(seed.code()))
                .map(seed -> {
                    Account account = new Account();
                    account.setCode(seed.code());
                    account.setName(seed.name());
                    account.setCompany(company);
                    account.setAccountClass(AccountClass.ofCode(seed.code()));
                    account.setNature(seed.nature());
                    account.setPostable(seed.postable());
                    account.setActive(true);
                    account.setParentCode(Account.parentCodeOf(seed.code()));
                    account.setCreatedBy(CurrentUserContext.getUsername());
                    return account;
                }).toList();
        accountRepository.saveAll(accounts);
        return accounts.size();
    }

    public AccountDTO createAccount(SaveAccountRequest request) {
        PermissionGuard.requireManagerOrAdmin("criar conta do plano");
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        String code = request.code() == null ? "" : request.code().trim();
        if (code.isEmpty()) {
            throw new BusinessRuleException("O código da conta é obrigatório.");
        }
        accountRepository.findByCompanyIdAndCode(companyId, code).ifPresent(existing -> {
            throw new BusinessRuleException("Já existe a conta " + code + " nesta empresa.");
        });

        Account account = new Account();
        account.setCode(code);
        account.setName(request.name());
        account.setCompany(companyService.getCurrentCompanyReference(companyId));
        account.setAccountClass(AccountClass.ofCode(code));
        account.setNature(request.nature() == null
                ? AccountClass.ofCode(code).defaultNature() : request.nature());
        account.setPostable(request.postable());
        account.setActive(true);
        account.setParentCode(Account.parentCodeOf(code));
        account.setCreatedBy(CurrentUserContext.getUsername());
        return ChartOfAccountsService.toDTO(accountRepository.save(account));
    }
}
