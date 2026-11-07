package mz.multicore.erp.gui;

import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.pos.dto.PosSalePaymentSummaryDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness de docs/POS_HISTORICO_PAGAMENTO_CLIENTE_SPEC.md (PHP-01 e PHP-04). */
class PosSalesHistoryPaymentHarnessTest {

    @Test
    void walkInSaleShowsTypedNameTenderedAndChange() {
        InvoiceDTO sale = sale("Consumidor Final", "Miranda");
        Object[] row = PosSalesHistoryViewCoordinator.historyRow(sale,
                new PosSalePaymentSummaryDTO(257L, new BigDecimal("2000.00"), new BigDecimal("540.00")));

        assertThat(row[4]).isEqualTo("Miranda");
        assertThat(row[5]).isEqualTo(new BigDecimal("1460.00"));
        assertThat(row[6]).isEqualTo(new BigDecimal("2000.00"));
        assertThat(row[7]).isEqualTo(new BigDecimal("540.00"));
        assertThat(row).hasSize(10);
    }

    @Test
    void registeredClientKeepsClientNameAndMissingPaymentShowsDash() {
        Object[] row = PosSalesHistoryViewCoordinator.historyRow(sale("Loja Xai-Xai Lda", null), null);

        assertThat(row[4]).isEqualTo("Loja Xai-Xai Lda");
        assertThat(row[6]).isEqualTo("—");
        assertThat(row[7]).isEqualTo("—");
    }

    private static InvoiceDTO sale(String clientName, String customerName) {
        return new InvoiceDTO(257L, "FT-2026/1", 1L, clientName, null,
                new BigDecimal("1258.62"), new BigDecimal("201.38"), new BigDecimal("1460.00"), new BigDecimal("1460.00"),
                InvoiceStatus.PAID, null, List.of(), LocalDateTime.of(2026, 11, 5, 21, 37), "caixa1",
                null, 0, null, null, null, customerName);
    }
}
