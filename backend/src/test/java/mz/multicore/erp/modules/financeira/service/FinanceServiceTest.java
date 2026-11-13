package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import mz.multicore.erp.modules.comercial.service.InvoiceSettlementService;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Liquidação de fatura pela tesouraria (RP-20..RP-23, docs/RECEBIMENTOS_SALDO_HARNESS.md). Desde 2026-11-12 passa
 * sempre por recibo (RC): antes punha a fatura paga sem documento e sem lançamento na contabilidade.
 */
class FinanceServiceTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long ACCOUNT_ID = 20L;
    private static final Long INVOICE_ID = 7L;

    private InvoiceRepository invoiceRepository;
    private ComercialService comercialService;
    private FinanceService financeService;
    private InvoiceSettlementService service;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        comercialService = mock(ComercialService.class);
        financeService = mock(FinanceService.class);
        when(financeService.accountType(ACCOUNT_ID)).thenReturn(TreasuryAccountType.CASH);
        service = new InvoiceSettlementService(invoiceRepository, comercialService, financeService);
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test // RP-20
    void payInvoice_semPerfilAutorizado_bloqueia() {
        CurrentUserContext.setCurrentUser("caixa", "EMPLOYEE");
        when(invoiceRepository.lockByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                .thenReturn(Optional.of(invoice(InvoiceStatus.APPROVED, "1000", "0")));

        assertThrows(BusinessRuleException.class, () -> service.settle(INVOICE_ID, ACCOUNT_ID, null));
        verify(comercialService, never()).createReceipt(anyLong(), anyLong(), anyString(), any());
    }

    @Test // RP-21
    void payInvoice_comPerfilAutorizado_emiteReciboPeloTotal() {
        when(invoiceRepository.lockByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                .thenReturn(Optional.of(invoice(InvoiceStatus.APPROVED, "1000", "0")));

        service.settle(INVOICE_ID, ACCOUNT_ID, null);

        verify(comercialService).createReceipt(INVOICE_ID, ACCOUNT_ID, "CASH", new BigDecimal("1000"));
    }

    @Test // RP-22
    void payInvoice_faturaParcialmentePaga_recebeSoOSaldoEmDivida() {
        // 1000 de total com 400 já recebidos por recibo: só faltam 600, e por banco é transferência.
        when(financeService.accountType(ACCOUNT_ID)).thenReturn(TreasuryAccountType.BANK);
        when(invoiceRepository.lockByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                .thenReturn(Optional.of(invoice(InvoiceStatus.PARTIALLY_PAID, "1000", "400")));

        service.settle(INVOICE_ID, ACCOUNT_ID, null);

        verify(comercialService).createReceipt(INVOICE_ID, ACCOUNT_ID, "BANK_TRANSFER", new BigDecimal("600"));
    }

    @Test // RP-23
    void payInvoice_faturaJaPaga_bloqueia() {
        when(invoiceRepository.lockByIdAndCompanyId(INVOICE_ID, COMPANY_ID))
                .thenReturn(Optional.of(invoice(InvoiceStatus.PAID, "1000", "1000")));

        assertThrows(BusinessRuleException.class, () -> service.settle(INVOICE_ID, ACCOUNT_ID, null));
        verify(comercialService, never()).createReceipt(anyLong(), anyLong(), anyString(), any());
    }

    private Invoice invoice(InvoiceStatus status, String total, String paid) {
        Invoice invoice = new Invoice();
        invoice.setId(INVOICE_ID);
        invoice.setInvoiceNumber("FT-2026/1");
        invoice.setStatus(status);
        invoice.setTotalAmount(new BigDecimal(total));
        invoice.setAmountPaid(new BigDecimal(paid));
        return invoice;
    }
}
