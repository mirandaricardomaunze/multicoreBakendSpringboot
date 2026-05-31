package com.phcpro.modules.crm.model;

import com.phcpro.architecture.BaseEntity;
import com.phcpro.modules.comercial.model.Client;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "crm_tickets")
@Getter
@Setter
public class SupportTicket extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "status", nullable = false)
    private String status = "OPEN"; // "OPEN", "RESOLVED"
}
