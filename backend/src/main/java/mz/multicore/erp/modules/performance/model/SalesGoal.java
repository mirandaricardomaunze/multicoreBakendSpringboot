package mz.multicore.erp.modules.performance.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.performance.model.BonusType;
import mz.multicore.erp.modules.performance.model.GoalPeriod;
import mz.multicore.erp.modules.performance.model.GoalScope;
import mz.multicore.erp.modules.performance.model.GoalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "sales_goals", uniqueConstraints = {
        @UniqueConstraint(name = "uk_sales_goals_company_name_start", columnNames = {"company_id", "name", "period_start"})
})
@Getter
@Setter
public class SalesGoal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false, length = 32)
    private GoalPeriod period;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 32)
    private GoalScope scope;

    @Column(name = "scope_ref_id")
    private Long scopeRefId;

    @Column(name = "scope_label")
    private String scopeLabel;

    @Column(name = "target_revenue", precision = 14, scale = 2)
    private BigDecimal targetRevenue;

    @Column(name = "target_margin", precision = 14, scale = 2)
    private BigDecimal targetMargin;

    @Column(name = "target_margin_pct", precision = 7, scale = 4)
    private BigDecimal targetMarginPct;

    @Enumerated(EnumType.STRING)
    @Column(name = "bonus_type", nullable = false, length = 32)
    private BonusType bonusType;

    @Column(name = "bonus_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal bonusValue = BigDecimal.ZERO;

    @Column(name = "bonus_cap", precision = 14, scale = 2)
    private BigDecimal bonusCap;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private GoalStatus status = GoalStatus.ACTIVE;

    @Column(name = "auto_apply_bonus", nullable = false)
    private boolean autoApplyBonus = true;
}
