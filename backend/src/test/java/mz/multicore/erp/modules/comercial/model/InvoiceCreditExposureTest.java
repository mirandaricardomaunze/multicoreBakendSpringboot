package mz.multicore.erp.modules.comercial.model;

import mz.multicore.erp.modules.advances.AdvancePayments;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** An advance invoice is paid on the spot, so it never counts against the client's credit limit. */
class InvoiceCreditExposureTest {

    private static InvoiceLine line(String sku, String total) {
        Product product = new Product();
        product.setSku(sku);
        InvoiceLine line = new InvoiceLine();
        line.setProduct(product);
        line.setLineTotal(new BigDecimal(total));
        return line;
    }

    @Test
    void advanceLinesDoNotCountAgainstTheCreditLimit() {
        Invoice advance = new Invoice();
        advance.addLine(line(AdvancePayments.systemSku(1L), "3000.00"));
        advance.setTotalAmount(new BigDecimal("3000.00"));
        assertThat(advance.creditExposure()).isEqualByComparingTo("0");

        Invoice sale = new Invoice();
        sale.addLine(line("CIM-42", "11600.00"));
        sale.setTotalAmount(new BigDecimal("11600.00"));
        assertThat(sale.creditExposure()).isEqualByComparingTo("11600");
    }
}
