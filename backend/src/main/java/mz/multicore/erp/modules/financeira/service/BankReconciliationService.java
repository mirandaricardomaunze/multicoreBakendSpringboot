package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Serviço de reconciliação de extractos bancários e conferência de tesouraria.
 */
@Service
public class BankReconciliationService {

    private final BankStatementRepository statementRepository;
    private final BankStatementItemRepository itemRepository;
    private final TreasuryAccountRepository accountRepository;
    private final TreasuryTransactionRepository transactionRepository;
    private final FinanceService financeService;
    private final CompanyService companyService;

    public BankReconciliationService(
            BankStatementRepository statementRepository,
            BankStatementItemRepository itemRepository,
            TreasuryAccountRepository accountRepository,
            TreasuryTransactionRepository transactionRepository,
            FinanceService financeService,
            CompanyService companyService
    ) {
        this.statementRepository = statementRepository;
        this.itemRepository = itemRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.financeService = financeService;
        this.companyService = companyService;
    }

    /**
     * Importa um novo extracto bancário e executa automaticamente o primeiro passe de correspondência.
     */
    @Transactional
    public BankStatementDTO importStatement(ImportBankStatementRequest req) {
        if (req.treasuryAccountId() == null) {
            throw new BusinessRuleException("Conta de tesouraria obrigatória para importar o extracto.");
        }
        if (req.statementReference() == null || req.statementReference().isBlank()) {
            throw new BusinessRuleException("Referência do extracto é obrigatória.");
        }
        if (req.startDate() == null || req.endDate() == null) {
            throw new BusinessRuleException("Período (datas início e fim) obrigatório.");
        }
        if (req.endDate().isBefore(req.startDate())) {
            throw new BusinessRuleException("Data de fim não pode ser anterior à data de início.");
        }
        if (req.items() == null || req.items().isEmpty()) {
            throw new BusinessRuleException("O extracto deve conter pelo menos uma linha de movimento.");
        }

        Long companyId = CurrentUserContext.getCurrentCompanyId();
        TreasuryAccount account = accountRepository.findByIdAndCompanyId(req.treasuryAccountId(), companyId)
                .orElseThrow(() -> new BusinessRuleException("Conta bancária não encontrada na empresa actual."));

        Company company = companyService.getCompanyById(companyId);

        BankStatement statement = new BankStatement();
        statement.setCompany(company);
        statement.setTreasuryAccount(account);
        statement.setStatementReference(req.statementReference().trim());
        statement.setStartDate(req.startDate());
        statement.setEndDate(req.endDate());
        statement.setOpeningBalance(req.openingBalance() != null ? req.openingBalance() : BigDecimal.ZERO);
        statement.setClosingBalance(req.closingBalance() != null ? req.closingBalance() : BigDecimal.ZERO);
        statement.setStatus(BankReconciliationStatus.OPEN);
        statement.setCreatedAt(LocalDateTime.now());
        statement.setCreatedBy(CurrentUserContext.getUsername());

        for (BankStatementItemImportDTO itemReq : req.items()) {
            BankStatementItem item = new BankStatementItem();
            item.setTransactionDate(itemReq.transactionDate() != null ? itemReq.transactionDate() : req.startDate());
            item.setValueDate(itemReq.valueDate());
            item.setDescription(itemReq.description() != null ? itemReq.description().trim() : "(sem descrição)");
            item.setReference(itemReq.reference());
            item.setAmount(itemReq.amount() != null ? itemReq.amount() : BigDecimal.ZERO);
            item.setBalanceAfter(itemReq.balanceAfter());
            item.setStatus(BankStatementItemStatus.UNMATCHED);
            item.setCreatedAt(LocalDateTime.now());
            item.setCreatedBy(CurrentUserContext.getUsername());
            statement.addItem(item);
        }

        statement = statementRepository.save(statement);

        // Executar auto-matching preliminar
        autoMatchInternal(statement);

        return toDTO(statement);
    }

    /**
     * Executa o algoritmo de reconciliação automática para um extracto bancário.
     */
    @Transactional
    public int autoMatch(Long statementId) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        BankStatement statement = statementRepository.findByIdAndCompanyId(statementId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Extracto não encontrado: " + statementId));

        return autoMatchInternal(statement);
    }

    private int autoMatchInternal(BankStatement statement) {
        Long accountId = statement.getTreasuryAccount().getId();
        LocalDateTime windowStart = statement.getStartDate().minusDays(5).atStartOfDay();
        LocalDateTime windowEnd = statement.getEndDate().plusDays(5).atTime(23, 59, 59);

        List<TreasuryTransaction> candidateTransactions = transactionRepository
                .findByTreasuryAccountIdAndTransactionDateBetween(accountId, windowStart, windowEnd);

        // Conjunto de IDs de transações já associadas
        Set<Long> matchedTxIds = new HashSet<>();
        for (BankStatementItem it : statement.getItems()) {
            if (it.getMatchedTransaction() != null) {
                matchedTxIds.add(it.getMatchedTransaction().getId());
            }
        }

        int newlyMatchedCount = 0;

        for (BankStatementItem item : statement.getItems()) {
            if (item.getStatus() == BankStatementItemStatus.MATCHED) {
                continue; // já conciliado
            }

            BigDecimal itemAmountAbs = item.getAmount().abs();
            boolean isCredit = item.getAmount().signum() > 0; // entrada no banco = DEBIT no sistema

            TreasuryTransaction bestMatch = null;

            // Passo 1: Correspondência por Referência + Valor Exato
            if (item.getReference() != null && !item.getReference().isBlank()) {
                String refClean = item.getReference().trim().toLowerCase();
                for (TreasuryTransaction tx : candidateTransactions) {
                    if (matchedTxIds.contains(tx.getId())) continue;

                    boolean typeMatches = (isCredit && tx.getTransactionType() == TransactionType.DEBIT)
                            || (!isCredit && tx.getTransactionType() == TransactionType.CREDIT);

                    if (typeMatches && tx.getAmount().compareTo(itemAmountAbs) == 0) {
                        if (tx.getDescription() != null && tx.getDescription().toLowerCase().contains(refClean)) {
                            bestMatch = tx;
                            break;
                        }
                    }
                }
            }

            // Passo 2: Correspondência por Valor Exato + Data dentro de ±3 dias
            if (bestMatch == null) {
                List<TreasuryTransaction> exactAmountCandidates = new ArrayList<>();
                for (TreasuryTransaction tx : candidateTransactions) {
                    if (matchedTxIds.contains(tx.getId())) continue;

                    boolean typeMatches = (isCredit && tx.getTransactionType() == TransactionType.DEBIT)
                            || (!isCredit && tx.getTransactionType() == TransactionType.CREDIT);

                    if (typeMatches && tx.getAmount().compareTo(itemAmountAbs) == 0) {
                        long daysDiff = Math.abs(ChronoUnit.DAYS.between(
                                item.getTransactionDate(), tx.getTransactionDate().toLocalDate()));
                        if (daysDiff <= 3) {
                            exactAmountCandidates.add(tx);
                        }
                    }
                }

                // Só associa automaticamente se houver correspondência inequívoca (exatamente 1 candidato)
                if (exactAmountCandidates.size() == 1) {
                    bestMatch = exactAmountCandidates.get(0);
                }
            }

            if (bestMatch != null) {
                item.setStatus(BankStatementItemStatus.MATCHED);
                item.setMatchedTransaction(bestMatch);
                item.setNotes("Conciliado automaticamente por correspondência de valor/data");
                matchedTxIds.add(bestMatch.getId());
                newlyMatchedCount++;
            }
        }

        // Atualizar estado global do extracto
        updateStatementStatus(statement);
        statementRepository.save(statement);

        return newlyMatchedCount;
    }

    /**
     * Reconciliação manual de uma linha de extracto com uma transação específica.
     */
    @Transactional
    public BankStatementItemDTO manualMatch(Long statementItemId, Long transactionId) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        BankStatementItem item = itemRepository.findByIdAndBankStatementCompanyId(statementItemId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Linha do extracto não encontrada."));

        TreasuryTransaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessRuleException("Transação de tesouraria não encontrada: " + transactionId));

        item.setStatus(BankStatementItemStatus.MATCHED);
        item.setMatchedTransaction(tx);
        item.setNotes("Conciliado manualmente por " + CurrentUserContext.getUsername());
        item = itemRepository.save(item);

        updateStatementStatus(item.getBankStatement());
        statementRepository.save(item.getBankStatement());

        return toItemDTO(item);
    }

    /**
     * Desfaz a conciliação de uma linha do extracto.
     */
    @Transactional
    public BankStatementItemDTO unmatch(Long statementItemId) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        BankStatementItem item = itemRepository.findByIdAndBankStatementCompanyId(statementItemId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Linha do extracto não encontrada."));

        item.setStatus(BankStatementItemStatus.UNMATCHED);
        item.setMatchedTransaction(null);
        item.setNotes(null);
        item = itemRepository.save(item);

        updateStatementStatus(item.getBankStatement());
        statementRepository.save(item.getBankStatement());

        return toItemDTO(item);
    }

    /**
     * Cria um encargo ou despesa bancária (ex.: comissão de manutenção, imposto de selo) e concilia logo.
     */
    @Transactional
    public BankStatementItemDTO createAndMatchExpense(Long statementItemId, String description, BigDecimal amount) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        BankStatementItem item = itemRepository.findByIdAndBankStatementCompanyId(statementItemId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Linha do extracto não encontrada."));

        BigDecimal expenseAmount = amount != null ? amount : item.getAmount().abs();
        if (expenseAmount.signum() <= 0) {
            throw new BusinessRuleException("Valor do encargo bancário deve ser positivo.");
        }

        String desc = description != null && !description.isBlank()
                ? description.trim()
                : "Encargo Bancário s/ Extracto " + item.getBankStatement().getStatementReference() + " (" + item.getDescription() + ")";

        TreasuryTransactionDTO createdTx = financeService.registerTransaction(
                item.getBankStatement().getTreasuryAccount().getId(),
                "CREDIT", // Saída de tesouraria / Despesa
                expenseAmount,
                desc
        );

        TreasuryTransaction tx = transactionRepository.findById(createdTx.id())
                .orElseThrow(() -> new BusinessRuleException("Falha ao recuperar a transação criada."));

        item.setStatus(BankStatementItemStatus.MATCHED);
        item.setMatchedTransaction(tx);
        item.setNotes("Encargo bancário lançado e conciliado de imediato");
        item = itemRepository.save(item);

        updateStatementStatus(item.getBankStatement());
        statementRepository.save(item.getBankStatement());

        return toItemDTO(item);
    }

    /**
     * Encerra formalmente a reconciliação de um extracto bancário.
     */
    @Transactional
    public BankStatementDTO closeStatement(Long statementId) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        BankStatement statement = statementRepository.findByIdAndCompanyId(statementId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Extracto não encontrado: " + statementId));

        statement.setStatus(BankReconciliationStatus.CLOSED);
        statement.setUpdatedAt(LocalDateTime.now());
        statement = statementRepository.save(statement);

        return toDTO(statement);
    }

    @Transactional(readOnly = true)
    public List<BankStatementDTO> getStatements(Long treasuryAccountId) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        List<BankStatement> list;
        if (treasuryAccountId != null) {
            list = statementRepository.findByCompanyIdAndTreasuryAccountIdOrderByStartDateDesc(companyId, treasuryAccountId);
        } else {
            list = statementRepository.findByCompanyIdOrderByStartDateDesc(companyId);
        }
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BankStatementDTO getStatement(Long statementId) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        BankStatement statement = statementRepository.findByIdAndCompanyId(statementId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Extracto não encontrado: " + statementId));
        return toDTO(statement);
    }

    @Transactional(readOnly = true)
    public List<BankStatementItemDTO> getStatementItems(Long statementId) {
        return itemRepository.findByBankStatementIdOrderByTransactionDateAscIdAsc(statementId).stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BankReconciliationSummaryDTO getSummary(Long treasuryAccountId) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        TreasuryAccount account = accountRepository.findByIdAndCompanyId(treasuryAccountId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Conta bancária não encontrada: " + treasuryAccountId));

        List<BankStatement> statements = statementRepository
                .findByCompanyIdAndTreasuryAccountIdOrderByStartDateDesc(companyId, treasuryAccountId);

        BigDecimal bankBalance = BigDecimal.ZERO;
        int totalPendingItems = 0;
        int openCount = 0;
        LocalDate lastReconciled = null;

        if (!statements.isEmpty()) {
            BankStatement latest = statements.get(0);
            bankBalance = latest.getClosingBalance();

            for (BankStatement st : statements) {
                if (st.getStatus() != BankReconciliationStatus.CLOSED) {
                    openCount++;
                    for (BankStatementItem it : st.getItems()) {
                        if (it.getStatus() == BankStatementItemStatus.UNMATCHED) {
                            totalPendingItems++;
                        }
                    }
                } else if (lastReconciled == null) {
                    lastReconciled = st.getEndDate();
                }
            }
        }

        BigDecimal systemBalance = account.getBalance();
        BigDecimal diff = systemBalance.subtract(bankBalance);

        return new BankReconciliationSummaryDTO(
                account.getId(),
                account.getName(),
                bankBalance,
                systemBalance,
                diff,
                totalPendingItems,
                openCount,
                lastReconciled
        );
    }

    private void updateStatementStatus(BankStatement statement) {
        if (statement.getStatus() == BankReconciliationStatus.CLOSED) return;

        boolean hasUnmatched = false;
        for (BankStatementItem it : statement.getItems()) {
            if (it.getStatus() == BankStatementItemStatus.UNMATCHED) {
                hasUnmatched = true;
                break;
            }
        }

        if (!hasUnmatched && statement.difference().signum() == 0) {
            statement.setStatus(BankReconciliationStatus.RECONCILED);
        } else {
            statement.setStatus(BankReconciliationStatus.OPEN);
        }
    }

    private BankStatementDTO toDTO(BankStatement st) {
        int total = st.getItems().size();
        int matched = 0;
        int unmatched = 0;
        for (BankStatementItem it : st.getItems()) {
            if (it.getStatus() == BankStatementItemStatus.MATCHED) matched++;
            else if (it.getStatus() == BankStatementItemStatus.UNMATCHED) unmatched++;
        }

        return new BankStatementDTO(
                st.getId(),
                st.getTreasuryAccount().getId(),
                st.getTreasuryAccount().getName(),
                st.getStatementReference(),
                st.getStartDate(),
                st.getEndDate(),
                st.getOpeningBalance(),
                st.getClosingBalance(),
                st.calculatedBalance(),
                st.difference(),
                st.getStatus(),
                total,
                matched,
                unmatched,
                st.getCreatedAt()
        );
    }

    private BankStatementItemDTO toItemDTO(BankStatementItem it) {
        Long matchedTxId = it.getMatchedTransaction() != null ? it.getMatchedTransaction().getId() : null;
        return new BankStatementItemDTO(
                it.getId(),
                it.getBankStatement().getId(),
                it.getTransactionDate(),
                it.getValueDate(),
                it.getDescription(),
                it.getReference(),
                it.getAmount(),
                it.getBalanceAfter(),
                it.getStatus(),
                matchedTxId,
                it.getNotes()
        );
    }
}
