package mz.multicore.erp.modules.comercial.controller;

import mz.multicore.erp.modules.comercial.dto.*;
import mz.multicore.erp.modules.comercial.service.CustomerOrderFulfillmentService;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comercial/orders")
public class CustomerOrderFulfillmentController {
    private final CustomerOrderFulfillmentService service;

    public CustomerOrderFulfillmentController(CustomerOrderFulfillmentService service) {
        this.service = service;
    }

    @PostMapping("/fulfillment")
    public ResponseEntity<OrderDTO> submit(@RequestBody @Valid CreateFulfillmentOrderRequest request) {
        return ResponseEntity.ok(service.submit(request));
    }

    @PostMapping("/{id}/picking-print")
    public ResponseEntity<ByteArrayResource> print(@PathVariable Long id,
            @RequestBody(required = false) OrderActionRequest request) {
        return pdf(service.printForPicking(id, request == null ? null : request.terminalName()), "separacao-" + id);
    }

    @PostMapping("/{id}/picking-reprint")
    public ResponseEntity<ByteArrayResource> reprint(@PathVariable Long id,
            @RequestBody @Valid ReprintAuthorizationRequest request) {
        return pdf(service.reprint(id, request), "reimpressao-separacao-" + id);
    }

    @PostMapping("/{id}/separate")
    public ResponseEntity<OrderDTO> separate(@PathVariable Long id, @RequestBody OrderActionRequest request) {
        return ResponseEntity.ok(service.completeSeparation(id, request));
    }

    @PostMapping("/{id}/fulfillment-bill")
    public ResponseEntity<InvoiceDTO> bill(@PathVariable Long id, @RequestBody OrderActionRequest request) {
        return ResponseEntity.ok(service.bill(id, request));
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<List<OrderEventDTO>> events(@PathVariable Long id) {
        return ResponseEntity.ok(service.events(id));
    }

    private ResponseEntity<ByteArrayResource> pdf(byte[] bytes, String name) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + name + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(bytes.length).body(new ByteArrayResource(bytes));
    }
}
