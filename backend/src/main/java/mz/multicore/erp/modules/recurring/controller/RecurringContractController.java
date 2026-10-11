package mz.multicore.erp.modules.recurring.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.recurring.dto.BatchBillingResultDTO;
import mz.multicore.erp.modules.recurring.dto.CreateRecurringContractRequest;
import mz.multicore.erp.modules.recurring.dto.RecurringContractDTO;
import mz.multicore.erp.modules.recurring.model.ContractStatus;
import mz.multicore.erp.modules.recurring.service.RecurringContractService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/api/recurring-contracts", "/api/recurring/contracts"})
public class RecurringContractController {

    private final RecurringContractService contractService;

    public RecurringContractController(RecurringContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping
    public ResponseEntity<List<RecurringContractDTO>> listContracts() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(contractService.listContracts(companyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecurringContractDTO> getContract(@PathVariable Long id) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(contractService.getContract(companyId, id));
    }

    @PostMapping
    public ResponseEntity<RecurringContractDTO> createContract(@RequestBody @Valid CreateRecurringContractRequest request) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(contractService.createContract(companyId, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RecurringContractDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam ContractStatus status) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(contractService.updateStatus(companyId, id, status));
    }

    @PostMapping("/batch-billing")
    public ResponseEntity<BatchBillingResultDTO> runBatchBilling(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(contractService.processBatchBilling(companyId, referenceDate));
    }
}
