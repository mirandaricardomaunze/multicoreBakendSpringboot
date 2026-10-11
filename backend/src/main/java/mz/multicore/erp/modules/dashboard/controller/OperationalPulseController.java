package mz.multicore.erp.modules.dashboard.controller;

import mz.multicore.erp.modules.dashboard.dto.OperationalPulseDTO;
import mz.multicore.erp.modules.dashboard.service.OperationalPulseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/operational-pulse")
public class OperationalPulseController {

    private final OperationalPulseService operationalPulseService;

    public OperationalPulseController(OperationalPulseService operationalPulseService) {
        this.operationalPulseService = operationalPulseService;
    }

    @GetMapping
    public ResponseEntity<OperationalPulseDTO> getOperationalPulse() {
        return ResponseEntity.ok(operationalPulseService.getOperationalPulse());
    }
}
