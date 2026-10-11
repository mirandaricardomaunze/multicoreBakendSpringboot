package mz.multicore.erp.modules.intelligence.repository;

import mz.multicore.erp.modules.intelligence.model.AiManagedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiManagedDocumentRepository extends JpaRepository<AiManagedDocument, Long> {
    Optional<AiManagedDocument> findByIdAndCompanyId(Long id, Long companyId);

    List<AiManagedDocument> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
}
