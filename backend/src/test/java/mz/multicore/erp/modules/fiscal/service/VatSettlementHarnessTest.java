package mz.multicore.erp.modules.fiscal.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.fiscal.dto.IvaSummaryDTO;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Harness canónico do apuramento de IVA de Moçambique (SPEC-AIVA-001 / HARNESS-AIVA-001).
 */
class VatSettlementHarnessTest {

    private static final Long COMPANY_ID = 1L;

    private InvoiceRepository invoiceRepository;
    private PurchaseRepository purchaseRepository;
    private FiscalSummaryService fiscalSummaryService;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        purchaseRepository = mock(PurchaseRepository.class);
        fiscalSummaryService = new FiscalSummaryService(invoiceRepository, purchaseRepository);
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void testSettlementPayableWithoutPreviousCredit() {
        // Output tax: 16,000 MT, Input tax: 6,000 MT -> net: 10,000 MT (A_PAGAR)
        Invoice inv = createInvoice("FT-001", new BigDecimal("100000.00"), new BigDecimal("16000.00"), new BigDecimal("116000.00"), InvoiceStatus.APPROVED);
        Purchase pur = createPurchase("PC-001", new BigDecimal("37500.00"), new BigDecimal("6000.00"), new BigDecimal("43500.00"), "APPROVED");

        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(inv));
        when(purchaseRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(pur));

        IvaSummaryDTO result = fiscalSummaryService.computeMonth(COMPANY_ID, 2026, 6, BigDecimal.ZERO);

        assertEquals(0, new BigDecimal("16000.00").compareTo(result.outputTax()));
        assertEquals(0, new BigDecimal("6000.00").compareTo(result.inputTax()));
        assertEquals(0, new BigDecimal("10000.00").compareTo(result.payableAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.creditToCarry()));
        assertEquals("A_PAGAR", result.fiscalStatus());
    }

    @Test
    void testSettlementCreditToCarryWithoutPreviousCredit() {
        // Output tax: 5,000 MT, Input tax: 12,000 MT -> credit to carry: 7,000 MT (CREDITO_A_TRANSPORTAR)
        Invoice inv = createInvoice("FT-002", new BigDecimal("31250.00"), new BigDecimal("5000.00"), new BigDecimal("36250.00"), InvoiceStatus.PAID);
        Purchase pur = createPurchase("PC-002", new BigDecimal("75000.00"), new BigDecimal("12000.00"), new BigDecimal("87000.00"), "CONFIRMED");

        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(inv));
        when(purchaseRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(pur));

        IvaSummaryDTO result = fiscalSummaryService.computeMonth(COMPANY_ID, 2026, 6, BigDecimal.ZERO);

        assertEquals(0, new BigDecimal("5000.00").compareTo(result.outputTax()));
        assertEquals(0, new BigDecimal("12000.00").compareTo(result.inputTax()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.payableAmount()));
        assertEquals(0, new BigDecimal("7000.00").compareTo(result.creditToCarry()));
        assertEquals("CREDITO_A_TRANSPORTAR", result.fiscalStatus());
    }

    @Test
    void testSettlementPayableAbsorbedByPreviousCredit() {
        // Output: 10,000, Input: 2,000 -> raw diff: 8,000. Previous credit: 15,000 -> credit to carry: 7,000 MT
        Invoice inv = createInvoice("FT-003", new BigDecimal("62500.00"), new BigDecimal("10000.00"), new BigDecimal("72500.00"), InvoiceStatus.APPROVED);
        Purchase pur = createPurchase("PC-003", new BigDecimal("12500.00"), new BigDecimal("2000.00"), new BigDecimal("14500.00"), "APPROVED");

        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(inv));
        when(purchaseRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(pur));

        IvaSummaryDTO result = fiscalSummaryService.computeMonth(COMPANY_ID, 2026, 6, new BigDecimal("15000.00"));

        assertEquals(0, new BigDecimal("15000.00").compareTo(result.previousCredit()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.payableAmount()));
        assertEquals(0, new BigDecimal("7000.00").compareTo(result.creditToCarry()));
        assertEquals("CREDITO_A_TRANSPORTAR", result.fiscalStatus());
    }

    @Test
    void testSettlementPayablePartiallyAbsorbedByPreviousCredit() {
        // Output: 20,000, Input: 5,000 -> raw diff: 15,000. Previous credit: 6,000 -> payable: 9,000 MT
        Invoice inv = createInvoice("FT-004", new BigDecimal("125000.00"), new BigDecimal("20000.00"), new BigDecimal("145000.00"), InvoiceStatus.APPROVED);
        Purchase pur = createPurchase("PC-004", new BigDecimal("31250.00"), new BigDecimal("5000.00"), new BigDecimal("36250.00"), "APPROVED");

        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(inv));
        when(purchaseRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(pur));

        IvaSummaryDTO result = fiscalSummaryService.computeMonth(COMPANY_ID, 2026, 6, new BigDecimal("6000.00"));

        assertEquals(0, new BigDecimal("6000.00").compareTo(result.previousCredit()));
        assertEquals(0, new BigDecimal("9000.00").compareTo(result.payableAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.creditToCarry()));
        assertEquals("A_PAGAR", result.fiscalStatus());
    }

    @Test
    void testIgnoresCancelledInvoicesAndPurchases() {
        Invoice cancelledInv = createInvoice("FT-CAN", new BigDecimal("50000.00"), new BigDecimal("8000.00"), new BigDecimal("58000.00"), InvoiceStatus.CANCELLED);
        Purchase cancelledPur = createPurchase("PC-CAN", new BigDecimal("20000.00"), new BigDecimal("3200.00"), new BigDecimal("23200.00"), "CANCELLED");

        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(cancelledInv));
        when(purchaseRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(cancelledPur));

        IvaSummaryDTO result = fiscalSummaryService.computeMonth(COMPANY_ID, 2026, 6, BigDecimal.ZERO);

        assertEquals(0, BigDecimal.ZERO.compareTo(result.outputTax()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.inputTax()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.payableAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.creditToCarry()));
    }

    private Invoice createInvoice(String num, BigDecimal base, BigDecimal tax, BigDecimal total, InvoiceStatus status) {
        Invoice inv = new Invoice();
        inv.setInvoiceNumber(num);
        inv.setTotalBeforeTax(base);
        inv.setTaxAmount(tax);
        inv.setTotalAmount(total);
        inv.setStatus(status);
        inv.setCreatedAt(LocalDateTime.of(2026, 6, 15, 10, 0));
        Client client = new Client();
        client.setName("Cliente Teste");
        inv.setClient(client);
        return inv;
    }

    private Purchase createPurchase(String num, BigDecimal base, BigDecimal tax, BigDecimal total, String status) {
        Purchase pur = new Purchase();
        pur.setPurchaseNumber(num);
        pur.setTaxAmount(tax);
        pur.setTotalAmount(total);
        pur.setStatus(status);
        pur.setPurchaseDate(LocalDateTime.of(2026, 6, 16, 11, 0));
        Supplier sup = new Supplier();
        sup.setName("Fornecedor Teste");
        pur.setSupplier(sup);
        return pur;
    }
}
