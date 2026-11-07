package mz.multicore.erp.modules.numbering.service;

import mz.multicore.erp.architecture.events.CompanyCreatedEvent;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Cria as séries de numeração do ano corrente <b>e do seguinte</b> para cada empresa, no arranque e
 * quando nasce uma empresa. Assim a primeira rajada de documentos do ano (incluindo a de 1 de
 * Janeiro, com o servidor ligado desde Dezembro) não disputa a criação da mesma série.
 * Ver {@link DocumentNumberService}.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DocumentSequenceBootstrap implements ApplicationRunner {

    private final CompanyRepository companyRepository;
    private final DocumentNumberService documentNumberService;

    public DocumentSequenceBootstrap(CompanyRepository companyRepository, DocumentNumberService documentNumberService) {
        this.companyRepository = companyRepository;
        this.documentNumberService = documentNumberService;
    }

    @EventListener
    public void onCompanyCreated(CompanyCreatedEvent event) {
        ensureFor(event.companyId());
    }

    @Override
    public void run(ApplicationArguments args) {
        companyRepository.findAll().stream().map(Company::getId).forEach(this::ensureFor);
    }

    private void ensureFor(Long companyId) {
        int year = LocalDate.now().getYear();
        for (String series : DocumentSeries.ALL) {
            documentNumberService.ensureSequence(companyId, series, year);
            documentNumberService.ensureSequence(companyId, series, year + 1);
        }
    }
}
