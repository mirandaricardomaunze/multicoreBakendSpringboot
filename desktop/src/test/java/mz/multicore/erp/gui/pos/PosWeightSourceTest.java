package mz.multicore.erp.gui.pos;

import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** A cart line of a product sold by weight knows whether its quantity came from the scale (SA-C21). */
class PosWeightSourceTest {

    private static ProductDTO product(String saleType) {
        ProductDTO p = mock(ProductDTO.class);
        when(p.saleType()).thenReturn(saleType);
        return p;
    }

    @Test
    void weighedLineIsScaleTypedOrEditedIsManualOtherProductsHaveNone() {
        PosCartItem weighed = new PosCartItem(product("WEIGHT"), new BigDecimal("1.250"), BigDecimal.ZERO, null, null);
        weighed.scaleWeight = new BigDecimal("1.250");
        assertThat(weighed.weightSource()).isEqualTo("SCALE");

        weighed.qty = new BigDecimal("1.500");
        assertThat(weighed.weightSource()).as("weight changed after reading the scale").isEqualTo("MANUAL");

        PosCartItem typed = new PosCartItem(product("WEIGHT"), new BigDecimal("2"), BigDecimal.ZERO, null, null);
        assertThat(typed.weightSource()).isEqualTo("MANUAL");

        PosCartItem unit = new PosCartItem(product("UNIT"), BigDecimal.ONE, BigDecimal.ZERO, null, null);
        assertThat(unit.weightSource()).isNull();
    }
}
