package mz.multicore.erp.modules.comercial.model;

import mz.multicore.erp.architecture.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "customer_order_events", indexes = @Index(name = "idx_order_events_order_created", columnList = "order_id,created_at"))
@Getter @Setter
public class OrderEvent extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false) private Order order;
    @Column(name = "event_type", nullable = false, length = 50, updatable = false) private String eventType;
    @Column(name = "previous_status", length = 40, updatable = false) private String previousStatus;
    @Column(name = "new_status", length = 40, updatable = false) private String newStatus;
    @Column(name = "actor_role", nullable = false, length = 30, updatable = false) private String actorRole;
    @Column(name = "terminal_name", length = 100, updatable = false) private String terminalName;
    @Column(name = "details", length = 1000, updatable = false) private String details;
}
