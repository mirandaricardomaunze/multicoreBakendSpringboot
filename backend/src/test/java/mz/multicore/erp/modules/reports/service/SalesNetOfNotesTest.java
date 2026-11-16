package mz.multicore.erp.modules.reports.service;

import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** A sale partly returned with a credit note counts only what stayed sold. */
class SalesNetOfNotesTest {

    @Test
    void creditNotesComeOffTheSalesTotal() {
        Invoice sale = new Invoice();
        sale.setStatus(InvoiceStatus.PAID);
        sale.setTotalAmount(new BigDecimal("1000.00"));
        sale.applyNoteAdjustment(new BigDecimal("-300.00"));

        assertThat(new ReportSalesAnalyzer().summarise(List.of(sale)).totalAmount()).isEqualByComparingTo("700");
    }
}
