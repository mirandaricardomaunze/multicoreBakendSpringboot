package com.phcpro.modules.pos.model;

import com.phcpro.architecture.BaseEntity;
import com.phcpro.modules.comercial.model.Invoice;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Uma entrada de pagamento associada a uma Invoice. Cada venda pode ter
 * várias entradas (pagamento dividido: dinheiro + cartão, etc.).
 */
@Entity
@Table(name = "payment_entries")
@Getter
@Setter
public class PaymentEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    /** Valor entregue pelo cliente (para CASH; outros = amount). */
    @Column(name = "tendered_amount", precision = 14, scale = 2)
    private BigDecimal tenderedAmount;

    /** Troco devolvido ao cliente (CASH). */
    @Column(name = "change_given", precision = 14, scale = 2)
    private BigDecimal changeGiven = BigDecimal.ZERO;

    /** Referência externa: nº autorização cartão, comprovativo de transferência, etc. */
    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt = LocalDateTime.now();
}
