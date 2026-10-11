package mz.multicore.erp.modules.intelligence.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_documents", uniqueConstraints = @UniqueConstraint(
        name = "uk_ai_documents_company_number", columnNames = {"company_id", "document_number"}))
@Getter
@Setter
public class AiManagedDocument extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "document_number", nullable = false, length = 32)
    private String documentNumber;
    @Column(nullable = false, length = 160)
    private String title;
    @Column(name = "document_type", nullable = false, length = 40)
    private String documentType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AiDocumentStatus status = AiDocumentStatus.RASCUNHO;
    @Column(name = "current_version", nullable = false)
    private int currentVersion = 1;
    @Column(name = "author_username", nullable = false, length = 120)
    private String authorUsername;
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    @Column(name = "reviewed_by", length = 120)
    private String reviewedBy;
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("versionNumber DESC")
    private List<AiDocumentVersion> versions = new ArrayList<>();
}
