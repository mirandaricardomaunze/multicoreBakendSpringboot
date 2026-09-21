package mz.multicore.erp.modules.performance.service;

import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.performance.dto.SalesGoalProgressDTO;
import mz.multicore.erp.modules.performance.model.AlertLevel;
import mz.multicore.erp.modules.performance.model.GoalScope;
import mz.multicore.erp.modules.performance.model.SalesGoal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class GoalProgressEngine {

    private final InvoiceRepository invoiceRepository;
    private final EmployeeRepository employeeRepository;
    private final BonusCalculatorEngine bonusCalculatorEngine;

    public GoalProgressEngine(InvoiceRepository invoiceRepository,
                              EmployeeRepository employeeRepository,
                              BonusCalculatorEngine bonusCalculatorEngine) {
        this.invoiceRepository = invoiceRepository;
        this.employeeRepository = employeeRepository;
        this.bonusCalculatorEngine = bonusCalculatorEngine;
    }

    public SalesGoalProgressDTO calculateProgress(SalesGoal goal, LocalDate asOfDate) {
        LocalDate today = asOfDate != null ? asOfDate : LocalDate.now();
        Long companyId = goal.getCompany().getId();
        LocalDateTime from = goal.getPeriodStart().atStartOfDay();
        LocalDateTime to = goal.getPeriodEnd().atTime(LocalTime.MAX);

        List<Invoice> invoices = invoiceRepository.findByCompanyIdAndCreatedAtBetween(companyId, from, to);

        // Filter realised sales only (CDC-BIZ-12)
        List<Invoice> realisedSales = invoices.stream()
                .filter(inv -> inv.getStatus() != null && inv.getStatus().isRealisedSale())
                .toList();

        // If scope is EMPLOYEE, find employee identifier to match createdBy
        Employee targetEmployee = null;
        if (goal.getScope() == GoalScope.EMPLOYEE && goal.getScopeRefId() != null) {
            targetEmployee = employeeRepository.findById(goal.getScopeRefId()).orElse(null);
        }

        BigDecimal currentRevenue = BigDecimal.ZERO;
        BigDecimal currentMargin = BigDecimal.ZERO;

        for (Invoice invoice : realisedSales) {
            if (!matchesInvoiceScope(goal, invoice, targetEmployee)) {
                continue;
            }

            for (InvoiceLine line : invoice.getLines()) {
                if (!matchesLineScope(goal, line)) {
                    continue;
                }

                BigDecimal rev = line.getLineTotal() != null ? line.getLineTotal() : BigDecimal.ZERO;
                // CDC-BIZ-10: Custo histórico lineCost()
                BigDecimal cost = line.lineCost();
                BigDecimal margin = rev.subtract(cost);

                currentRevenue = currentRevenue.add(rev);
                currentMargin = currentMargin.add(margin);
            }
        }

        // Days calculation
        long daysTotal = Math.max(1, ChronoUnit.DAYS.between(goal.getPeriodStart(), goal.getPeriodEnd()) + 1);
        long daysElapsed;
        if (today.isBefore(goal.getPeriodStart())) {
            daysElapsed = 0;
        } else if (today.isAfter(goal.getPeriodEnd())) {
            daysElapsed = daysTotal;
        } else {
            daysElapsed = ChronoUnit.DAYS.between(goal.getPeriodStart(), today) + 1;
        }

        // Progress percentage calculation
        BigDecimal targetRevenue = goal.getTargetRevenue();
        BigDecimal targetMargin = goal.getTargetMargin();
        BigDecimal progressPct = BigDecimal.ZERO;

        if (targetRevenue != null && targetRevenue.compareTo(BigDecimal.ZERO) > 0) {
            progressPct = currentRevenue.multiply(BigDecimal.valueOf(100))
                    .divide(targetRevenue, 2, RoundingMode.HALF_UP);
        } else if (targetMargin != null && targetMargin.compareTo(BigDecimal.ZERO) > 0) {
            progressPct = currentMargin.multiply(BigDecimal.valueOf(100))
                    .divide(targetMargin, 2, RoundingMode.HALF_UP);
        }

        // Projected revenue & on-track
        BigDecimal projectedRevenue = BigDecimal.ZERO;
        boolean isOnTrack = false;
        if (daysElapsed > 0) {
            projectedRevenue = currentRevenue
                    .multiply(BigDecimal.valueOf(daysTotal))
                    .divide(BigDecimal.valueOf(daysElapsed), 2, RoundingMode.HALF_UP);

            if (targetRevenue != null && targetRevenue.compareTo(BigDecimal.ZERO) > 0) {
                isOnTrack = projectedRevenue.compareTo(targetRevenue) >= 0;
            } else if (targetMargin != null && targetMargin.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal projectedMargin = currentMargin
                        .multiply(BigDecimal.valueOf(daysTotal))
                        .divide(BigDecimal.valueOf(daysElapsed), 2, RoundingMode.HALF_UP);
                isOnTrack = projectedMargin.compareTo(targetMargin) >= 0;
            }
        }

        // Expected percentage at this point in time
        BigDecimal expectedPct = daysElapsed > 0
                ? BigDecimal.valueOf(daysElapsed).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(daysTotal), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Alert Level calculation according to SPEC §4.2:
        AlertLevel alertLevel;
        if (progressPct.compareTo(BigDecimal.valueOf(100)) >= 0 || isOnTrack) {
            alertLevel = AlertLevel.NONE;
        } else {
            BigDecimal cautionThreshold = expectedPct.multiply(new BigDecimal("0.85"));
            BigDecimal lateThreshold = expectedPct.multiply(new BigDecimal("0.70"));

            if (progressPct.compareTo(cautionThreshold) >= 0) {
                alertLevel = AlertLevel.CAUTION;
            } else if (progressPct.compareTo(lateThreshold) >= 0) {
                alertLevel = AlertLevel.LATE;
            } else {
                alertLevel = AlertLevel.CRITICAL;
            }
        }

        BigDecimal bonusEstimate = bonusCalculatorEngine.calculateBonus(goal, currentRevenue, currentMargin);

        return new SalesGoalProgressDTO(
                goal.getId(),
                goal.getName(),
                goal.getScope(),
                goal.getScopeLabel(),
                goal.getTargetRevenue(),
                currentRevenue,
                goal.getTargetMargin(),
                currentMargin,
                progressPct,
                daysElapsed,
                daysTotal,
                projectedRevenue,
                isOnTrack,
                bonusEstimate,
                alertLevel
        );
    }

    private boolean matchesInvoiceScope(SalesGoal goal, Invoice invoice, Employee employee) {
        if (goal.getScope() == GoalScope.WAREHOUSE) {
            if (goal.getScopeRefId() == null) return true;
            return invoice.getWarehouse() != null && goal.getScopeRefId().equals(invoice.getWarehouse().getId());
        }
        if (goal.getScope() == GoalScope.EMPLOYEE) {
            if (employee == null) return false;
            String createdBy = invoice.getCreatedBy();
            if (createdBy == null) return false;
            if (employee.getAppUser() != null && createdBy.equalsIgnoreCase(employee.getAppUser().getUsername())) {
                return true;
            }
            return createdBy.equalsIgnoreCase(employee.getName())
                    || (employee.getEmployeeNumber() != null && createdBy.equalsIgnoreCase(employee.getEmployeeNumber()));
        }
        return true;
    }

    private boolean matchesLineScope(SalesGoal goal, InvoiceLine line) {
        if (goal.getScope() == GoalScope.CATEGORY) {
            if (goal.getScopeRefId() == null) return true;
            return line.getProduct() != null
                    && line.getProduct().getCategory() != null
                    && goal.getScopeRefId().equals(line.getProduct().getCategory().getId());
        }
        if (goal.getScope() == GoalScope.PRODUCT) {
            if (goal.getScopeRefId() == null) return true;
            return line.getProduct() != null && goal.getScopeRefId().equals(line.getProduct().getId());
        }
        return true;
    }
}
