package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.accounting.dto.BudgetVarianceDTO;
import mz.multicore.erp.modules.accounting.dto.BudgetVarianceLineDTO;
import mz.multicore.erp.modules.accounting.dto.BudgetVarianceQuery;
import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.model.AccountClass;
import mz.multicore.erp.modules.accounting.model.Budget;
import mz.multicore.erp.modules.accounting.model.BudgetLine;
import mz.multicore.erp.modules.accounting.model.BudgetVarianceDirection;
import mz.multicore.erp.modules.accounting.model.CostCenter;
import mz.multicore.erp.modules.accounting.model.CostCenterType;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.repository.BudgetRepository;
import mz.multicore.erp.modules.company.model.Company;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Harness OR-03 (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.2): the actual of each account × cost centre in a month, or
 * from January up to that month, is the sum of its journal lines signed by class (class 6 debit − credit, class 7
 * credit − debit), shown next to the budgeted amount of the same cells.
 * Harness OR-04 (§4.2): each row carries the variance (actual − budgeted), the variance % over the budget (2 decimals,
 * empty when nothing was budgeted) and whether it is favourable or unfavourable for its class.
 */
class BudgetVarianceTest {

    private static final Long COMPANY_ID = 1L;
    private static final int YEAR = 2027;

    private final BudgetRepository budgetRepository = mock(BudgetRepository.class);
    private final JournalService journalService = mock(JournalService.class);
    private final BudgetVarianceService service = new BudgetVarianceService(budgetRepository,
            new BudgetActualsCalculator(journalService));

    private final Account rent = account("6221", "Rendas");
    private final Account fuel = account("6242", "Combustíveis");
    private final Account sales = account("7111", "Vendas de mercadorias");
    private final Account bank = account("1201", "Depósitos à ordem");
    private final CostCenter adm = center(40L, "ADM", true);
    private final CostCenter shop = center(41L, "LOJA", true);
    private final CostCenter fleet = center(42L, "FROTA", false);

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
        when(budgetRepository.findByCompanyIdAndFiscalYear(COMPANY_ID, YEAR)).thenReturn(Optional.empty());
        entries();
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void or03_realDoMesComSinalPorClasse() {
        entries(
                entry(LocalDate.of(YEAR, 3, 2), debit(rent, adm, "1000"), credit(bank, null, "1000")),
                entry(LocalDate.of(YEAR, 3, 9), credit(rent, adm, "150"), debit(bank, null, "150")),
                entry(LocalDate.of(YEAR, 3, 15), credit(sales, shop, "9000"), debit(bank, null, "9000")),
                entry(LocalDate.of(YEAR, 3, 20), debit(sales, shop, "400"), credit(bank, null, "400")));

        BudgetVarianceDTO report = service.report(new BudgetVarianceQuery(YEAR, 3, false, null));

        assertEquals(YEAR, report.year());
        assertEquals(3, report.month());
        assertThat(report.lines()).hasSize(2);
        BudgetVarianceLineDTO rentAdm = report.lines().get(0);
        assertEquals("6221", rentAdm.accountCode());
        assertEquals(6, rentAdm.accountClass());
        assertEquals("ADM", rentAdm.costCenterCode());
        assertThat(rentAdm.actual()).isEqualByComparingTo("850");
        BudgetVarianceLineDTO salesShop = report.lines().get(1);
        assertEquals("7111", salesShop.accountCode());
        assertEquals(7, salesShop.accountClass());
        assertThat(salesShop.actual()).isEqualByComparingTo("8600");
    }

    @Test
    void or03_separaPorContaECentro_soOMesPedido() {
        entries(
                entry(LocalDate.of(YEAR, 2, 28), debit(rent, adm, "999"), credit(bank, null, "999")),
                entry(LocalDate.of(YEAR, 3, 1), debit(rent, adm, "100"), debit(rent, shop, "200"),
                        debit(fuel, shop, "50"), credit(bank, null, "350")),
                entry(LocalDate.of(YEAR, 4, 1), debit(rent, shop, "777"), credit(bank, null, "777")));

        BudgetVarianceDTO report = service.report(new BudgetVarianceQuery(YEAR, 3, false, null));

        assertThat(report.lines()).extracting(line -> line.accountCode() + "/" + line.costCenterCode())
                .containsExactly("6221/ADM", "6221/LOJA", "6242/LOJA");
        assertThat(report.lines()).extracting(BudgetVarianceLineDTO::actual)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("50"));
    }

    @Test
    void or03_acumuladoSomaDeJaneiroAteAoMes() {
        entries(
                entry(LocalDate.of(YEAR, 1, 5), debit(rent, adm, "100"), credit(bank, null, "100")),
                entry(LocalDate.of(YEAR, 2, 5), debit(rent, adm, "200"), credit(bank, null, "200")),
                entry(LocalDate.of(YEAR, 3, 5), debit(rent, adm, "300"), credit(bank, null, "300")),
                entry(LocalDate.of(YEAR, 4, 5), debit(rent, adm, "400"), credit(bank, null, "400")));
        budget(cell(rent, adm, 1, "150"), cell(rent, adm, 2, "150"), cell(rent, adm, 3, "150"),
                cell(rent, adm, 4, "150"));

        BudgetVarianceDTO report = service.report(new BudgetVarianceQuery(YEAR, 3, true, null));

        assertThat(report.cumulative()).isTrue();
        assertThat(report.lines()).hasSize(1);
        assertThat(report.lines().get(0).actual()).isEqualByComparingTo("600");
        assertThat(report.lines().get(0).budgeted()).isEqualByComparingTo("450");
    }

    @Test
    void or03_juntaOrcadoEReal_ladoEmFaltaValeZero() {
        entries(entry(LocalDate.of(YEAR, 5, 5), debit(fuel, shop, "80"), credit(bank, null, "80")));
        budget(cell(rent, adm, 5, "1000"), cell(rent, adm, 6, "999"));

        BudgetVarianceDTO report = service.report(new BudgetVarianceQuery(YEAR, 5, false, null));

        assertThat(report.lines()).hasSize(2);
        BudgetVarianceLineDTO budgetedOnly = report.lines().get(0);
        assertEquals("6221", budgetedOnly.accountCode());
        assertThat(budgetedOnly.budgeted()).isEqualByComparingTo("1000");
        assertThat(budgetedOnly.actual()).isEqualByComparingTo("0");
        BudgetVarianceLineDTO actualOnly = report.lines().get(1);
        assertEquals("6242", actualOnly.accountCode());
        assertThat(actualOnly.budgeted()).isEqualByComparingTo("0");
        assertThat(actualOnly.actual()).isEqualByComparingTo("80");
    }

    @Test
    void or03_partidasSemCentroOuForaDas6e7FicamDeFora_centroInactivoMantemHistorico() {
        entries(
                entry(LocalDate.of(YEAR, 6, 1), debit(rent, null, "500"), credit(bank, null, "500")),
                entry(LocalDate.of(YEAR, 6, 2), debit(fuel, fleet, "70"), credit(bank, adm, "70")));

        BudgetVarianceDTO report = service.report(new BudgetVarianceQuery(YEAR, 6, false, null));

        assertThat(report.lines()).hasSize(1);
        assertEquals("FROTA", report.lines().get(0).costCenterCode());
        assertThat(report.lines().get(0).actual()).isEqualByComparingTo("70");
    }

    @Test
    void or03_filtroPorCentro() {
        entries(entry(LocalDate.of(YEAR, 7, 1), debit(rent, adm, "100"), debit(rent, shop, "200"),
                credit(bank, null, "300")));
        budget(cell(rent, adm, 7, "120"), cell(rent, shop, 7, "180"));

        BudgetVarianceDTO report = service.report(new BudgetVarianceQuery(YEAR, 7, false, 41L));

        assertEquals(41L, report.costCenterId());
        assertThat(report.lines()).hasSize(1);
        assertEquals("LOJA", report.lines().get(0).costCenterCode());
        assertThat(report.lines().get(0).budgeted()).isEqualByComparingTo("180");
        assertThat(report.lines().get(0).actual()).isEqualByComparingTo("200");
    }

    @Test
    void or03_leSoOAnoDaEmpresaActiva() {
        service.report(new BudgetVarianceQuery(YEAR, 1, false, null));

        verify(journalService).findEntriesBetween(COMPANY_ID, LocalDate.of(YEAR, 1, 1), LocalDate.of(YEAR, 12, 31));
        verify(budgetRepository).findByCompanyIdAndFiscalYear(COMPANY_ID, YEAR);
    }

    @Test
    void or03_validaAnoMesEPerfil() {
        assertThrows(BusinessRuleException.class, () -> service.report(new BudgetVarianceQuery(null, 1, false, null)));
        for (Integer month : new Integer[]{null, 0, 13}) {
            BusinessRuleException error = assertThrows(BusinessRuleException.class,
                    () -> service.report(new BudgetVarianceQuery(YEAR, month, false, null)));
            assertThat(error.getMessage()).contains("1 e 12");
        }

        CurrentUserContext.setCurrentUser("caixa", "CASHIER");
        assertThrows(BusinessRuleException.class, () -> service.report(new BudgetVarianceQuery(YEAR, 1, false, null)));
        verify(budgetRepository, never()).findByCompanyIdAndFiscalYear(anyLong(), anyInt());
        verify(journalService, never()).findEntriesBetween(any(), any(), any());
    }

    @Test
    void or04_custoAcimaDoOrcadoEDesfavoravel_abaixoEFavoravel() {
        entries(entry(LocalDate.of(YEAR, 8, 1), debit(rent, adm, "1100"), debit(fuel, adm, "300"),
                credit(bank, null, "1400")));
        budget(cell(rent, adm, 8, "1000"), cell(fuel, adm, 8, "400"));

        List<BudgetVarianceLineDTO> lines = service.report(new BudgetVarianceQuery(YEAR, 8, false, null)).lines();

        BudgetVarianceLineDTO rentAdm = lines.get(0);
        assertThat(rentAdm.variance()).isEqualByComparingTo("100");
        assertThat(rentAdm.variancePercent()).isEqualByComparingTo("10.00");
        assertEquals(BudgetVarianceDirection.UNFAVORABLE, rentAdm.direction());
        BudgetVarianceLineDTO fuelAdm = lines.get(1);
        assertThat(fuelAdm.variance()).isEqualByComparingTo("-100");
        assertThat(fuelAdm.variancePercent()).isEqualByComparingTo("-25.00");
        assertEquals(BudgetVarianceDirection.FAVORABLE, fuelAdm.direction());
    }

    @Test
    void or04_proveitoAbaixoDoOrcadoEDesfavoravel_acimaEFavoravel() {
        entries(entry(LocalDate.of(YEAR, 9, 1), credit(sales, shop, "8000"), credit(sales, adm, "1200"),
                debit(bank, null, "9200")));
        budget(cell(sales, adm, 9, "1000"), cell(sales, shop, 9, "10000"));

        List<BudgetVarianceLineDTO> lines = service.report(new BudgetVarianceQuery(YEAR, 9, false, null)).lines();

        BudgetVarianceLineDTO salesAdm = lines.get(0);
        assertThat(salesAdm.variance()).isEqualByComparingTo("200");
        assertThat(salesAdm.variancePercent()).isEqualByComparingTo("20.00");
        assertEquals(BudgetVarianceDirection.FAVORABLE, salesAdm.direction());
        BudgetVarianceLineDTO salesShop = lines.get(1);
        assertThat(salesShop.variance()).isEqualByComparingTo("-2000");
        assertThat(salesShop.variancePercent()).isEqualByComparingTo("-20.00");
        assertEquals(BudgetVarianceDirection.UNFAVORABLE, salesShop.direction());
    }

    @Test
    void or04_orcadoZeroSemPercentagem_desvioZeroNoOrcamento_percentagemADuasCasas() {
        entries(entry(LocalDate.of(YEAR, 10, 1), debit(rent, adm, "500"), debit(fuel, shop, "100"),
                debit(rent, shop, "200"), credit(bank, null, "800")));
        budget(cell(rent, adm, 10, "0"), cell(fuel, shop, 10, "100"), cell(rent, shop, 10, "300"));

        List<BudgetVarianceLineDTO> lines = service.report(new BudgetVarianceQuery(YEAR, 10, false, null)).lines();

        BudgetVarianceLineDTO zeroBudget = lines.get(0);
        assertThat(zeroBudget.variance()).isEqualByComparingTo("500");
        assertThat(zeroBudget.variancePercent()).isNull();
        assertEquals(BudgetVarianceDirection.UNFAVORABLE, zeroBudget.direction());
        BudgetVarianceLineDTO thirds = lines.get(1);
        assertEquals("6221/LOJA", thirds.accountCode() + "/" + thirds.costCenterCode());
        assertThat(thirds.variancePercent()).isEqualByComparingTo("-33.33");
        BudgetVarianceLineDTO onBudget = lines.get(2);
        assertThat(onBudget.variance()).isEqualByComparingTo("0");
        assertThat(onBudget.variancePercent()).isEqualByComparingTo("0");
        assertEquals(BudgetVarianceDirection.ON_BUDGET, onBudget.direction());
    }

    @Test
    void or04_desvioNoAcumuladoUsaOsTotaisDoPeriodo() {
        entries(
                entry(LocalDate.of(YEAR, 1, 5), debit(rent, adm, "100"), credit(bank, null, "100")),
                entry(LocalDate.of(YEAR, 2, 5), debit(rent, adm, "233"), credit(bank, null, "233")));
        budget(cell(rent, adm, 1, "150"), cell(rent, adm, 2, "150"));

        BudgetVarianceLineDTO line = service.report(new BudgetVarianceQuery(YEAR, 2, true, null)).lines().get(0);

        assertThat(line.variance()).isEqualByComparingTo("33");
        assertThat(line.variancePercent()).isEqualByComparingTo("11.00");
        assertEquals(BudgetVarianceDirection.UNFAVORABLE, line.direction());
    }

    private void entries(JournalEntry... entries) {
        when(journalService.findEntriesBetween(COMPANY_ID, LocalDate.of(YEAR, 1, 1), LocalDate.of(YEAR, 12, 31)))
                .thenReturn(List.of(entries));
    }

    private void budget(BudgetLine... cells) {
        Budget budget = new Budget();
        budget.setId(90L);
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

    private static CostCenter center(Long id, String code, boolean active) {
        CostCenter center = new CostCenter();
        center.setId(id);
        Company company = new Company();
        company.setId(COMPANY_ID);
        center.setCompany(company);
        center.setCode(code);
        center.setName(code);
        center.setType(CostCenterType.MANUAL);
        center.setActive(active);
        return center;
    }
}
