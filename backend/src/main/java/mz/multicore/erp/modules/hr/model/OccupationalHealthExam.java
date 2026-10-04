package mz.multicore.erp.modules.hr.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.purchases.model.Supplier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Exame de saúde ocupacional imutável no tempo; uma renovação cria uma nova linha. */
@Entity
@Table(name = "occupational_health_exams")
@Getter
@Setter
public class OccupationalHealthExam extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "card_number", length = 80)
    private String cardNumber;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    /** FIT · FIT_WITH_RESTRICTIONS · UNFIT. */
    @Column(name = "fitness_result", nullable = false, length = 30)
    private String fitnessResult;

    /**
     * O prestador que fez o exame, no registo de fornecedores — é onde a factura da clínica vive.
     * Anulável: clínicas ainda não cadastradas e os registos anteriores à V59 só têm {@link #clinic}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Supplier provider;

    /** Nome da clínica em texto livre, para quando não há prestador cadastrado. */
    @Column(name = "clinic", length = 160)
    private String clinic;

    @Column(name = "doctor_name", length = 160)
    private String doctorName;

    @Column(name = "restrictions", length = 1000)
    private String restrictions;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "attachment_name", length = 255)
    private String attachmentName;

    @Column(name = "attachment_data", length = 16_777_216)
    private byte[] attachmentData;

    /** Custo do exame, encargo do empregador. Anulável: nem toda a gente regista a factura. */
    @Column(name = "cost", precision = 19, scale = 2)
    private BigDecimal cost;

    @Column(name = "invoice_number", length = 60)
    private String invoiceNumber;

    /** Data em que a clínica foi paga. Nulo = por pagar. */
    @Column(name = "paid_at")
    private LocalDate paidAt;

    public long daysUntilExpiry(LocalDate today) {
        return ChronoUnit.DAYS.between(today, expiryDate);
    }

    /** Um exame sem custo registado não está "por pagar" — está por facturar. */
    public boolean isPayable() {
        return cost != null && cost.signum() > 0;
    }

    public boolean isPaid() {
        return paidAt != null;
    }

    /** O nome que se mostra: o prestador cadastrado manda sobre o texto livre. */
    public String providerLabel() {
        return provider != null ? provider.getName() : clinic;
    }
}
