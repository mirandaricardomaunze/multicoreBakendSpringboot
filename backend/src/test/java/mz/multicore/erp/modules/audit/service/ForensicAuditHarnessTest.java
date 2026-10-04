package mz.multicore.erp.modules.audit.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.dto.ForensicAnomalyDTO;
import mz.multicore.erp.modules.audit.dto.ForensicAuditSummaryDTO;
import mz.multicore.erp.modules.audit.dto.ForensicCategory;
import mz.multicore.erp.modules.audit.dto.ForensicSeverity;
import mz.multicore.erp.modules.audit.model.AuditLog;
import mz.multicore.erp.modules.audit.repository.AuditLogRepository;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.inventory.model.StockWaste;
import mz.multicore.erp.modules.inventory.model.WasteReason;
import mz.multicore.erp.modules.inventory.repository.StockWasteRepository;
import mz.multicore.erp.modules.printing.ForensicAuditPrintService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Suite de testes de conformidade do Harness de Auditoria Forense (HARNESS-AFF-001 / SPEC-AFF-001).
 * Cobre critérios AFF-01 a AFF-06.
 */
class ForensicAuditHarnessTest {

    private static final Long COMPANY_ID = 1L;

    private InvoiceRepository invoiceRepository;
    private StockWasteRepository stockWasteRepository;
    private AuditLogRepository auditLogRepository;
    private CompanyService companyService;

    private ForensicAuditService forensicAuditService;
    private ForensicAuditPrintService printService;
    private Company company;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        stockWasteRepository = mock(StockWasteRepository.class);
        auditLogRepository = mock(AuditLogRepository.class);
        companyService = mock(CompanyService.class);

        company = new Company();
        company.setId(COMPANY_ID);
        company.setName("Multicore Enterprise Lda");
        company.setTaxId("400987654");
        company.setAddress("Avenida Julius Nyerere, Maputo");
        company.setPhone("+258 84 999 8888");
        company.setEmail("auditoria@multicore.co.mz");

        when(companyService.getCompanyById(COMPANY_ID)).thenReturn(company);

        forensicAuditService = new ForensicAuditService(
                invoiceRepository,
                stockWasteRepository,
                auditLogRepository
        );

        printService = new ForensicAuditPrintService(forensicAuditService, companyService);

        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("auditor_chefe", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    // AFF-01: Faturas canceladas > 5.000 MT ou sem motivo -> CRITICAL, caso contrário -> SUSPICIOUS
    @Test
    void aff01_invoiceCancellationSeverityClassification() {
        Invoice highValueCancelled = new Invoice();
        highValueCancelled.setId(101L);
        highValueCancelled.setInvoiceNumber("FT-2026/001");
        highValueCancelled.setStatus(InvoiceStatus.CANCELLED);
        highValueCancelled.setTotalAmount(new BigDecimal("12500.00"));
        highValueCancelled.setCancellationReason("Erro de digitação");
        highValueCancelled.setUpdatedAt(LocalDateTime.of(2026, 9, 10, 14, 30));

        Invoice noReasonCancelled = new Invoice();
        noReasonCancelled.setId(102L);
        noReasonCancelled.setInvoiceNumber("FT-2026/002");
        noReasonCancelled.setStatus(InvoiceStatus.CANCELLED);
        noReasonCancelled.setTotalAmount(new BigDecimal("1500.00"));
        noReasonCancelled.setCancellationReason("   "); // sem motivo expresso
        noReasonCancelled.setUpdatedAt(LocalDateTime.of(2026, 9, 11, 10, 15));

        Invoice normalCancelled = new Invoice();
        normalCancelled.setId(103L);
        normalCancelled.setInvoiceNumber("FT-2026/003");
        normalCancelled.setStatus(InvoiceStatus.CANCELLED);
        normalCancelled.setTotalAmount(new BigDecimal("2200.00"));
        normalCancelled.setCancellationReason("Cliente desistiu da compra");
        normalCancelled.setUpdatedAt(LocalDateTime.of(2026, 9, 12, 11, 0));

        when(invoiceRepository.findByCompanyId(COMPANY_ID))
                .thenReturn(List.of(highValueCancelled, noReasonCancelled, normalCancelled));
        when(stockWasteRepository.findByCompanyIdOrderByCreatedAtDesc(COMPANY_ID))
                .thenReturn(List.of());
        when(auditLogRepository.findByCompanyIdOrderByEventTimeDesc(COMPANY_ID))
                .thenReturn(List.of());

        ForensicAuditSummaryDTO summary = forensicAuditService.getForensicSummary(null, null, null, null, null);

        assertThat(summary.anomalies()).hasSize(3);

        ForensicAnomalyDTO a1 = summary.anomalies().stream().filter(a -> a.documentOrReference().equals("FT-2026/001")).findFirst().orElseThrow();
        assertThat(a1.severity()).isEqualTo(ForensicSeverity.CRITICAL);
        assertThat(a1.category()).isEqualTo(ForensicCategory.DOC_CANCELLATION);

        ForensicAnomalyDTO a2 = summary.anomalies().stream().filter(a -> a.documentOrReference().equals("FT-2026/002")).findFirst().orElseThrow();
        assertThat(a2.severity()).isEqualTo(ForensicSeverity.CRITICAL); // sem motivo = CRITICAL

        ForensicAnomalyDTO a3 = summary.anomalies().stream().filter(a -> a.documentOrReference().equals("FT-2026/003")).findFirst().orElseThrow();
        assertThat(a3.severity()).isEqualTo(ForensicSeverity.SUSPICIOUS); // <= 5.000 com motivo = SUSPICIOUS
    }

    // AFF-02: Deteção de Descontos Excessivos (>10%)
    @Test
    void aff02_excessiveDiscountDetection() {
        Invoice invoiceWithDiscounts = new Invoice();
        invoiceWithDiscounts.setId(201L);
        invoiceWithDiscounts.setInvoiceNumber("FT-2026/050");
        invoiceWithDiscounts.setStatus(InvoiceStatus.APPROVED);
        invoiceWithDiscounts.setUpdatedAt(LocalDateTime.of(2026, 9, 14, 16, 0));

        Product p1 = new Product();
        p1.setName("Cabo de Rede Cat6");

        InvoiceLine normalLine = new InvoiceLine();
        normalLine.setProduct(p1);
        normalLine.setUnitPrice(new BigDecimal("100.00"));
        normalLine.setQuantity(new BigDecimal("10.00"));
        normalLine.setDiscountPercentage(new BigDecimal("5.00")); // <= 10%, regular

        InvoiceLine abnormalLine = new InvoiceLine();
        abnormalLine.setProduct(p1);
        abnormalLine.setUnitPrice(new BigDecimal("500.00"));
        abnormalLine.setQuantity(new BigDecimal("20.00")); // Total base = 10.000 MT
        abnormalLine.setDiscountPercentage(new BigDecimal("35.00")); // > 10% (Desconto = 3.500 MT -> CRITICAL (> 3000))

        invoiceWithDiscounts.setLines(List.of(normalLine, abnormalLine));

        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(invoiceWithDiscounts));
        when(stockWasteRepository.findByCompanyIdOrderByCreatedAtDesc(COMPANY_ID)).thenReturn(List.of());
        when(auditLogRepository.findByCompanyIdOrderByEventTimeDesc(COMPANY_ID)).thenReturn(List.of());

        ForensicAuditSummaryDTO summary = forensicAuditService.getForensicSummary(null, null, null, null, null);

        assertThat(summary.anomalies()).hasSize(1);
        ForensicAnomalyDTO discAnomaly = summary.anomalies().get(0);
        assertThat(discAnomaly.category()).isEqualTo(ForensicCategory.EXCESSIVE_DISCOUNT);
        assertThat(discAnomaly.severity()).isEqualTo(ForensicSeverity.CRITICAL);
        assertThat(discAnomaly.financialImpact()).isEqualByComparingTo("3500.00");
    }

    // AFF-03: Deteção de Quebras de Stock Anormais
    @Test
    void aff03_stockShrinkageClassification() {
        Product prod = new Product();
        prod.setName("Roteador Cisco");

        StockWaste wasteCritical = new StockWaste();
        wasteCritical.setId(301L);
        wasteCritical.setProduct(prod);
        wasteCritical.setQuantity(new BigDecimal("5.000"));
        wasteCritical.setTotalCost(new BigDecimal("15000.00")); // > 10.000 -> CRITICAL
        wasteCritical.setReason(WasteReason.THEFT_OR_SHRINKAGE);
        wasteCritical.setRegisteredBy("armazem_01");
        wasteCritical.setCreatedAt(LocalDateTime.of(2026, 9, 8, 9, 0));

        StockWaste wasteSuspicious = new StockWaste();
        wasteSuspicious.setId(302L);
        wasteSuspicious.setProduct(prod);
        wasteSuspicious.setQuantity(new BigDecimal("2.000"));
        wasteSuspicious.setTotalCost(new BigDecimal("5000.00")); // 3.000 - 10.000 -> SUSPICIOUS
        wasteSuspicious.setReason(WasteReason.DAMAGED);
        wasteSuspicious.setRegisteredBy("armazem_02");
        wasteSuspicious.setCreatedAt(LocalDateTime.of(2026, 9, 9, 10, 0));

        StockWaste wasteInfo = new StockWaste();
        wasteInfo.setId(303L);
        wasteInfo.setProduct(prod);
        wasteInfo.setQuantity(new BigDecimal("1.000"));
        wasteInfo.setTotalCost(new BigDecimal("1200.00")); // <= 3.000 -> INFO
        wasteInfo.setReason(WasteReason.EXPIRED);
        wasteInfo.setRegisteredBy("armazem_01");
        wasteInfo.setCreatedAt(LocalDateTime.of(2026, 9, 9, 11, 0));

        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of());
        when(stockWasteRepository.findByCompanyIdOrderByCreatedAtDesc(COMPANY_ID))
                .thenReturn(List.of(wasteCritical, wasteSuspicious, wasteInfo));
        when(auditLogRepository.findByCompanyIdOrderByEventTimeDesc(COMPANY_ID)).thenReturn(List.of());

        ForensicAuditSummaryDTO summary = forensicAuditService.getForensicSummary(null, null, null, null, null);

        assertThat(summary.anomalies()).hasSize(3);

        ForensicAnomalyDTO aCrit = summary.anomalies().stream().filter(a -> a.documentOrReference().equals("QB-301")).findFirst().orElseThrow();
        assertThat(aCrit.severity()).isEqualTo(ForensicSeverity.CRITICAL);

        ForensicAnomalyDTO aSusp = summary.anomalies().stream().filter(a -> a.documentOrReference().equals("QB-302")).findFirst().orElseThrow();
        assertThat(aSusp.severity()).isEqualTo(ForensicSeverity.SUSPICIOUS);

        ForensicAnomalyDTO aInfo = summary.anomalies().stream().filter(a -> a.documentOrReference().equals("QB-303")).findFirst().orElseThrow();
        assertThat(aInfo.severity()).isEqualTo(ForensicSeverity.INFO);
    }

    // AFF-04 & AFF-05: Exposição Financeira Total e Índice de Conformidade
    @Test
    void aff04_and_aff05_financialRiskSumAndComplianceScore() {
        Invoice inv = new Invoice();
        inv.setId(401L);
        inv.setInvoiceNumber("FT-2026/888");
        inv.setStatus(InvoiceStatus.CANCELLED);
        inv.setTotalAmount(new BigDecimal("8000.00")); // CRITICAL
        inv.setCancellationReason("Erro de cliente");

        Product prod = new Product();
        prod.setName("Teclado Sem Fios");
        StockWaste waste = new StockWaste();
        waste.setId(402L);
        waste.setProduct(prod);
        waste.setTotalCost(new BigDecimal("4000.00")); // SUSPICIOUS
        waste.setReason(WasteReason.DAMAGED);
        waste.setRegisteredBy("operador");

        AuditLog log = new AuditLog();
        log.setId(403L);
        log.setAction("OVERRIDE_SECURITY_PERMISSION");
        log.setUsername("admin");
        log.setEventTime(LocalDateTime.now());

        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(inv));
        when(stockWasteRepository.findByCompanyIdOrderByCreatedAtDesc(COMPANY_ID)).thenReturn(List.of(waste));
        when(auditLogRepository.findByCompanyIdOrderByEventTimeDesc(COMPANY_ID)).thenReturn(List.of(log));

        ForensicAuditSummaryDTO summary = forensicAuditService.getForensicSummary(null, null, null, null, null);

        assertThat(summary.totalAnomalies()).isEqualTo(3);
        assertThat(summary.criticalCount()).isEqualTo(1);
        assertThat(summary.suspiciousCount()).isEqualTo(1);
        assertThat(summary.infoCount()).isEqualTo(1);

        // AFF-04: Soma de CRITICAL (8000) + SUSPICIOUS (4000) = 12.000 MT (INFO não soma no risco financeiro)
        assertThat(summary.totalFinancialRisk()).isEqualByComparingTo("12000.00");

        // AFF-05: 12.000 MT fica na faixa de 10.000 a 50.000 MT -> 68%
        assertThat(summary.complianceScore()).contains("68%");
    }

    // AFF-06: Geração de PDF do Dossiê Forense
    @Test
    void aff06_generatePdfForensicDossier() {
        Invoice inv = new Invoice();
        inv.setId(501L);
        inv.setInvoiceNumber("FT-2026/999");
        inv.setStatus(InvoiceStatus.CANCELLED);
        inv.setTotalAmount(new BigDecimal("6500.00"));
        inv.setCancellationReason("Duplicidade de emissão");

        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(inv));
        when(stockWasteRepository.findByCompanyIdOrderByCreatedAtDesc(COMPANY_ID)).thenReturn(List.of());
        when(auditLogRepository.findByCompanyIdOrderByEventTimeDesc(COMPANY_ID)).thenReturn(List.of());

        byte[] pdfBytes = printService.render(COMPANY_ID, LocalDate.now().minusDays(30), LocalDate.now(), null, null, null);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(500);

        // Header %PDF-
        String header = new String(pdfBytes, 0, 8);
        assertThat(header).startsWith("%PDF-");
    }
}
