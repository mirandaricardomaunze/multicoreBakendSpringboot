package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.pos.dto.PosSalePaymentSummaryDTO;
import mz.multicore.erp.modules.pos.model.PaymentEntry;
import mz.multicore.erp.modules.pos.model.PaymentMethod;
import mz.multicore.erp.modules.pos.repository.PaymentEntryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Harness de docs/POS_HISTORICO_PAGAMENTO_CLIENTE_SPEC.md (PHP-02 e PHP-03). */
class PosSalePaymentQueryServiceTest {

    private static final Long COMPANY_ID = 7L;

    private final PaymentEntryRepository repository = mock(PaymentEntryRepository.class);
    private final PosSalePaymentQueryService service = new PosSalePaymentQueryService(repository);

    @BeforeEach
    void login() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("caixa1", "SELLER");
    }

    @AfterEach
    void logout() {
        CurrentUserContext.clear();
    }

    @Test
    void cashSaleReturnsTenderedAndChangeAsRecorded() {
        when(repository.findByInvoiceCompanyIdAndInvoiceIdIn(eq(COMPANY_ID), any()))
                .thenReturn(List.of(entry(257L, PaymentMethod.CASH, "1460.00", "2000.00", "540.00")));

        List<PosSalePaymentSummaryDTO> result = service.summarize(List.of(257L));

        assertThat(result).containsExactly(new PosSalePaymentSummaryDTO(257L, new BigDecimal("2000.00"), new BigDecimal("540.00")));
    }

    @Test
    void splitPaymentSumsTenderedAndCountsCardAmountAsTendered() {
        when(repository.findByInvoiceCompanyIdAndInvoiceIdIn(eq(COMPANY_ID), any())).thenReturn(List.of(
                entry(9L, PaymentMethod.CASH, "500.00", "600.00", "100.00"),
                entry(9L, PaymentMethod.CARD, "300.00", null, null)));

        assertThat(service.summarize(List.of(9L)))
                .containsExactly(new PosSalePaymentSummaryDTO(9L, new BigDecimal("900.00"), new BigDecimal("100.00")));
    }

    @Test
    void queryIsScopedToTheActiveCompany() {
        when(repository.findByInvoiceCompanyIdAndInvoiceIdIn(eq(COMPANY_ID), any())).thenReturn(List.of());

        service.summarize(List.of(1L, 2L, 2L));

        verify(repository).findByInvoiceCompanyIdAndInvoiceIdIn(COMPANY_ID, Set.of(1L, 2L));
    }

    @Test
    void emptyListDoesNotHitTheDatabaseAndTooManyIdsAreRejected() {
        assertThat(service.summarize(List.of())).isEmpty();
        verify(repository, never()).findByInvoiceCompanyIdAndInvoiceIdIn(any(), any());

        List<Long> tooMany = LongStream.rangeClosed(1, PosSalePaymentQueryService.MAX_INVOICES + 1).boxed().toList();
        assertThatThrownBy(() -> service.summarize(tooMany))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no máximo");
    }

    private static PaymentEntry entry(Long invoiceId, PaymentMethod method, String amount, String tendered, String change) {
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        PaymentEntry entry = new PaymentEntry();
        entry.setInvoice(invoice);
        entry.setMethod(method);
        entry.setAmount(new BigDecimal(amount));
        entry.setTenderedAmount(tendered == null ? null : new BigDecimal(tendered));
        entry.setChangeGiven(change == null ? null : new BigDecimal(change));
        return entry;
    }
}
