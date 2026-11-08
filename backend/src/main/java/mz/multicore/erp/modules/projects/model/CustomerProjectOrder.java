package mz.multicore.erp.modules.projects.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Order;

/**
 * Link between an order and a project. Kept outside the order so existing order contracts and fiscal
 * documents stay untouched; an order belongs to at most one project.
 */
@Entity
@Table(name = "customer_project_orders", uniqueConstraints = {
        @UniqueConstraint(name = "uk_customer_project_order", columnNames = {"order_id"})
})
@Getter
@Setter
public class CustomerProjectOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
}
