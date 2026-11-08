package mz.multicore.erp.modules.units.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Product;

import java.math.BigDecimal;

/**
 * Alternative unit of a product (SA-C02): one {@link #unit} equals {@link #factorToBase} base units.
 * It only converts input and display; stock, prices and documents stay in the base unit.
 */
@Entity
@Table(name = "product_units", uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_unit", columnNames = {"product_id", "unit_id"}),
        @UniqueConstraint(name = "uk_product_unit_barcode", columnNames = {"barcode"})
})
@Getter
@Setter
public class ProductUnit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private UnitOfMeasure unit;

    @Column(name = "factor_to_base", nullable = false, precision = 18, scale = 6)
    private BigDecimal factorToBase;

    /** Barcode of this package itself, for example the pallet EAN. */
    @Column(name = "barcode", length = 64)
    private String barcode;

    @Column(name = "sellable", nullable = false)
    private boolean sellable = true;

    @Column(name = "purchasable", nullable = false)
    private boolean purchasable = true;

    @Column(name = "default_sale", nullable = false)
    private boolean defaultSale;

    @Column(name = "default_purchase", nullable = false)
    private boolean defaultPurchase;
}
