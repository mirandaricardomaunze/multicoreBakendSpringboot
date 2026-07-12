package com.phcpro.modules.numbering.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Contador persistido por (empresa, série, ano) usado para emitir números de documento
 * sequenciais e sem saltos (gapless), como exigido pela AT/SAF-T.
 *
 * A numeração é <b>por empresa</b>: cada empresa (contribuinte/NUIT) tem a sua própria sequência,
 * pelo que não há saltos causados por documentos de outras empresas. O acesso concorrente é
 * serializado por bloqueio pessimista no {@code DocumentNumberService}.
 */
@Entity
@Table(
        name = "document_sequences",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_document_sequence_company_series_year",
                columnNames = {"company_id", "series", "doc_year"})
)
@Getter
@Setter
public class DocumentSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "series", nullable = false)
    private String series;

    // "year" é palavra reservada em SQL (H2/Postgres) — mapear para coluna não-reservada.
    @Column(name = "doc_year", nullable = false)
    private int year;

    @Column(name = "last_number", nullable = false)
    private long lastNumber;
}
