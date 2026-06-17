package com.phcpro.modules.users.model;

import com.phcpro.architecture.BaseEntity;
import com.phcpro.modules.company.model.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "app_users")
@Getter
@Setter
public class AppUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "role", nullable = false)
    private String role; // EMPLOYEE, MANAGER, ADMIN

    @Column(name = "active")
    private boolean active = true;

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER, cascade = jakarta.persistence.CascadeType.ALL,
            orphanRemoval = true)
    private Set<AppUserCompanyAccess> companyAccesses = new LinkedHashSet<>();

    public boolean hasCompany(Long companyId) {
        return companyId != null && companyAccesses.stream()
                .anyMatch(access -> companyId.equals(access.getCompany().getId()));
    }

    public String getRoleForCompany(Long companyId) {
        return findCompanyAccess(companyId)
                .map(AppUserCompanyAccess::getRole)
                .orElse(role);
    }

    public void grantCompany(Company company, String companyRole) {
        String normalizedRole = UserRole.normalize(companyRole);
        companyAccesses.stream()
                .filter(access -> company.getId() != null && company.getId().equals(access.getCompany().getId()))
                .findFirst()
                .ifPresentOrElse(
                        access -> access.setRole(normalizedRole),
                        () -> companyAccesses.add(new AppUserCompanyAccess(this, company, normalizedRole))
                );
    }

    public Set<Company> getCompanies() {
        return companyAccesses.stream()
                .map(AppUserCompanyAccess::getCompany)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Optional<AppUserCompanyAccess> findCompanyAccess(Long companyId) {
        return companyAccesses.stream()
                .filter(access -> companyId != null && companyId.equals(access.getCompany().getId()))
                .findFirst();
    }
}
