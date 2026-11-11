package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.events.CompanyCreatedEvent;
import mz.multicore.erp.modules.accounting.repository.AccountRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Garante que nenhuma empresa fica sem plano de contas: sem ele, os lançamentos automáticos são
 * ignorados e as vendas ficam fora da contabilidade sem qualquer erro.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ChartOfAccountsBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ChartOfAccountsBootstrap.class);

    private final CompanyLookup companyLookup;
    private final AccountRepository accountRepository;
    private final ChartOfAccountsMutationService mutationService;

    public ChartOfAccountsBootstrap(CompanyLookup companyLookup, AccountRepository accountRepository,
                                    ChartOfAccountsMutationService mutationService) {
        this.companyLookup = companyLookup;
        this.accountRepository = accountRepository;
        this.mutationService = mutationService;
    }

    @EventListener
    @Transactional
    public void onCompanyCreated(CompanyCreatedEvent event) {
        mutationService.seedChartFor(companyLookup.reference(event.companyId()));
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Company company : companyLookup.findAll()) {
            if (accountRepository.existsByCompanyId(company.getId())) continue;
            int created = mutationService.seedChartFor(company);
            log.info("Plano de contas criado na empresa {}: {} contas.", company.getId(), created);
        }
    }
}
