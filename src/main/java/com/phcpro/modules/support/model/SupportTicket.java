package com.phcpro.modules.support.model;

import com.phcpro.architecture.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Pedido de assistência aberto por uma empresa e tratado pelo superadmin. */
// Nome de entidade distinto para não colidir com crm.model.SupportTicket (suporte ao cliente).
@Entity(name = "PlatformSupportTicket")
@Table(name = "support_tickets")
@Getter
@Setter
public class SupportTicket extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private TicketPriority priority = TicketPriority.NORMAL;

    /** Superadmin responsável (username), preenchido quando alguém responde/assume. */
    @Column(name = "assignee")
    private String assignee;
}
