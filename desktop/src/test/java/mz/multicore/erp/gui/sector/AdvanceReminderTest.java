package mz.multicore.erp.gui.sector;

import mz.multicore.erp.modules.advances.dto.CustomerAdvanceDTO;
import mz.multicore.erp.modules.advances.model.CustomerAdvanceStatus;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** After an invoice, the user is reminded only of open advances of the same client. */
class AdvanceReminderTest {

    private static CustomerAdvanceDTO advance(long clientId, String amount, CustomerAdvanceStatus status, long advanceInvoiceId) {
        return new CustomerAdvanceDTO(1L, clientId, "Ana", null, null, new BigDecimal(amount), status, advanceInvoiceId, "FT 1",
                1L, "RC 1", "CASH", null, null, null, null, null, null, "gestor", null, null);
    }

    private static InvoiceDTO invoice(long id, long clientId) {
        InvoiceDTO invoice = mock(InvoiceDTO.class);
        when(invoice.id()).thenReturn(id);
        when(invoice.clientId()).thenReturn(clientId);
        when(invoice.clientName()).thenReturn("Ana");
        when(invoice.invoiceNumber()).thenReturn("FT 2026/50");
        return invoice;
    }

    @Test
    void remindsOnlyOpenAdvancesOfTheSameClient() {
        List<CustomerAdvanceDTO> advances = List.of(advance(7, "3000", CustomerAdvanceStatus.OPEN, 10),
                advance(7, "500", CustomerAdvanceStatus.APPLIED, 11), advance(8, "900", CustomerAdvanceStatus.OPEN, 12));
        String message = AdvanceReminder.message(advances, invoice(50, 7));
        assertThat(message).contains("Ana").contains("FT 2026/50").contains("Aplicar na Fatura");
        assertThat(message).doesNotContain("3 500");
        assertThat(AdvanceReminder.message(advances, invoice(50, 9))).isNull();
        assertThat(AdvanceReminder.message(advances, invoice(10, 7))).as("the advance invoice itself").isNull();
    }
}
