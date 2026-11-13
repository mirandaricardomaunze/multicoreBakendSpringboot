package mz.multicore.erp.modules.manufacturing.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.manufacturing.model.BillOfMaterials;
import mz.multicore.erp.modules.manufacturing.repository.BillOfMaterialsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Porta de leitura das receitas (listas de materiais) para os outros módulos
 * (docs/MODULE_DECOUPLING_SPEC.md §2).
 */
@Service
@Transactional(readOnly = true)
public class BillOfMaterialsLookup {

    private static final String NOT_FOUND = "Receita não encontrada.";

    private final BillOfMaterialsRepository bomRepository;

    public BillOfMaterialsLookup(BillOfMaterialsRepository bomRepository) {
        this.bomRepository = bomRepository;
    }

    /** Receitas activas da empresa, ordenadas pelo nome. */
    public List<BillOfMaterials> listActiveForCompany(Long companyId) {
        if (companyId == null) {
            return List.of();
        }
        return bomRepository.findByCompanyIdAndActiveTrueOrderByNameAsc(companyId);
    }

    /** Receita da empresa (activa ou não); falha se não existir ou for de outra empresa. */
    public BillOfMaterials requireForCompany(Long bomId, Long companyId) {
        if (bomId == null || companyId == null) {
            throw new BusinessRuleException(NOT_FOUND);
        }
        return bomRepository.findByIdAndCompanyId(bomId, companyId)
                .orElseThrow(() -> new BusinessRuleException(NOT_FOUND));
    }
}
