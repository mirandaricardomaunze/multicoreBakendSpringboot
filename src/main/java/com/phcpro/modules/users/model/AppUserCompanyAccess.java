package com.phcpro.modules.users.model;

import com.phcpro.modules.company.model.Company;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_user_companies")
@Getter
@Setter
@NoArgsConstructor
public class AppUserCompanyAccess {

    @EmbeddedId
    private AppUserCompanyAccessId id = new AppUserCompanyAccessId();

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @MapsId("companyId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "role", nullable = false, length = 30)
    private String role;

    public AppUserCompanyAccess(AppUser user, Company company, String role) {
        this.user = user;
        this.company = company;
        this.role = role;
    }
}
