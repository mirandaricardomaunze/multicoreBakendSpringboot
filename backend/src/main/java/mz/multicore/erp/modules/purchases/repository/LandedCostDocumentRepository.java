package mz.multicore.erp.modules.purchases.repository;

import mz.multicore.erp.modules.purchases.model.LandedCostDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LandedCostDocumentRepository extends JpaRepository<LandedCostDocument, Long> {
}
