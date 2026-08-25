package mz.multicore.erp.modules.hr.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

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

    @Column(name = "attachment_data")
    private byte[] attachmentData;

    public long daysUntilExpiry(LocalDate today) {
        return ChronoUnit.DAYS.between(today, expiryDate);
    }
}
