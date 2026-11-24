package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.model.CostCenter;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Actual of a year per account × cost centre × month (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.2): the sum of the
 * posted journal lines of classes 6 and 7 that carry a centre, signed as debit − credit for class 6 and
 * credit − debit for class 7. Lines without a centre cannot be compared with a budget cell and are left out.
 */
@Component
public class BudgetActualsCalculator {

    private final JournalService journalService;

    public BudgetActualsCalculator(JournalService journalService) {
        this.journalService = journalService;
    }

    public Collection<ActualCell> actualsOf(Long companyId, int year) {
        Map<String, ActualCell> cells = new LinkedHashMap<>();
        for (JournalEntry entry : journalService.findEntriesBetween(companyId,
                LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))) {
            int month = entry.getEntryDate().getMonthValue();
            for (JournalLine line : entry.getLines()) {
                CostCenter center = line.getCostCenter();
                int digit = line.getAccount().getAccountClass().digit();
                if (center == null || (digit != 6 && digit != 7)) continue;
                BigDecimal signed = digit == 6
                        ? line.safeDebit().subtract(line.safeCredit())
                        : line.safeCredit().subtract(line.safeDebit());
                String key = line.getAccount().getCode() + "|" + center.getId() + "|" + month;
                cells.merge(key, new ActualCell(line.getAccount(), center, month, signed),
                        (current, added) -> current.plus(added.amount()));
            }
        }
        return cells.values();
    }

    /** Actual amount of one account × centre × month. */
    public record ActualCell(Account account, CostCenter costCenter, int month, BigDecimal amount) {
        ActualCell plus(BigDecimal more) {
            return new ActualCell(account, costCenter, month, amount.add(more));
        }
    }
}
