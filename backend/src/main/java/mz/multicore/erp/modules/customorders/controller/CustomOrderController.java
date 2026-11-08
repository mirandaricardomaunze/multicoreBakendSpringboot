package mz.multicore.erp.modules.customorders.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.customorders.dto.*;
import mz.multicore.erp.modules.customorders.service.CustomOrderService;
import mz.multicore.erp.modules.printing.CustomOrderSheetPrintService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Agenda of orders with a date, their personalisation, kitchen stage, production and kitchen sheet. */
@RestController
@RequestMapping("/api/custom-orders")
public class CustomOrderController {

    private final CustomOrderService service;
    private final CustomOrderSheetPrintService printService;

    public CustomOrderController(CustomOrderService service, CustomOrderSheetPrintService printService) {
        this.service = service;
        this.printService = printService;
    }

    @GetMapping("/agenda")
    public ResponseEntity<List<CustomOrderDTO>> agenda(@RequestParam Long companyId,
                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.agenda(companyId, from, to));
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<CustomOrderDTO> save(@PathVariable Long orderId, @RequestBody @Valid SaveCustomOrderRequest request) {
        return ResponseEntity.ok(service.save(orderId, request));
    }

    @PostMapping("/{orderId}/stage")
    public ResponseEntity<CustomOrderDTO> stage(@PathVariable Long orderId, @RequestBody @Valid CustomOrderStageRequest request) {
        return ResponseEntity.ok(service.stage(orderId, request));
    }

    @PostMapping("/{orderId}/send-to-plan")
    public ResponseEntity<SendToPlanResultDTO> sendToPlan(@PathVariable Long orderId, @RequestParam Long companyId) {
        return ResponseEntity.ok(service.sendToPlan(orderId, companyId));
    }

    @GetMapping("/{orderId}/image")
    public ResponseEntity<byte[]> image(@PathVariable Long orderId, @RequestParam Long companyId) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(service.image(orderId, companyId));
    }

    @GetMapping("/{orderId}/sheet")
    public ResponseEntity<byte[]> sheet(@PathVariable Long orderId, @RequestParam Long companyId) {
        byte[] pdf = printService.render(companyId, orderId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"encomenda-" + orderId + ".pdf\"")
                .contentLength(pdf.length)
                .body(pdf);
    }
}
