package mz.multicore.erp.modules.performance.service;

import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.performance.dto.EmployeeRankingDTO;
import mz.multicore.erp.modules.performance.model.GoalScope;
import mz.multicore.erp.modules.performance.model.GoalStatus;
import mz.multicore.erp.modules.performance.model.SalesGoal;
import mz.multicore.erp.modules.performance.repository.SalesGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class EmployeeRankingService {

    private final InvoiceRepository invoiceRepository;
    private final EmployeeRepository employeeRepository;
    private final SalesGoalRepository salesGoalRepository;

    public EmployeeRankingService(InvoiceRepository invoiceRepository,
                                  EmployeeRepository employeeRepository,
                                  SalesGoalRepository salesGoalRepository) {
        this.invoiceRepository = invoiceRepository;
        this.employeeRepository = employeeRepository;
        this.salesGoalRepository = salesGoalRepository;
    }

    @Transactional(readOnly = true)
    public List<EmployeeRankingDTO> getRanking(Long companyId, LocalDate from, LocalDate to) {
        LocalDateTime start = (from != null ? from : LocalDate.now().withDayOfMonth(1)).atStartOfDay();
        LocalDateTime end = (to != null ? to : LocalDate.now()).atTime(LocalTime.MAX);

        List<Invoice> invoices = invoiceRepository.findByCompanyIdAndCreatedAtBetween(companyId, start, end);

        // CDC-BIZ-12: Apenas faturas com isRealisedSale() == true
        List<Invoice> realised = invoices.stream()
                .filter(inv -> inv.getStatus() != null && inv.getStatus().isRealisedSale())
                .toList();

        List<Employee> employees = employeeRepository.findByCompanyIdOrderByName(companyId);
        Map<String, Employee> employeeByUsername = new HashMap<>();
        Map<String, Employee> employeeByName = new HashMap<>();

        for (Employee emp : employees) {
            if (emp.getAppUser() != null && emp.getAppUser().getUsername() != null) {
                employeeByUsername.put(emp.getAppUser().getUsername().toLowerCase(), emp);
            }
            if (emp.getName() != null) {
                employeeByName.put(emp.getName().toLowerCase(), emp);
            }
        }

        // Aggregate by operator/employee
        Map<Long, OperatorStat> statsByEmployeeId = new HashMap<>();
        Map<String, OperatorStat> statsByRawOperator = new HashMap<>();

        for (Invoice inv : realised) {
            String createdBy = inv.getCreatedBy() != null ? inv.getCreatedBy().trim() : "Outro";
            Employee matched = employeeByUsername.get(createdBy.toLowerCase());
            if (matched == null) {
                matched = employeeByName.get(createdBy.toLowerCase());
            }
            final Employee finalEmp = matched;

            BigDecimal invRevenue = inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal invMargin = BigDecimal.ZERO;
            if (inv.getLines() != null) {
                for (InvoiceLine line : inv.getLines()) {
                    BigDecimal rev = line.getLineTotal() != null ? line.getLineTotal() : BigDecimal.ZERO;
                    BigDecimal cost = line.lineCost();
                    invMargin = invMargin.add(rev.subtract(cost));
                }
            }

            if (finalEmp != null) {
                OperatorStat stat = statsByEmployeeId.computeIfAbsent(finalEmp.getId(),
                        id -> new OperatorStat(finalEmp.getId(), finalEmp.getName()));
                stat.revenue = stat.revenue.add(invRevenue);
                stat.margin = stat.margin.add(invMargin);
                stat.invoiceCount++;
            } else {
                OperatorStat stat = statsByRawOperator.computeIfAbsent(createdBy.toLowerCase(),
                        key -> new OperatorStat(null, createdBy));
                stat.revenue = stat.revenue.add(invRevenue);
                stat.margin = stat.margin.add(invMargin);
                stat.invoiceCount++;
            }
        }

        List<OperatorStat> allStats = new ArrayList<>(statsByEmployeeId.values());
        allStats.addAll(statsByRawOperator.values());

        // Sort by revenue desc
        allStats.sort(Comparator.comparing((OperatorStat s) -> s.revenue).reversed());

        // Find active employee goals for the company to attach goalProgressPct
        List<SalesGoal> activeEmployeeGoals = salesGoalRepository
                .findByCompanyIdAndStatus(companyId, GoalStatus.ACTIVE).stream()
                .filter(g -> g.getScope() == GoalScope.EMPLOYEE && g.getScopeRefId() != null)
                .toList();

        Map<Long, BigDecimal> goalProgressByEmployee = new HashMap<>();
        for (SalesGoal g : activeEmployeeGoals) {
            if (g.getTargetRevenue() != null && g.getTargetRevenue().compareTo(BigDecimal.ZERO) > 0) {
                OperatorStat stat = statsByEmployeeId.get(g.getScopeRefId());
                if (stat != null) {
                    BigDecimal progress = stat.revenue.multiply(BigDecimal.valueOf(100))
                            .divide(g.getTargetRevenue(), 2, RoundingMode.HALF_UP);
                    goalProgressByEmployee.put(g.getScopeRefId(), progress);
                }
            }
        }

        List<EmployeeRankingDTO> ranking = new ArrayList<>();
        int rank = 1;
        for (OperatorStat stat : allStats) {
            BigDecimal avgTicket = stat.invoiceCount > 0
                    ? stat.revenue.divide(BigDecimal.valueOf(stat.invoiceCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal progressPct = stat.employeeId != null
                    ? goalProgressByEmployee.getOrDefault(stat.employeeId, BigDecimal.ZERO)
                    : BigDecimal.ZERO;

            ranking.add(new EmployeeRankingDTO(
                    rank++,
                    stat.employeeId,
                    stat.name,
                    stat.revenue,
                    stat.margin,
                    avgTicket,
                    stat.invoiceCount,
                    progressPct
            ));
        }

        return ranking;
    }

    private static class OperatorStat {
        final Long employeeId;
        final String name;
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal margin = BigDecimal.ZERO;
        int invoiceCount = 0;

        OperatorStat(Long employeeId, String name) {
            this.employeeId = employeeId;
            this.name = name;
        }
    }
}
