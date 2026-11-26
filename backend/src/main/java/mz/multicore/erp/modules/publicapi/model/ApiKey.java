package mz.multicore.erp.modules.publicapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Public API key of a company (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.1, API-01). The full key is never stored:
 * only its SHA-256 hash (to authenticate) and the first 8 characters after {@code mk_live_} (to identify it).
 */
@Entity
@Table(name = "api_keys")
@Getter
@Setter
public class ApiKey extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "key_prefix", nullable = false, length = 8)
    private String keyPrefix;

    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    private String keyHash;

    @Convert(converter = ApiKeyPermissionsConverter.class)
    @Column(name = "permissions", nullable = false, length = 100)
    private Set<ApiKeyPermission> permissions = EnumSet.noneOf(ApiKeyPermission.class);

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by")
    private String revokedBy;

    public boolean isActive() {
        return revokedAt == null;
    }
}
