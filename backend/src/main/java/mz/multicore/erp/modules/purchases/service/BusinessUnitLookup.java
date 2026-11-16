package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.modules.purchases.model.BusinessUnit;
import mz.multicore.erp.modules.purchases.repository.BusinessUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Porta de leitura das linhas de negócio para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2).
 * Devolve {@link Optional}: cada consumidor mantém a sua mensagem de "não encontrada", já coberta por testes.
 * A entidade serve só para relações JPA dentro do backend e nunca sai para um Controller.
 */
@Service
@Transactional(readOnly = true)
public class BusinessUnitLookup {

    private final BusinessUnitRepository businessUnitRepository;

    public BusinessUnitLookup(BusinessUnitRepository businessUnitRepository) {
        this.businessUnitRepository = businessUnitRepository;
    }

    /** Linha de negócio da empresa, se existir. */
    public Optional<BusinessUnit> findForCompany(Long id, Long companyId) {
        return id == null ? Optional.empty() : businessUnitRepository.findByIdAndCompanyId(id, companyId);
    }
}
