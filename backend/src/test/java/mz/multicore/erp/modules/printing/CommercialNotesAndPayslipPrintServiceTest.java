package mz.multicore.erp.modules.printing;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.*;
import mz.multicore.erp.modules.comercial.service.CreditNoteService;
import mz.multicore.erp.modules.comercial.service.DebitNoteService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO;
import mz.multicore.erp.modules.documents.model.DocumentType;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.hr.service.HRService;
import mz.multicore.erp.modules.hr.service.PayrollDeductionService;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.ProductBatchRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommercialNotesAndPayslipPrintServiceTest {

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(1L);
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void creditNote_geraQuatroViasNominais() throws Exception {
        CreditNoteService service = mock(CreditNoteService.class);
        DocumentConfigService configService = mock(DocumentConfigService.class);
        LineRowMapper lineRowMapper = new LineRowMapper(mock(ProductBatchRepository.class));

        CreditNote note = sampleCreditNote();
        when(service.loadForPrint(100L)).thenReturn(note);
        when(configService.getColumns(eq(1L), any(DocumentType.class))).thenReturn(DocumentColumnsDTO.all());

        CreditNotePrintService printService = new CreditNotePrintService(service, lineRowMapper, configService);
        byte[] pdf = printService.render(100L);

        PdfReader reader = new PdfReader(pdf);
        try {
            assertEquals(4, reader.getNumberOfPages(), "A nota de crédito deve ter 4 vias nominais (4 páginas)");
        } finally {
            reader.close();
        }

        String text = extract(pdf);
        assertTrue(text.contains("NC 2026/001"), "Deve conter o número da nota: " + text);
        assertTrue(text.contains("1.ª VIA — ORIGINAL (Cliente)"), "Deve conter 1.ª Via Original: " + text);
        assertTrue(text.contains("2.ª VIA — DUPLICADO (Contabilidade / Fisco)"), "Deve conter 2.ª Via Duplicado: " + text);
        assertTrue(text.contains("3.ª VIA — TRIPLICADO (Arquivo / Cobrança)"), "Deve conter 3.ª Via Triplicado: " + text);
        assertTrue(text.contains("4.ª VIA — QUADRUPLICADO (Arquivo Geral)"), "Deve conter 4.ª Via Quadruplicado: " + text);
    }

    @Test
    void debitNote_geraQuatroViasNominais() throws Exception {
        DebitNoteService service = mock(DebitNoteService.class);
        DebitNote note = sampleDebitNote();
        when(service.loadForPrint(200L)).thenReturn(note);

        DebitNotePrintService printService = new DebitNotePrintService(service);
        byte[] pdf = printService.render(200L);

        PdfReader reader = new PdfReader(pdf);
        try {
            assertEquals(4, reader.getNumberOfPages(), "A nota de débito deve ter 4 vias nominais (4 páginas)");
        } finally {
            reader.close();
        }

        String text = extract(pdf);
        assertTrue(text.contains("ND 2026/001"), "Deve conter o número da nota: " + text);
        assertTrue(text.contains("1.ª VIA — ORIGINAL (Cliente)"), "Deve conter 1.ª Via Original: " + text);
        assertTrue(text.contains("2.ª VIA — DUPLICADO (Contabilidade / Fisco)"), "Deve conter 2.ª Via Duplicado: " + text);
        assertTrue(text.contains("3.ª VIA — TRIPLICADO (Arquivo / Cobrança)"), "Deve conter 3.ª Via Triplicado: " + text);
        assertTrue(text.contains("4.ª VIA — QUADRUPLICADO (Arquivo Geral)"), "Deve conter 4.ª Via Quadruplicado: " + text);
    }

    @Test
    void payslip_geraDuasViasNominais() throws Exception {
        HRService hrService = mock(HRService.class);
        PayrollDeductionService deductionService = mock(PayrollDeductionService.class);

        Payslip payslip = samplePayslip();
        when(hrService.loadPayslipForPrint(300L)).thenReturn(payslip);
        when(deductionService.linesOf(300L)).thenReturn(List.of());

        PayslipPrintService printService = new PayslipPrintService(hrService, deductionService);
        byte[] pdf = printService.render(300L);

        PdfReader reader = new PdfReader(pdf);
        try {
            assertEquals(2, reader.getNumberOfPages(), "O recibo de salário deve ter 2 vias nominais (2 páginas)");
        } finally {
            reader.close();
        }

        String text = extract(pdf);
        assertTrue(text.contains("REC-SAL-2026/01"), "Deve conter o número do recibo: " + text);
        assertTrue(text.contains("1.ª VIA — ORIGINAL (Trabalhador)"), "Deve conter 1.ª Via Trabalhador: " + text);
        assertTrue(text.contains("2.ª VIA — DUPLICADO (Recursos Humanos / Arquivo)"), "Deve conter 2.ª Via Arquivo RH: " + text);
    }

    private static Company sampleCompany() {
        Company company = new Company();
        company.setId(1L);
        company.setName("Empresa Moçambique Lda");
        company.setTaxId("400123456");
        company.setAddress("Av. Julius Nyerere, Maputo");
        return company;
    }

    private static CreditNote sampleCreditNote() {
        Company company = sampleCompany();
        Client client = new Client();
        client.setId(10L);
        client.setName("Cliente Alfa Lda");
        client.setTaxId("400888777");

        Invoice invoice = new Invoice();
        invoice.setId(50L);
        invoice.setInvoiceNumber("FT 2026/100");

        Warehouse warehouse = new Warehouse();
        warehouse.setId(5L);
        warehouse.setName("Armazém Central");

        CreditNote note = new CreditNote();
        note.setId(100L);
        note.setNoteNumber("NC 2026/001");
        note.setCompany(company);
        note.setClient(client);
        note.setInvoice(invoice);
        note.setWarehouse(warehouse);
        note.setReason(CreditNoteReason.RETURN);
        note.setStatus(NoteStatus.APPROVED);
        note.setIssueDate(LocalDateTime.of(2026, 10, 4, 12, 0));
        note.setTotalBeforeTax(new BigDecimal("500.00"));
        note.setTaxAmount(new BigDecimal("80.00"));
        note.setTotalAmount(new BigDecimal("580.00"));

        Product product = new Product();
        product.setId(20L);
        product.setName("Artigo Devolvido");
        product.setReference("ART-01");

        CreditNoteLine line = new CreditNoteLine();
        line.setProduct(product);
        line.setQuantity(new BigDecimal("2"));
        line.setUnitPrice(new BigDecimal("250.00"));
        line.setTaxRate(new BigDecimal("0.16"));
        line.setLineTotal(new BigDecimal("500.00"));
        note.setLines(List.of(line));

        return note;
    }

    private static DebitNote sampleDebitNote() {
        Company company = sampleCompany();
        Client client = new Client();
        client.setId(10L);
        client.setName("Cliente Alfa Lda");
        client.setTaxId("400888777");

        Invoice invoice = new Invoice();
        invoice.setId(50L);
        invoice.setInvoiceNumber("FT 2026/100");

        DebitNote note = new DebitNote();
        note.setId(200L);
        note.setNoteNumber("ND 2026/001");
        note.setCompany(company);
        note.setClient(client);
        note.setInvoice(invoice);
        note.setReason(DebitNoteReason.FREIGHT);
        note.setStatus(NoteStatus.APPROVED);
        note.setIssueDate(LocalDateTime.of(2026, 10, 4, 12, 30));
        note.setTotalBeforeTax(new BigDecimal("200.00"));
        note.setTaxAmount(new BigDecimal("32.00"));
        note.setTotalAmount(new BigDecimal("232.00"));

        DebitNoteLine line = new DebitNoteLine();
        line.setDescription("Taxa de frete adicional");
        line.setAmount(new BigDecimal("200.00"));
        line.setTaxRate(new BigDecimal("0.16"));
        line.setLineTotal(new BigDecimal("200.00"));
        note.setLines(List.of(line));

        return note;
    }

    private static Payslip samplePayslip() {
        Company company = sampleCompany();

        Employee employee = new Employee();
        employee.setId(15L);
        employee.setName("João Mondlane");
        employee.setCompany(company);
        employee.setDepartment("Operações");

        Payslip payslip = new Payslip();
        payslip.setId(300L);
        payslip.setPayslipNumber("REC-SAL-2026/01");
        payslip.setEmployee(employee);
        payslip.setYear(2026);
        payslip.setMonth(9);
        payslip.setStatus("PAID");
        payslip.setBaseSalary(new BigDecimal("30000.00"));
        payslip.setAllowances(new BigDecimal("5000.00"));
        payslip.setOvertime(BigDecimal.ZERO);
        payslip.setSalesBonus(BigDecimal.ZERO);
        payslip.setIrpsDeduction(new BigDecimal("4200.00"));
        payslip.setInssDeduction(new BigDecimal("1050.00"));
        payslip.setEmployerInss(new BigDecimal("1400.00"));
        payslip.setAbsenceDeduction(BigDecimal.ZERO);
        payslip.setOtherDeductions(BigDecimal.ZERO);
        payslip.setNetPay(new BigDecimal("29750.00"));
        return payslip;
    }

    private static String extract(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            StringBuilder text = new StringBuilder();
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page)).append('\n');
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }
}
