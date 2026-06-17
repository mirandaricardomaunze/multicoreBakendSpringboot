package com.phcpro.modules.comercial.service;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.comercial.model.Client;
import com.phcpro.modules.comercial.repository.ClientRepository;
import com.phcpro.modules.company.repository.CompanyRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Devolve (e cria on-demand) o cliente genérico "Consumidor Final" usado em
 * vendas balcão / encomendas sem cliente identificado. Único por taxId.
 */
@Component
public class WalkInClientProvider {

    public static final String WALK_IN_TAX_ID = "000000000";
    public static final String WALK_IN_NAME = "Consumidor Final";

    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;

    public WalkInClientProvider(ClientRepository clientRepository, CompanyRepository companyRepository) {
        this.clientRepository = clientRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional
    public Client getOrCreate() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return clientRepository.findByTaxIdAndCompaniesId(WALK_IN_TAX_ID, companyId).orElseGet(() -> {
            Client shared = clientRepository.findByTaxId(WALK_IN_TAX_ID).orElse(null);
            if (shared != null) {
                shared.getCompanies().add(companyRepository.getReferenceById(companyId));
                return clientRepository.save(shared);
            }
            Client c = new Client();
            c.setName(WALK_IN_NAME);
            c.setTaxId(WALK_IN_TAX_ID);
            c.setEmail("walkin@local");
            c.setAddress("—");
            c.setCreatedBy("SYSTEM");
            c.getCompanies().add(companyRepository.getReferenceById(companyId));
            return clientRepository.save(c);
        });
    }
}
