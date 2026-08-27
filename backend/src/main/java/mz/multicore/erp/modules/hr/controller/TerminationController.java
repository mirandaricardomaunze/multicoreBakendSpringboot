package mz.multicore.erp.modules.hr.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.hr.dto.CreateTerminationRequest;
import mz.multicore.erp.modules.hr.dto.TerminationDTO;
import mz.multicore.erp.modules.hr.service.TerminationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Cessações e acertos finais. Ver docs/RH_COMPLETO_SPEC.md §B3. */
@RestController
@RequestMapping("/api/hr/terminations")
public class TerminationController {

    private final TerminationService terminationService;

    public TerminationController(TerminationService terminationService) {
        this.terminationService = terminationService;
    }

    @GetMapping
    public ResponseEntity<List<TerminationDTO>> list() {
        return ResponseEntity.ok(terminationService.list());
    }

    /** Mostra a conta antes de a cometer — cessar é irreversível. */
    @PostMapping("/preview")
    public ResponseEntity<TerminationDTO> preview(@RequestBody @Valid CreateTerminationRequest request) {
        return ResponseEntity.ok(terminationService.preview(request));
    }

    @PostMapping
    public ResponseEntity<TerminationDTO> terminate(@RequestBody @Valid CreateTerminationRequest request) {
        return ResponseEntity.ok(terminationService.terminate(request));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<TerminationDTO> pay(@PathVariable Long id) {
        return ResponseEntity.ok(terminationService.paySettlement(id));
    }
}
