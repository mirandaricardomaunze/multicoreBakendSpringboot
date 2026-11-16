package mz.multicore.erp.modules.fiscal.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.fiscal.model.TaxRate;
import mz.multicore.erp.modules.fiscal.repository.TaxRateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Porta de leitura das taxas de imposto para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2).
 * As gravações alheias passam pelo {@link TaxRateCommandService}. As entidades devolvidas servem só
 * para uso dentro do backend.
 */
@Service
@Transactional(readOnly = true)
public class TaxRateLookup {

    private static final String NOT_FOUND = "Taxa de IVA não encontrada.";

    private final TaxRateRepository taxRateRepository;

    public TaxRateLookup(TaxRateRepository taxRateRepository) {
        this.taxRateRepository = taxRateRepository;
    }

    /** Número total de taxas registadas (todas as empresas). */
    public long countAll() {
        return taxRateRepository.count();
    }

    /** Todas as taxas registadas (todas as empresas). */
    public List<TaxRate> listAll() {
        return taxRateRepository.findAll();
    }

    /** Taxa pelo código (ex.: "IVA16"). */
    public Optional<TaxRate> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return taxRateRepository.findByCode(code);
    }

    /** Taxa associada à empresa; falha se não existir ou não estiver associada. */
    public TaxRate requireForCompany(Long id, Long companyId) {
        if (id == null || companyId == null) {
            throw new BusinessRuleException(NOT_FOUND);
        }
        return taxRateRepository.findByIdAndCompaniesId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException(NOT_FOUND));
    }

    /** Taxas activas da empresa, por tipo e taxa decrescente. */
    public List<TaxRate> listActiveForCompany(Long companyId) {
        if (companyId == null) {
            return List.of();
        }
        return taxRateRepository.findDistinctByCompaniesIdAndActiveTrueOrderByTypeAscRateDesc(companyId);
    }
}
