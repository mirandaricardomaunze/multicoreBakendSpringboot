package mz.multicore.erp.modules.expiry.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.expiry.dto.*;
import mz.multicore.erp.modules.expiry.service.ExpiryService;
import mz.multicore.erp.modules.inventory.dto.StockWasteDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Expiry board and markdowns: sellers read and write off; managers mark down and define the rules. */
@RestController
@RequestMapping("/api/expiry")
public class ExpiryController {

    private final ExpiryService service;

    public ExpiryController(ExpiryService service) {
        this.service = service;
    }

    @GetMapping("/board")
    public ResponseEntity<ExpiryBoardDTO> board(@RequestParam Long companyId, @RequestParam(defaultValue = "14") int days) {
        return ResponseEntity.ok(service.board(companyId, days));
    }

    @GetMapping("/rules")
    public ResponseEntity<List<MarkdownRuleDTO>> rules(@RequestParam Long companyId) {
        return ResponseEntity.ok(service.rules(companyId));
    }

    @PutMapping("/rules")
    public ResponseEntity<MarkdownRuleDTO> saveRule(@RequestBody @Valid SaveMarkdownRuleRequest request) {
        return ResponseEntity.ok(service.saveRule(request));
    }

    @PostMapping("/markdowns")
    public ResponseEntity<ExpiryLotDTO> markdown(@RequestBody @Valid MarkdownRequest request) {
        return ResponseEntity.ok(service.markdown(request));
    }

    @PostMapping("/markdowns/apply-rules")
    public ResponseEntity<ApplyMarkdownsResultDTO> applyRules(@RequestParam Long companyId) {
        return ResponseEntity.ok(service.applyRules(companyId));
    }

    @PostMapping("/write-offs")
    public ResponseEntity<StockWasteDTO> writeOff(@RequestBody @Valid WriteOffExpiredRequest request) {
        return ResponseEntity.ok(service.writeOff(request));
    }
}
