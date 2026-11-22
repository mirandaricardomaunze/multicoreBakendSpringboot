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

/**
 * Aggregates posted journal lines into per-account debit and credit totals, feeding the trial balance, the
 * ledger of a single account and the financial statements.
 */
@Component
public class LedgerAndTrialBalanceCalculator {

    private final JournalService journalService;
    private final AccountRepository accountRepository;

    public LedgerAndTrialBalanceCalculator(JournalService journalService, AccountRepository accountRepository) {
        this.journalService = journalService;
        this.accountRepository = accountRepository;
    }

    public TrialBalanceDTO getTrialBalance(LocalDate from, LocalDate to) {
        return getTrialBalance(from, to, null);
    }

    /** Balancete; com {@code costCenterId} só conta as partidas desse centro (CC-08), vazio dá o completo. */
    public TrialBalanceDTO getTrialBalance(LocalDate from, LocalDate to, Long costCenterId) {
        List<TrialBalanceLineDTO> lines = totalsBetween(from, to, costCenterId).values().stream()
                .sorted(Comparator.comparing(totals -> totals.account().getCode()))
                .map(Totals::toLine)
                .toList();

        BigDecimal totalDebit = lines.stream().map(TrialBalanceLineDTO::totalDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCredit = lines.stream().map(TrialBalanceLineDTO::totalCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        return new TrialBalanceDTO(from, to, lines, totalDebit, totalCredit, totalDebit.compareTo(totalCredit) == 0);
    }

    /** Razão da conta; com {@code costCenterId} só as partidas desse centro, também no saldo inicial (CC-08). */
    public LedgerDTO getLedger(String accountCode, LocalDate from, LocalDate to, Long costCenterId) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        Account account = accountRepository.findByCompanyIdAndCode(companyId, accountCode)
                .orElseThrow(() -> new BusinessRuleException("Conta " + accountCode + " não existe no plano."));

        BigDecimal opening = openingBalance(account, from, costCenterId);

        List<LedgerMovementDTO> movements = new ArrayList<>();
        BigDecimal running = opening;
        BigDecimal totalDebit = BigDecimal.ZERO, totalCredit = BigDecimal.ZERO;

        for (JournalEntry entry : journalService.findEntriesBetween(companyId, from, to)) {
            for (JournalLine line : entry.getLines()) {
                if (!line.getAccount().getCode().equals(accountCode) || !inCenter(line, costCenterId)) continue;
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

    /** Totais por conta no período; {@code costCenterId} vazio conta todas as partidas (CC-08). */
    public Map<String, Totals> totalsBetween(LocalDate from, LocalDate to, Long costCenterId) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        Map<String, Totals> totals = new LinkedHashMap<>();
        for (JournalEntry entry : journalService.findEntriesBetween(companyId, from, to)) {
            for (JournalLine line : entry.getLines()) {
                if (!inCenter(line, costCenterId)) continue;
                totals.computeIfAbsent(line.getAccount().getCode(), ignored -> new Totals(line.getAccount()))
                        .add(line.safeDebit(), line.safeCredit());
            }
        }
        return totals;
    }

    /** Sem filtro ({@code null}) todas as partidas contam; com filtro, só as marcadas com esse centro. */
    private static boolean inCenter(JournalLine line, Long costCenterId) {
        return costCenterId == null
                || (line.getCostCenter() != null && costCenterId.equals(line.getCostCenter().getId()));
    }

    private BigDecimal openingBalance(Account account, LocalDate from, Long costCenterId) {
        Totals before = totalsBetween(LocalDate.of(1970, 1, 1), from.minusDays(1), costCenterId).get(account.getCode());
        return before == null ? BigDecimal.ZERO : before.balance();
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

        public BigDecimal balance() {
            return account.getNature().balanceOf(debit, credit);
        }

        public TrialBalanceLineDTO toLine() {
            return new TrialBalanceLineDTO(
                    account.getCode(), account.getName(), account.getAccountClass().label(),
                    account.getNature(), debit.setScale(2, RoundingMode.HALF_UP),
                    credit.setScale(2, RoundingMode.HALF_UP), balance().setScale(2, RoundingMode.HALF_UP));
        }

        public FinancialStatementLineDTO toStatementLine() {
            return new FinancialStatementLineDTO(account.getCode(), account.getName(),
                    balance().setScale(2, RoundingMode.HALF_UP));
        }
    }
}
