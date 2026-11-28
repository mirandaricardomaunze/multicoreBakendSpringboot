package mz.multicore.erp.modules.onlineshop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Home delivery zone of the online shop: name and fixed fee >= 0 (docs/LOJA_ONLINE_SPEC.md §3.1, LO-01). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class OnlineShopDeliveryZone {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal fee;

    public OnlineShopDeliveryZone(String name, BigDecimal fee) {
        this.name = name;
        this.fee = fee;
    }
}
