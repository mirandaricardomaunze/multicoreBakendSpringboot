package com.phcpro.modules.audit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime = LocalDateTime.now();

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "action", nullable = false)
    private String action; // LOGIN, LOGOUT, DOC_CANCEL, BACKUP, stock_adjust, POS_SESSION

    @Column(name = "details", length = 1000)
    private String details;
}
