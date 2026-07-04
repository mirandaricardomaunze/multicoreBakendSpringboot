package com.phcpro.modules.documents.repository;

import com.phcpro.modules.documents.model.DocumentColumnConfig;
import com.phcpro.modules.documents.model.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentColumnConfigRepository extends JpaRepository<DocumentColumnConfig, Long> {

    Optional<DocumentColumnConfig> findByCompanyIdAndDocumentType(Long companyId, DocumentType documentType);
}
