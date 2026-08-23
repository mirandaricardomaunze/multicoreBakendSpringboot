package mz.multicore.erp.modules.crm.model;

import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.company.model.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private TicketPriority priority = TicketPriority.NORMAL;

    /** Técnico responsável pelo pedido. Nulo enquanto ninguém o assumir. */
    @Column(name = "assigned_technician")
    private String assignedTechnician;

    /** Momento em que o pedido saiu de aberto (resolvido ou anulado). Nulo enquanto estiver em curso. */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /** Motivo da anulação, ou nota de fecho quando resolvido sem folha de obra. */
    @Column(name = "closing_note", length = 500)
    private String closingNote;
}
