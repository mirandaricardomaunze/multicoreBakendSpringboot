package com.phcpro.modules.comercial.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.comercial.dto.CreateCreditNoteLineRequest;
import com.phcpro.modules.comercial.dto.CreateCreditNoteRequest;
import com.phcpro.modules.comercial.dto.CreditNoteDTO;
import com.phcpro.modules.comercial.model.Client;
import com.phcpro.modules.comercial.model.CreditNote;
import com.phcpro.modules.comercial.model.CreditNoteLine;
import com.phcpro.modules.comercial.model.CreditNoteReason;
import com.phcpro.modules.comercial.model.Invoice;
import com.phcpro.modules.comercial.model.InvoiceLine;
import com.phcpro.modules.comercial.model.NoteStatus;
import com.phcpro.modules.comercial.model.Product;
import com.phcpro.modules.comercial.repository.CreditNoteRepository;
import com.phcpro.modules.comercial.repository.InvoiceRepository;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.repository.WarehouseRepository;
import com.phcpro.modules.inventory.service.InventoryService;
import com.phcpro.modules.numbering.service.DocumentNumberService;
import com.phcpro.modules.numbering.service.DocumentSeries;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes do ciclo da nota de crédito. Foco: o stock só volta ao armazém quando uma
 * NC com motivo RETURN é APROVADA; só MANAGER/ADMIN aprovam; a quantidade devolvida
 * não pode exceder o vendido menos o já devolvido; e o valor da NC não pode exceder
 * o da fatura. Repositórios e inventário mockados — não levanta o Spring.
 */
class CreditNoteServiceTest {

    private CreditNoteRepository creditNoteRepository;
    private InvoiceRepository invoiceRepository;
    private WarehouseRepository warehouseRepository;
    private InventoryService inventoryService;
    private DocumentNumberService documentNumberService;
    private AuditLogService auditLogService;
    private CreditNoteService service;

    private Company company;
    private Client client;
    private Warehouse warehouse;
    private Product product;

    @BeforeEach
    void setUp() {
        creditNoteRepository = mock(CreditNoteRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        warehouseRepository = mock(WarehouseRepository.class);
        inventoryService = mock(InventoryService.class);
        documentNumberService = mock(DocumentNumberService.class);
        auditLogService = mock(AuditLogService.class);

        service = new CreditNoteService(creditNoteRepository, invoiceRepository, warehouseRepository,
                inventoryService, documentNumberService, auditLogService);

        company = company(1L);
        client = client(5L, "Cliente Fiado");
        warehouse = warehouse(20L, "Loja");
        product = product(100L, "SKU-1", "Arroz 1kg");

        CurrentUserContext.setCurrentCompanyId(1L);
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    // ────────────────────────── create ──────────────────────────

    @Test
    void create_devolucaoValida_ficaPendente_eNumeraNaSerieNC() {
        Invoice invoice = invoiceWithLine(10L, new BigDecimal("5"), new BigDecimal("100"), new BigDecimal("0.16"));
        invoice.setTotalAmount(new BigDecimal("580.00"));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(warehouse));
        when(creditNoteRepository.sumNonVoidedReturnedByInvoiceLineId(10L)).thenReturn(BigDecimal.ZERO);
        when(documentNumberService.next(DocumentSeries.CREDIT_NOTE)).thenReturn("NC-2026/1");
        when(creditNoteRepository.save(any(CreditNote.class))).thenAnswer(inv -> inv.getArgument(0));

        CreditNoteDTO dto = service.create(request("RETURN", 20L, line(10L, new BigDecimal("2"))));

        assertEquals(NoteStatus.PENDING_APPROVAL.name(), dto.status());
        assertEquals("NC-2026/1", dto.noteNumber());
        assertEquals(0, new BigDecimal("232.00").compareTo(dto.totalAmount())); // 2 × 100 × 1.16
        // Criar uma NC NÃO mexe em stock — só a aprovação o faz.
        verify(inventoryService, never()).registerMovement(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void create_quantidadeExcedeRestante_lancaExcecao() {
        Invoice invoice = invoiceWithLine(10L, new BigDecimal("5"), new BigDecimal("100"), BigDecimal.ZERO);
        invoice.setTotalAmount(new BigDecimal("500.00"));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(warehouse));
        // Já foram devolvidas 4 das 5 — só resta 1, mas pedem 2.
        when(creditNoteRepository.sumNonVoidedReturnedByInvoiceLineId(10L)).thenReturn(new BigDecimal("4"));

        assertThrows(BusinessRuleException.class,
                () -> service.create(request("RETURN", 20L, line(10L, new BigDecimal("2")))));
        verify(creditNoteRepository, never()).save(any());
    }

    @Test
    void create_valorExcedeFatura_lancaExcecao() {
        Invoice invoice = invoiceWithLine(10L, new BigDecimal("5"), new BigDecimal("100"), BigDecimal.ZERO);
        invoice.setTotalAmount(new BigDecimal("1.00")); // fatura "barata" face às linhas
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(warehouse));
        when(creditNoteRepository.sumNonVoidedReturnedByInvoiceLineId(10L)).thenReturn(BigDecimal.ZERO);
        when(documentNumberService.next(DocumentSeries.CREDIT_NOTE)).thenReturn("NC-2026/1");

        assertThrows(BusinessRuleException.class,
                () -> service.create(request("RETURN", 20L, line(10L, new BigDecimal("5")))));
        verify(creditNoteRepository, never()).save(any());
    }

    @Test
    void create_motivoInvalido_lancaExcecao() {
        assertThrows(BusinessRuleException.class,
                () -> service.create(request("INEXISTENTE", 20L, line(10L, BigDecimal.ONE))));
    }

    // ────────────────────────── approve ──────────────────────────

    @Test
    void approve_devolucaoRETURN_repoeStock_eFicaAprovada() {
        CreditNote note = pendingReturnNote(new BigDecimal("2"), "LOTE-A");
        when(creditNoteRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(note));
        when(creditNoteRepository.save(any(CreditNote.class))).thenAnswer(inv -> inv.getArgument(0));

        CreditNoteDTO dto = service.approve(1L);

        assertEquals(NoteStatus.APPROVED.name(), dto.status());
        // Entrada positiva no armazém, tipo RETURN, com o lote da linha de origem.
        verify(inventoryService).registerMovement(
                eq(product), eq(warehouse), eq(new BigDecimal("2")), eq("RETURN"),
                eq("LOTE-A"), eq(null), any());
    }

    @Test
    void approve_motivoNaoRETURN_naoMexeEmStock() {
        CreditNote note = pendingReturnNote(new BigDecimal("2"), null);
        note.setReason(CreditNoteReason.DISCOUNT);
        note.setWarehouse(null);
        when(creditNoteRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(note));
        when(creditNoteRepository.save(any(CreditNote.class))).thenAnswer(inv -> inv.getArgument(0));

        CreditNoteDTO dto = service.approve(1L);

        assertEquals(NoteStatus.APPROVED.name(), dto.status());
        verify(inventoryService, never()).registerMovement(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void approve_semPerfilAutorizado_lancaExcecao_eNaoMexeEmStock() {
        CurrentUserContext.setCurrentUser("caixa", "EMPLOYEE");

        assertThrows(BusinessRuleException.class, () -> service.approve(1L));
        verify(inventoryService, never()).registerMovement(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void approve_notaJaAprovada_lancaExcecao() {
        CreditNote note = pendingReturnNote(new BigDecimal("2"), "LOTE-A");
        note.setStatus(NoteStatus.APPROVED);
        when(creditNoteRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(note));

        assertThrows(BusinessRuleException.class, () -> service.approve(1L));
        verify(inventoryService, never()).registerMovement(any(), any(), any(), any(), any(), any(), any());
    }

    // ────────────────────────── helpers ──────────────────────────

    private CreateCreditNoteRequest request(String reason, Long warehouseId, CreateCreditNoteLineRequest... lines) {
        return new CreateCreditNoteRequest(1L, reason, warehouseId, "Devolução de teste", List.of(lines));
    }

    private CreateCreditNoteLineRequest line(Long invoiceLineId, BigDecimal qty) {
        return new CreateCreditNoteLineRequest(invoiceLineId, qty);
    }

    private Invoice invoiceWithLine(Long lineId, BigDecimal qty, BigDecimal price, BigDecimal taxRate) {
        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setInvoiceNumber("FT-2026/1");
        invoice.setCompany(company);
        invoice.setClient(client);
        invoice.setWarehouse(warehouse);
        InvoiceLine il = new InvoiceLine();
        il.setId(lineId);
        il.setProduct(product);
        il.setQuantity(qty);
        il.setUnitPrice(price);
        il.setTaxRate(taxRate);
        invoice.addLine(il);
        return invoice;
    }

    private CreditNote pendingReturnNote(BigDecimal qty, String batch) {
        CreditNote note = new CreditNote();
        note.setId(1L);
        note.setNoteNumber("NC-2026/1");
        note.setCompany(company);
        note.setClient(client);
        note.setReason(CreditNoteReason.RETURN);
        note.setWarehouse(warehouse);
        note.setStatus(NoteStatus.PENDING_APPROVAL);

        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setInvoiceNumber("FT-2026/1");
        note.setInvoice(invoice);

        CreditNoteLine line = new CreditNoteLine();
        line.setProduct(product);
        line.setQuantity(qty);
        line.setUnitPrice(new BigDecimal("100"));
        line.setTaxRate(BigDecimal.ZERO);
        line.setLineTotal(new BigDecimal("200"));
        line.setBatchNumber(batch);
        note.addLine(line);
        return note;
    }

    private static Company company(long id) {
        Company c = new Company();
        c.setId(id);
        return c;
    }

    private static Client client(long id, String name) {
        Client c = new Client();
        c.setId(id);
        c.setName(name);
        return c;
    }

    private static Warehouse warehouse(long id, String name) {
        Warehouse w = new Warehouse();
        w.setId(id);
        w.setName(name);
        return w;
    }

    private static Product product(long id, String sku, String name) {
        Product p = new Product();
        p.setId(id);
        p.setSku(sku);
        p.setName(name);
        p.setUnitPrice(new BigDecimal("100"));
        return p;
    }
}
