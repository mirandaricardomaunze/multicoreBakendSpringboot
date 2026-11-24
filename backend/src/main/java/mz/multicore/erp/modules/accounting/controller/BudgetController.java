package mz.multicore.erp.modules.accounting.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.accounting.dto.BudgetAlertDTO;
import mz.multicore.erp.modules.accounting.dto.BudgetDTO;
import mz.multicore.erp.modules.accounting.dto.BudgetVarianceDTO;
import mz.multicore.erp.modules.accounting.dto.BudgetVarianceQuery;
import mz.multicore.erp.modules.accounting.dto.CopyBudgetRequest;
import mz.multicore.erp.modules.accounting.dto.SaveBudgetRequest;
import mz.multicore.erp.modules.accounting.service.BudgetAlertService;
import mz.multicore.erp.modules.accounting.service.BudgetService;
import mz.multicore.erp.modules.accounting.service.BudgetVarianceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Orçamento anual, real vs orçado e alertas da empresa activa (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4, OR-07).
 * Só HTTP: as guardas de perfil (ADMIN/MANAGER), de empresa e a auditoria vivem nos Services.
 */
@RestController
@RequestMapping("/api/accounting/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final BudgetVarianceService varianceService;
    private final BudgetAlertService alertService;

    public BudgetController(BudgetService budgetService, BudgetVarianceService varianceService,
                            BudgetAlertService alertService) {
        this.budgetService = budgetService;
        this.varianceService = varianceService;
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<BudgetDTO> get(@RequestParam int year) {
        return ResponseEntity.ok(budgetService.get(year));
    }

    @PutMapping
    public ResponseEntity<BudgetDTO> save(@RequestBody @Valid SaveBudgetRequest request) {
        return ResponseEntity.ok(budgetService.save(request));
    }

    @PostMapping("/copy-previous-year")
    public ResponseEntity<BudgetDTO> copyFromPreviousYear(@RequestBody @Valid CopyBudgetRequest request) {
        return ResponseEntity.ok(budgetService.copyFromPreviousYear(request));
    }

    @GetMapping("/variance")
    public ResponseEntity<BudgetVarianceDTO> variance(@RequestParam Integer year, @RequestParam Integer month,
                                                      @RequestParam(defaultValue = "false") boolean cumulative,
                                                      @RequestParam(required = false) Long costCenterId) {
        return ResponseEntity.ok(varianceService.report(new BudgetVarianceQuery(year, month, cumulative, costCenterId)));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<BudgetAlertDTO>> alerts(@RequestParam int year) {
        return ResponseEntity.ok(alertService.listAlerts(year));
    }
}
