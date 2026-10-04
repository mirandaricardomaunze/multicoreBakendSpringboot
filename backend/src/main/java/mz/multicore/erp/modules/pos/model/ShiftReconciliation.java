package mz.multicore.erp.modules.pos.model;

import mz.multicore.erp.modules.company.model.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registo de reconciliação parcial (passagem de turno) entre dois operadores
 * dentro da mesma sessão de caixa. Permite rastrear quem entregou a gaveta,
 * o saldo esperado vs contado, e a divergência no momento do handover.
 */
@Entity
@Table(name = "shift_reconciliations")
@Getter
@Setter
public class ShiftReconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "till_session_id", nullable = false)
    private TillSession tillSession;

    @Column(name = "outgoing_operator", nullable = false, length = 100)
    private String outgoingOperator;

    @Column(name = "incoming_operator", nullable = false, length = 100)
    private String incomingOperator;

    @Column(name = "reconciled_at", nullable = false)
    private LocalDateTime reconciledAt = LocalDateTime.now();

    @Column(name = "expected_cash", nullable = false, precision = 14, scale = 2)
    private BigDecimal expectedCash;

    @Column(name = "counted_cash", nullable = false, precision = 14, scale = 2)
    private BigDecimal countedCash;

    @Column(name = "difference", nullable = false, precision = 14, scale = 2)
    private BigDecimal difference;

    @Column(name = "cash_breakdown_json", columnDefinition = "CLOB")
    private String cashBreakdownJson;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
}
