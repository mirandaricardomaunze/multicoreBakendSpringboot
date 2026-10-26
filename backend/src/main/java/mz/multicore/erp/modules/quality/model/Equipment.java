package mz.multicore.erp.modules.quality.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.quality.dto.EquipmentCategory;
import mz.multicore.erp.modules.quality.dto.PeriodUnit;

@Entity
@Table(name = "quality_equipments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_quality_equipment_code", columnNames = {"company_id", "code"})
})
@Getter
@Setter
public class Equipment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private EquipmentCategory category = EquipmentCategory.PPE_PROTECTION;

    @Column(name = "period_value", nullable = false)
    private int periodValue = 6;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_unit", nullable = false, length = 20)
    private PeriodUnit periodUnit = PeriodUnit.MONTHS;

    @Column(name = "warning_days_before", nullable = false)
    private int warningDaysBefore = 15;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "description", length = 500)
    private String description;
}
