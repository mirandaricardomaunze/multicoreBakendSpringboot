package mz.multicore.erp.modules.hr.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A cessação do vínculo e o <b>acerto final</b> que a acompanha.
 * Ver docs/RH_COMPLETO_SPEC.md §B3.
 *
 * <p><b>O que isto substitui:</b> {@code changeEmployeeStatus(id, "TERMINATED")} — uma String, e
 * nada mais acontecia. O colaborador que saía tinha direito a proporcionais que o sistema já sabia
 * calcular (13.º, férias não gozadas) e nunca calculava neste contexto.
 *
 * <p><b>As linhas são gravadas, ao contrário dos totais do ponto</b>, e isso é deliberado: um acerto
 * é um <b>documento</b>. O que foi acordado e pago naquele dia não pode mudar porque o direito a
 * férias da empresa mudou no ano seguinte — mesma razão pela qual a {@code InvoiceLine} fotografa o
 * custo (V37) e a fatura fotografa o preço.
 */
@Entity
@Table(name = "terminations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_terminations_employee", columnNames = {"company_id", "employee_id"}),
        @UniqueConstraint(name = "uk_terminations_number", columnNames = {"company_id", "settlement_number"})})
@Getter
@Setter
public class Termination extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** Cessa-se um contrato. Nulo só para quem saiu sem contrato registado (anterior ao B1). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private EmploymentContract contract;

    @Column(name = "settlement_number", nullable = false, length = 40)
    private String settlementNumber;

    @Column(name = "termination_date", nullable = false)
    private LocalDate terminationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private TerminationReason reason;

    @Column(name = "notice_served", nullable = false)
    private boolean noticeServed = true;

    @Column(name = "total_earnings", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalEarnings = BigDecimal.ZERO;

    @Column(name = "total_deductions", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SettlementStatus status = SettlementStatus.POR_PAGAR;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "notes", length = 500)
    private String notes;

    @OneToMany(mappedBy = "termination", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("lineOrder ASC")
    private List<TerminationSettlementLine> lines = new ArrayList<>();

    public void addLine(TerminationSettlementLine line) {
        line.setTermination(this);
        line.setLineOrder(lines.size() + 1);
        lines.add(line);
    }

    /**
     * Recalcula os totais a partir das linhas. <b>O líquido pode ser negativo</b> — quem sai com um
     * empréstimo maior do que os proporcionais fica a dever à empresa, e esconder isso num zero
     * fingiria que a dívida desapareceu com a saída.
     */
    public void recalculateTotals() {
        totalEarnings = lines.stream().filter(TerminationSettlementLine::isEarning)
                .map(TerminationSettlementLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        totalDeductions = lines.stream().filter(l -> !l.isEarning())
                .map(TerminationSettlementLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        netAmount = totalEarnings.subtract(totalDeductions);
    }
}
