package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.financeira.model.TreasuryAccount;
import mz.multicore.erp.modules.financeira.model.TreasuryTransaction;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.financeira.repository.TreasuryTransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes do FinanceService. O módulo financeira era o único módulo de dinheiro sem
 * PermissionGuard: qualquer token de EMPLOYEE liquidava faturas e injectava entradas na
 * tesouraria. Cobre também o valor recebido, que era o total da fatura mesmo quando parte
 * já tinha sido paga. Ver docs/RECEBIMENTOS_SALDO_SPEC.md. Dependências mockadas.
 */
class FinanceServiceTest {

    private TreasuryAccountRepository accountRepository;
    private TreasuryTransactionRepository transactionRepository;
    private InvoiceRepository invoiceRepository;
    private FinanceService service;

    private static final Long COMPANY_ID = 1L;
    private static final Long ACCOUNT_ID = 20L;
    private static final Long INVOICE_ID = 7L;

    @BeforeEach
    void setUp() {
        accountRepository = mock(TreasuryAccountRepository.class);
        transactionRepository = mock(TreasuryTransactionRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);

        service = new FinanceService(accountRepository, transactionRepository, invoiceRepository);

        when(accountRepository.findByIdAndCompanyId(ACCOUNT_ID, COMPANY_ID))
                .thenReturn(Optional.of(account()));
        when(accountRepository.save(any(TreasuryAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(TreasuryTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

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
        Invoice invoice = invoice(InvoiceStatus.APPROVED, "1000", "0");
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));

        assertThrows(BusinessRuleException.class, () -> service.payInvoice(INVOICE_ID, ACCOUNT_ID));

        // Nem a fatura muda de estado nem entra dinheiro na tesouraria.
        assertEquals(InvoiceStatus.APPROVED, invoice.getStatus());
        verify(transactionRepository, never()).save(any());
    }

    @Test // RP-21
    void payInvoice_comPerfilAutorizado_liquidaERegistaEntrada() {
        Invoice invoice = invoice(InvoiceStatus.APPROVED, "1000", "0");
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));

        service.payInvoice(INVOICE_ID, ACCOUNT_ID);

        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals(0, invoice.getAmountPaid().compareTo(new BigDecimal("1000")));
        assertEquals(0, capturedAmount().compareTo(new BigDecimal("1000")));
    }

    @Test // RP-22
    void payInvoice_faturaParcialmentePaga_recebeSoOSaldoEmDivida() {
        // 1000 de total com 400 já recebidos por recibo: só faltam 600.
        Invoice invoice = invoice(InvoiceStatus.PARTIALLY_PAID, "1000", "400");
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));

        service.payInvoice(INVOICE_ID, ACCOUNT_ID);

        // Antes do fix entravam os 1000 outra vez na tesouraria — 400 contados em duplicado.
        assertEquals(0, capturedAmount().compareTo(new BigDecimal("600")),
                "só o saldo em dívida pode entrar na tesouraria");
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals(0, invoice.getAmountPaid().compareTo(new BigDecimal("1000")));
    }

    @Test // RP-23
    void payInvoice_faturaJaPaga_bloqueia() {
        Invoice invoice = invoice(InvoiceStatus.PAID, "1000", "1000");
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));

        assertThrows(BusinessRuleException.class, () -> service.payInvoice(INVOICE_ID, ACCOUNT_ID));
        verify(transactionRepository, never()).save(any());
    }

    // ────────────────────────── helpers ──────────────────────────

    private BigDecimal capturedAmount() {
        ArgumentCaptor<TreasuryTransaction> captor = ArgumentCaptor.forClass(TreasuryTransaction.class);
        verify(transactionRepository).save(captor.capture());
        return captor.getValue().getAmount();
    }

    private TreasuryAccount account() {
        TreasuryAccount account = new TreasuryAccount();
        account.setId(ACCOUNT_ID);
        account.setName("Caixa Geral");
        account.setBalance(BigDecimal.ZERO);
        account.setCompany(company());
        return account;
    }

    private Invoice invoice(InvoiceStatus status, String total, String paid) {
        Invoice invoice = new Invoice();
        invoice.setId(INVOICE_ID);
        invoice.setInvoiceNumber("FT-2026/1");
        invoice.setCompany(company());
        invoice.setStatus(status);
        invoice.setTotalAmount(new BigDecimal(total));
        invoice.setAmountPaid(new BigDecimal(paid));
        Client client = new Client();
        client.setName("Cliente Loja");
        invoice.setClient(client);
        return invoice;
    }

    private Company company() {
        Company c = new Company();
        c.setId(COMPANY_ID);
        return c;
    }
}
