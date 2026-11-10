package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.modules.pos.dto.PosPaymentRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** A CREDIT line is still owed and a voucher is not money: neither may reach the till or the cash account. */
class PosPaymentSplitTest {

    private static PosPaymentRequest pay(String method, String amount) {
        return new PosPaymentRequest(method, new BigDecimal(amount), null, null, null, null);
    }

    @Test
    void splitsWhatSettlesTheInvoiceFromTheMoneyThatCameIn() {
        PosPaymentSplit split = PosPaymentSplit.of(List.of(
                pay("CASH", "100"), pay("MPESA", "50"), pay("STORE_CREDIT", "30"), pay("CREDIT", "20")));

        assertThat(split.declared()).isEqualByComparingTo("200");
        assertThat(split.settled()).isEqualByComparingTo("180");
        assertThat(split.money()).isEqualByComparingTo("150");
        assertThat(split.cash()).isEqualByComparingTo("100");
    }
}
