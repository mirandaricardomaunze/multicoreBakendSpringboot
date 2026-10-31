package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.financeira.dto.BankStatementItemDTO;
import mz.multicore.erp.modules.financeira.dto.TreasuryTransactionDTO;
import mz.multicore.erp.modules.financeira.model.BankReconciliationStatus;
import mz.multicore.erp.modules.financeira.model.BankStatement;
import mz.multicore.erp.modules.financeira.model.BankStatementItem;
import mz.multicore.erp.modules.financeira.model.BankStatementItemStatus;
import mz.multicore.erp.modules.financeira.model.TransactionType;
import mz.multicore.erp.modules.financeira.model.TreasuryTransaction;
import mz.multicore.erp.modules.financeira.repository.BankStatementItemRepository;
import mz.multicore.erp.modules.financeira.repository.BankStatementRepository;
import mz.multicore.erp.modules.financeira.repository.TreasuryTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Motor de conciliação automática, manual e tratamento de encargos bancários.
 */
@Service
public class BankReconciliationMatcher {

    private final BankStatementRepository statementRepository;
    private final BankStatementItemRepository itemRepository;
    private final TreasuryTransactionRepository transactionRepository;
    private final FinanceService financeService;

    public BankReconciliationMatcher(
            BankStatementRepository statementRepository,
            BankStatementItemRepository itemRepository,
            TreasuryTransactionRepository transactionRepository,
            FinanceService financeService
    ) {
        this.statementRepository = statementRepository;
        this.itemRepository = itemRepository;
        this.transactionRepository = transactionRepository;
        this.financeService = financeService;
    }

    @Transactional
    public int autoMatch(Long statementId) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        BankStatement statement = statementRepository.findByIdAndCompanyId(statementId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Extracto não encontrado: " + statementId));

        return autoMatchInternal(statement);
    }

    public int autoMatchInternal(BankStatement statement) {
        Long accountId = statement.getTreasuryAccount().getId();
        LocalDateTime windowStart = statement.getStartDate().minusDays(5).atStartOfDay();
        LocalDateTime windowEnd = statement.getEndDate().plusDays(5).atTime(23, 59, 59);

        List<TreasuryTransaction> candidateTransactions = transactionRepository
                .findByTreasuryAccountIdAndTransactionDateBetween(accountId, windowStart, windowEnd);

        Set<Long> matchedTxIds = new HashSet<>();
        for (BankStatementItem it : statement.getItems()) {
            if (it.getMatchedTransaction() != null) {
                matchedTxIds.add(it.getMatchedTransaction().getId());
            }
        }

        int newlyMatchedCount = 0;

        for (BankStatementItem item : statement.getItems()) {
            if (item.getStatus() == BankStatementItemStatus.MATCHED) {
                continue;
            }

            BigDecimal itemAmountAbs = item.getAmount().abs();
            boolean isCredit = item.getAmount().signum() > 0;

            TreasuryTransaction bestMatch = null;

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

        updateStatementStatus(statement);
        statementRepository.save(statement);

        return newlyMatchedCount;
    }

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

        return BankReconciliationQueryService.toItemDTO(item);
    }

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

        return BankReconciliationQueryService.toItemDTO(item);
    }

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
                "CREDIT",
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

        return BankReconciliationQueryService.toItemDTO(item);
    }

    public static void updateStatementStatus(BankStatement statement) {
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
}
