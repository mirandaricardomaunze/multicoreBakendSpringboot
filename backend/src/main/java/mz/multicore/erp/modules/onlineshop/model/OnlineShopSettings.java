package mz.multicore.erp.modules.onlineshop.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.util.ArrayList;
import java.util.List;

/**
 * Online shop of a company (docs/LOJA_ONLINE_SPEC.md §3.1, LO-01): one row per company, created the first time the
 * ADMIN saves the settings. Disabling keeps the settings so the shop can be switched on again as it was.
 */
@Entity
@Table(name = "online_shop_settings")
@Getter
@Setter
public class OnlineShopSettings extends BaseEntity {

    /** Default automatic cancellation period of an unbilled online order (§3.1). */
    public static final int DEFAULT_AUTO_CANCEL_DAYS = 7;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private Company company;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    /** Public code of the shop in {@code /loja/{code}}: unique in the whole system. */
    @Column(name = "public_code", length = 50, unique = true)
    private String publicCode;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "auto_cancel_days", nullable = false)
    private int autoCancelDays = DEFAULT_AUTO_CANCEL_DAYS;

    /** Sales warehouses where customers pick up their orders, in the order chosen by the ADMIN. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "online_shop_pickup_warehouses", joinColumns = @JoinColumn(name = "settings_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "warehouse_id", nullable = false)
    private List<Long> pickupWarehouseIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "online_shop_delivery_zones", joinColumns = @JoinColumn(name = "settings_id"))
    @OrderColumn(name = "sort_order")
    private List<OnlineShopDeliveryZone> deliveryZones = new ArrayList<>();
}
