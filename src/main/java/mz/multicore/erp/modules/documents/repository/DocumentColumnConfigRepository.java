package mz.multicore.erp.modules.documents.repository;

import mz.multicore.erp.modules.documents.model.DocumentColumnConfig;
import mz.multicore.erp.modules.documents.model.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentColumnConfigRepository extends JpaRepository<DocumentColumnConfig, Long> {

    Optional<DocumentColumnConfig> findByCompanyIdAndDocumentType(Long companyId, DocumentType documentType);
}
