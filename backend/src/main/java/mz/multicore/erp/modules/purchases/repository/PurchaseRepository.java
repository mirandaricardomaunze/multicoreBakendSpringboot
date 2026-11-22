package mz.multicore.erp.modules.purchases.repository;

import mz.multicore.erp.modules.purchases.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    List<Purchase> findByCompanyId(Long companyId);

    /** Chave do Saldo Inicial Fornecedor: fornecedor + número original na empresa (IMPORTACAO_DADOS_SPEC §5.3). */
    boolean existsByCompanyIdAndSupplierIdAndOriginalNumber(Long companyId, Long supplierId, String originalNumber);
}
