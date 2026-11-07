package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Devolve (e cria on-demand) o cliente genérico "Consumidor Final" usado em
 * vendas balcão / encomendas sem cliente identificado. Único por taxId.
 *
 * <p>Normalmente já existe: {@link WalkInClientBootstrap} cria-o no arranque e quando nasce uma
 * empresa. Se ainda assim faltar, é criado numa transacção à parte. A carga de 2026-11-07 mostrou
 * porquê: numa base nova, cinco postos a vender ao mesmo tempo tentaram criá-lo os cinco, a chave
 * única recusou quatro, e quatro vendas falharam. Agora quem perde a corrida lê o que o outro gravou.
 */
@Component
public class WalkInClientProvider {

    public static final String WALK_IN_TAX_ID = "000000000";
    public static final String WALK_IN_NAME = "Consumidor Final";

    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;
    private final TransactionTemplate ownTransaction;

    public WalkInClientProvider(ClientRepository clientRepository, CompanyRepository companyRepository,
                                PlatformTransactionManager transactionManager) {
        this.clientRepository = clientRepository;
        this.companyRepository = companyRepository;
        this.ownTransaction = new TransactionTemplate(transactionManager);
        this.ownTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public Client getOrCreate() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return clientRepository.findByTaxIdAndCompaniesId(WALK_IN_TAX_ID, companyId)
                .orElseGet(() -> {
                    ensureFor(companyId);
                    return clientRepository.findByTaxIdAndCompaniesId(WALK_IN_TAX_ID, companyId)
                            .orElseThrow(() -> new IllegalStateException("O cliente Consumidor Final não ficou disponível."));
                });
    }

    /**
     * Garante o "Consumidor Final" ligado à empresa, numa transacção própria. Uma recusa da chave
     * única quer dizer que outro posto o criou no mesmo instante, e isso é sucesso, não erro: a
     * venda que pediu o cliente continua intacta porque a falha ficou na transacção à parte.
     */
    public void ensureFor(Long companyId) {
        try {
            ownTransaction.executeWithoutResult(status -> linkOrCreate(companyId));
        } catch (DataIntegrityViolationException createdByAnotherTerminal) {
            // Outro posto ganhou a corrida; o registo dele serve.
        }
    }

    private void linkOrCreate(Long companyId) {
        if (clientRepository.findByTaxIdAndCompaniesId(WALK_IN_TAX_ID, companyId).isPresent()) return;
        Client client = clientRepository.findByTaxId(WALK_IN_TAX_ID).orElseGet(() -> {
            Client c = new Client();
            c.setName(WALK_IN_NAME);
            c.setTaxId(WALK_IN_TAX_ID);
            c.setEmail("walkin@local");
            c.setAddress("—");
            c.setCreatedBy("SYSTEM");
            return c;
        });
        client.getCompanies().add(companyRepository.getReferenceById(companyId));
        clientRepository.saveAndFlush(client);
    }
}
