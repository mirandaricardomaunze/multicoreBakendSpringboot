package mz.multicore.erp.modules.customorders.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.company.model.Company;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Delivery and personalisation of an order (SA-C13, {@code docs/ENCOMENDAS_PERSONALIZADAS_SPEC.md}). Kept apart so
 * the order and its HTTP contracts do not change; the delivery date itself stays on the order.
 */
@Entity
@Table(name = "order_customizations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_order_customization_order", columnNames = {"order_id"})
})
@Getter
@Setter
public class OrderCustomization extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "delivery_time")
    private LocalTime deliveryTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", nullable = false, length = 20)
    private DeliveryMode mode = DeliveryMode.PICKUP;

    @Column(name = "delivery_address", length = 300)
    private String deliveryAddress;

    @Column(name = "contact_phone", length = 40)
    private String contactPhone;

    @Column(name = "personalization", length = 1000)
    private String personalization;

    @Column(name = "image_data", length = 2_200_000)
    private byte[] imageData;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 20)
    private CustomOrderStage stage = CustomOrderStage.TO_PRODUCE;

    @Column(name = "sent_to_plan_at")
    private LocalDateTime sentToPlanAt;
}
