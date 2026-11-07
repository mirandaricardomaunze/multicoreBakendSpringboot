package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.events.CompanyCreatedEvent;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.event.EventListener;
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

    private final CompanyRepository companyRepository;
    private final WalkInClientProvider walkInClientProvider;

    public WalkInClientBootstrap(CompanyRepository companyRepository, WalkInClientProvider walkInClientProvider) {
        this.companyRepository = companyRepository;
        this.walkInClientProvider = walkInClientProvider;
    }

    @EventListener
    public void onCompanyCreated(CompanyCreatedEvent event) {
        walkInClientProvider.ensureFor(event.companyId());
    }

    @Override
    public void run(ApplicationArguments args) {
        for (Company company : companyRepository.findAll()) {
            walkInClientProvider.ensureFor(company.getId());
        }
    }
}
