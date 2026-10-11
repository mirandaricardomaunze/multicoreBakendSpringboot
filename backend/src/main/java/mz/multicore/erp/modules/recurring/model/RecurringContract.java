package mz.multicore.erp.modules.recurring.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.company.model.Company;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recurring_contracts", uniqueConstraints = @UniqueConstraint(
        name = "uk_company_contract_number", columnNames = {"company_id", "contract_number"}))
@Getter
@Setter
public class RecurringContract extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "contract_number", nullable = false, length = 32)
    private String contractNumber;

    @Column(name = "description", nullable = false, length = 250)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ContractStatus status = ContractStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 30)
    private BillingFrequency frequency = BillingFrequency.MONTHLY;

    @Column(name = "billing_day_of_month", nullable = false)
    private int billingDayOfMonth = 1;

    @Column(name = "fixed_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal fixedAmount = BigDecimal.ZERO;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "next_billing_date", nullable = false)
    private LocalDate nextBillingDate;

    @Column(name = "last_billed_date")
    private LocalDate lastBilledDate;

    @Column(name = "auto_renew", nullable = false)
    private boolean autoRenew = true;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecurringBillingLog> billingLogs = new ArrayList<>();
}
