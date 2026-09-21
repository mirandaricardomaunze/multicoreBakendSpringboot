package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.CustomerStatementDTO;
import mz.multicore.erp.modules.comercial.dto.CustomerStatementLineDTO;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.CreditNote;
import mz.multicore.erp.modules.comercial.model.CreditNoteReason;
import mz.multicore.erp.modules.comercial.model.DebitNote;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.model.NoteStatus;
import mz.multicore.erp.modules.comercial.model.Receipt;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.DebitNoteRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.ReceiptRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.printing.CustomerStatementPrintService;
import mz.multicore.erp.modules.printing.SupplierStatementPrintService;
import mz.multicore.erp.modules.purchases.dto.SupplierStatementDTO;
import mz.multicore.erp.modules.purchases.dto.SupplierStatementLineDTO;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import mz.multicore.erp.modules.purchases.service.SupplierStatementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Harness automatizado da Fase 2: Extrato de Conta Corrente de Clientes & Fornecedores.
 * Verifica critérios canónicos ECC-01 a ECC-05 definidos em docs/EXTRATO_CONTA_CORRENTE_HARNESS.md.
 */
@ExtendWith(MockitoExtension.class)
class AccountStatementHarnessTest {

    private static final Long TEST_COMPANY_ID = 1L;
    private static final Long TEST_CLIENT_ID = 100L;
    private static final Long TEST_SUPPLIER_ID = 200L;

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private ReceiptRepository receiptRepository;
    @Mock
    private CreditNoteRepository creditNoteRepository;
    @Mock
    private DebitNoteRepository debitNoteRepository;

    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private CompanyService companyService;

    @InjectMocks
    private CustomerStatementService customerStatementService;

    @InjectMocks
    private SupplierStatementService supplierStatementService;

    private CustomerStatementPrintService customerPrintService;
    private SupplierStatementPrintService supplierPrintService;

    private Company testCompany;
    private Client testClient;
    private Supplier testSupplier;

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(TEST_COMPANY_ID);

        testCompany = new Company();
        testCompany.setId(TEST_COMPANY_ID);
        testCompany.setName("Multicore Enterprise Lda");
        testCompany.setTaxId("400123456");
        testCompany.setAddress("Av. 24 de Julho, Maputo");

        testClient = new Client();
        testClient.setId(TEST_CLIENT_ID);
        testClient.setName("Cliente Comercial Teste Lda");
        testClient.setTaxId("400987654");
        testClient.setEmail("financeiro@cliente.mz");
        testClient.setAddress("Bairro Central, Maputo");

        testSupplier = new Supplier();
        testSupplier.setId(TEST_SUPPLIER_ID);
        testSupplier.setName("Fornecedor Industrial SA");
        testSupplier.setTaxId("400555666");
        testSupplier.setEmail("vendas@fornecedor.mz");
        testSupplier.setPhone("+258 84 000 1122");
        testSupplier.setAddress("Zona Industrial, Matola");

        customerPrintService = new CustomerStatementPrintService(customerStatementService, companyService);
        supplierPrintService = new SupplierStatementPrintService(supplierStatementService, companyService);

        lenient().when(companyService.getCompanyById(TEST_COMPANY_ID)).thenReturn(testCompany);
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("ECC-01: Contratos de DTO são records puros e imutáveis")
    void testStatementDtoStructureAndImmutability() {
        assertTrue(CustomerStatementDTO.class.isRecord());
        assertTrue(CustomerStatementLineDTO.class.isRecord());
        assertTrue(SupplierStatementDTO.class.isRecord());
        assertTrue(SupplierStatementLineDTO.class.isRecord());
    }

    @Test
    @DisplayName("ECC-02: Cálculo progressivo de saldo de cliente: FT -> RC -> NC -> ND")
    void testCustomerRunningBalanceCalculation() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        when(clientRepository.findById(TEST_CLIENT_ID)).thenReturn(Optional.of(testClient));

        // FT 10.000 MT em 02/01
        Invoice inv = new Invoice();
        inv.setId(1L);
        inv.setInvoiceNumber("FT-2026/001");
        inv.setClient(testClient);
        inv.setTotalAmount(new BigDecimal("10000.00"));
        inv.setCreatedAt(LocalDateTime.of(2026, 1, 2, 10, 0));
        inv.setStatus(InvoiceStatus.APPROVED);
        when(invoiceRepository.findByCompanyId(TEST_COMPANY_ID)).thenReturn(List.of(inv));

        // RC 4.000 MT em 05/01
        Receipt rc = new Receipt();
        rc.setId(1L);
        rc.setReceiptNumber("RC-2026/001");
        rc.setInvoice(inv);
        rc.setAmountPaid(new BigDecimal("4000.00"));
        rc.setReceiptDate(LocalDateTime.of(2026, 1, 5, 14, 0));
        rc.setStatus("COMPLETED");
        rc.setPaymentMethod("BANK_TRANSFER");
        when(receiptRepository.findByCompanyId(TEST_COMPANY_ID)).thenReturn(List.of(rc));

        // NC 1.000 MT em 10/01
        CreditNote cn = new CreditNote();
        cn.setId(1L);
        cn.setNoteNumber("NC-2026/001");
        cn.setClient(testClient);
        cn.setInvoice(inv);
        cn.setTotalAmount(new BigDecimal("1000.00"));
        cn.setIssueDate(LocalDateTime.of(2026, 1, 10, 11, 0));
        cn.setStatus(NoteStatus.APPROVED);
        cn.setReason(CreditNoteReason.RETURN);
        when(creditNoteRepository.findByCompanyId(TEST_COMPANY_ID)).thenReturn(List.of(cn));

        // ND 500 MT em 15/01
        DebitNote dn = new DebitNote();
        dn.setId(1L);
        dn.setNoteNumber("ND-2026/001");
        dn.setClient(testClient);
        dn.setInvoice(inv);
        dn.setTotalAmount(new BigDecimal("500.00"));
        dn.setIssueDate(LocalDateTime.of(2026, 1, 15, 9, 0));
        dn.setStatus(NoteStatus.APPROVED);
        when(debitNoteRepository.findByCompanyId(TEST_COMPANY_ID)).thenReturn(List.of(dn));

        CustomerStatementDTO stmt = customerStatementService.getStatement(TEST_CLIENT_ID, start, end);

        assertNotNull(stmt);
        assertEquals(BigDecimal.ZERO, stmt.openingBalance());
        assertEquals(4, stmt.lines().size());

        // 1. FT: +10.000 -> saldo 10.000
        CustomerStatementLineDTO l1 = stmt.lines().get(0);
        assertEquals("FT", l1.documentType());
        assertEquals(new BigDecimal("10000.00"), l1.debit());
        assertEquals(new BigDecimal("10000.00"), l1.runningBalance());

        // 2. RC: -4.000 -> saldo 6.000
        CustomerStatementLineDTO l2 = stmt.lines().get(1);
        assertEquals("RC", l2.documentType());
        assertEquals(new BigDecimal("4000.00"), l2.credit());
        assertEquals(new BigDecimal("6000.00"), l2.runningBalance());

        // 3. NC: -1.000 -> saldo 5.000
        CustomerStatementLineDTO l3 = stmt.lines().get(2);
        assertEquals("NC", l3.documentType());
        assertEquals(new BigDecimal("1000.00"), l3.credit());
        assertEquals(new BigDecimal("5000.00"), l3.runningBalance());

        // 4. ND: +500 -> saldo 5.500
        CustomerStatementLineDTO l4 = stmt.lines().get(3);
        assertEquals("ND", l4.documentType());
        assertEquals(new BigDecimal("500.00"), l4.debit());
        assertEquals(new BigDecimal("5500.00"), l4.runningBalance());

        assertEquals(new BigDecimal("5500.00"), stmt.closingBalance());
    }

    @Test
    @DisplayName("ECC-03: Consolidação do saldo anterior (openingBalance) fora do período")
    void testOpeningBalanceConsolidation() {
        LocalDate start = LocalDate.of(2026, 2, 1);
        LocalDate end = LocalDate.of(2026, 2, 28);

        when(clientRepository.findById(TEST_CLIENT_ID)).thenReturn(Optional.of(testClient));

        // FT em 15/01 (anterior a fevereiro)
        Invoice oldInv = new Invoice();
        oldInv.setId(10L);
        oldInv.setInvoiceNumber("FT-2026/000");
        oldInv.setClient(testClient);
        oldInv.setTotalAmount(new BigDecimal("8000.00"));
        oldInv.setCreatedAt(LocalDateTime.of(2026, 1, 15, 10, 0));
        oldInv.setStatus(InvoiceStatus.APPROVED);

        // FT em 10/02 (dentro de fevereiro)
        Invoice newInv = new Invoice();
        newInv.setId(11L);
        newInv.setInvoiceNumber("FT-2026/005");
        newInv.setClient(testClient);
        newInv.setTotalAmount(new BigDecimal("2000.00"));
        newInv.setCreatedAt(LocalDateTime.of(2026, 2, 10, 10, 0));
        newInv.setStatus(InvoiceStatus.APPROVED);

        when(invoiceRepository.findByCompanyId(TEST_COMPANY_ID)).thenReturn(List.of(oldInv, newInv));
        when(receiptRepository.findByCompanyId(TEST_COMPANY_ID)).thenReturn(Collections.emptyList());
        when(creditNoteRepository.findByCompanyId(TEST_COMPANY_ID)).thenReturn(Collections.emptyList());
        when(debitNoteRepository.findByCompanyId(TEST_COMPANY_ID)).thenReturn(Collections.emptyList());

        CustomerStatementDTO stmt = customerStatementService.getStatement(TEST_CLIENT_ID, start, end);

        assertEquals(new BigDecimal("8000.00"), stmt.openingBalance(), "Saldo anterior deve consolidar faturas anteriores");
        assertEquals(1, stmt.lines().size(), "Apenas a fatura de fevereiro deve figurar nas linhas");
        assertEquals(new BigDecimal("10000.00"), stmt.closingBalance());
    }

    @Test
    @DisplayName("ECC-04: Saldo progressivo de fornecedores com compra e amortização")
    void testSupplierRunningBalanceCalculation() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        when(supplierRepository.findById(TEST_SUPPLIER_ID)).thenReturn(Optional.of(testSupplier));

        Purchase p = new Purchase();
        p.setId(1L);
        p.setPurchaseNumber("V/FT-2026/1");
        p.setSupplier(testSupplier);
        p.setCompany(testCompany);
        p.setPurchaseDate(LocalDateTime.of(2026, 1, 5, 9, 0));
        p.setTotalAmount(new BigDecimal("20000.00"));
        p.setAmountPaid(new BigDecimal("15000.00"));
        p.setStatus("COMPLETED");

        when(purchaseRepository.findByCompanyId(TEST_COMPANY_ID)).thenReturn(List.of(p));

        SupplierStatementDTO stmt = supplierStatementService.getStatement(TEST_SUPPLIER_ID, start, end);

        assertNotNull(stmt);
        assertEquals(2, stmt.lines().size());

        // Linha 1: Compra (+20.000 crédito a pagar)
        SupplierStatementLineDTO l1 = stmt.lines().get(0);
        assertEquals("V/FT", l1.documentType());
        assertEquals(new BigDecimal("20000.00"), l1.credit());
        assertEquals(new BigDecimal("20000.00"), l1.runningBalance());

        // Linha 2: Pagamento (-15.000 débito)
        SupplierStatementLineDTO l2 = stmt.lines().get(1);
        assertEquals("PG", l2.documentType());
        assertEquals(new BigDecimal("15000.00"), l2.debit());
        assertEquals(new BigDecimal("5000.00"), l2.runningBalance());

        assertEquals(new BigDecimal("5000.00"), stmt.closingBalance(), "Dívida residual ao fornecedor deve ser 5.000 MT");
    }

    @Test
    @DisplayName("ECC-05: Emissão de PDF canónico de extrato de clientes e fornecedores")
    void testCustomerStatementPdfGeneration() {
        when(clientRepository.findById(TEST_CLIENT_ID)).thenReturn(Optional.of(testClient));
        when(invoiceRepository.findByCompanyId(TEST_COMPANY_ID)).thenReturn(Collections.emptyList());
        when(receiptRepository.findByCompanyId(TEST_COMPANY_ID)).thenReturn(Collections.emptyList());
        when(creditNoteRepository.findByCompanyId(TEST_COMPANY_ID)).thenReturn(Collections.emptyList());
        when(debitNoteRepository.findByCompanyId(TEST_COMPANY_ID)).thenReturn(Collections.emptyList());

        byte[] pdfBytes = customerPrintService.render(TEST_COMPANY_ID, TEST_CLIENT_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF deve conter conteúdo gerado");

        String header = new String(pdfBytes, 0, Math.min(10, pdfBytes.length), StandardCharsets.US_ASCII);
        assertTrue(header.startsWith("%PDF"), "Ficheiro gerado deve ser um documento PDF válido");
    }
}
