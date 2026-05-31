package com.phcpro.modules.comercial.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A debit-note line is purely financial — no product reference. The description
 * captures what the additional charge is for (e.g. "Frete adicional", "Juros mora").
 */
@Entity
@Table(name = "debit_note_lines")
@Getter
@Setter
public class DebitNoteLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debit_note_id", nullable = false)
    private DebitNote debitNote;

    @Column(name = "description", nullable = false, length = 300)
    private String description;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;
}
