package mz.multicore.erp.modules.pricing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Product;

import java.math.BigDecimal;

/**
 * Product price in a list, expressed in the base unit and on the same VAT basis as {@code Product.unitPrice}.
 * Several tiers for the same product are distinguished by {@link #minQuantity}.
 */
@Entity
@Table(name = "price_list_items", uniqueConstraints = {
        @UniqueConstraint(name = "uk_price_list_item", columnNames = {"price_list_id", "product_id", "min_quantity"})
}, indexes = {
        @Index(name = "idx_price_list_item_lookup", columnList = "price_list_id, product_id")
})
@Getter
@Setter
public class PriceListItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "price_list_id", nullable = false)
    private PriceList priceList;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "min_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal minQuantity = BigDecimal.ZERO;

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;
}
