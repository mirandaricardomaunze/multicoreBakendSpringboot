package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.accounting.dto.*;
import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import mz.multicore.erp.modules.accounting.repository.AccountRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Razão e balancete — as leituras da contabilidade.
 *
 * <p>Serviço separado do {@code JournalService} por responsabilidade: um escreve factos, o
 * outro soma-os. Não repete nenhuma regra: a natureza do saldo vem de {@code AccountNature}.
 */
@Service
public class AccountingReportService {

    private final JournalService journalService;
    private final AccountRepository accountRepository;

    public AccountingReportService(JournalService journalService, AccountRepository accountRepository) {
        this.journalService = journalService;
        this.accountRepository = accountRepository;
    }

    /**
     * Balancete do período: por conta movimentada, os totais a débito e a crédito e o saldo.
     *
     * <p>O campo {@code balanced} não é decoração: se a soma dos débitos não bater com a dos
     * créditos, há lançamentos corrompidos e o relatório tem de o dizer em vez de apresentar
     * números com ar de certos.
     */
    @Transactional(readOnly = true)
    public TrialBalanceDTO getTrialBalance(LocalDate from, LocalDate to) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        List<JournalEntry> entries = journalService.findEntriesBetween(companyId, from, to);

        Map<String, Totals> byAccount = new LinkedHashMap<>();
        for (JournalEntry entry : entries) {
            for (JournalLine line : entry.getLines()) {
                Account account = line.getAccount();
                byAccount.computeIfAbsent(account.getCode(), code -> new Totals(account))
                        .add(line.safeDebit(), line.safeCredit());
            }
        }

        List<TrialBalanceLineDTO> lines = byAccount.values().stream()
                .sorted(Comparator.comparing(totals -> totals.account.getCode()))
                .map(Totals::toLine)
                .toList();

        BigDecimal totalDebit = lines.stream().map(TrialBalanceLineDTO::totalDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCredit = lines.stream().map(TrialBalanceLineDTO::totalCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        return new TrialBalanceDTO(from, to, lines, totalDebit, totalCredit,
                totalDebit.compareTo(totalCredit) == 0);
    }

    /** Extracto de uma conta no período, com saldo de abertura e saldo acumulado por movimento. */
    @Transactional(readOnly = true)
    public LedgerDTO getLedger(String accountCode, LocalDate from, LocalDate to) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        Account account = accountRepository.findByCompanyIdAndCode(companyId, accountCode)
                .orElseThrow(() -> new BusinessRuleException("Conta " + accountCode + " não existe no plano."));

        // Saldo de abertura: tudo o que foi lançado ANTES do período. Sem isto, o extracto de
        // Março começaria do zero e ninguém saberia quanto o cliente já devia a 1 de Março.
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
                        entry.getEntryDate(),
                        entry.getEntryNumber(),
                        entry.getDescription(),
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

    private BigDecimal balanceOf(Account account, LocalDate from, LocalDate to, Long companyId) {
        if (to.isBefore(from)) return BigDecimal.ZERO;
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        for (JournalEntry entry : journalService.findEntriesBetween(companyId, from, to)) {
            for (JournalLine line : entry.getLines()) {
                if (!line.getAccount().getCode().equals(account.getCode())) continue;
                debit = debit.add(line.safeDebit());
                credit = credit.add(line.safeCredit());
            }
        }
        return account.getNature().balanceOf(debit, credit);
    }

    /** Acumulador por conta do balancete. */
    private static final class Totals {
        private final Account account;
        private BigDecimal debit = BigDecimal.ZERO;
        private BigDecimal credit = BigDecimal.ZERO;

        private Totals(Account account) {
            this.account = account;
        }

        private void add(BigDecimal lineDebit, BigDecimal lineCredit) {
            debit = debit.add(lineDebit);
            credit = credit.add(lineCredit);
        }

        private TrialBalanceLineDTO toLine() {
            return new TrialBalanceLineDTO(
                    account.getCode(),
                    account.getName(),
                    account.getAccountClass().label(),
                    account.getNature(),
                    debit.setScale(2, RoundingMode.HALF_UP),
                    credit.setScale(2, RoundingMode.HALF_UP),
                    account.getNature().balanceOf(debit, credit).setScale(2, RoundingMode.HALF_UP));
        }
    }
}
