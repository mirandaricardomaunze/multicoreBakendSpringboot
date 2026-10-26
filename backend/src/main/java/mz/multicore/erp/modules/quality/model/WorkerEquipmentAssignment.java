package mz.multicore.erp.modules.quality.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.quality.dto.AssignmentStatus;
import mz.multicore.erp.modules.quality.dto.PeriodUnit;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "quality_worker_assignments", indexes = {
        @Index(name = "idx_qwa_company_status", columnList = "company_id, status"),
        @Index(name = "idx_qwa_employee", columnList = "company_id, employee_id"),
        @Index(name = "idx_qwa_expiry", columnList = "company_id, expiry_date")
})
@Getter
@Setter
public class WorkerEquipmentAssignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "assigned_date", nullable = false)
    private LocalDate assignedDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "quantity", nullable = false)
    private int quantity = 1;

    @Column(name = "size_or_spec", length = 60)
    private String sizeOrSpec;

    @Column(name = "serial_number", length = 80)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AssignmentStatus status = AssignmentStatus.ACTIVE;

    @Column(name = "notes", length = 500)
    private String notes;

    public static LocalDate calculateExpiryDate(LocalDate assignedDate, int periodValue, PeriodUnit periodUnit) {
        if (assignedDate == null) {
            assignedDate = LocalDate.now();
        }
        int value = Math.max(1, periodValue);
        if (periodUnit == PeriodUnit.DAYS) {
            return assignedDate.plusDays(value);
        }
        return assignedDate.plusMonths(value);
    }

    public long calculateDaysUntilExpiry(LocalDate today) {
        if (expiryDate == null || today == null) return 0;
        return ChronoUnit.DAYS.between(today, expiryDate);
    }

    public AssignmentStatus computeCurrentStatus(LocalDate today) {
        if (status == AssignmentStatus.RENEWED || status == AssignmentStatus.RETURNED || status == AssignmentStatus.DISCARDED) {
            return status;
        }
        if (expiryDate == null || today == null) {
            return AssignmentStatus.ACTIVE;
        }
        if (today.isAfter(expiryDate)) {
            return AssignmentStatus.EXPIRED;
        }
        int warningDays = equipment != null ? equipment.getWarningDaysBefore() : 15;
        if (today.plusDays(warningDays).isAfter(expiryDate) || today.plusDays(warningDays).isEqual(expiryDate)) {
            return AssignmentStatus.EXPIRING_SOON;
        }
        return AssignmentStatus.ACTIVE;
    }
}
