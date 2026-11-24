package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.events.JournalEntryPostedEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.accounting.dto.BudgetAlertDTO;
import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.model.AccountClass;
import mz.multicore.erp.modules.accounting.model.Budget;
import mz.multicore.erp.modules.accounting.model.BudgetAlert;
import mz.multicore.erp.modules.accounting.model.BudgetLine;
import mz.multicore.erp.modules.accounting.model.CostCenter;
import mz.multicore.erp.modules.accounting.model.CostCenterType;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.repository.BudgetAlertRepository;
import mz.multicore.erp.modules.accounting.repository.BudgetRepository;
import mz.multicore.erp.modules.accounting.repository.JournalEntryRepository;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Harness OR-05 (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.3): when the actual costs (class 6) of a cost centre in a
 * month reach 90% and 100% of the budgeted costs of that centre and month, one alert is raised per threshold, centre
 * and month — never repeated. Posting a journal entry with class 6 lines triggers the evaluation of its centres; the
 * alerts are listed for ADMIN and MANAGER of the active company (the bell).
 */
class BudgetAlertTest {

    private static final Long COMPANY_ID = 1L;
    private static final int YEAR = 2027;
    private static final int MONTH = 3;

    private final BudgetRepository budgetRepository = mock(BudgetRepository.class);
    private final BudgetAlertRepository alertRepository = mock(BudgetAlertRepository.class);
    private final JournalService journalService = mock(JournalService.class);
    private final BudgetAlertService service = new BudgetAlertService(budgetRepository, alertRepository,
            new BudgetActualsCalculator(journalService));
    private final List<BudgetAlert> stored = new ArrayList<>();

    private final Company company = company();
    private final Account rent = account("6221", "Rendas");
    private final Account fuel = account("6242", "Combustíveis");
    private final Account sales = account("7111", "Vendas de mercadorias");
    private final Account bank = account("1201", "Depósitos à ordem");
    private final CostCenter adm = center(40L, "ADM");
    private final CostCenter shop = center(41L, "LOJA");

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
        when(budgetRepository.findByCompanyIdAndFiscalYear(COMPANY_ID, YEAR)).thenReturn(Optional.empty());
        when(alertRepository.existsByCompanyIdAndCostCenterIdAndFiscalYearAndMonthAndThresholdPercent(
                anyLong(), anyLong(), anyInt(), anyInt(), anyInt())).thenAnswer(invocation -> stored.stream()
                .anyMatch(alert -> alert.getCostCenter().getId().equals(invocation.getArgument(1))
                        && alert.getFiscalYear() == (int) invocation.getArgument(2)
                        && alert.getMonth() == (int) invocation.getArgument(3)
                        && alert.getThresholdPercent() == (int) invocation.getArgument(4)));
        when(alertRepository.save(any(BudgetAlert.class))).thenAnswer(invocation -> {
            BudgetAlert alert = invocation.getArgument(0);
            alert.setId((long) stored.size() + 1);
            stored.add(alert);
            return alert;
        });
        actuals();
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void or05_abaixoDe90PorCentoNaoAlerta() {
        budget(cell(rent, adm, MONTH, "1000"));
        actuals(entry(LocalDate.of(YEAR, MONTH, 5), debit(rent, adm, "899.99"), credit(bank, null, "899.99")));

        assertThat(service.evaluate(COMPANY_ID, adm.getId(), YEAR, MONTH)).isEmpty();
        verify(alertRepository, never()).save(any());
    }

    @Test
    void or05_aos90PorCentoGeraAlertaDe90() {
        budget(cell(rent, adm, MONTH, "600"), cell(fuel, adm, MONTH, "400"));
        actuals(entry(LocalDate.of(YEAR, MONTH, 5), debit(rent, adm, "700"), debit(fuel, adm, "200"),
                credit(bank, null, "900")));

        List<BudgetAlertDTO> raised = service.evaluate(COMPANY_ID, adm.getId(), YEAR, MONTH);

        assertEquals(1, raised.size());
        BudgetAlertDTO alert = raised.get(0);
        assertEquals(90, alert.thresholdPercent());
        assertEquals(adm.getId(), alert.costCenterId());
        assertEquals("ADM", alert.costCenterCode());
        assertEquals(YEAR, alert.year());
        assertEquals(MONTH, alert.month());
        assertThat(alert.budgeted()).isEqualByComparingTo("1000");
        assertThat(alert.actual()).isEqualByComparingTo("900");
        assertThat(alert.message()).contains("ADM").contains("90%");
        assertEquals(company, stored.get(0).getCompany());
    }

    @Test
    void or05_aos100PorCentoGeraOsDoisLimiaresQueAindaFaltam() {
        budget(cell(rent, adm, MONTH, "1000"));
        actuals(entry(LocalDate.of(YEAR, MONTH, 5), debit(rent, adm, "1200"), credit(bank, null, "1200")));

        List<BudgetAlertDTO> raised = service.evaluate(COMPANY_ID, adm.getId(), YEAR, MONTH);

        assertThat(raised).extracting(BudgetAlertDTO::thresholdPercent).containsExactly(90, 100);
    }

    @Test
    void or05_naoRepeteOAlertaDoMesmoLimiarCentroEMes() {
        budget(cell(rent, adm, MONTH, "1000"));
        actuals(entry(LocalDate.of(YEAR, MONTH, 5), debit(rent, adm, "950"), credit(bank, null, "950")));
        assertThat(service.evaluate(COMPANY_ID, adm.getId(), YEAR, MONTH))
                .extracting(BudgetAlertDTO::thresholdPercent).containsExactly(90);

        assertThat(service.evaluate(COMPANY_ID, adm.getId(), YEAR, MONTH)).isEmpty();

        actuals(entry(LocalDate.of(YEAR, MONTH, 5), debit(rent, adm, "1100"), credit(bank, null, "1100")));
        assertThat(service.evaluate(COMPANY_ID, adm.getId(), YEAR, MONTH))
                .extracting(BudgetAlertDTO::thresholdPercent).containsExactly(100);
        assertEquals(2, stored.size());
    }

    @Test
    void or05_soContamCustosDaClasse6DoCentroEDoMes() {
        budget(cell(rent, adm, MONTH, "1000"), cell(sales, adm, MONTH, "5000"),
                cell(rent, shop, MONTH, "10"), cell(rent, adm, MONTH + 1, "10"));
        actuals(
                entry(LocalDate.of(YEAR, MONTH, 5), debit(bank, null, "4000"), credit(sales, adm, "4000")),
                entry(LocalDate.of(YEAR, MONTH, 6), debit(rent, shop, "800"), credit(bank, null, "800")),
                entry(LocalDate.of(YEAR, MONTH + 1, 1), debit(rent, adm, "800"), credit(bank, null, "800")),
                entry(LocalDate.of(YEAR, MONTH, 7), debit(rent, adm, "500"), credit(bank, null, "500")));

        assertThat(service.evaluate(COMPANY_ID, adm.getId(), YEAR, MONTH)).isEmpty();
    }

    @Test
    void or05_semOrcadoDeCustosNoMesNaoHaAlerta() {
        budget(cell(sales, adm, MONTH, "1000"), cell(rent, adm, MONTH, "0"));
        actuals(entry(LocalDate.of(YEAR, MONTH, 5), debit(rent, adm, "300"), credit(bank, null, "300")));

        assertThat(service.evaluate(COMPANY_ID, adm.getId(), YEAR, MONTH)).isEmpty();
        verify(alertRepository, never()).save(any());
    }

    @Test
    void or05_lancamentoComCustosPublicaOsCentrosDasPartidas6() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        JournalService posting = postingService(publisher);

        posting.save(entry(LocalDate.of(YEAR, MONTH, 9), debit(rent, adm, "100"), debit(fuel, shop, "50"),
                credit(sales, adm, "150")), COMPANY_ID);

        ArgumentCaptor<JournalEntryPostedEvent> event = ArgumentCaptor.forClass(JournalEntryPostedEvent.class);
        verify(publisher).publishEvent(event.capture());
        assertEquals(COMPANY_ID, event.getValue().companyId());
        assertEquals(LocalDate.of(YEAR, MONTH, 9), event.getValue().entryDate());
        assertEquals(Set.of(adm.getId(), shop.getId()), event.getValue().costCenterIds());
    }

    @Test
    void or05_lancamentoSemCustosNaoPublica() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        JournalService posting = postingService(publisher);

        posting.save(entry(LocalDate.of(YEAR, MONTH, 9), debit(bank, null, "150"), credit(sales, adm, "150")),
                COMPANY_ID);

        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void or05_eventoDoLancamentoAvaliaCadaCentroNoMesDoLancamento() {
        budget(cell(rent, adm, MONTH, "100"), cell(fuel, shop, MONTH, "1000"));
        actuals(entry(LocalDate.of(YEAR, MONTH, 9), debit(rent, adm, "100"), debit(fuel, shop, "50"),
                credit(bank, null, "150")));

        service.onJournalEntryPosted(new JournalEntryPostedEvent(COMPANY_ID, LocalDate.of(YEAR, MONTH, 9),
                Set.of(adm.getId(), shop.getId())));

        assertThat(stored).extracting(BudgetAlert::getThresholdPercent).containsExactly(90, 100);
        assertThat(stored).allMatch(alert -> alert.getCostCenter() == adm);
    }

    @Test
    void or05_listaOsAlertasDaEmpresaActivaSoParaAdminEManager() {
        BudgetAlert alert = new BudgetAlert();
        alert.setId(7L);
        alert.setCompany(company);
        alert.setCostCenter(adm);
        alert.setFiscalYear(YEAR);
        alert.setMonth(MONTH);
        alert.setThresholdPercent(100);
        alert.setBudgetedAmount(new BigDecimal("1000"));
        alert.setActualAmount(new BigDecimal("1010"));
        when(alertRepository.findByCompanyIdAndFiscalYearOrderByMonthDescThresholdPercentDescIdDesc(COMPANY_ID, YEAR))
                .thenReturn(List.of(alert));

        List<BudgetAlertDTO> alerts = service.listAlerts(YEAR);

        assertEquals(1, alerts.size());
        assertEquals(7L, alerts.get(0).id());
        assertEquals(100, alerts.get(0).thresholdPercent());

        CurrentUserContext.setCurrentUser("caixa", "CASHIER");
        BusinessRuleException denied = assertThrows(BusinessRuleException.class, () -> service.listAlerts(YEAR));
        assertThat(denied.getMessage()).contains("MANAGER ou ADMIN");
    }

    /** The real posting door with mocked collaborators, to observe the event it publishes. */
    private JournalService postingService(ApplicationEventPublisher publisher) {
        JournalEntryRepository journalEntryRepository = mock(JournalEntryRepository.class);
        CompanyService companyService = mock(CompanyService.class);
        DocumentNumberService documentNumberService = mock(DocumentNumberService.class);
        when(companyService.getCurrentCompanyReference(COMPANY_ID)).thenReturn(company);
        when(documentNumberService.next(any())).thenReturn("LC-0001");
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return new JournalService(journalEntryRepository, mock(ChartOfAccountsService.class), documentNumberService,
                companyService, mock(AuditLogService.class), mock(CostCenterService.class), publisher);
    }

    private void actuals(JournalEntry... entries) {
        doAnswer(invocation -> {
            LocalDate from = invocation.getArgument(1);
            LocalDate to = invocation.getArgument(2);
            return List.of(entries).stream()
                    .filter(entry -> !entry.getEntryDate().isBefore(from) && !entry.getEntryDate().isAfter(to))
                    .toList();
        }).when(journalService).findEntriesBetween(any(), any(), any());
    }

    private void budget(BudgetLine... cells) {
        Budget budget = new Budget();
        budget.setId(90L);
        budget.setCompany(company);
        budget.setFiscalYear(YEAR);
        for (BudgetLine cell : cells) budget.addLine(cell);
        when(budgetRepository.findByCompanyIdAndFiscalYear(COMPANY_ID, YEAR)).thenReturn(Optional.of(budget));
    }

    private static JournalEntry entry(LocalDate date, JournalLine... lines) {
        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(date);
        for (JournalLine line : lines) entry.addLine(line);
        return entry;
    }

    private static JournalLine debit(Account account, CostCenter center, String amount) {
        return journalLine(account, center, new BigDecimal(amount), BigDecimal.ZERO);
    }

    private static JournalLine credit(Account account, CostCenter center, String amount) {
        return journalLine(account, center, BigDecimal.ZERO, new BigDecimal(amount));
    }

    private static JournalLine journalLine(Account account, CostCenter center, BigDecimal debit, BigDecimal credit) {
        JournalLine line = new JournalLine();
        line.setAccount(account);
        line.setCostCenter(center);
        line.setDebit(debit);
        line.setCredit(credit);
        return line;
    }

    private static BudgetLine cell(Account account, CostCenter center, int month, String amount) {
        BudgetLine line = new BudgetLine();
        line.setAccount(account);
        line.setCostCenter(center);
        line.setMonth(month);
        line.setAmount(new BigDecimal(amount));
        return line;
    }

    private static Account account(String code, String name) {
        Account account = new Account();
        account.setCode(code);
        account.setName(name);
        account.setAccountClass(AccountClass.ofCode(code));
        account.setPostable(true);
        account.setActive(true);
        return account;
    }

    private CostCenter center(Long id, String code) {
        CostCenter center = new CostCenter();
        center.setId(id);
        center.setCompany(company);
        center.setCode(code);
        center.setName(code);
        center.setType(CostCenterType.MANUAL);
        center.setActive(true);
        return center;
    }

    private static Company company() {
        Company company = new Company();
        company.setId(COMPANY_ID);
        return company;
    }
}
