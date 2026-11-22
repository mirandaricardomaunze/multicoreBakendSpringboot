package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.model.AccountClass;
import mz.multicore.erp.modules.accounting.model.CostCenter;
import mz.multicore.erp.modules.accounting.model.CostCenterType;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.repository.JournalEntryRepository;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Harness CC-04 (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.2): a journal line of an account of class 6 or 7 must carry
 * a cost centre; a line of classes 1–5 never carries one; the centre must be active and belong to the entry's company.
 * The rule lives in the entry's domain and is applied by the single posting door ({@link JournalService#save}).
 */
class JournalLineCostCenterTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long OTHER_COMPANY_ID = 2L;

    private final JournalEntryRepository journalEntryRepository = mock(JournalEntryRepository.class);
    private final ChartOfAccountsService chartOfAccountsService = mock(ChartOfAccountsService.class);
    private final DocumentNumberService documentNumberService = mock(DocumentNumberService.class);
    private final CompanyService companyService = mock(CompanyService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final CostCenterService costCenterService = mock(CostCenterService.class);
    private final JournalService journalService = new JournalService(journalEntryRepository, chartOfAccountsService,
            documentNumberService, companyService, auditLogService, costCenterService);

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
        when(companyService.getCurrentCompanyReference(COMPANY_ID)).thenReturn(company(COMPANY_ID));
        when(documentNumberService.next(any())).thenReturn("LC-0001");
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void cc04_linhaDeCustoSemCentroERecusadaNoDominio() {
        JournalEntry entry = entry(COMPANY_ID);
        entry.addLine(JournalLine.debit(account("6221"), new BigDecimal("500.00"), null));
        entry.addLine(JournalLine.credit(account("1101"), new BigDecimal("500.00"), null));

        BusinessRuleException error = assertThrows(BusinessRuleException.class, entry::validateCostCenters);

        assertTrue(error.getMessage().contains("6221"));
        assertTrue(error.getMessage().contains("centro de custo"));
    }

    @Test
    void cc04_linhaDeProveitoSemCentroERecusadaNoDominio() {
        JournalEntry entry = entry(COMPANY_ID);
        entry.addLine(JournalLine.debit(account("1101"), new BigDecimal("200.00"), null));
        entry.addLine(JournalLine.credit(account("7101"), new BigDecimal("200.00"), null));

        BusinessRuleException error = assertThrows(BusinessRuleException.class, entry::validateCostCenters);

        assertTrue(error.getMessage().contains("7101"));
    }

    @Test
    void cc04_linhasDeClasse6e7ComCentroDaEmpresaPassam() {
        CostCenter store = center(10L, COMPANY_ID, true);
        JournalEntry entry = entry(COMPANY_ID);
        entry.addLine(withCenter(JournalLine.debit(account("6221"), new BigDecimal("500.00"), null), store));
        entry.addLine(withCenter(JournalLine.credit(account("7101"), new BigDecimal("500.00"), null), store));

        assertDoesNotThrow(entry::validateCostCenters);
    }

    @Test
    void cc04_linhaDeClasse1a5ComCentroERecusada() {
        CostCenter store = center(10L, COMPANY_ID, true);
        for (String code : new String[]{"1101", "2101", "3101", "4101", "5101"}) {
            JournalEntry entry = entry(COMPANY_ID);
            entry.addLine(withCenter(JournalLine.debit(account(code), new BigDecimal("100.00"), null), store));
            entry.addLine(withCenter(JournalLine.credit(account("7101"), new BigDecimal("100.00"), null), store));

            BusinessRuleException error = assertThrows(BusinessRuleException.class, entry::validateCostCenters,
                    "classe da conta " + code);
            assertTrue(error.getMessage().contains(code));
            assertTrue(error.getMessage().contains("não leva centro de custo"));
        }
    }

    @Test
    void cc04_centroDeOutraEmpresaERecusado() {
        CostCenter foreign = center(20L, OTHER_COMPANY_ID, true);
        JournalEntry entry = entry(COMPANY_ID);
        entry.addLine(withCenter(JournalLine.debit(account("6221"), new BigDecimal("500.00"), null), foreign));
        entry.addLine(JournalLine.credit(account("1101"), new BigDecimal("500.00"), null));

        BusinessRuleException error = assertThrows(BusinessRuleException.class, entry::validateCostCenters);

        assertTrue(error.getMessage().contains("outra empresa"));
    }

    @Test
    void cc04_centroInactivoERecusado() {
        CostCenter inactive = center(30L, COMPANY_ID, false);
        JournalEntry entry = entry(COMPANY_ID);
        entry.addLine(withCenter(JournalLine.debit(account("6221"), new BigDecimal("500.00"), null), inactive));
        entry.addLine(JournalLine.credit(account("1101"), new BigDecimal("500.00"), null));

        BusinessRuleException error = assertThrows(BusinessRuleException.class, entry::validateCostCenters);

        assertTrue(error.getMessage().contains("inactivo"));
    }

    @Test
    void cc04_portaDeGravacaoAplicaARegra_centroEmClasse2Recusado() {
        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(LocalDate.of(2026, 11, 22));
        entry.setDescription("Venda");
        entry.addLine(withCenter(JournalLine.debit(account("2111"), new BigDecimal("100.00"), null),
                center(10L, COMPANY_ID, true)));
        entry.addLine(JournalLine.credit(account("7101"), new BigDecimal("100.00"), null));

        assertThrows(BusinessRuleException.class, () -> journalService.save(entry, COMPANY_ID));

        verify(journalEntryRepository, never()).save(any());
    }

    @Test
    void cc04_portaDeGravacaoCompletaLinhaDe6ou7SemCentroComOPorOmissao() {
        CostCenter adm = center(1L, COMPANY_ID, true);
        when(costCenterService.requireDefault(COMPANY_ID)).thenReturn(adm);
        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(LocalDate.of(2026, 11, 22));
        entry.setDescription("Renda");
        entry.addLine(JournalLine.debit(account("6221"), new BigDecimal("500.00"), null));
        entry.addLine(JournalLine.credit(account("1101"), new BigDecimal("500.00"), null));

        JournalEntry saved = journalService.save(entry, COMPANY_ID);

        assertSame(adm, saved.getLines().get(0).getCostCenter());
        assertNull(saved.getLines().get(1).getCostCenter());
    }

    @Test
    void cc04_semLinhasDe6ou7NaoProcuraCentro() {
        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(LocalDate.of(2026, 11, 22));
        entry.setDescription("Depósito");
        entry.addLine(JournalLine.debit(account("1201"), new BigDecimal("300.00"), null));
        entry.addLine(JournalLine.credit(account("1101"), new BigDecimal("300.00"), null));

        assertDoesNotThrow(() -> journalService.save(entry, COMPANY_ID));

        verify(costCenterService, never()).requireDefault(any());
    }

    private static JournalEntry entry(Long companyId) {
        JournalEntry entry = new JournalEntry();
        entry.setCompany(company(companyId));
        entry.setEntryDate(LocalDate.of(2026, 11, 22));
        entry.setDescription("Teste");
        return entry;
    }

    private static JournalLine withCenter(JournalLine line, CostCenter center) {
        line.setCostCenter(center);
        return line;
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

    private static CostCenter center(Long id, Long companyId, boolean active) {
        CostCenter center = new CostCenter();
        center.setId(id);
        center.setCompany(company(companyId));
        center.setCode("C" + id);
        center.setName("Centro " + id);
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
