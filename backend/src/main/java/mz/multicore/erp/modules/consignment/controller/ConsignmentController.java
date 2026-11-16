package mz.multicore.erp.modules.consignment.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.consignment.dto.ConsignmentDTO;
import mz.multicore.erp.modules.consignment.dto.DeliverConsignmentRequest;
import mz.multicore.erp.modules.consignment.dto.SettleConsignmentRequest;
import mz.multicore.erp.modules.consignment.service.ConsignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Consignment deliveries to resellers (sellers deliver) and their settlement (managers invoice the sold part). */
@RestController
@RequestMapping("/api/consignments")
public class ConsignmentController {

    private final ConsignmentService service;

    public ConsignmentController(ConsignmentService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ConsignmentDTO>> list(@RequestParam Long companyId, @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(service.list(companyId, days));
    }

    @PostMapping
    public ResponseEntity<ConsignmentDTO> deliver(@RequestBody @Valid DeliverConsignmentRequest request) {
        return ResponseEntity.ok(service.deliver(request));
    }

    @PostMapping("/{id}/settle")
    public ResponseEntity<ConsignmentDTO> settle(@PathVariable Long id, @RequestBody @Valid SettleConsignmentRequest request) {
        return ResponseEntity.ok(service.settle(id, request));
    }
}
