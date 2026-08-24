package mz.multicore.erp.modules.hr.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;

import java.math.BigDecimal;

/** Uma linha do acerto final: um ganho a pagar ou um desconto a abater. §B3. */
@Entity
@Table(name = "termination_settlement_lines")
@Getter
@Setter
public class TerminationSettlementLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "termination_id", nullable = false)
    private Termination termination;

    /** Diz de onde veio o número. Um acerto com linhas anónimas não se confere contra nada. */
    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** Ganho a pagar (verdadeiro) ou desconto a abater (falso). */
    @Column(name = "earning", nullable = false)
    private boolean earning = true;

    @Column(name = "line_order", nullable = false)
    private int lineOrder;
}
