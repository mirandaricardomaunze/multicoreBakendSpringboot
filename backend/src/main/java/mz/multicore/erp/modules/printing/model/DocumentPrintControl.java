package mz.multicore.erp.modules.printing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.printing.dto.SensitiveDocumentType;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_print_controls", uniqueConstraints = @UniqueConstraint(
        name = "uk_document_print_control", columnNames = {"company_id", "document_type", "document_id"}))
@Getter
@Setter
public class DocumentPrintControl extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    private SensitiveDocumentType documentType;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "print_count", nullable = false)
    private int printCount;

    @Column(name = "printed_at")
    private LocalDateTime printedAt;

    @Column(name = "last_printed_by", length = 80)
    private String lastPrintedBy;

    @Column(name = "last_authorized_by", length = 80)
    private String lastAuthorizedBy;

    @Column(name = "last_reason", length = 500)
    private String lastReason;

    @Column(name = "last_terminal", length = 120)
    private String lastTerminal;
}
