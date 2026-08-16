package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.accounting.dto.AccountDTO;
import mz.multicore.erp.modules.accounting.dto.SaveAccountRequest;
import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.model.AccountClass;
import mz.multicore.erp.modules.accounting.model.AccountNature;
import mz.multicore.erp.modules.accounting.repository.AccountRepository;
import mz.multicore.erp.modules.company.service.CompanyService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Plano de contas da empresa activa.
 *
 * <p>Responsabilidade única: a <b>estrutura</b> das contas. Quem lança é o {@code JournalService};
 * quem soma é o {@code AccountingReportService}.
 */
@Service
public class ChartOfAccountsService {

    private final AccountRepository accountRepository;
    private final CompanyService companyService;

    public ChartOfAccountsService(AccountRepository accountRepository, CompanyService companyService) {
        this.accountRepository = accountRepository;
        this.companyService = companyService;
    }

    @Transactional(readOnly = true)
    public List<AccountDTO> listAccounts() {
        return accountRepository.findByCompanyIdOrderByCode(CurrentUserContext.getCurrentCompanyId())
                .stream().map(ChartOfAccountsService::toDTO).toList();
    }

    /**
     * Semeia o PGC-NIRF na empresa activa. <b>Idempotente</b>: se já houver plano, não faz nada
     * — semear por cima criaria contas duplicadas ou reporia contas que o contabilista desactivou.
     *
     * @return número de contas criadas (zero quando já existia plano)
     */
    @Transactional
    public int seedDefaultChart() {
        PermissionGuard.requireManagerOrAdmin("semear o plano de contas");
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (accountRepository.existsByCompanyId(companyId)) {
            return 0;
        }
        var company = companyService.getCurrentCompanyReference(CurrentUserContext.getCurrentCompanyId());
        List<Account> accounts = PgcNirfChart.accounts().stream().map(seed -> {
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

    /** Cria uma conta nova no plano da empresa activa. */
    @Transactional
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
        account.setCompany(companyService.getCurrentCompanyReference(CurrentUserContext.getCurrentCompanyId()));
        account.setAccountClass(AccountClass.ofCode(code));   // valida a classe pelo 1.º dígito
        account.setNature(request.nature() == null
                ? AccountClass.ofCode(code).defaultNature() : request.nature());
        account.setPostable(request.postable());
        account.setActive(true);
        account.setParentCode(Account.parentCodeOf(code));
        account.setCreatedBy(CurrentUserContext.getUsername());
        return toDTO(accountRepository.save(account));
    }

    /**
     * Conta movimentável pelo código, para os lançamentos automáticos. Lança com uma mensagem
     * que diz o que fazer — sem plano semeado, o utilizador tem de o semear primeiro.
     */
    @Transactional(readOnly = true)
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

    /** Existe plano de contas nesta empresa. */
    @Transactional(readOnly = true)
    public boolean hasChart(Long companyId) {
        return accountRepository.existsByCompanyId(companyId);
    }

    static AccountDTO toDTO(Account account) {
        return new AccountDTO(
                account.getId(),
                account.getCode(),
                account.getName(),
                account.getAccountClass(),
                account.getAccountClass().label(),
                account.getNature(),
                account.isPostable(),
                account.isActive(),
                account.getParentCode());
    }

    /** Natureza por omissão de um código, para a UI sugerir sem repetir a regra. */
    public static AccountNature defaultNatureFor(String code) {
        return AccountClass.ofCode(code).defaultNature();
    }
}
