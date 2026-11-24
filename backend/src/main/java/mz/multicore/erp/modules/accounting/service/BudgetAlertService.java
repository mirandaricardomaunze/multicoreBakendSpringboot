package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.events.JournalEntryPostedEvent;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.architecture.warnings.OperationWarnings;
import mz.multicore.erp.modules.accounting.dto.BudgetAlertDTO;
import mz.multicore.erp.modules.accounting.model.AccountClass;
import mz.multicore.erp.modules.accounting.model.Budget;
import mz.multicore.erp.modules.accounting.model.BudgetAlert;
import mz.multicore.erp.modules.accounting.model.BudgetLine;
import mz.multicore.erp.modules.accounting.model.CostCenter;
import mz.multicore.erp.modules.accounting.repository.BudgetAlertRepository;
import mz.multicore.erp.modules.accounting.repository.BudgetRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Budget alerts (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.3, OR-05): when the actual costs (class 6) of a cost centre
 * in a month reach 90% and 100% of its budgeted costs, one alert is stored per threshold, centre and month and is never
 * raised again. Evaluated on every posting with class 6 lines; listed for ADMIN and MANAGER (the bell).
 * Each new alert is also a non-blocking warning of the operation that posted it — a purchase, a staff expense —
 * returned with its response once committed (OR-06).
 */
@Service
public class BudgetAlertService {

    static final List<Integer> THRESHOLDS = List.of(90, 100);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BudgetRepository budgetRepository;
    private final BudgetAlertRepository alertRepository;
    private final BudgetActualsCalculator actualsCalculator;

    public BudgetAlertService(BudgetRepository budgetRepository, BudgetAlertRepository alertRepository,
                              BudgetActualsCalculator actualsCalculator) {
        this.budgetRepository = budgetRepository;
        this.alertRepository = alertRepository;
        this.actualsCalculator = actualsCalculator;
    }

    /**
     * Runs inside the posting transaction: the alert is born with the entry that caused it, and the operation still
     * saves — its new alerts only come back as warnings (OR-06).
     */
    @EventListener
    public void onJournalEntryPosted(JournalEntryPostedEvent event) {
        int year = event.entryDate().getYear();
        int month = event.entryDate().getMonthValue();
        event.costCenterIds().stream()
                .flatMap(centerId -> evaluate(event.companyId(), centerId, year, month).stream())
                .forEach(alert -> OperationWarnings.add(alert.message()));
    }

    /** Raises the thresholds the centre reached in the month and has not been alerted for yet; returns the new ones. */
    @Transactional
    public List<BudgetAlertDTO> evaluate(Long companyId, Long costCenterId, int year, int month) {
        Optional<Budget> budget = budgetRepository.findByCompanyIdAndFiscalYear(companyId, year);
        List<BudgetLine> costCells = budget.map(Budget::getLines).orElse(List.of()).stream()
                .filter(cell -> cell.getMonth() == month)
                .filter(cell -> Objects.equals(cell.getCostCenter().getId(), costCenterId))
                .filter(cell -> cell.getAccount().getAccountClass() == AccountClass.CUSTOS_E_PERDAS)
                .toList();
        BigDecimal budgeted = costCells.stream().map(BudgetLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (budgeted.signum() <= 0) return List.of();

        BigDecimal actual = actualsCalculator.actualsOfMonth(companyId, year, month).stream()
                .filter(cell -> Objects.equals(cell.costCenter().getId(), costCenterId))
                .filter(cell -> cell.account().getAccountClass() == AccountClass.CUSTOS_E_PERDAS)
                .map(BudgetActualsCalculator.ActualCell::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CostCenter center = costCells.get(0).getCostCenter();
        List<BudgetAlertDTO> raised = new ArrayList<>();
        for (int threshold : THRESHOLDS) {
            boolean reached = actual.multiply(HUNDRED).compareTo(budgeted.multiply(BigDecimal.valueOf(threshold))) >= 0;
            if (!reached || alertRepository.existsByCompanyIdAndCostCenterIdAndFiscalYearAndMonthAndThresholdPercent(
                    companyId, costCenterId, year, month, threshold)) {
                continue;
            }
            BudgetAlert alert = new BudgetAlert();
            alert.setCompany(budget.get().getCompany());
            alert.setCostCenter(center);
            alert.setFiscalYear(year);
            alert.setMonth(month);
            alert.setThresholdPercent(threshold);
            alert.setBudgetedAmount(budgeted);
            alert.setActualAmount(actual);
            raised.add(toDTO(alertRepository.save(alert)));
        }
        return raised;
    }

    /** Alerts of the active company in a year, latest month and highest threshold first. */
    @Transactional(readOnly = true)
    public List<BudgetAlertDTO> listAlerts(int year) {
        PermissionGuard.requireManagerOrAdmin("consultar os alertas de orçamento");
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return alertRepository.findByCompanyIdAndFiscalYearOrderByMonthDescThresholdPercentDescIdDesc(companyId, year)
                .stream().map(BudgetAlertService::toDTO).toList();
    }

    private static BudgetAlertDTO toDTO(BudgetAlert alert) {
        CostCenter center = alert.getCostCenter();
        String message = "Centro " + center.getCode() + " — " + center.getName() + " atingiu "
                + alert.getThresholdPercent() + "% do orçamento de custos de "
                + String.format("%02d/%d", alert.getMonth(), alert.getFiscalYear())
                + " (real " + alert.getActualAmount().toPlainString() + " MT de "
                + alert.getBudgetedAmount().toPlainString() + " MT orçados).";
        return new BudgetAlertDTO(alert.getId(), center.getId(), center.getCode(), center.getName(),
                alert.getFiscalYear(), alert.getMonth(), alert.getThresholdPercent(),
                alert.getBudgetedAmount(), alert.getActualAmount(), message, alert.getCreatedAt());
    }
}
