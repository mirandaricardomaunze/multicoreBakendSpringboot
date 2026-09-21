package mz.multicore.erp.modules.performance.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.performance.model.BonusStatus;

import java.math.BigDecimal;

@Entity
@Table(name = "sales_goal_bonuses")
@Getter
@Setter
public class SalesGoalBonus extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false)
    private SalesGoal goal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "calculated_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal calculatedAmount = BigDecimal.ZERO;

    @Column(name = "approved_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal approvedAmount = BigDecimal.ZERO;

    @Column(name = "justification", length = 500)
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BonusStatus status = BonusStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payslip_id")
    private Payslip payslip;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;
}
