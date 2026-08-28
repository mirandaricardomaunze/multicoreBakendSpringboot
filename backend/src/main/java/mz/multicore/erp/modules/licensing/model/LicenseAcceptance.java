package mz.multicore.erp.modules.licensing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "license_acceptances", uniqueConstraints =
        @UniqueConstraint(name = "uk_license_acceptance_company_version", columnNames = {"company_id", "license_version"}))
@Getter
@Setter
public class LicenseAcceptance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "license_version", nullable = false, length = 40)
    private String licenseVersion;

    @Column(name = "license_sha256", nullable = false, length = 64)
    private String licenseSha256;

    @Column(name = "accepted_by", nullable = false, length = 120)
    private String acceptedBy;

    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;

    @Column(name = "declaration", nullable = false, length = 500)
    private String declaration;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "client_version", length = 40)
    private String clientVersion;

    @Column(name = "user_agent", length = 300)
    private String userAgent;
}
