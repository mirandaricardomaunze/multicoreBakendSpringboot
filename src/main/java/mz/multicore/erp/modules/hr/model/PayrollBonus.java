package mz.multicore.erp.modules.hr.model;

import mz.multicore.erp.architecture.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Subsídio legal pago a um colaborador (13.º mês / subsídio de férias). Registo persistido para
 * garantir idempotência (não pagar duas vezes) e rasto da saída de tesouraria.
 */
@Entity
@Table(name = "payroll_bonuses", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"employee_id", "bonus_type", "ref_year", "reference_id"})
})
@Getter
@Setter
public class PayrollBonus extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "bonus_type", nullable = false, length = 30)
    private String bonusType; // THIRTEENTH_MONTH, VACATION_ALLOWANCE

    @Column(name = "ref_year", nullable = false)
    private int year;

    /** Documento de origem (ex.: pedido de férias) quando aplicável; 0 quando não aplicável. */
    @Column(name = "reference_id", nullable = false)
    private long referenceId = 0L;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PAID";

    @Column(name = "payment_date")
    private LocalDate paymentDate;
}
