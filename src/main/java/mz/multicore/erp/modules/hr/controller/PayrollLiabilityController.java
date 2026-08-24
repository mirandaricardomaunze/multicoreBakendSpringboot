package mz.multicore.erp.modules.hr.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.hr.dto.HrPolicyConfigDTO;
import mz.multicore.erp.modules.hr.dto.PayrollCostDTO;
import mz.multicore.erp.modules.hr.dto.PayrollLiabilityDTO;
import mz.multicore.erp.modules.hr.dto.SaveHrPolicyConfigRequest;
import mz.multicore.erp.modules.hr.service.HrPolicyService;
import mz.multicore.erp.modules.hr.service.PayrollLiabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Retenções da folha e valores legais de RH. Controller próprio, e não mais um bloco no
 * {@code HRController}, pelo mesmo motivo dos contratos e do ponto: é um assunto inteiro.
 * Ver docs/RH_COMPLETO_SPEC.md §B5 e §6.
 */
@RestController
@RequestMapping("/api/hr/payroll")
public class PayrollLiabilityController {

    private final PayrollLiabilityService liabilityService;
    private final HrPolicyService hrPolicyService;

    public PayrollLiabilityController(PayrollLiabilityService liabilityService,
                                      HrPolicyService hrPolicyService) {
        this.liabilityService = liabilityService;
        this.hrPolicyService = hrPolicyService;
    }

    @GetMapping("/liabilities")
    public ResponseEntity<List<PayrollLiabilityDTO>> liabilities() {
        return ResponseEntity.ok(liabilityService.list());
    }

    /** As que já estão atrasadas, as que estão a ≤7 dias do prazo e as que não têm prazo definido. */
    @GetMapping("/liabilities/alerts")
    public ResponseEntity<List<PayrollLiabilityDTO>> liabilityAlerts() {
        return ResponseEntity.ok(liabilityService.dueAlerts());
    }

    @PostMapping("/liabilities/{id}/deliver")
    public ResponseEntity<PayrollLiabilityDTO> deliver(@PathVariable Long id,
                                                       @RequestParam(required = false) String reference) {
        return ResponseEntity.ok(liabilityService.markDelivered(id, reference));
    }

    /**
     * Reapura as obrigações de um período a partir dos recibos já pagos. Existe para quem tinha
     * folhas pagas antes deste bloco: sem isto, o passado ficava sem obrigação nenhuma registada.
     */
    @PostMapping("/liabilities/accrue/{year}/{month}")
    public ResponseEntity<List<PayrollLiabilityDTO>> accrue(@PathVariable int year,
                                                            @PathVariable int month) {
        return ResponseEntity.ok(liabilityService.accrueForPeriod(year, month));
    }

    /** Custo total do trabalhador — ilíquido + INSS patronal (RHC-55). */
    @GetMapping("/cost/{year}/{month}")
    public ResponseEntity<PayrollCostDTO> cost(@PathVariable int year, @PathVariable int month) {
        return ResponseEntity.ok(liabilityService.monthlyCost(year, month));
    }

    // ─── Valores legais (§6) ──────────────────────────────────────────────────

    @GetMapping("/policy")
    public ResponseEntity<List<HrPolicyConfigDTO>> policies() {
        return ResponseEntity.ok(hrPolicyService.list());
    }

    @PostMapping("/policy")
    public ResponseEntity<HrPolicyConfigDTO> createPolicy(
            @RequestBody @Valid SaveHrPolicyConfigRequest request) {
        return ResponseEntity.ok(hrPolicyService.create(request));
    }

    @DeleteMapping("/policy/{id}")
    public ResponseEntity<Map<String, Object>> deactivatePolicy(@PathVariable Long id) {
        hrPolicyService.deactivate(id);
        return ResponseEntity.ok(Map.of("deactivated", id));
    }
}
