package mz.multicore.erp.modules.activity.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

/** One activity profile chosen by a company (SA-C01); one row per profile. */
@Entity
@Table(name = "company_activity_profiles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_company_activity_profile", columnNames = {"company_id", "profile"})
})
@Getter
@Setter
public class CompanyActivityProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile", nullable = false, length = 40)
    private ActivityProfile profile;
}
