package mz.multicore.erp.modules.intelligence.repository;

import mz.multicore.erp.modules.intelligence.model.AiDocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiDocumentVersionRepository extends JpaRepository<AiDocumentVersion, Long> {
    List<AiDocumentVersion> findByDocumentIdOrderByVersionNumberDesc(Long documentId);
    Optional<AiDocumentVersion> findByDocumentIdAndVersionNumber(Long documentId, int versionNumber);
}
