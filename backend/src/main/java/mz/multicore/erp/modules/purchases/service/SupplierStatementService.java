package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.purchases.dto.SupplierStatementDTO;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Serviço de geração de extrato de conta corrente de fornecedores (contas a pagar).
 */
@Service
public class SupplierStatementService {

    private final SupplierRepository supplierRepository;
    private final PurchaseRepository purchaseRepository;
    private final SupplierStatementMovementBuilder movementBuilder;

    @Autowired
    public SupplierStatementService(
            SupplierRepository supplierRepository,
            PurchaseRepository purchaseRepository,
            SupplierStatementMovementBuilder movementBuilder
    ) {
        this.supplierRepository = supplierRepository;
        this.purchaseRepository = purchaseRepository;
        this.movementBuilder = movementBuilder;
    }

    public SupplierStatementService(
            SupplierRepository supplierRepository,
            PurchaseRepository purchaseRepository
    ) {
        this(supplierRepository, purchaseRepository, new SupplierStatementMovementBuilder());
    }

    @Transactional(readOnly = true)
    public SupplierStatementDTO getStatement(Long supplierId, LocalDate startDate, LocalDate endDate) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (supplierId == null) {
            throw new BusinessRuleException("Identificador de fornecedor é obrigatório.");
        }

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new BusinessRuleException("Fornecedor não encontrado: " + supplierId));

        LocalDate from = startDate != null ? startDate : LocalDate.now().withDayOfYear(1);
        LocalDate to = endDate != null ? endDate : LocalDate.now();

        return movementBuilder.buildStatement(supplier, from, to, purchaseRepository.findByCompanyId(companyId));
    }
}
