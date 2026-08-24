package mz.multicore.erp.modules.hr.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.hr.dto.ContractAlertsDTO;
import mz.multicore.erp.modules.hr.dto.CreateContractRequest;
import mz.multicore.erp.modules.hr.dto.EmploymentContractDTO;
import mz.multicore.erp.modules.hr.dto.RenewContractRequest;
import mz.multicore.erp.modules.hr.service.EmploymentContractService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Contratos de trabalho. Controller próprio — o ciclo de vida do contrato é um assunto inteiro, e o
 * {@code HRController} já carrega colaboradores, recibos, faltas, férias, despesas e impostos.
 */
@RestController
@RequestMapping("/api/hr/contracts")
public class EmploymentContractController {

    private final EmploymentContractService contractService;

    public EmploymentContractController(EmploymentContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping
    public ResponseEntity<List<EmploymentContractDTO>> list() {
        return ResponseEntity.ok(contractService.getAllContracts());
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<EmploymentContractDTO>> listOfEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(contractService.getContractsOfEmployee(employeeId));
    }

    /** Fins de contrato (≤30 dias) e de período experimental (≤7) — alimenta o sino, numa só ida. */
    @GetMapping("/alerts")
    public ResponseEntity<ContractAlertsDTO> alerts() {
        return ResponseEntity.ok(contractService.getContractAlerts());
    }

    @PostMapping
    public ResponseEntity<EmploymentContractDTO> create(@RequestBody @Valid CreateContractRequest request) {
        return ResponseEntity.ok(contractService.createContract(request));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<EmploymentContractDTO> activate(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.activateContract(id));
    }

    @PostMapping("/{id}/renew")
    public ResponseEntity<EmploymentContractDTO> renew(@PathVariable Long id,
                                                       @RequestBody @Valid RenewContractRequest request) {
        return ResponseEntity.ok(contractService.renewContract(id, request));
    }

    @PostMapping("/{id}/terminate")
    public ResponseEntity<EmploymentContractDTO> terminate(
            @PathVariable Long id,
            @RequestParam(required = false) LocalDate terminationDate,
            @RequestParam String reason) {
        return ResponseEntity.ok(contractService.terminateContract(id, terminationDate, reason));
    }
}
