package mz.multicore.erp.gui.pos;

import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** docs/TABELAS_PRECO_SPEC.md, POS: the cart shows the price the server charges, and never sends it. */
class PosCartListPriceTest {

    private static ProductDTO product() {
        ProductDTO p = mock(ProductDTO.class);
        when(p.unitPrice()).thenReturn(new BigDecimal("900.00"));
        when(p.taxRate()).thenReturn(BigDecimal.ZERO);
        return p;
    }

    @Test
    @DisplayName("TP-POS-01: Preço da tabela substitui o normal; preço de cotação manda sobre os dois")
    void listPriceReplacesNormalPriceButNotTheQuotedOne() {
        PosCartItem line = new PosCartItem(product(), BigDecimal.TEN, BigDecimal.ZERO, null, null);
        assertThat(line.getTotal()).isEqualByComparingTo("9000.00");

        line.listUnitPrice = new BigDecimal("850.00");
        assertThat(line.getEffectiveUnitPrice()).isEqualByComparingTo("850.00");
        assertThat(line.getTotal()).isEqualByComparingTo("8500.00");

        line.customUnitPrice = new BigDecimal("800.00");
        assertThat(line.getEffectiveUnitPrice()).as("quoted price").isEqualByComparingTo("800.00");

        line.customUnitPrice = null;
        line.listUnitPrice = null;
        assertThat(line.getEffectiveUnitPrice()).as("client without list").isEqualByComparingTo("900.00");
    }

    @Test
    @DisplayName("TP-POS-02: O checkout não envia o preço mostrado; o servidor volta a resolvê-lo")
    void checkoutDoesNotSendTheDisplayedListPrice() throws Exception {
        String checkout = Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/PosCheckoutCoordinator.java"));
        assertThat(checkout).contains("i.customUnitPrice").doesNotContain("listUnitPrice");
        String sector = Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/PosSectorPricing.java"));
        assertThat(sector).contains("pricing.resolve(").doesNotContain("customUnitPrice =");
    }
}
