package mz.multicore.erp.modules.printing;

import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.SalesChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Harness CD-10 e CD-11: só uma venda POS se chama recibo. */
class ReceiptTitleHarnessTest {

    private Invoice invoice(SalesChannel channel) {
        Invoice invoice = new Invoice();
        invoice.setSalesChannel(channel);
        return invoice;
    }

    @Test
    void vendaPos_eRecibo() {
        assertEquals("RECIBO POS", ReceiptDocumentRenderer.title(invoice(SalesChannel.POS)));
    }

    @Test
    void faturaManualOuDeEncomenda_naoERecibo() {
        assertEquals("FATURA", ReceiptDocumentRenderer.title(invoice(SalesChannel.MANUAL)));
        assertEquals("FATURA", ReceiptDocumentRenderer.title(invoice(SalesChannel.ORDER)));
    }
}
