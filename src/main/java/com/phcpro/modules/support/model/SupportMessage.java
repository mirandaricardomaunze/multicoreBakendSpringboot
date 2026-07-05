package com.phcpro.modules.support.model;

import com.phcpro.architecture.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Mensagem numa conversa de um pedido de assistência. */
@Entity
@Table(name = "support_messages")
@Getter
@Setter
public class SupportMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "author", nullable = false)
    private String author;

    /** Distingue a resposta do superadmin da mensagem da empresa. */
    @Column(name = "from_super_admin", nullable = false)
    private boolean fromSuperAdmin = false;

    @Column(name = "body", nullable = false, length = 2000)
    private String body;
}
