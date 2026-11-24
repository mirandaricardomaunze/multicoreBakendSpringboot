package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.accounting.dto.BudgetDTO;
import mz.multicore.erp.modules.accounting.dto.CopyBudgetRequest;
import mz.multicore.erp.modules.accounting.dto.SaveBudgetRequest;
import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.model.AccountClass;
import mz.multicore.erp.modules.accounting.model.Budget;
import mz.multicore.erp.modules.accounting.model.BudgetLine;
import mz.multicore.erp.modules.accounting.model.CostCenter;
import mz.multicore.erp.modules.accounting.model.CostCenterType;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.repository.AccountRepository;
import mz.multicore.erp.modules.accounting.repository.BudgetRepository;
import mz.multicore.erp.modules.accounting.repository.CostCenterRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Harness OR-01 (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.1): one budget per company and year; its lines use only
 * postable accounts of classes 6 or 7, an active cost centre of the company, a month 1–12 and an amount ≥ 0, with one
 * line per account × centre × month. Saving again updates the same budget instead of creating another one.
 * Harness OR-02 (§4.1): copying from the previous year fills only the empty cells with the previous year's actual of
 * the same account, centre and month, adjusted by an optional percentage and rounded to 2 decimals.
 */
class BudgetRulesTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long OTHER_COMPANY_ID = 2L;
    private static final int YEAR = 2027;

    private final BudgetRepository budgetRepository = mock(BudgetRepository.class);
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final CostCenterRepository costCenterRepository = mock(CostCenterRepository.class);
    private final CompanyService companyService = mock(CompanyService.class);
    private final JournalService journalService = mock(JournalService.class);
    private final BudgetService service = new BudgetService(budgetRepository,
            new ChartOfAccountsService(accountRepository, companyService),
            new CostCenterService(costCenterRepository, companyService), companyService,
            new BudgetActualsCalculator(journalService));

    private final Account rent = account(10L, "6221", "Rendas", true);
    private final Account sales = account(11L, "7111", "Vendas de mercadorias", true);
    private final Account costsParent = account(12L, "62", "Fornecimentos e serviços", false);
    private final Account bank = account(13L, "1201", "Depósitos à ordem", true);
    private final CostCenter adm = center(40L, COMPANY_ID, "ADM", true);
    private final CostCenter fleet = center(41L, COMPANY_ID, "FROTA", false);
    private final CostCenter foreign = center(42L, OTHER_COMPANY_ID, "LOJA", true);

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
        when(companyService.getCurrentCompanyReference(COMPANY_ID)).thenReturn(company(COMPANY_ID));
        for (Account account : List.of(rent, sales, costsParent, bank)) {
            when(accountRepository.findByCompanyIdAndCode(COMPANY_ID, account.getCode())).thenReturn(Optional.of(account));
        }
        for (CostCenter center : List.of(adm, fleet, foreign)) {
            when(costCenterRepository.findById(center.getId())).thenReturn(Optional.of(center));
        }
        when(budgetRepository.findByCompanyIdAndFiscalYear(COMPANY_ID, YEAR)).thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget budget = invocation.getArgument(0);
            if (budget.getId() == null) budget.setId(90L);
            return budget;
        });
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void or01_gravaOrcamentoAnualDaEmpresaActiva() {
        BudgetDTO saved = service.save(new SaveBudgetRequest(YEAR, List.of(
                line("6221", 40L, 1, "15000.00"),
                line("6221", 40L, 2, "15000"),
                line("7111", 40L, 1, "80000.50"))));

        assertEquals(90L, saved.id());
        assertEquals(YEAR, saved.year());
        assertThat(saved.lines()).hasSize(3);
        assertThat(saved.totalExpenses()).isEqualByComparingTo("30000.00");
        assertThat(saved.totalRevenues()).isEqualByComparingTo("80000.50");
        assertEquals("6221", saved.lines().get(0).accountCode());
        assertEquals("ADM", saved.lines().get(0).costCenterCode());
        assertEquals(1, saved.lines().get(0).month());
    }

    @Test
    void or01_umOrcamentoPorEmpresaEAno_regravarActualizaAsMesmasLinhas() {
        Budget existing = new Budget();
        existing.setId(77L);
        existing.setCompany(company(COMPANY_ID));
        existing.setFiscalYear(YEAR);
        BudgetLine january = cell(rent, adm, 1, "100");
        BudgetLine february = cell(rent, adm, 2, "100");
        existing.addLine(january);
        existing.addLine(february);
        when(budgetRepository.findByCompanyIdAndFiscalYear(COMPANY_ID, YEAR)).thenReturn(Optional.of(existing));

        BudgetDTO saved = service.save(new SaveBudgetRequest(YEAR, List.of(
                line("6221", 40L, 1, "250"),
                line("6221", 40L, 3, "300"))));

        assertEquals(77L, saved.id());
        assertThat(existing.getLines()).hasSize(2);
        assertSame(january, existing.getLines().get(0));
        assertThat(january.getAmount()).isEqualByComparingTo("250");
        assertThat(existing.getLines()).doesNotContain(february);
        assertThat(saved.totalExpenses()).isEqualByComparingTo("550");
    }

    @Test
    void or01_soContasMovimentaveisDasClasses6e7() {
        BusinessRuleException outsideClasses = assertThrows(BusinessRuleException.class,
                () -> service.save(new SaveBudgetRequest(YEAR, List.of(line("1201", 40L, 1, "10")))));
        assertThat(outsideClasses.getMessage()).contains("1201").contains("classes 6 e 7");

        assertThrows(BusinessRuleException.class,
                () -> service.save(new SaveBudgetRequest(YEAR, List.of(line("62", 40L, 1, "10")))));
        assertThrows(BusinessRuleException.class,
                () -> service.save(new SaveBudgetRequest(YEAR, List.of(line("9999", 40L, 1, "10")))));
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void or01_centroObrigatorioActivoEDaEmpresa() {
        BusinessRuleException missing = assertThrows(BusinessRuleException.class,
                () -> service.save(new SaveBudgetRequest(YEAR, List.of(line("6221", null, 1, "10")))));
        assertThat(missing.getMessage()).contains("centro de custo");

        BusinessRuleException inactive = assertThrows(BusinessRuleException.class,
                () -> service.save(new SaveBudgetRequest(YEAR, List.of(line("6221", 41L, 1, "10")))));
        assertThat(inactive.getMessage()).contains("FROTA").contains("inactivo");

        assertThrows(BusinessRuleException.class,
                () -> service.save(new SaveBudgetRequest(YEAR, List.of(line("6221", 42L, 1, "10")))));
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void or01_mesEntre1e12() {
        for (Integer month : new Integer[]{null, 0, 13}) {
            BusinessRuleException error = assertThrows(BusinessRuleException.class,
                    () -> service.save(new SaveBudgetRequest(YEAR, List.of(line("6221", 40L, month, "10")))));
            assertThat(error.getMessage()).contains("1 e 12");
        }
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void or01_valorObrigatorioENaoNegativo_zeroAceite() {
        assertThrows(BusinessRuleException.class,
                () -> service.save(new SaveBudgetRequest(YEAR, List.of(line("6221", 40L, 1, "-0.01")))));
        assertThrows(BusinessRuleException.class,
                () -> service.save(new SaveBudgetRequest(YEAR, List.of(line("6221", 40L, 1, null)))));
        verify(budgetRepository, never()).save(any());

        BudgetDTO saved = service.save(new SaveBudgetRequest(YEAR, List.of(line("6221", 40L, 1, "0"))));
        assertThat(saved.lines()).hasSize(1);
    }

    @Test
    void or01_linhasUnicasPorContaCentroEMes() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.save(new SaveBudgetRequest(YEAR, List.of(
                        line("6221", 40L, 4, "10"),
                        line("6221", 40L, 4, "20")))));
        assertThat(error.getMessage()).contains("6221").contains("ADM").contains("mais do que uma vez");
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void or01_anoObrigatorio() {
        assertThrows(BusinessRuleException.class, () -> service.save(new SaveBudgetRequest(null, List.of())));
    }

    @Test
    void or01_soGestorOuAdminGere() {
        CurrentUserContext.setCurrentUser("caixa", "CASHIER");

        assertThrows(BusinessRuleException.class,
                () -> service.save(new SaveBudgetRequest(YEAR, List.of(line("6221", 40L, 1, "10")))));
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void or01_anoSemOrcamentoDevolveGrelhaVazia() {
        BudgetDTO budget = service.get(YEAR);

        assertThat(budget.id()).isNull();
        assertEquals(YEAR, budget.year());
        assertThat(budget.lines()).isEmpty();
        assertThat(budget.totalExpenses()).isEqualByComparingTo("0");
    }

    @Test
    void or02_copiaORealDoAnoAnteriorPorContaCentroEMes() {
        previousYearEntries(
                entry(LocalDate.of(YEAR - 1, 1, 10), debit(rent, adm, "1000"), credit(bank, null, "1000")),
                entry(LocalDate.of(YEAR - 1, 1, 20), debit(rent, adm, "500"), credit(bank, null, "500")),
                entry(LocalDate.of(YEAR - 1, 1, 25), credit(rent, adm, "200"), debit(bank, null, "200")),
                entry(LocalDate.of(YEAR - 1, 3, 5), credit(sales, adm, "8000"), debit(bank, null, "8000")));

        BudgetDTO copied = service.copyFromPreviousYear(new CopyBudgetRequest(YEAR, null));

        assertThat(copied.lines()).hasSize(2);
        assertEquals("6221", copied.lines().get(0).accountCode());
        assertEquals(1, copied.lines().get(0).month());
        assertThat(copied.lines().get(0).amount()).isEqualByComparingTo("1300.00");
        assertEquals("7111", copied.lines().get(1).accountCode());
        assertEquals(3, copied.lines().get(1).month());
        assertThat(copied.lines().get(1).amount()).isEqualByComparingTo("8000.00");
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void or02_ajustePercentualArredondadoADuasCasas() {
        previousYearEntries(entry(LocalDate.of(YEAR - 1, 2, 1), debit(rent, adm, "333.33"), credit(bank, null, "333.33")));

        BudgetDTO copied = service.copyFromPreviousYear(new CopyBudgetRequest(YEAR, new BigDecimal("10")));

        assertThat(copied.lines().get(0).amount()).isEqualByComparingTo("366.66");
        assertEquals(2, copied.lines().get(0).amount().scale());
    }

    @Test
    void or02_soPreencheCelulasVazias_nuncaSobrescreve() {
        Budget existing = new Budget();
        existing.setId(77L);
        existing.setCompany(company(COMPANY_ID));
        existing.setFiscalYear(YEAR);
        BudgetLine januaryZero = cell(rent, adm, 1, "0");
        existing.addLine(januaryZero);
        when(budgetRepository.findByCompanyIdAndFiscalYear(COMPANY_ID, YEAR)).thenReturn(Optional.of(existing));
        previousYearEntries(
                entry(LocalDate.of(YEAR - 1, 1, 10), debit(rent, adm, "1000"), credit(bank, null, "1000")),
                entry(LocalDate.of(YEAR - 1, 2, 10), debit(rent, adm, "900"), credit(bank, null, "900")));

        BudgetDTO copied = service.copyFromPreviousYear(new CopyBudgetRequest(YEAR, new BigDecimal("50")));

        assertEquals(77L, copied.id());
        assertThat(copied.lines()).hasSize(2);
        assertThat(januaryZero.getAmount()).isEqualByComparingTo("0");
        assertThat(copied.lines().get(1).amount()).isEqualByComparingTo("1350.00");
    }

    @Test
    void or02_ignoraRealNaoPositivoECentrosInactivos() {
        previousYearEntries(
                entry(LocalDate.of(YEAR - 1, 4, 1), debit(rent, fleet, "700"), credit(bank, null, "700")),
                entry(LocalDate.of(YEAR - 1, 5, 1), debit(rent, adm, "100"), credit(bank, null, "100")),
                entry(LocalDate.of(YEAR - 1, 5, 2), credit(rent, adm, "100"), debit(bank, null, "100")));

        BudgetDTO copied = service.copyFromPreviousYear(new CopyBudgetRequest(YEAR, null));

        assertThat(copied.lines()).isEmpty();
    }

    @Test
    void or02_leSoOAnoAnteriorDaEmpresaActiva() {
        previousYearEntries();

        service.copyFromPreviousYear(new CopyBudgetRequest(YEAR, null));

        verify(journalService).findEntriesBetween(COMPANY_ID, LocalDate.of(YEAR - 1, 1, 1), LocalDate.of(YEAR - 1, 12, 31));
    }

    @Test
    void or02_validaAnoAjusteEPerfil() {
        assertThrows(BusinessRuleException.class, () -> service.copyFromPreviousYear(new CopyBudgetRequest(null, null)));
        BusinessRuleException tooLow = assertThrows(BusinessRuleException.class,
                () -> service.copyFromPreviousYear(new CopyBudgetRequest(YEAR, new BigDecimal("-100.01"))));
        assertThat(tooLow.getMessage()).contains("-100%");

        CurrentUserContext.setCurrentUser("caixa", "CASHIER");
        assertThrows(BusinessRuleException.class, () -> service.copyFromPreviousYear(new CopyBudgetRequest(YEAR, null)));
        verify(budgetRepository, never()).save(any());
    }

    private void previousYearEntries(JournalEntry... entries) {
        when(journalService.findEntriesBetween(COMPANY_ID, LocalDate.of(YEAR - 1, 1, 1), LocalDate.of(YEAR - 1, 12, 31)))
                .thenReturn(List.of(entries));
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

    private static SaveBudgetRequest.Line line(String accountCode, Long costCenterId, Integer month, String amount) {
        return new SaveBudgetRequest.Line(accountCode, costCenterId, month, amount == null ? null : new BigDecimal(amount));
    }

    private static BudgetLine cell(Account account, CostCenter center, int month, String amount) {
        BudgetLine line = new BudgetLine();
        line.setAccount(account);
        line.setCostCenter(center);
        line.setMonth(month);
        line.setAmount(new BigDecimal(amount));
        return line;
    }

    private static Account account(Long id, String code, String name, boolean postable) {
        Account account = new Account();
        account.setId(id);
        account.setCode(code);
        account.setName(name);
        account.setAccountClass(AccountClass.ofCode(code));
        account.setPostable(postable);
        account.setActive(true);
        return account;
    }

    private static CostCenter center(Long id, Long companyId, String code, boolean active) {
        CostCenter center = new CostCenter();
        center.setId(id);
        center.setCompany(company(companyId));
        center.setCode(code);
        center.setName(code);
        center.setType(CostCenterType.MANUAL);
        center.setActive(active);
        return center;
    }

    private static Company company(Long id) {
        Company company = new Company();
        company.setId(id);
        return company;
    }
}
