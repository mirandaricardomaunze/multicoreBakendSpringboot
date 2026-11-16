package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.events.CompanyCreatedEvent;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Cria o "Consumidor Final" de cada empresa antes da primeira venda, no arranque e quando nasce uma
 * empresa. Assim a primeira rajada de vendas ao balcão não disputa a criação do mesmo cliente.
 * Ver {@link WalkInClientProvider}.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class WalkInClientBootstrap implements ApplicationRunner {

    private final CompanyLookup companyLookup;
    private final WalkInClientProvider walkInClientProvider;

    public WalkInClientBootstrap(CompanyLookup companyLookup, WalkInClientProvider walkInClientProvider) {
        this.companyLookup = companyLookup;
        this.walkInClientProvider = walkInClientProvider;
    }

    /**
     * Runs after the company's transaction commits. {@link WalkInClientProvider#ensureFor} uses a
     * transaction of its own, which cannot see a company that is not committed yet: running it
     * inside the creating transaction made every company registration fail. Without a surrounding
     * transaction (seeders) it runs immediately.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCompanyCreated(CompanyCreatedEvent event) {
        walkInClientProvider.ensureFor(event.companyId());
    }

    @Override
    public void run(ApplicationArguments args) {
        for (Company company : companyLookup.findAll()) {
            walkInClientProvider.ensureFor(company.getId());
        }
    }
}
