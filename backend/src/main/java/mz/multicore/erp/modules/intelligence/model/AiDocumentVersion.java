package mz.multicore.erp.modules.intelligence.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;

@Entity
@Table(name = "ai_document_versions", uniqueConstraints = @UniqueConstraint(
        name = "uk_ai_document_version", columnNames = {"document_id", "version_number"}))
@Getter
@Setter
public class AiDocumentVersion extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private AiManagedDocument document;
    @Column(name = "version_number", nullable = false)
    private int versionNumber;
    @Lob @Column(nullable = false, columnDefinition = "TEXT")
    private String instructions;
    @Lob @Column(name = "generated_content", nullable = false, columnDefinition = "TEXT")
    private String generatedContent;
    @Lob @Column(name = "pdf_data", nullable = false)
    private byte[] pdfData;
    @Column(name = "generated_by", nullable = false, length = 120)
    private String generatedBy;
}
