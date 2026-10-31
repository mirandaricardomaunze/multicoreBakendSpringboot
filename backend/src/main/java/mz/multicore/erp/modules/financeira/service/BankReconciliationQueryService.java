package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.financeira.dto.BankReconciliationSummaryDTO;
import mz.multicore.erp.modules.financeira.dto.BankStatementDTO;
import mz.multicore.erp.modules.financeira.dto.BankStatementItemDTO;
import mz.multicore.erp.modules.financeira.model.BankReconciliationStatus;
import mz.multicore.erp.modules.financeira.model.BankStatement;
import mz.multicore.erp.modules.financeira.model.BankStatementItem;
import mz.multicore.erp.modules.financeira.model.BankStatementItemStatus;
import mz.multicore.erp.modules.financeira.model.TreasuryAccount;
import mz.multicore.erp.modules.financeira.repository.BankStatementItemRepository;
import mz.multicore.erp.modules.financeira.repository.BankStatementRepository;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Consultas analíticas, resumos executivos de tesouraria e mapeamento DTO de reconciliação bancária.
 */
@Service
public class BankReconciliationQueryService {

    private final BankStatementRepository statementRepository;
    private final BankStatementItemRepository itemRepository;
    private final TreasuryAccountRepository accountRepository;

    public BankReconciliationQueryService(
            BankStatementRepository statementRepository,
            BankStatementItemRepository itemRepository,
            TreasuryAccountRepository accountRepository
    ) {
        this.statementRepository = statementRepository;
        this.itemRepository = itemRepository;
        this.accountRepository = accountRepository;
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
        return list.stream().map(BankReconciliationQueryService::toDTO).collect(Collectors.toList());
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
                .map(BankReconciliationQueryService::toItemDTO)
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

    public static BankStatementDTO toDTO(BankStatement st) {
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

    public static BankStatementItemDTO toItemDTO(BankStatementItem it) {
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
