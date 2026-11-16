package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Porta de leitura de clientes para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2).
 * É o único ponto pelo qual um módulo alheio lê {@link Client}; escritas passam por
 * {@link ClientCommandService}. As entidades devolvidas servem só para relações JPA dentro do backend
 * e nunca saem para um Controller.
 */
@Service
@Transactional(readOnly = true)
public class ClientLookup {

    static final String NOT_FOUND = "Cliente não encontrado.";

    private final ClientRepository clientRepository;

    public ClientLookup(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    /** Cliente da empresa; {@code BusinessRuleException("Cliente não encontrado.")} se não existir nela. */
    public Client requireForCompany(Long id, Long companyId) {
        return findForCompany(id, companyId).orElseThrow(() -> new BusinessRuleException(NOT_FOUND));
    }

    public Optional<Client> findForCompany(Long id, Long companyId) {
        return id == null ? Optional.empty() : clientRepository.findByIdAndCompaniesId(id, companyId);
    }

    /** Cliente pelo id, sem filtro de empresa (contratos recorrentes herdados). */
    public Client require(Long id) {
        return find(id).orElseThrow(() -> new BusinessRuleException(NOT_FOUND));
    }

    public Optional<Client> find(Long id) {
        return id == null ? Optional.empty() : clientRepository.findById(id);
    }

    /** Clientes da empresa ordenados por nome. */
    public List<Client> listForCompany(Long companyId) {
        return clientRepository.findDistinctByCompaniesIdOrderByName(companyId);
    }
}
