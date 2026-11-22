package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.events.CreditNoteApprovedEvent;
import mz.multicore.erp.architecture.events.DebitNoteApprovedEvent;
import mz.multicore.erp.architecture.events.InventoryValueChangedEvent;
import mz.multicore.erp.architecture.events.InvoiceCancelledEvent;
import mz.multicore.erp.architecture.events.LandedCostConfirmedEvent;
import mz.multicore.erp.architecture.events.PayslipPaidEvent;
import mz.multicore.erp.architecture.events.ProductionCompletedEvent;
import mz.multicore.erp.architecture.events.SaleRegisteredEvent;
import mz.multicore.erp.architecture.events.SupplierPaymentRegisteredEvent;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.model.AccountClass;
import mz.multicore.erp.modules.accounting.model.CostCenter;
import mz.multicore.erp.modules.accounting.model.CostCenterType;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.model.JournalSource;
import mz.multicore.erp.modules.accounting.repository.CostCenterRepository;
import mz.multicore.erp.modules.accounting.repository.JournalEntryRepository;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Harness CC-05 (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.2): automatic postings put their class 6/7 lines in the cost
 * centre of the source document's warehouse (sales, POS, credit/debit notes, wastes, adjustments, production, landed
 * costs, supplier payments); events without a warehouse use the default centre; a reversal keeps the original centres.
 */
class AutomaticPostingCostCenterTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long STORE_ID = 5L;
    private static final Long DEPOT_ID = 6L;
    private static final LocalDate DAY = LocalDate.of(2026, 11, 22);

    private final JournalEntryRepository journalEntryRepository = mock(JournalEntryRepository.class);
    private final CompanyService companyService = mock(CompanyService.class);
    private final DocumentNumberService documentNumberService = mock(DocumentNumberService.class);
    private final CostCenterService costCenterService = mock(CostCenterService.class);
    private final JournalService journalService = new JournalService(journalEntryRepository,
            mock(ChartOfAccountsService.class), documentNumberService, companyService, mock(AuditLogService.class),
            costCenterService);
    private final PostingHelper helper = mock(PostingHelper.class);

    private final CostCenter adm = center(1L, null, true);
    private final CostCenter store = center(50L, STORE_ID, true);
    private final CostCenter depot = center(60L, DEPOT_ID, true);

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
        when(companyService.getCurrentCompanyReference(COMPANY_ID)).thenReturn(company());
        when(documentNumberService.next(any())).thenReturn("LC-0001");
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(costCenterService.requireDefault(COMPANY_ID)).thenReturn(adm);
        when(costCenterService.postingCenterOf(COMPANY_ID, null)).thenReturn(adm);
        when(costCenterService.postingCenterOf(COMPANY_ID, STORE_ID)).thenReturn(store);
        when(costCenterService.postingCenterOf(COMPANY_ID, DEPOT_ID)).thenReturn(depot);

        when(helper.canPost(COMPANY_ID)).thenReturn(true);
        when(helper.hasPayrollAccounts(COMPANY_ID)).thenReturn(true);
        when(helper.hasAccounts(eq(COMPANY_ID), any(String[].class))).thenReturn(true);
        when(helper.account(eq(COMPANY_ID), anyString())).thenAnswer(inv -> account(inv.getArgument(1)));
        when(helper.cashAccount(eq(COMPANY_ID), anyBoolean())).thenAnswer(inv -> account(PgcNirfChart.CAIXA));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void cc05_vendaLancaProveitoECustoNoCentroDoArmazem() {
        commercial().postSale(sale(STORE_ID));

        JournalEntry saved = savedEntry();
        assertSame(store, line(saved, PgcNirfChart.VENDAS).getCostCenter());
        assertSame(store, line(saved, PgcNirfChart.CMVMC).getCostCenter());
        assertNull(line(saved, PgcNirfChart.CAIXA).getCostCenter());
        assertNull(line(saved, PgcNirfChart.MERCADORIAS).getCostCenter());
    }

    @Test
    void cc05_vendaSemArmazemUsaOCentroPorOmissao() {
        commercial().postSale(sale(null));

        assertSame(adm, line(savedEntry(), PgcNirfChart.VENDAS).getCostCenter());
    }

    @Test
    void cc05_anulacaoDaVendaFicaNosCentrosDaVendaOriginal() {
        JournalEntry original = new JournalEntry();
        JournalLine revenue = JournalLine.credit(account(PgcNirfChart.VENDAS), new BigDecimal("100.00"), "Vendas");
        revenue.setCostCenter(store);
        original.addLine(JournalLine.debit(account(PgcNirfChart.CAIXA), new BigDecimal("100.00"), "Recebido"));
        original.addLine(revenue);
        when(journalEntryRepository.findByCompanyIdAndSourceAndSourceDocumentId(COMPANY_ID, JournalSource.INVOICE, 9L))
                .thenReturn(Optional.of(original));

        commercial().postInvoiceCancellation(new InvoiceCancelledEvent(COMPANY_ID, 9L, "FT-9", DAY));

        JournalEntry saved = savedEntry();
        assertSame(store, line(saved, PgcNirfChart.VENDAS).getCostCenter());
        assertNull(line(saved, PgcNirfChart.CAIXA).getCostCenter());
        verify(costCenterService, never()).requireDefault(any());
    }

    @Test
    void cc05_anulacaoComCentroOriginalInactivoCaiNoPorOmissao() {
        CostCenter closed = center(70L, DEPOT_ID, false);
        JournalEntry original = new JournalEntry();
        JournalLine revenue = JournalLine.credit(account(PgcNirfChart.VENDAS), new BigDecimal("100.00"), "Vendas");
        revenue.setCostCenter(closed);
        original.addLine(JournalLine.debit(account(PgcNirfChart.CAIXA), new BigDecimal("100.00"), "Recebido"));
        original.addLine(revenue);
        when(journalEntryRepository.findByCompanyIdAndSourceAndSourceDocumentId(COMPANY_ID, JournalSource.INVOICE, 9L))
                .thenReturn(Optional.of(original));

        commercial().postInvoiceCancellation(new InvoiceCancelledEvent(COMPANY_ID, 9L, "FT-9", DAY));

        assertSame(adm, line(savedEntry(), PgcNirfChart.VENDAS).getCostCenter());
    }

    @Test
    void cc05_notaDeCreditoFicaNoCentroDoArmazemDaNota() {
        commercial().postCreditNote(new CreditNoteApprovedEvent(COMPANY_ID, 11L, "NC-1", DAY,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"), new BigDecimal("60.00"), STORE_ID));

        JournalEntry saved = savedEntry();
        assertSame(store, line(saved, PgcNirfChart.VENDAS).getCostCenter());
        assertSame(store, line(saved, PgcNirfChart.CMVMC).getCostCenter());
    }

    @Test
    void cc05_notaDeDebitoFicaNoCentroDoArmazemDaFatura() {
        commercial().postDebitNote(new DebitNoteApprovedEvent(COMPANY_ID, 12L, "ND-1", DAY,
                new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("50.00"), DEPOT_ID));

        assertSame(depot, line(savedEntry(), PgcNirfChart.OUTROS_PROVEITOS_OPERACIONAIS).getCostCenter());
    }

    @Test
    void cc05_quebraEAjusteDeStockFicamNoCentroDoArmazem() {
        GoodsAccounts goods = new GoodsAccounts(helper, null);
        new InventoryPostingService(helper, journalService, goods).onInventoryValueChanged(new InventoryValueChangedEvent(
                COMPANY_ID, JournalSource.STOCK_WASTE, 21L, "Quebra", DAY, new BigDecimal("30.00"), BigDecimal.ZERO,
                null, null, DEPOT_ID));

        JournalEntry saved = savedEntry();
        assertSame(depot, line(saved, PgcNirfChart.CMVMC).getCostCenter());
        assertNull(line(saved, PgcNirfChart.MERCADORIAS).getCostCenter());
    }

    @Test
    void cc05_producaoConcluidaFicaNoCentroDoArmazemDeDestino() {
        operations().postProduction(new ProductionCompletedEvent(COMPANY_ID, 31L, "OP-1", DAY,
                new BigDecimal("400.00"), STORE_ID));

        assertSame(store, line(savedEntry(), PgcNirfChart.VARIACAO_PRODUCAO).getCostCenter());
    }

    @Test
    void cc05_diferencaCambialDoPagamentoFicaNoCentroDoArmazemDaCompra() {
        new ProcurementPostingHandler(helper, journalService).postSupplierPayment(new SupplierPaymentRegisteredEvent(
                COMPANY_ID, 41L, "CP-1", DAY, new BigDecimal("110.00"), true, new BigDecimal("100.00"), DEPOT_ID));

        assertSame(depot, line(savedEntry(), PgcNirfChart.EXCHANGE_LOSS).getCostCenter());
    }

    @Test
    void cc05_custosDeImportacaoRepartemAParteVendidaPeloArmazemDeCadaCompra() {
        when(helper.alreadyPosted(any(), any(), any())).thenReturn(false);
        new LandedCostPostingService(helper, journalService).onLandedCostConfirmed(new LandedCostConfirmedEvent(
                COMPANY_ID, 51L, "CI-1", DAY, new BigDecimal("100.00"), new BigDecimal("100.00"), BigDecimal.ZERO,
                List.of(new LandedCostConfirmedEvent.Payable("CI-1-1", "Despachante", new BigDecimal("200.00"))),
                List.of(new LandedCostConfirmedEvent.WarehouseShare(STORE_ID, new BigDecimal("60.00")),
                        new LandedCostConfirmedEvent.WarehouseShare(DEPOT_ID, new BigDecimal("40.00")))));

        List<JournalLine> sold = savedEntry().getLines().stream()
                .filter(l -> PgcNirfChart.CMVMC.equals(l.getAccount().getCode())).toList();
        assertEquals(2, sold.size());
        assertEquals(new BigDecimal("60.00"), sold.get(0).getDebit());
        assertSame(store, sold.get(0).getCostCenter());
        assertEquals(new BigDecimal("40.00"), sold.get(1).getDebit());
        assertSame(depot, sold.get(1).getCostCenter());
    }

    @Test
    void cc05_custosDeImportacaoSemReparticaoUsamOPorOmissao() {
        new LandedCostPostingService(helper, journalService).onLandedCostConfirmed(new LandedCostConfirmedEvent(
                COMPANY_ID, 52L, "CI-2", DAY, BigDecimal.ZERO, new BigDecimal("80.00"), BigDecimal.ZERO,
                List.of(new LandedCostConfirmedEvent.Payable("CI-2-1", "Despachante", new BigDecimal("80.00")))));

        assertSame(adm, line(savedEntry(), PgcNirfChart.CMVMC).getCostCenter());
    }

    @Test
    void cc05_salariosSemArmazemUsamOCentroPorOmissao() {
        operations().postPayslip(new PayslipPaidEvent(COMPANY_ID, 61L, "RV-1", "Maria", DAY,
                new BigDecimal("1000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("1000.00")));

        assertSame(adm, line(savedEntry(), PgcNirfChart.CUSTOS_PESSOAL).getCostCenter());
        verify(costCenterService, never()).postingCenterOf(any(), any());
    }

    @Test
    void cc05_centroDoArmazemActivoDaEmpresaEUsado() {
        CostCenterRepository repository = mock(CostCenterRepository.class);
        CostCenterService service = new CostCenterService(repository, companyService);
        when(repository.findByWarehouseId(STORE_ID)).thenReturn(Optional.of(store));

        assertSame(store, service.postingCenterOf(COMPANY_ID, STORE_ID));
        verify(repository, never()).findFirstByCompanyIdAndDefaultCenterTrue(any());
    }

    @Test
    void cc05_armazemSemCentroActivoOuDeOutraEmpresaCaiNoPorOmissao() {
        CostCenterRepository repository = mock(CostCenterRepository.class);
        CostCenterService service = new CostCenterService(repository, companyService);
        CostCenter foreign = center(80L, 8L, true);
        foreign.setCompany(otherCompany());
        when(repository.findFirstByCompanyIdAndDefaultCenterTrue(COMPANY_ID)).thenReturn(Optional.of(adm));
        when(repository.findByWarehouseId(DEPOT_ID)).thenReturn(Optional.of(center(70L, DEPOT_ID, false)));
        when(repository.findByWarehouseId(8L)).thenReturn(Optional.of(foreign));
        when(repository.findByWarehouseId(9L)).thenReturn(Optional.empty());

        assertSame(adm, service.postingCenterOf(COMPANY_ID, DEPOT_ID), "centro do armazém inactivo");
        assertSame(adm, service.postingCenterOf(COMPANY_ID, 8L), "centro de outra empresa");
        assertSame(adm, service.postingCenterOf(COMPANY_ID, 9L), "armazém sem centro");
        assertSame(adm, service.postingCenterOf(COMPANY_ID, null), "evento sem armazém");
    }

    private CommercialPostingHandler commercial() {
        return new CommercialPostingHandler(helper, journalService, new GoodsAccounts(helper, null));
    }

    private OperationsPostingHandler operations() {
        return new OperationsPostingHandler(helper, journalService, mock(AuditLogService.class));
    }

    private static SaleRegisteredEvent sale(Long warehouseId) {
        return new SaleRegisteredEvent(COMPANY_ID, 9L, "FT-9", DAY, new BigDecimal("100.00"), BigDecimal.ZERO,
                new BigDecimal("100.00"), new BigDecimal("60.00"), new BigDecimal("100.00"), true,
                new BigDecimal("100.00"), warehouseId);
    }

    private JournalEntry savedEntry() {
        ArgumentCaptor<JournalEntry> captor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryRepository).save(captor.capture());
        return captor.getValue();
    }

    private static JournalLine line(JournalEntry entry, String accountCode) {
        return entry.getLines().stream()
                .filter(l -> accountCode.equals(l.getAccount().getCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("sem partida na conta " + accountCode));
    }

    private static Account account(String code) {
        Account account = new Account();
        account.setCode(code);
        account.setName("Conta " + code);
        account.setAccountClass(AccountClass.ofCode(code));
        account.setNature(AccountClass.ofCode(code).defaultNature());
        account.setPostable(true);
        account.setActive(true);
        return account;
    }

    private static CostCenter center(Long id, Long warehouseId, boolean active) {
        CostCenter center = new CostCenter();
        center.setId(id);
        center.setCompany(company());
        center.setCode("C" + id);
        center.setName("Centro " + id);
        center.setType(warehouseId == null ? CostCenterType.MANUAL : CostCenterType.WAREHOUSE);
        center.setWarehouseId(warehouseId);
        center.setActive(active);
        return center;
    }

    private static Company company() {
        Company company = new Company();
        company.setId(COMPANY_ID);
        return company;
    }

    private static Company otherCompany() {
        Company company = new Company();
        company.setId(2L);
        return company;
    }
}
