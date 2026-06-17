package com.phcpro.modules.users.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class AppUserCompanyAccessId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "company_id")
    private Long companyId;

    public AppUserCompanyAccessId(Long userId, Long companyId) {
        this.userId = userId;
        this.companyId = companyId;
    }
}
