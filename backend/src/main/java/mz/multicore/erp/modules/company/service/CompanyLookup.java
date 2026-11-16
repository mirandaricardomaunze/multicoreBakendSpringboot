package mz.multicore.erp.modules.company.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Porta de leitura da empresa para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2).
 * É o único ponto pelo qual um módulo alheio lê {@link Company}; escritas passam por
 * {@link CompanyService#saveCompany(Company)}. A entidade devolvida serve só para relações JPA
 * dentro do backend e nunca sai para um Controller.
 */
@Service
@Transactional(readOnly = true)
public class CompanyLookup {

    static final String NOT_FOUND = "Empresa não encontrada.";
    static final String ACTIVE_NOT_FOUND = "Empresa activa não encontrada.";

    private final CompanyRepository companyRepository;

    public CompanyLookup(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    /** Empresa pelo id; {@code BusinessRuleException("Empresa não encontrada.")} se não existir. */
    public Company require(Long id) {
        return find(id).orElseThrow(() -> new BusinessRuleException(NOT_FOUND));
    }

    /** Como {@link #require(Long)}, para a empresa activa da sessão: "Empresa activa não encontrada.". */
    public Company requireActive(Long id) {
        return find(id).orElseThrow(() -> new BusinessRuleException(ACTIVE_NOT_FOUND));
    }

    public Optional<Company> find(Long id) {
        return id == null ? Optional.empty() : companyRepository.findById(id);
    }

    /** Referência JPA (proxy, sem SELECT) para associar a empresa a outro agregado. */
    public Company reference(Long id) {
        if (id == null) {
            throw new BusinessRuleException(NOT_FOUND);
        }
        return companyRepository.getReferenceById(id);
    }

    public List<Company> findAll() {
        return companyRepository.findAll();
    }

    public long count() {
        return companyRepository.count();
    }

    public Optional<Company> findByTaxId(String taxId) {
        return taxId == null ? Optional.empty() : companyRepository.findByTaxId(taxId);
    }

    /**
     * Mutex de BD (lock pessimista) para invariantes criadas dentro de uma empresa (ex.: abrir caixa).
     * Só faz sentido dentro da transacção de escrita do chamador, por isso exige-a.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Company lockForUpdate(Long id) {
        if (id == null) {
            throw new BusinessRuleException(NOT_FOUND);
        }
        return companyRepository.lockById(id).orElseThrow(() -> new BusinessRuleException(NOT_FOUND));
    }
}
