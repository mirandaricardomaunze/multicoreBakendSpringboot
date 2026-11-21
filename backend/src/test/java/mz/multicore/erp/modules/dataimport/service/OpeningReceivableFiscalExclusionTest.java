package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.repository.ApprovalRequestRepository;
import mz.multicore.erp.modules.approvals.service.ApprovalRequestLookup;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceDocumentKind;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.DebitNoteRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.comercial.service.InvoiceNoteLookup;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.fiscal.dto.FiscalSalesExportDTO;
import mz.multicore.erp.modules.fiscal.dto.IvaSummaryDTO;
import mz.multicore.erp.modules.fiscal.service.FiscalSalesExportService;
import mz.multicore.erp.modules.fiscal.service.FiscalSummaryService;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.EmploymentContractRepository;
import mz.multicore.erp.modules.hr.repository.PayslipRepository;
import mz.multicore.erp.modules.hr.service.EmployeeLookup;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.service.StockLookup;
import mz.multicore.erp.modules.performance.dto.SalesGoalProgressDTO;
import mz.multicore.erp.modules.performance.model.BonusType;
import mz.multicore.erp.modules.performance.model.GoalPeriod;
import mz.multicore.erp.modules.performance.model.GoalScope;
import mz.multicore.erp.modules.performance.model.GoalStatus;
import mz.multicore.erp.modules.performance.model.SalesGoal;
import mz.multicore.erp.modules.performance.service.BonusCalculatorEngine;
import mz.multicore.erp.modules.performance.service.GoalProgressEngine;
import mz.multicore.erp.modules.pos.repository.MobilePaymentRepository;
import mz.multicore.erp.modules.pos.repository.PaymentEntryRepository;
import mz.multicore.erp.modules.pos.repository.StoreVoucherRepository;
import mz.multicore.erp.modules.pos.repository.TillMovementRepository;
import mz.multicore.erp.modules.pos.repository.TillSessionRepository;
import mz.multicore.erp.modules.pos.service.PosPaymentLookup;
import mz.multicore.erp.modules.pos.service.TillSessionLookup;
import mz.multicore.erp.modules.purchases.repository.PurchaseOrderRepository;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierReturnRepository;
import mz.multicore.erp.modules.purchases.service.SupplierLookup;
import mz.multicore.erp.modules.reports.dto.DailyStoreReportDTO;
import mz.multicore.erp.modules.reports.dto.StoreDashboardDTO;
import mz.multicore.erp.modules.reports.service.ReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * IMPORTACAO_DADOS_HARNESS — IMP-SI-03: o "Saldo Inicial" importado não é documento fiscal. Fica fora do ficheiro
 * SAF-T de vendas, do apuramento/declaração de IVA e dos relatórios de vendas e receita (painel da loja, relatório
 * diário, metas comerciais), mesmo emitido e no mesmo período que uma venda; a dívida continua por cobrar
 * (SPEC §5.2).
 */
class OpeningReceivableFiscalExclusionTest {

    private static final long COMPANY_ID = 7L;

    private final InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
    private final InvoiceLookup invoiceLookup = new InvoiceLookup(invoiceRepository);
    private final CompanyRepository companyRepository = mock(CompanyRepository.class);

    @BeforeEach
    void activeCompany() {
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        Company company = new Company();
        company.setId(COMPANY_ID);
        company.setName("Loja Central");
        company.setTaxId("400123456");
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    private static Invoice sale(String number, LocalDateTime createdAt, String net, String tax, String gross) {
        Invoice invoice = document(number, createdAt, net, tax, gross);
        invoice.setDocumentKind(InvoiceDocumentKind.SALE);
        return invoice;
    }

    private static Invoice openingBalance(String number, LocalDateTime createdAt, String amount) {
        Invoice invoice = document(number, createdAt, amount, "0", amount);
        invoice.setDocumentKind(InvoiceDocumentKind.OPENING_BALANCE);
        invoice.setOriginalNumber("FT 2025/118");
        invoice.setDocumentDate(LocalDate.of(2025, 10, 15));
        return invoice;
    }

    private static Invoice document(String number, LocalDateTime createdAt, String net, String tax, String gross) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(number);
        invoice.setStatus(InvoiceStatus.APPROVED);
        invoice.setCreatedAt(createdAt);
        invoice.setTotalBeforeTax(new BigDecimal(net));
        invoice.setTaxAmount(new BigDecimal(tax));
        invoice.setTotalAmount(new BigDecimal(gross));
        Client client = new Client();
        client.setId(31L);
        client.setName("Acme Lda");
        client.setTaxId("400111222");
        invoice.setClient(client);
        return invoice;
    }

    /** Responde às consultas como a base responderia, sem fixar qual o serviço usa. */
    private void stubInvoices(List<Invoice> all) {
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(all);
        when(invoiceRepository.findByCompanyIdAndCreatedAtBetween(eq(COMPANY_ID), any(), any()))
                .thenAnswer(call -> {
                    LocalDateTime from = call.getArgument(1);
                    LocalDateTime to = call.getArgument(2);
                    return all.stream()
                            .filter(i -> !i.getCreatedAt().isBefore(from) && !i.getCreatedAt().isAfter(to))
                            .toList();
                });
        when(invoiceRepository.findByCompanyIdAndStatusIn(eq(COMPANY_ID), anyCollection()))
                .thenAnswer(call -> {
                    Collection<InvoiceStatus> statuses = call.getArgument(1);
                    return all.stream().filter(i -> statuses.contains(i.getStatus())).toList();
                });
    }

    @Test
    void onlyOrdinaryInvoicesAreSales() {
        LocalDateTime now = LocalDateTime.now();
        Invoice sale = sale("FT-1", now, "100", "16", "116");
        Invoice opening = openingBalance("SI-2026/1", now, "500");

        assertThat(sale.isSale()).isTrue();
        assertThat(sale.countsAsSale()).isTrue();
        assertThat(opening.isSale()).isFalse();
        assertThat(opening.countsAsSale()).isFalse();
        assertThat(opening.getStatus().isCollectable()).isTrue();
    }

    @Test
    void openingBalanceIsNotInTheSaftSalesFile() {
        LocalDateTime june = LocalDateTime.of(2026, 6, 10, 9, 0);
        stubInvoices(List.of(sale("FT-001", june, "100.00", "16.00", "116.00"),
                openingBalance("SI-2026/1", june, "500.00")));
        FiscalSalesExportService saft = new FiscalSalesExportService(invoiceLookup, new CompanyLookup(companyRepository));

        FiscalSalesExportDTO export = saft.exportSales(COMPANY_ID, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(export.numberOfInvoices()).isEqualTo(1);
        assertThat(export.xml()).contains("<InvoiceNo>FT-001</InvoiceNo>").doesNotContain("SI-2026/1");
        assertThat(export.totalGross()).isEqualByComparingTo("116.00");
        assertThat(export.totalNet()).isEqualByComparingTo("100.00");
    }

    @Test
    void openingBalanceIsNotInTheVatSettlement() {
        LocalDateTime june = LocalDateTime.of(2026, 6, 15, 10, 0);
        stubInvoices(List.of(sale("FT-001", june, "100.00", "16.00", "116.00"),
                openingBalance("SI-2026/1", june, "500.00")));
        PurchaseRepository purchases = mock(PurchaseRepository.class);
        when(purchases.findByCompanyId(COMPANY_ID)).thenReturn(List.of());
        FiscalSummaryService vat = new FiscalSummaryService(invoiceLookup,
                new InvoiceNoteLookup(mock(CreditNoteRepository.class), mock(DebitNoteRepository.class)),
                new SupplierLookup(mock(SupplierRepository.class), purchases,
                        mock(PurchaseOrderRepository.class), mock(SupplierReturnRepository.class)));

        IvaSummaryDTO summary = vat.computeMonth(COMPANY_ID, 2026, 6);

        assertThat(summary.sales()).extracting(IvaSummaryDTO.IvaLineDTO::documentNumber).containsExactly("FT-001");
        assertThat(summary.salesBase()).isEqualByComparingTo("100.00");
        assertThat(summary.outputTax()).isEqualByComparingTo("16.00");
    }

    @Test
    void openingBalanceIsNotInTheStoreSalesReportsButStaysToCollect() {
        LocalDateTime now = LocalDateTime.now();
        stubInvoices(List.of(sale("FT-001", now, "100", "16", "116"),
                openingBalance("SI-2026/1", now, "500")));
        StockRepository stock = mock(StockRepository.class);
        when(stock.findByWarehouseCompanyId(COMPANY_ID)).thenReturn(List.of());
        ApprovalRequestRepository approvals = mock(ApprovalRequestRepository.class);
        when(approvals.findByCompanyIdAndStatus(eq(COMPANY_ID), any())).thenReturn(List.of());
        TillSessionRepository tillSessions = mock(TillSessionRepository.class);
        when(tillSessions.findByCompanyId(COMPANY_ID)).thenReturn(List.of());
        PaymentEntryRepository payments = mock(PaymentEntryRepository.class);
        when(payments.findByInvoiceCompanyIdAndPaidAtBetween(eq(COMPANY_ID), any(), any())).thenReturn(List.of());
        TillMovementRepository tillMovements = mock(TillMovementRepository.class);
        when(tillMovements.findByTillSessionCompanyIdAndMovementDateBetween(eq(COMPANY_ID), any(), any()))
                .thenReturn(List.of());
        ReportService reports = new ReportService(invoiceLookup, new StockLookup(stock),
                new ApprovalRequestLookup(approvals), new TillSessionLookup(tillSessions, tillMovements),
                new PosPaymentLookup(payments, mock(StoreVoucherRepository.class), mock(MobilePaymentRepository.class)));

        StoreDashboardDTO dashboard = reports.buildStoreDashboard(COMPANY_ID);
        DailyStoreReportDTO daily = reports.buildDailyStoreReport(COMPANY_ID, LocalDate.now());

        assertThat(dashboard.salesToday().count()).isEqualTo(1);
        assertThat(dashboard.salesToday().totalAmount()).isEqualByComparingTo("116");
        assertThat(daily.sales().count()).isEqualTo(1);
        assertThat(daily.sales().totalAmount()).isEqualByComparingTo("116");
        assertThat(dashboard.unpaidInvoicesAmount()).isEqualByComparingTo("616");
    }

    @Test
    void openingBalanceDoesNotCountTowardsSalesGoals() {
        LocalDateTime october = LocalDateTime.of(2026, 10, 10, 9, 0);
        Invoice sale = sale("FT-001", october, "1000", "0", "1000");
        InvoiceLine line = new InvoiceLine();
        line.setLineTotal(new BigDecimal("1000"));
        line.setQuantity(BigDecimal.ONE);
        line.setUnitCost(new BigDecimal("600"));
        sale.addLine(line);
        stubInvoices(List.of(sale, openingBalance("SI-2026/1", october, "50000")));
        GoalProgressEngine engine = new GoalProgressEngine(invoiceLookup,
                new EmployeeLookup(mock(EmployeeRepository.class), mock(PayslipRepository.class),
                        mock(EmploymentContractRepository.class)),
                new BonusCalculatorEngine());

        SalesGoalProgressDTO progress = engine.calculateProgress(monthlyGoal(), LocalDate.of(2026, 10, 15));

        assertThat(progress.currentRevenue()).isEqualByComparingTo("1000");
    }

    private static SalesGoal monthlyGoal() {
        Company company = new Company();
        company.setId(COMPANY_ID);
        SalesGoal goal = new SalesGoal();
        goal.setId(10L);
        goal.setCompany(company);
        goal.setName("Meta de Outubro");
        goal.setPeriod(GoalPeriod.MONTHLY);
        goal.setPeriodStart(LocalDate.of(2026, 10, 1));
        goal.setPeriodEnd(LocalDate.of(2026, 10, 31));
        goal.setScope(GoalScope.COMPANY);
        goal.setTargetRevenue(new BigDecimal("100000"));
        goal.setBonusType(BonusType.FIXED);
        goal.setBonusValue(new BigDecimal("1000"));
        goal.setStatus(GoalStatus.ACTIVE);
        return goal;
    }
}
