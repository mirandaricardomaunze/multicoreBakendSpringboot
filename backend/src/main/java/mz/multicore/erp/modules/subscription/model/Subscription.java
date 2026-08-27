package mz.multicore.erp.modules.subscription.model;

import mz.multicore.erp.architecture.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Assinatura de uma empresa na plataforma (1:1 com a empresa). */
@Entity
@Table(name = "subscriptions")
@Getter
@Setter
public class Subscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false, unique = true)
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 20)
    private PlanType plan = PlanType.TRIAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status = SubscriptionStatus.TRIAL;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "monthly_price")
    private BigDecimal monthlyPrice = BigDecimal.ZERO;

    /**
     * Estado efectivo: uma assinatura ACTIVE/TRIAL cuja validade já passou conta como EXPIRED,
     * sem precisar de um job a mudar o campo. SUSPENDED prevalece (suspensão manual).
     */
    public SubscriptionStatus effectiveStatus() {
        if (status == SubscriptionStatus.SUSPENDED) {
            return SubscriptionStatus.SUSPENDED;
        }
        if (validUntil != null && validUntil.isBefore(LocalDate.now())) {
            return SubscriptionStatus.EXPIRED;
        }
        return status;
    }
}
