package mz.multicore.erp.modules.returnables.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.returnables.dto.*;
import mz.multicore.erp.modules.returnables.service.ReturnableService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Returnables: sellers deliver and receive; managers define the items and charge what is not returned. */
@RestController
@RequestMapping("/api/returnables")
public class ReturnableController {

    private final ReturnableService service;

    public ReturnableController(ReturnableService service) {
        this.service = service;
    }

    @GetMapping("/items")
    public ResponseEntity<List<ReturnableItemDTO>> items(@RequestParam Long companyId) {
        return ResponseEntity.ok(service.items(companyId));
    }

    @PutMapping("/items")
    public ResponseEntity<ReturnableItemDTO> saveItem(@RequestBody @Valid SaveReturnableItemRequest request) {
        return ResponseEntity.ok(service.saveItem(request));
    }

    @GetMapping("/balances")
    public ResponseEntity<List<ReturnableBalanceDTO>> balances(@RequestParam Long companyId, @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(service.balances(companyId, clientId));
    }

    @GetMapping("/movements")
    public ResponseEntity<List<ReturnableMovementDTO>> movements(@RequestParam Long companyId, @RequestParam(defaultValue = "60") int days) {
        return ResponseEntity.ok(service.movements(companyId, days));
    }

    @PostMapping("/movements")
    public ResponseEntity<ReturnableMovementDTO> record(@RequestBody @Valid RecordReturnableRequest request) {
        return ResponseEntity.ok(service.record(request));
    }

    @PostMapping("/charges")
    public ResponseEntity<ReturnableMovementDTO> charge(@RequestBody @Valid ChargeReturnablesRequest request) {
        return ResponseEntity.ok(service.charge(request));
    }
}
