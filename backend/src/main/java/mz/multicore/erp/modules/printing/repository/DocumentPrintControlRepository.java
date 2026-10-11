package mz.multicore.erp.modules.printing.repository;

import mz.multicore.erp.modules.printing.dto.SensitiveDocumentType;
import mz.multicore.erp.modules.printing.model.DocumentPrintControl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DocumentPrintControlRepository extends JpaRepository<DocumentPrintControl, Long> {
    Optional<DocumentPrintControl> findByCompanyIdAndDocumentTypeAndDocumentId(
            Long companyId, SensitiveDocumentType documentType, Long documentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select control from DocumentPrintControl control "
            + "where control.companyId = :companyId and control.documentType = :documentType "
            + "and control.documentId = :documentId")
    Optional<DocumentPrintControl> findForUpdate(
            @Param("companyId") Long companyId,
            @Param("documentType") SensitiveDocumentType documentType,
            @Param("documentId") Long documentId);

    List<DocumentPrintControl> findByCompanyIdAndDocumentTypeAndDocumentIdIn(
            Long companyId, SensitiveDocumentType documentType, Collection<Long> documentIds);
}
