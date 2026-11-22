package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.accounting.dto.CostCenterResultDTO;
import mz.multicore.erp.modules.accounting.dto.CostCenterResultLineDTO;
import mz.multicore.erp.modules.accounting.dto.IncomeStatementDTO;
import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.model.AccountClass;
import mz.multicore.erp.modules.accounting.model.CostCenter;
import mz.multicore.erp.modules.accounting.model.CostCenterType;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.repository.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Harness CC-07 (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.3): the "Resultados por centro de custo" report gives, per
 * centre, revenue (class 7, credit − debit), costs (class 6, debit − credit) and result, plus a total that is equal to
 * the income statement of the same period.
 */
class CostCenterResultReportTest {

    private static final Long COMPANY_ID = 1L;
    private static final LocalDate FROM = LocalDate.of(2026, 11, 1);
    private static final LocalDate TO = LocalDate.of(2026, 11, 30);

    private final JournalService journalService = mock(JournalService.class);
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final AccountingReportService service = new AccountingReportService(journalService, accountRepository);

    private final CostCenter baixa = center(10L, "LJ01", "Loja da Baixa");
    private final CostCenter porto = center(20L, "AR02", "Armazém do Porto");
    private final CostCenter adm = center(1L, "ADM", "Administração");

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void cc07_proveitosCustosEResultadoPorCentro() {
        when(journalService.findEntriesBetween(COMPANY_ID, FROM, TO)).thenReturn(sampleEntries());

        CostCenterResultDTO report = service.getCostCenterResult(FROM, TO);

        CostCenterResultLineDTO loja = line(report, "LJ01");
        assertEquals(new BigDecimal("900.00"), loja.revenue());   // 1 000 vendas − 100 devolução
        assertEquals(new BigDecimal("650.00"), loja.expense());   // 600 CMV + 50 quebra
        assertEquals(new BigDecimal("250.00"), loja.result());
        assertEquals("Loja da Baixa", loja.costCenterName());

        CostCenterResultLineDTO armazem = line(report, "AR02");
        assertEquals(new BigDecimal("0.00"), armazem.revenue());
        assertEquals(new BigDecimal("300.00"), armazem.expense()); // 320 renda − 20 regularização
        assertEquals(new BigDecimal("-300.00"), armazem.result());

        CostCenterResultLineDTO administracao = line(report, "ADM");
        assertEquals(new BigDecimal("40.00"), administracao.revenue());
        assertEquals(new BigDecimal("150.00"), administracao.expense());
        assertEquals(new BigDecimal("-110.00"), administracao.result());
    }

    @Test
    void cc07_totalIgualADemonstracaoDeResultadosDoPeriodo() {
        when(journalService.findEntriesBetween(COMPANY_ID, FROM, TO)).thenReturn(sampleEntries());

        CostCenterResultDTO report = service.getCostCenterResult(FROM, TO);
        IncomeStatementDTO incomeStatement = service.getIncomeStatement(FROM, TO);

        assertEquals(incomeStatement.totalRevenue(), report.totalRevenue());
        assertEquals(incomeStatement.totalExpense(), report.totalExpense());
        assertEquals(incomeStatement.netResult(), report.netResult());
        assertEquals(new BigDecimal("-160.00"), report.netResult());
        BigDecimal sumOfCenters = report.centers().stream().map(CostCenterResultLineDTO::result)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, sumOfCenters.compareTo(report.netResult()));
    }

    @Test
    void cc07_contasDasClasses1a5NaoEntramNoResultado() {
        when(journalService.findEntriesBetween(COMPANY_ID, FROM, TO)).thenReturn(sampleEntries());

        CostCenterResultDTO report = service.getCostCenterResult(FROM, TO);

        assertEquals(3, report.centers().size());
        assertTrue(report.centers().stream().allMatch(l -> l.costCenterId() != null));
    }

    @Test
    void cc07_centrosOrdenadosPorCodigo() {
        when(journalService.findEntriesBetween(COMPANY_ID, FROM, TO)).thenReturn(sampleEntries());

        List<String> codes = service.getCostCenterResult(FROM, TO).centers().stream()
                .map(CostCenterResultLineDTO::costCenterCode).toList();

        assertEquals(List.of("ADM", "AR02", "LJ01"), codes);
    }

    @Test
    void cc07_linhaAntigaSemCentroApareceEmSemCentroParaOTotalBater() {
        JournalEntry legacy = entry();
        legacy.addLine(JournalLine.debit(account("6221"), new BigDecimal("70.00"), null));
        legacy.addLine(JournalLine.credit(account("1101"), new BigDecimal("70.00"), null));
        when(journalService.findEntriesBetween(COMPANY_ID, FROM, TO)).thenReturn(List.of(legacy));

        CostCenterResultDTO report = service.getCostCenterResult(FROM, TO);

        assertEquals(1, report.centers().size());
        CostCenterResultLineDTO noCenter = report.centers().get(0);
        assertNull(noCenter.costCenterId());
        assertEquals("Sem centro", noCenter.costCenterName());
        assertEquals(new BigDecimal("-70.00"), noCenter.result());
        assertEquals(service.getIncomeStatement(FROM, TO).netResult(), report.netResult());
    }

    @Test
    void cc07_periodoSemMovimentosDaTotaisAZero() {
        when(journalService.findEntriesBetween(COMPANY_ID, FROM, TO)).thenReturn(List.of());

        CostCenterResultDTO report = service.getCostCenterResult(FROM, TO);

        assertTrue(report.centers().isEmpty());
        assertEquals(new BigDecimal("0.00"), report.totalRevenue());
        assertEquals(new BigDecimal("0.00"), report.totalExpense());
        assertEquals(new BigDecimal("0.00"), report.netResult());
        assertEquals(FROM, report.from());
        assertEquals(TO, report.to());
    }

    @Test
    void cc07_periodoObrigatorioEValido() {
        assertThrows(BusinessRuleException.class, () -> service.getCostCenterResult(null, TO));
        assertThrows(BusinessRuleException.class, () -> service.getCostCenterResult(TO, FROM));
    }

    private List<JournalEntry> sampleEntries() {
        JournalEntry sale = entry();
        sale.addLine(JournalLine.debit(account("1101"), new BigDecimal("1000.00"), null));
        sale.addLine(on(JournalLine.credit(account("7101"), new BigDecimal("1000.00"), null), baixa));
        sale.addLine(on(JournalLine.debit(account("6101"), new BigDecimal("600.00"), null), baixa));
        sale.addLine(JournalLine.credit(account("3101"), new BigDecimal("600.00"), null));

        JournalEntry creditNote = entry();
        creditNote.addLine(on(JournalLine.debit(account("7101"), new BigDecimal("100.00"), null), baixa));
        creditNote.addLine(JournalLine.credit(account("1101"), new BigDecimal("100.00"), null));

        JournalEntry breakage = entry();
        breakage.addLine(on(JournalLine.debit(account("6301"), new BigDecimal("50.00"), null), baixa));
        breakage.addLine(JournalLine.credit(account("3101"), new BigDecimal("50.00"), null));

        JournalEntry rent = entry();
        rent.addLine(on(JournalLine.debit(account("6221"), new BigDecimal("320.00"), null), porto));
        rent.addLine(on(JournalLine.debit(account("6201"), new BigDecimal("150.00"), null), adm));
        rent.addLine(JournalLine.credit(account("1201"), new BigDecimal("470.00"), null));

        JournalEntry adjustment = entry();
        adjustment.addLine(JournalLine.debit(account("1201"), new BigDecimal("60.00"), null));
        adjustment.addLine(on(JournalLine.credit(account("6221"), new BigDecimal("20.00"), null), porto));
        adjustment.addLine(on(JournalLine.credit(account("7601"), new BigDecimal("40.00"), null), adm));

        return List.of(sale, creditNote, breakage, rent, adjustment);
    }

    private static CostCenterResultLineDTO line(CostCenterResultDTO report, String code) {
        return report.centers().stream().filter(l -> code.equals(l.costCenterCode())).findFirst()
                .orElseThrow(() -> new AssertionError("centro " + code + " em falta"));
    }

    private static JournalLine on(JournalLine line, CostCenter center) {
        line.setCostCenter(center);
        return line;
    }

    private static JournalEntry entry() {
        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(LocalDate.of(2026, 11, 15));
        entry.setDescription("Teste");
        return entry;
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

    private static CostCenter center(Long id, String code, String name) {
        CostCenter center = new CostCenter();
        center.setId(id);
        center.setCode(code);
        center.setName(name);
        center.setType(CostCenterType.MANUAL);
        center.setActive(true);
        return center;
    }
}
