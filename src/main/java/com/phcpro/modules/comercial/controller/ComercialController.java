package com.phcpro.modules.comercial.controller;

import com.phcpro.modules.comercial.dto.*;
import com.phcpro.modules.comercial.service.ComercialService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comercial")
public class ComercialController {

    private final ComercialService comercialService;

    public ComercialController(ComercialService comercialService) {
        this.comercialService = comercialService;
    }

    @GetMapping("/clients")
    public ResponseEntity<List<ClientDTO>> getClients() {
        return ResponseEntity.ok(comercialService.getAllClients());
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductDTO>> getProducts() {
        return ResponseEntity.ok(comercialService.getAllProducts());
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceDTO>> getInvoices() {
        return ResponseEntity.ok(comercialService.getAllInvoices());
    }

    @PostMapping("/invoices")
    public ResponseEntity<InvoiceDTO> createInvoice(@RequestBody @Valid CreateInvoiceRequest request) {
        return ResponseEntity.ok(comercialService.createInvoice(request));
    }
}
