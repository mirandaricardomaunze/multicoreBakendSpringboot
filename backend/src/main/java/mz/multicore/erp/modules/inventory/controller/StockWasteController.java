package mz.multicore.erp.modules.inventory.controller;

import mz.multicore.erp.modules.inventory.dto.*;
import mz.multicore.erp.modules.inventory.model.WasteStatus;
import mz.multicore.erp.modules.inventory.service.StockWasteService;
import mz.multicore.erp.modules.printing.WasteReportPrintService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/inventory/waste")
public class StockWasteController {

    private final StockWasteService wasteService;
    private final WasteReportPrintService reportPrintService;

    public StockWasteController(StockWasteService wasteService, WasteReportPrintService reportPrintService) {
        this.wasteService = wasteService;
        this.reportPrintService = reportPrintService;
    }

    @GetMapping
    public ResponseEntity<List<StockWasteDTO>> list(
            @RequestParam Long companyId,
            @RequestParam(required = false) WasteStatus status
    ) {
        if (status != null) {
            return ResponseEntity.ok(wasteService.findByCompanyAndStatus(companyId, status));
        }
        return ResponseEntity.ok(wasteService.findByCompany(companyId));
    }

    @PostMapping
    public ResponseEntity<StockWasteDTO> register(@RequestBody @Valid CreateStockWasteRequest request) {
        return ResponseEntity.ok(wasteService.registerWaste(request));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<StockWasteDTO> approve(
            @PathVariable Long id,
            @RequestBody @Valid ApproveWasteRequest request
    ) {
        return ResponseEntity.ok(wasteService.approveWaste(id, request));
    }

    @GetMapping("/radar")
    public ResponseEntity<List<ExpiringBatchAlertDTO>> getExpiringRadar(
            @RequestParam Long companyId,
            @RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(wasteService.getExpiringBatchesRadar(companyId, days));
    }

    @GetMapping("/summary")
    public ResponseEntity<WasteSummaryDTO> getSummary(
            @RequestParam Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return ResponseEntity.ok(wasteService.getSummary(companyId, start, end));
    }

    @GetMapping("/report/pdf")
    public ResponseEntity<byte[]> printReport(
            @RequestParam Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        byte[] pdf = reportPrintService.render(companyId, start, end);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "relatorio-quebras.pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
