package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.accounting.dto.FinancialStatementLineDTO;
import mz.multicore.erp.modules.accounting.dto.LedgerDTO;
import mz.multicore.erp.modules.accounting.dto.LedgerMovementDTO;
import mz.multicore.erp.modules.accounting.dto.TrialBalanceDTO;
import mz.multicore.erp.modules.accounting.dto.TrialBalanceLineDTO;
import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.repository.AccountRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class LedgerAndTrialBalanceCalculator {

    private final JournalService journalService;
    private final AccountRepository accountRepository;

    public LedgerAndTrialBalanceCalculator(JournalService journalService, AccountRepository accountRepository) {
        this.journalService = journalService;
        this.accountRepository = accountRepository;
    }

    public TrialBalanceDTO getTrialBalance(LocalDate from, LocalDate to) {
        Map<String, Totals> byAccount = totalsBetween(from, to);

        List<TrialBalanceLineDTO> lines = byAccount.values().stream()
                .sorted(Comparator.comparing(totals -> totals.account().getCode()))
                .map(Totals::toLine)
                .toList();

        BigDecimal totalDebit = lines.stream().map(TrialBalanceLineDTO::totalDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCredit = lines.stream().map(TrialBalanceLineDTO::totalCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        return new TrialBalanceDTO(from, to, lines, totalDebit, totalCredit, totalDebit.compareTo(totalCredit) == 0);
    }

    public LedgerDTO getLedger(String accountCode, LocalDate from, LocalDate to) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        Account account = accountRepository.findByCompanyIdAndCode(companyId, accountCode)
                .orElseThrow(() -> new BusinessRuleException("Conta " + accountCode + " não existe no plano."));

        BigDecimal opening = balanceOf(account, LocalDate.of(1970, 1, 1), from.minusDays(1), companyId);

        List<LedgerMovementDTO> movements = new ArrayList<>();
        BigDecimal running = opening;
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (JournalEntry entry : journalService.findEntriesBetween(companyId, from, to)) {
            for (JournalLine line : entry.getLines()) {
                if (!line.getAccount().getCode().equals(accountCode)) continue;
                totalDebit = totalDebit.add(line.safeDebit());
                totalCredit = totalCredit.add(line.safeCredit());
                running = running.add(account.getNature().balanceOf(line.safeDebit(), line.safeCredit()));
                movements.add(new LedgerMovementDTO(
                        entry.getEntryDate(), entry.getEntryNumber(), entry.getDescription(),
                        entry.getSourceDocumentNumber(),
                        line.safeDebit().setScale(2, RoundingMode.HALF_UP),
                        line.safeCredit().setScale(2, RoundingMode.HALF_UP),
                        running.setScale(2, RoundingMode.HALF_UP)));
            }
        }

        return new LedgerDTO(account.getCode(), account.getName(), from, to,
                opening.setScale(2, RoundingMode.HALF_UP), movements,
                totalDebit.setScale(2, RoundingMode.HALF_UP),
                totalCredit.setScale(2, RoundingMode.HALF_UP),
                running.setScale(2, RoundingMode.HALF_UP));
    }

    public Map<String, Totals> totalsBetween(LocalDate from, LocalDate to) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        Map<String, Totals> totals = new LinkedHashMap<>();
        for (JournalEntry entry : journalService.findEntriesBetween(companyId, from, to)) {
            for (JournalLine line : entry.getLines()) {
                totals.computeIfAbsent(line.getAccount().getCode(), ignored -> new Totals(line.getAccount()))
                        .add(line.safeDebit(), line.safeCredit());
            }
        }
        return totals;
    }

    private BigDecimal balanceOf(Account account, LocalDate from, LocalDate to, Long companyId) {
        if (to.isBefore(from)) return BigDecimal.ZERO;
        BigDecimal debit = BigDecimal.ZERO, credit = BigDecimal.ZERO;
        for (JournalEntry entry : journalService.findEntriesBetween(companyId, from, to)) {
            for (JournalLine line : entry.getLines()) {
                if (!line.getAccount().getCode().equals(account.getCode())) continue;
                debit = debit.add(line.safeDebit());
                credit = credit.add(line.safeCredit());
            }
        }
        return account.getNature().balanceOf(debit, credit);
    }

    public static final class Totals {
        private final Account account;
        private BigDecimal debit = BigDecimal.ZERO;
        private BigDecimal credit = BigDecimal.ZERO;

        public Totals(Account account) {
            this.account = account;
        }

        public Account account() {
            return account;
        }

        public void add(BigDecimal lineDebit, BigDecimal lineCredit) {
            debit = debit.add(lineDebit);
            credit = credit.add(lineCredit);
        }

        public TrialBalanceLineDTO toLine() {
            return new TrialBalanceLineDTO(
                    account.getCode(), account.getName(), account.getAccountClass().label(),
                    account.getNature(), debit.setScale(2, RoundingMode.HALF_UP),
                    credit.setScale(2, RoundingMode.HALF_UP),
                    account.getNature().balanceOf(debit, credit).setScale(2, RoundingMode.HALF_UP));
        }

        public FinancialStatementLineDTO toStatementLine() {
            return new FinancialStatementLineDTO(account.getCode(), account.getName(),
                    account.getNature().balanceOf(debit, credit).setScale(2, RoundingMode.HALF_UP));
        }
    }
}
