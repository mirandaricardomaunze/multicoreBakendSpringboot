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

    @PostMapping("/clients")
    public ResponseEntity<ClientDTO> createClient(@RequestBody @Valid SaveClientRequest request) {
        return ResponseEntity.ok(comercialService.createClient(
                request.name(), request.taxId(), request.email(), request.address()));
    }

    @PutMapping("/clients/{id}")
    public ResponseEntity<ClientDTO> updateClient(@PathVariable Long id, @RequestBody @Valid SaveClientRequest request) {
        return ResponseEntity.ok(comercialService.updateClient(
                id, request.name(), request.taxId(), request.email(), request.address()));
    }

    @DeleteMapping("/clients/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        comercialService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductDTO>> getProducts() {
        return ResponseEntity.ok(comercialService.getAllProducts());
    }

    /** Produtos vendáveis no POS (com stock rastreado e activos). */
    @GetMapping("/products/sellable")
    public ResponseEntity<List<ProductDTO>> getSellableProducts() {
        return ResponseEntity.ok(comercialService.getSellableProducts());
    }

    /** Localiza um produto pelo código de barras (leitor do POS). Corpo {@code null} se não existir. */
    @GetMapping("/products/by-barcode")
    public ResponseEntity<ProductDTO> findProductByBarcode(@RequestParam String barcode) {
        return ResponseEntity.ok(comercialService.findProductByBarcode(barcode));
    }

    @PostMapping("/products")
    public ResponseEntity<ProductDTO> createProduct(@RequestBody @Valid CreateProductRequest r) {
        return ResponseEntity.ok(comercialService.createProduct(
                r.sku(), r.reference(), r.barcode(), r.name(), r.unitPrice(), r.purchasePrice(), r.minStock(),
                r.unitsPerBox(), r.categoryId(), r.saleType(), r.stockTracked(), r.taxRateId(), r.description(),
                r.wholesalePrice(), r.wholesaleMinQty()));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id, @RequestBody @Valid CreateProductRequest r) {
        return ResponseEntity.ok(comercialService.updateProduct(
                id, r.reference(), r.barcode(), r.name(), r.unitPrice(), r.purchasePrice(), r.minStock(),
                r.unitsPerBox(), r.categoryId(), r.saleType(), r.stockTracked(), r.taxRateId(), r.description(),
                r.wholesalePrice(), r.wholesaleMinQty()));
    }

    @PostMapping(value = "/products/{id}/image", consumes = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Void> updateProductImage(@PathVariable Long id, @RequestBody byte[] image) {
        comercialService.updateProductImage(id, image);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/vat-rates")
    public ResponseEntity<List<com.phcpro.modules.fiscal.dto.TaxRateDTO>> getVatRates() {
        return ResponseEntity.ok(comercialService.getActiveVatRates());
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceDTO>> getInvoices() {
        return ResponseEntity.ok(comercialService.getAllInvoices());
    }

    /** Vendas de balcão (POS) da empresa — histórico do painel de vendas. */
    @GetMapping("/pos-sales")
    public ResponseEntity<List<InvoiceDTO>> getPOSSales(@RequestParam Long companyId) {
        return ResponseEntity.ok(comercialService.getPOSSalesByCompany(companyId));
    }

    @PostMapping("/invoices")
    public ResponseEntity<InvoiceDTO> createInvoice(@RequestBody @Valid CreateInvoiceRequest request) {
        return ResponseEntity.ok(comercialService.createInvoice(request));
    }
}
