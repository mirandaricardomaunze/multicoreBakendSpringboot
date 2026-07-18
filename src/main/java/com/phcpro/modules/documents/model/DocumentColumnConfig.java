package com.phcpro.modules.documents.model;

import com.phcpro.architecture.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuração (por empresa) de que colunas aparecem na tabela de linhas dos documentos comerciais
 * (fatura, encomenda, nota de crédito, guia — que partilham o {@code LineItemsTableRenderer}).
 * Uma linha por empresa; ausência = todas visíveis (default).
 */
@Entity
@Table(name = "document_column_config",
        uniqueConstraints = @UniqueConstraint(name = "uk_doc_col_company_type",
                columnNames = {"company_id", "document_type"}))
@Getter
@Setter
public class DocumentColumnConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    /** Tipo de documento a que esta configuração se aplica (comercial vs recibo POS). */
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private DocumentType documentType = DocumentType.COMMERCIAL;

    @Column(name = "show_barcode", nullable = false)
    private boolean showBarcode = true;

    @Column(name = "show_reference", nullable = false)
    private boolean showReference = true;

    @Column(name = "show_description", nullable = false)
    private boolean showDescription = true;

    @Column(name = "show_expiry", nullable = false)
    private boolean showExpiry = true;

    @Column(name = "show_quantity", nullable = false)
    private boolean showQuantity = true;

    @Column(name = "show_unit_price", nullable = false)
    private boolean showUnitPrice = true;

    @Column(name = "show_tax", nullable = false)
    private boolean showTax = true;

    @Column(name = "show_subtotal", nullable = false)
    private boolean showSubtotal = true;

    /** Comentário/rodapé personalizado (aplica-se ao recibo POS). Vazio = texto padrão. */
    @Column(name = "footer_comment", length = 255)
    private String footerComment;
}
