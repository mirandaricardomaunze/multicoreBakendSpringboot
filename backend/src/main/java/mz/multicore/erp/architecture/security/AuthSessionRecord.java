package mz.multicore.erp.architecture.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;

import java.time.Instant;

/** A persisted login session, keyed by the SHA-256 of its bearer token so the table never holds a usable token. */
@Entity
@Table(name = "auth_sessions")
@Getter
@Setter
public class AuthSessionRecord extends BaseEntity {

    @Id
    @Column(name = "token_hash", length = 64, nullable = false)
    private String tokenHash;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "restricted", nullable = false)
    private boolean restricted;
}
