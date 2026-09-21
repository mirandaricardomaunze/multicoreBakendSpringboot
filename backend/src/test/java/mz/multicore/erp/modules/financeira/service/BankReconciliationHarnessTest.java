package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.financeira.dto.BankReconciliationSummaryDTO;
import mz.multicore.erp.modules.financeira.dto.BankStatementDTO;
import mz.multicore.erp.modules.financeira.dto.BankStatementItemDTO;
import mz.multicore.erp.modules.financeira.dto.BankStatementItemImportDTO;
import mz.multicore.erp.modules.financeira.dto.ImportBankStatementRequest;
import mz.multicore.erp.modules.financeira.dto.TreasuryTransactionDTO;
import mz.multicore.erp.modules.financeira.model.BankReconciliationStatus;
import mz.multicore.erp.modules.financeira.model.BankStatement;
import mz.multicore.erp.modules.financeira.model.BankStatementItem;
import mz.multicore.erp.modules.financeira.model.BankStatementItemStatus;
import mz.multicore.erp.modules.financeira.model.TransactionType;
import mz.multicore.erp.modules.financeira.model.TreasuryAccount;
import mz.multicore.erp.modules.financeira.model.TreasuryTransaction;
import mz.multicore.erp.modules.financeira.repository.BankStatementItemRepository;
import mz.multicore.erp.modules.financeira.repository.BankStatementRepository;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.financeira.repository.TreasuryTransactionRepository;
import mz.multicore.erp.modules.printing.BankReconciliationPrintService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes de cobertura do Centro de Reconciliação Bancária & Importação de Extractos.
 */
class BankReconciliationHarnessTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;

    private BankStatementRepository statementRepository;
    private BankStatementItemRepository itemRepository;
    private TreasuryAccountRepository accountRepository;
    private TreasuryTransactionRepository transactionRepository;
    private FinanceService financeService;
    private CompanyService companyService;

    private BankReconciliationService service;
    private BankReconciliationPrintService printService;
    private Company company;
    private TreasuryAccount account;

    @BeforeEach
    void setUp() {
        statementRepository = mock(BankStatementRepository.class);
        itemRepository = mock(BankStatementItemRepository.class);
        accountRepository = mock(TreasuryAccountRepository.class);
        transactionRepository = mock(TreasuryTransactionRepository.class);
        financeService = mock(FinanceService.class);
        companyService = mock(CompanyService.class);

        company = new Company();
        company.setId(COMPANY_ID);
        company.setName("Multicore Enterprise Lda");
        company.setTaxId("400123999");
        when(companyService.getCompanyById(COMPANY_ID)).thenReturn(company);

        account = new TreasuryAccount();
        account.setId(ACCOUNT_ID);
        account.setName("Millennium BIM Conta Principal");
        account.setAccountNumber("00010000012345");
        account.setBalance(new BigDecimal("125000.00"));
        account.setCompany(company);

        when(accountRepository.findByIdAndCompanyId(ACCOUNT_ID, COMPANY_ID)).thenReturn(Optional.of(account));

        service = new BankReconciliationService(
                statementRepository,
                itemRepository,
                accountRepository,
                transactionRepository,
                financeService,
                companyService
        );

        printService = new BankReconciliationPrintService(service, companyService);

        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("tesoureiro", "USER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void importStatement_persistsAndCalculatesCorrectBalances() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 15);

        List<BankStatementItemImportDTO> items = List.of(
                new BankStatementItemImportDTO(LocalDate.of(2026, 9, 2), null, "Depósito Cheque Cliente A", "FT-001", new BigDecimal("50000.00"), new BigDecimal("150000.00")),
                new BankStatementItemImportDTO(LocalDate.of(2026, 9, 5), null, "Pagamento Fornecedor B", "PG-002", new BigDecimal("-25000.00"), new BigDecimal("125000.00"))
        );

        ImportBankStatementRequest req = new ImportBankStatementRequest(
                ACCOUNT_ID, "EXT-2026-09-01", start, end,
                new BigDecimal("100000.00"), new BigDecimal("125000.00"), items
        );

        when(statementRepository.save(any(BankStatement.class))).thenAnswer(inv -> {
            BankStatement st = inv.getArgument(0);
            st.setId(100L);
            long idGen = 1L;
            for (BankStatementItem it : st.getItems()) {
                it.setId(idGen++);
            }
            return st;
        });

        BankStatementDTO dto = service.importStatement(req);

        assertNotNull(dto);
        assertEquals(100L, dto.id());
        assertEquals("EXT-2026-09-01", dto.statementReference());
        assertEquals(new BigDecimal("100000.00"), dto.openingBalance());
        assertEquals(new BigDecimal("125000.00"), dto.closingBalance());
        assertEquals(new BigDecimal("125000.00"), dto.calculatedBalance());
        assertEquals(0, dto.difference().compareTo(BigDecimal.ZERO));
        assertEquals(2, dto.totalItems());
    }

    @Test
    void autoMatch_associatesTransactionsByExactAmountAndDateWindow() {
        BankStatement st = new BankStatement();
        st.setId(101L);
        st.setCompany(company);
        st.setTreasuryAccount(account);
        st.setStartDate(LocalDate.of(2026, 9, 1));
        st.setEndDate(LocalDate.of(2026, 9, 15));
        st.setOpeningBalance(new BigDecimal("10000.00"));
        st.setClosingBalance(new BigDecimal("25000.00"));

        BankStatementItem item = new BankStatementItem();
        item.setId(201L);
        item.setTransactionDate(LocalDate.of(2026, 9, 5));
        item.setAmount(new BigDecimal("15000.00")); // entrada
        item.setDescription("Transferência Recebida de Cliente");
        item.setStatus(BankStatementItemStatus.UNMATCHED);
        st.addItem(item);

        when(statementRepository.findByIdAndCompanyId(101L, COMPANY_ID)).thenReturn(Optional.of(st));

        // Transação no sistema com mesmo valor na janela de datas
        TreasuryTransaction tx = new TreasuryTransaction();
        tx.setId(501L);
        tx.setTransactionType(TransactionType.DEBIT); // entrada de fundos
        tx.setAmount(new BigDecimal("15000.00"));
        tx.setDescription("Recebimento Fatura FT-99");
        tx.setTransactionDate(LocalDate.of(2026, 9, 6).atTime(10, 0)); // +1 dia

        when(transactionRepository.findByTreasuryAccountIdAndTransactionDateBetween(eq(ACCOUNT_ID), any(), any()))
                .thenReturn(List.of(tx));

        int matchedCount = service.autoMatch(101L);

        assertEquals(1, matchedCount);
        assertEquals(BankStatementItemStatus.MATCHED, item.getStatus());
        assertEquals(tx, item.getMatchedTransaction());
        assertEquals(BankReconciliationStatus.RECONCILED, st.getStatus());
    }

    @Test
    void createAndMatchExpense_registersTransactionAndReconcilesItem() {
        BankStatement st = new BankStatement();
        st.setId(102L);
        st.setCompany(company);
        st.setTreasuryAccount(account);
        st.setOpeningBalance(BigDecimal.ZERO);
        st.setClosingBalance(BigDecimal.ZERO);

        BankStatementItem item = new BankStatementItem();
        item.setId(202L);
        item.setBankStatement(st);
        item.setAmount(new BigDecimal("-150.00")); // Saída/Comissão
        item.setDescription("Comissão Manutenção de Conta");
        item.setStatus(BankStatementItemStatus.UNMATCHED);

        when(itemRepository.findByIdAndBankStatementCompanyId(202L, COMPANY_ID)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(BankStatementItem.class))).thenAnswer(inv -> inv.getArgument(0));

        TreasuryTransactionDTO createdDto = new TreasuryTransactionDTO(
                701L, ACCOUNT_ID, "Conta Millennium", "CREDIT", new BigDecimal("150.00"), "Comissão Manutenção de Conta", LocalDateTime.now()
        );
        when(financeService.registerTransaction(eq(ACCOUNT_ID), eq("CREDIT"), eq(new BigDecimal("150.00")), any()))
                .thenReturn(createdDto);

        TreasuryTransaction savedTx = new TreasuryTransaction();
        savedTx.setId(701L);
        savedTx.setAmount(new BigDecimal("150.00"));
        savedTx.setTransactionType(TransactionType.CREDIT);
        when(transactionRepository.findById(701L)).thenReturn(Optional.of(savedTx));

        BankStatementItemDTO result = service.createAndMatchExpense(202L, "Comissão Manutenção de Conta", new BigDecimal("150.00"));

        assertNotNull(result);
        assertEquals(BankStatementItemStatus.MATCHED, result.status());
        assertEquals(701L, result.matchedTransactionId());
    }

    @Test
    void getSummaryAndRenderPdfReport_succeeds() {
        BankStatement st = new BankStatement();
        st.setId(103L);
        st.setCompany(company);
        st.setTreasuryAccount(account);
        st.setStatementReference("BIM-2026-SETEMBRO");
        st.setStartDate(LocalDate.of(2026, 9, 1));
        st.setEndDate(LocalDate.of(2026, 9, 15));
        st.setOpeningBalance(new BigDecimal("100000.00"));
        st.setClosingBalance(new BigDecimal("125000.00"));
        st.setStatus(BankReconciliationStatus.OPEN);

        BankStatementItem item = new BankStatementItem();
        item.setId(203L);
        item.setBankStatement(st);
        item.setTransactionDate(LocalDate.of(2026, 9, 10));
        item.setDescription("Recebimento Transferência");
        item.setAmount(new BigDecimal("25000.00"));
        item.setStatus(BankStatementItemStatus.MATCHED);
        st.addItem(item);

        when(statementRepository.findByCompanyIdAndTreasuryAccountIdOrderByStartDateDesc(COMPANY_ID, ACCOUNT_ID))
                .thenReturn(List.of(st));
        when(statementRepository.findByIdAndCompanyId(103L, COMPANY_ID)).thenReturn(Optional.of(st));
        when(itemRepository.findByBankStatementIdOrderByTransactionDateAscIdAsc(103L)).thenReturn(List.of(item));

        BankReconciliationSummaryDTO summary = service.getSummary(ACCOUNT_ID);
        assertNotNull(summary);
        assertEquals(new BigDecimal("125000.00"), summary.bankBalance());
        assertEquals(new BigDecimal("125000.00"), summary.systemBalance());
        assertEquals(0, summary.difference().compareTo(BigDecimal.ZERO));

        // Testar renderização do PDF oficial em OpenPDF
        byte[] pdfBytes = printService.render(COMPANY_ID, 103L);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }
}
