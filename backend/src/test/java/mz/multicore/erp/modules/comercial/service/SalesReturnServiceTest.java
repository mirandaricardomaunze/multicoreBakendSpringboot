package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.*;
import mz.multicore.erp.modules.comercial.model.*;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SalesReturnServiceTest {

    private InvoiceRepository invoiceRepository;
    private CreditNoteRepository creditNoteRepository;
    private CreditNoteService creditNoteService;
    private SalesReturnService salesReturnService;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        creditNoteRepository = mock(CreditNoteRepository.class);
        creditNoteService = mock(CreditNoteService.class);
        salesReturnService = new SalesReturnService(invoiceRepository, creditNoteRepository, creditNoteService);
        CurrentUserContext.setCurrentCompanyId(1L);
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    private Invoice createMockInvoiceWithLines() {
        Company comp = new Company();
        comp.setId(1L);

        Warehouse wh = new Warehouse();
        wh.setId(10L);

        Invoice inv = new Invoice();
        inv.setId(100L);
        inv.setInvoiceNumber("FT-2026/100");
        inv.setCompany(comp);
        inv.setWarehouse(wh);

        Product p1 = new Product();
        p1.setId(1L);
        p1.setSku("PRD-01");
        p1.setName("Óleo Alimentar 1L");
        p1.setUnitsPerBox(4);

        InvoiceLine l1 = new InvoiceLine();
        l1.setId(10L);
        l1.setInvoice(inv);
        l1.setProduct(p1);
        l1.setQuantity(new BigDecimal("10.00"));
        l1.setUnitPrice(new BigDecimal("100.00"));
        l1.setTaxRate(new BigDecimal("0.16"));

        Product p2 = new Product();
        p2.setId(2L);
        p2.setSku("PRD-02");
        p2.setName("Arroz Solto 1kg");
        p2.setUnitsPerBox(0); // sem caixa

        InvoiceLine l2 = new InvoiceLine();
        l2.setId(20L);
        l2.setInvoice(inv);
        l2.setProduct(p2);
        l2.setQuantity(new BigDecimal("20.00"));
        l2.setUnitPrice(new BigDecimal("50.00"));
        l2.setTaxRate(new BigDecimal("0.16"));

        inv.setLines(List.of(l1, l2));
        return inv;
    }

    @Test
    @DisplayName("Garante cálculo de linhas devolvíveis e percentagem de caixas")
    void testCalculationOfReturnableLinesAndPackaging() {
        Invoice inv = createMockInvoiceWithLines();
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(inv));

        // Linha 1 já teve 3 devolvidas -> Sobram 7 -> 7 / 4 = 1.75 cx
        when(creditNoteRepository.sumNonVoidedReturnedByInvoiceLineId(10L))
                .thenReturn(new BigDecimal("3.00"));

        // Linha 2 nunca teve devolução -> Sobram 20 -> 0.00 cx
        when(creditNoteRepository.sumNonVoidedReturnedByInvoiceLineId(20L))
                .thenReturn(BigDecimal.ZERO);

        List<ReturnableInvoiceLineDTO> lines = salesReturnService.getReturnableLines(100L);

        assertEquals(2, lines.size());

        ReturnableInvoiceLineDTO line1 = lines.get(0);
        assertEquals(10L, line1.invoiceLineId());
        assertEquals(new BigDecimal("10.00"), line1.invoicedQuantity());
        assertEquals(new BigDecimal("3.00"), line1.alreadyReturnedQuantity());
        assertEquals(new BigDecimal("7.00"), line1.returnableQuantity());
        assertEquals(new BigDecimal("1.75"), line1.boxes());

        ReturnableInvoiceLineDTO line2 = lines.get(1);
        assertEquals(20L, line2.invoiceLineId());
        assertEquals(new BigDecimal("20.00"), line2.returnableQuantity());
        assertEquals(new BigDecimal("0.00"), line2.boxes());
    }

    @Test
    @DisplayName("Garante rejeição de devolução com quantidade superior ao saldo devolvível")
    void testExcessReturnRejection() {
        Invoice inv = createMockInvoiceWithLines();
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(inv));
        when(creditNoteRepository.sumNonVoidedReturnedByInvoiceLineId(10L))
                .thenReturn(new BigDecimal("3.00")); // Sobram 7

        SalesReturnWizardRequest request = new SalesReturnWizardRequest(
                100L,
                "RETURN",
                10L,
                false,
                "Tentativa de devolver a mais",
                List.of(new SalesReturnWizardLineRequest(10L, new BigDecimal("8.00"), true)) // 8 > 7
        );

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                salesReturnService.processReturn(request));
        assertTrue(ex.getMessage().contains("excede o saldo devolvível"));
    }

    @Test
    @DisplayName("Garante criação e processamento da Nota de Crédito com reintegração FEFO")
    void testSuccessfulReturnAndCreditNoteCreation() {
        Invoice inv = createMockInvoiceWithLines();
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(inv));
        when(creditNoteRepository.sumNonVoidedReturnedByInvoiceLineId(10L)).thenReturn(BigDecimal.ZERO);

        CreditNoteDTO mockCn = new CreditNoteDTO(
                50L,
                "NC-2026/50",
                LocalDateTime.now(),
                1L,
                10L,
                "Cliente Teste",
                100L,
                "FT-2026/100",
                10L,
                "Armazém Central",
                "RETURN",
                "PENDING_APPROVAL",
                new BigDecimal("200.00"),
                new BigDecimal("32.00"),
                new BigDecimal("232.00"),
                "Devolução parcial",
                "admin",
                null,
                null,
                List.of()
        );

        when(creditNoteService.create(any())).thenReturn(mockCn);

        SalesReturnWizardRequest request = new SalesReturnWizardRequest(
                100L,
                "RETURN",
                10L,
                false,
                "Devolução aprovada pelo cliente",
                List.of(new SalesReturnWizardLineRequest(10L, new BigDecimal("2.00"), true))
        );

        SalesReturnWizardResultDTO result = salesReturnService.processReturn(request);

        assertNotNull(result);
        assertEquals(50L, result.creditNoteId());
        assertEquals("NC-2026/50", result.creditNoteNumber());
        assertEquals("PENDING_APPROVAL", result.status());
        assertEquals(1, result.restockedLinesCount());
        verify(creditNoteService, times(1)).create(any());
    }
}
