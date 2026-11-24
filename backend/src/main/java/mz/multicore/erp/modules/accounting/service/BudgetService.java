package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.accounting.dto.BudgetDTO;
import mz.multicore.erp.modules.accounting.dto.BudgetLineDTO;
import mz.multicore.erp.modules.accounting.dto.CopyBudgetRequest;
import mz.multicore.erp.modules.accounting.dto.SaveBudgetRequest;
import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.model.Budget;
import mz.multicore.erp.modules.accounting.model.BudgetLine;
import mz.multicore.erp.modules.accounting.model.CostCenter;
import mz.multicore.erp.modules.accounting.repository.BudgetRepository;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.service.CompanyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Annual budget of the active company (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.1, OR-01): one budget per company and
 * year whose cells use a postable account of class 6 or 7, an active cost centre of the company, a month 1–12 and an
 * amount ≥ 0, with one cell per account × centre × month. Saving sends the whole year and is allowed at any time.
 * Copying from the previous year (OR-02) fills only the empty cells with last year's actual, optionally adjusted.
 * Reading, saving and copying are for ADMIN/MANAGER; every save or copy is audited as {@code BUDGET_CHANGE} (OR-07).
 */
@Service
public class BudgetService {

    private static final Comparator<BudgetLine> CELL_ORDER = Comparator
            .comparing((BudgetLine line) -> line.getAccount().getCode())
            .thenComparing(line -> line.getCostCenter().getCode())
            .thenComparingInt(BudgetLine::getMonth);

    private final BudgetRepository repository;
    private final ChartOfAccountsService chartOfAccountsService;
    private final CostCenterService costCenterService;
    private final CompanyService companyService;
    private final BudgetActualsCalculator actualsCalculator;
    private final AuditLogService auditLogService;

    public BudgetService(BudgetRepository repository, ChartOfAccountsService chartOfAccountsService,
                         CostCenterService costCenterService, CompanyService companyService,
                         BudgetActualsCalculator actualsCalculator, AuditLogService auditLogService) {
        this.repository = repository;
        this.chartOfAccountsService = chartOfAccountsService;
        this.costCenterService = costCenterService;
        this.companyService = companyService;
        this.actualsCalculator = actualsCalculator;
        this.auditLogService = auditLogService;
    }

    /** Budget of {@code year} in the active company; a year without a budget returns an empty grid. */
    @Transactional(readOnly = true)
    public BudgetDTO get(int year) {
        PermissionGuard.requireManagerOrAdmin("consultar o orçamento");
        return repository.findByCompanyIdAndFiscalYear(CurrentUserContext.getCurrentCompanyId(), year)
                .map(BudgetService::toDTO)
                .orElseGet(() -> new BudgetDTO(null, year, List.of(), BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Transactional
    public BudgetDTO save(SaveBudgetRequest request) {
        PermissionGuard.requireManagerOrAdmin("gravar o orçamento");
        requireYear(request.year());
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        List<BudgetLine> cells = validCells(companyId,
                request.lines() == null ? List.of() : request.lines());

        Budget budget = budgetOf(companyId, request.year());
        budget.replaceLines(cells);
        return audited("gravado", repository.save(budget));
    }

    /**
     * Fills each empty cell of {@code request.year()} with the actual of the same account, centre and month of the
     * previous year, times {@code 1 + adjustment/100} rounded to 2 decimals (OR-02). Filled cells — zero included —
     * are never overwritten; non-positive actuals and centres that are inactive today are skipped.
     */
    @Transactional
    public BudgetDTO copyFromPreviousYear(CopyBudgetRequest request) {
        PermissionGuard.requireManagerOrAdmin("copiar o orçamento do ano anterior");
        requireYear(request.year());
        BigDecimal factor = adjustmentFactor(request.adjustmentPercent());
        Long companyId = CurrentUserContext.getCurrentCompanyId();

        Budget budget = budgetOf(companyId, request.year());
        Set<BudgetLine.CellKey> filled = new HashSet<>();
        budget.getLines().forEach(line -> filled.add(line.cellKey()));
        for (BudgetActualsCalculator.ActualCell actual : actualsCalculator.actualsOf(companyId, request.year() - 1)) {
            BigDecimal amount = actual.amount().multiply(factor).setScale(2, RoundingMode.HALF_UP);
            if (amount.signum() <= 0 || !actual.costCenter().isActive()) continue;
            BudgetLine cell = new BudgetLine();
            cell.setAccount(actual.account());
            cell.setCostCenter(actual.costCenter());
            cell.setMonth(actual.month());
            cell.setAmount(amount);
            if (filled.add(cell.cellKey())) {
                budget.addLine(cell);
            }
        }
        return audited("copiado do ano anterior", repository.save(budget));
    }

    /** Audits a save or copy as {@code BUDGET_CHANGE} with the year, both totals and the number of cells. */
    private BudgetDTO audited(String what, Budget budget) {
        BudgetDTO dto = toDTO(budget);
        auditLogService.logCurrent("BUDGET_CHANGE", "Orçamento " + dto.year() + " " + what + ": custos "
                + dto.totalExpenses().toPlainString() + " MT, proveitos " + dto.totalRevenues().toPlainString()
                + " MT (" + dto.lines().size() + " células)");
        return dto;
    }

    private static void requireYear(Integer year) {
        if (year == null) {
            throw new BusinessRuleException("Indique o ano do orçamento.");
        }
    }

    private static BigDecimal adjustmentFactor(BigDecimal adjustmentPercent) {
        BigDecimal percent = adjustmentPercent == null ? BigDecimal.ZERO : adjustmentPercent;
        if (percent.compareTo(new BigDecimal("-100")) < 0) {
            throw new BusinessRuleException("O ajuste não pode ser inferior a -100%.");
        }
        return BigDecimal.ONE.add(percent.movePointLeft(2));
    }

    private Budget budgetOf(Long companyId, int year) {
        return repository.findByCompanyIdAndFiscalYear(companyId, year)
                .orElseGet(() -> newBudget(companyId, year));
    }

    private List<BudgetLine> validCells(Long companyId, List<SaveBudgetRequest.Line> lines) {
        Map<String, Account> accounts = new HashMap<>();
        Map<Long, CostCenter> centers = new HashMap<>();
        Set<BudgetLine.CellKey> seen = new HashSet<>();
        List<BudgetLine> cells = new ArrayList<>();
        for (SaveBudgetRequest.Line line : lines) {
            Account account = accounts.computeIfAbsent(line.accountCode(),
                    code -> requireBudgetAccount(companyId, code));
            if (line.costCenterId() == null) {
                throw new BusinessRuleException("Indique o centro de custo da conta " + account.getCode()
                        + " no orçamento.");
            }
            CostCenter center = centers.computeIfAbsent(line.costCenterId(),
                    id -> costCenterService.requirePostable(companyId, id));
            if (line.month() == null || line.month() < 1 || line.month() > 12) {
                throw new BusinessRuleException("O mês da conta " + account.getCode()
                        + " no orçamento tem de estar entre 1 e 12.");
            }
            if (line.amount() == null || line.amount().signum() < 0) {
                throw new BusinessRuleException("O valor orçado da conta " + account.getCode() + " no mês "
                        + line.month() + " é obrigatório e não pode ser negativo.");
            }
            BudgetLine cell = new BudgetLine();
            cell.setAccount(account);
            cell.setCostCenter(center);
            cell.setMonth(line.month());
            cell.setAmount(line.amount());
            if (!seen.add(cell.cellKey())) {
                throw new BusinessRuleException("A conta " + account.getCode() + " no centro " + center.getCode()
                        + ", mês " + line.month() + ", aparece mais do que uma vez no orçamento.");
            }
            cells.add(cell);
        }
        return cells;
    }

    private Account requireBudgetAccount(Long companyId, String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessRuleException("Indique a conta de cada linha do orçamento.");
        }
        Account account = chartOfAccountsService.requirePostableAccount(companyId, code.trim());
        int digit = account.getAccountClass().digit();
        if (digit != 6 && digit != 7) {
            throw new BusinessRuleException("A conta " + account.getCode() + " (classe " + digit
                    + ") não entra no orçamento — só contas das classes 6 e 7.");
        }
        return account;
    }

    private Budget newBudget(Long companyId, int year) {
        Budget budget = new Budget();
        budget.setCompany(companyService.getCurrentCompanyReference(companyId));
        budget.setFiscalYear(year);
        budget.setCreatedBy(CurrentUserContext.getUsername());
        return budget;
    }

    static BudgetDTO toDTO(Budget budget) {
        List<BudgetLine> ordered = budget.getLines().stream().sorted(CELL_ORDER).toList();
        BigDecimal expenses = BigDecimal.ZERO;
        BigDecimal revenues = BigDecimal.ZERO;
        for (BudgetLine line : ordered) {
            if (line.getAccount().getAccountClass().digit() == 6) {
                expenses = expenses.add(line.getAmount());
            } else {
                revenues = revenues.add(line.getAmount());
            }
        }
        List<BudgetLineDTO> lines = ordered.stream().map(BudgetService::toLineDTO).toList();
        return new BudgetDTO(budget.getId(), budget.getFiscalYear(), lines, expenses, revenues);
    }

    private static BudgetLineDTO toLineDTO(BudgetLine line) {
        CostCenter center = line.getCostCenter();
        return new BudgetLineDTO(line.getAccount().getCode(), line.getAccount().getName(), center.getId(),
                center.getCode(), center.getName(), line.getMonth(), line.getAmount());
    }
}
