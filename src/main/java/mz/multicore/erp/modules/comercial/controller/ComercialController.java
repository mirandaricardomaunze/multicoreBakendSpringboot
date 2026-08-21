package mz.multicore.erp.modules.comercial.controller;

import mz.multicore.erp.architecture.concurrency.ConcurrencyRetry;
import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.modules.comercial.dto.*;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comercial")
public class ComercialController {

    private final ComercialService comercialService;
    private final ConcurrencyRetry concurrencyRetry;
    private final mz.multicore.erp.modules.comercial.service.InternalReplenishmentService replenishmentService;

    public ComercialController(ComercialService comercialService, ConcurrencyRetry concurrencyRetry,
            mz.multicore.erp.modules.comercial.service.InternalReplenishmentService replenishmentService) {
        this.comercialService = comercialService;
        this.concurrencyRetry = concurrencyRetry;
        this.replenishmentService = replenishmentService;
    }

    @GetMapping("/clients")
    public ResponseEntity<List<ClientDTO>> getClients() {
        return ResponseEntity.ok(comercialService.getAllClients());
    }

    @PostMapping("/clients")
    public ResponseEntity<ClientDTO> createClient(@RequestBody @Valid SaveClientRequest request) {
        return ResponseEntity.ok(comercialService.createClient(request));
    }

    @PutMapping("/clients/{id}")
    public ResponseEntity<ClientDTO> updateClient(@PathVariable Long id, @RequestBody @Valid SaveClientRequest request) {
        return ResponseEntity.ok(comercialService.updateClient(id, request));
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

    @GetMapping("/products/pos-catalog/page")
    public ResponseEntity<PageResponse<POSCatalogItemDTO>> getPOSCatalogPage(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "false") boolean availableOnly,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "36") Integer size) {
        return ResponseEntity.ok(comercialService.getPOSCatalogPage(query, availableOnly, page, size));
    }

    /** Localiza um produto pelo código de barras (leitor do POS). Corpo {@code null} se não existir. */
    @GetMapping("/products/by-barcode")
    public ResponseEntity<ProductDTO> findProductByBarcode(@RequestParam String barcode) {
        return ResponseEntity.ok(comercialService.findProductByBarcode(barcode));
    }

    @GetMapping("/products/pos-catalog/by-barcode")
    public ResponseEntity<POSCatalogItemDTO> findPOSCatalogItemByBarcode(@RequestParam String barcode) {
        return ResponseEntity.ok(comercialService.findPOSCatalogItemByBarcode(barcode));
    }

    @PostMapping("/products")
    public ResponseEntity<ProductDTO> createProduct(@RequestBody @Valid CreateProductRequest r) {
        return ResponseEntity.ok(comercialService.createProduct(
                r.sku(), r.reference(), r.barcode(), r.name(), r.unitPrice(), r.purchasePrice(), r.minStock(),
                r.unitsPerBox(), r.categoryId(), r.saleType(), r.stockTracked(), r.taxRateId(), r.description(),
                r.wholesalePrice(), r.wholesaleMinQty(), r.netUnitWeightKg(), r.grossUnitWeightKg()));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id, @RequestBody @Valid CreateProductRequest r) {
        return ResponseEntity.ok(comercialService.updateProduct(
                id, r.reference(), r.barcode(), r.name(), r.unitPrice(), r.purchasePrice(), r.minStock(),
                r.unitsPerBox(), r.categoryId(), r.saleType(), r.stockTracked(), r.taxRateId(), r.description(),
                r.wholesalePrice(), r.wholesaleMinQty(), r.netUnitWeightKg(), r.grossUnitWeightKg()));
    }

    @PostMapping(value = "/products/{id}/image", consumes = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Void> updateProductImage(@PathVariable Long id, @RequestBody byte[] image) {
        comercialService.updateProductImage(id, image);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/vat-rates")
    public ResponseEntity<List<mz.multicore.erp.modules.fiscal.dto.TaxRateDTO>> getVatRates() {
        return ResponseEntity.ok(comercialService.getActiveVatRates());
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceDTO>> getInvoices(@RequestParam(required = false) Long companyId) {
        return ResponseEntity.ok(companyId != null
                ? comercialService.getInvoicesByCompany(companyId)
                : comercialService.getAllInvoices());
    }

    /**
     * Página de faturas. Preferir a esta listagem sobre {@code /invoices}, que traz a tabela
     * toda. {@code page} começa em 0; {@code size} tem tecto no servidor (ver {@code PageQuery}).
     */
    @GetMapping("/invoices/page")
    public ResponseEntity<PageResponse<InvoiceDTO>> getInvoicePage(
            @RequestParam Long companyId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(comercialService.getInvoicePage(companyId, page, size));
    }

    /** Página do histórico de vendas do POS. */
    @GetMapping("/pos-sales/page")
    public ResponseEntity<PageResponse<InvoiceDTO>> getPOSSalesPage(
            @RequestParam Long companyId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(comercialService.getPOSSalesPage(companyId, page, size));
    }

    @GetMapping("/invoices/search")
    public ResponseEntity<List<InvoiceDTO>> searchInvoices(@RequestParam String query) {
        return ResponseEntity.ok(comercialService.searchInvoices(query));
    }

    @GetMapping("/invoices/outstanding")
    public ResponseEntity<List<InvoiceDTO>> getOutstandingInvoices(@RequestParam Long companyId) {
        return ResponseEntity.ok(comercialService.getOutstandingInvoicesByCompany(companyId));
    }

    /** Vendas de balcão (POS) da empresa — histórico do painel de vendas. */
    @GetMapping("/pos-sales")
    public ResponseEntity<List<InvoiceDTO>> getPOSSales(@RequestParam Long companyId) {
        return ResponseEntity.ok(comercialService.getPOSSalesByCompany(companyId));
    }

    @PostMapping("/invoices")
    public ResponseEntity<InvoiceDTO> createInvoice(@RequestBody @Valid CreateInvoiceRequest request) {
        // Rede de segurança para conflitos de concorrência (stock/tesouraria) entre postos.
        return ResponseEntity.ok(concurrencyRetry.run(() -> comercialService.createInvoice(request)));
    }

    @PostMapping("/invoices/{id}/cancel")
    public ResponseEntity<Void> cancelInvoice(@PathVariable Long id, @RequestBody CancelReasonRequest request) {
        comercialService.cancelInvoice(id, request.reason());
        return ResponseEntity.noContent().build();
    }

    // ─── Encomendas ──────────────────────────────────────────────────────────
    @GetMapping("/orders")
    public ResponseEntity<List<OrderDTO>> getOrders(@RequestParam Long companyId) {
        return ResponseEntity.ok(comercialService.getOrdersByCompany(companyId));
    }

    @GetMapping("/orders/pending")
    public ResponseEntity<List<OrderDTO>> getPendingOrders(@RequestParam Long companyId) {
        return ResponseEntity.ok(comercialService.getPendingOrdersByCompany(companyId));
    }

    @GetMapping("/orders/pending/search")
    public ResponseEntity<List<OrderDTO>> searchPendingOrders(@RequestParam String query) {
        return ResponseEntity.ok(comercialService.searchPendingOrders(query));
    }

    @GetMapping("/orders/cancellable/search")
    public ResponseEntity<List<OrderDTO>> searchCancellableOrders(@RequestParam String query) {
        return ResponseEntity.ok(comercialService.searchCancellableOrders(query));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(comercialService.getOrderById(id));
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderDTO> createOrder(@RequestBody @Valid CreateOrderRequest request) {
        return ResponseEntity.ok(comercialService.createOrder(request));
    }

    /** Converte uma reposição interna na transferência que a cumpre. Ver REPOSICAO_INTERNA_SPEC §4. */
    @PostMapping("/orders/{id}/transfer")
    public ResponseEntity<mz.multicore.erp.modules.inventory.dto.StockTransferDTO> convertToTransfer(
            @PathVariable Long id,
            @RequestBody(required = false) @Valid mz.multicore.erp.modules.comercial.dto.ConvertOrderToTransferRequest request) {
        return ResponseEntity.ok(replenishmentService.convertToTransfer(id, request));
    }

    @PostMapping("/orders/{id}/bill")
    public ResponseEntity<InvoiceDTO> billOrder(@PathVariable Long id) {
        return ResponseEntity.ok(comercialService.billOrder(id));
    }

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id, @RequestBody CancelReasonRequest request) {
        comercialService.cancelOrder(id, request.reason());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/orders/{id}/print")
    public ResponseEntity<OrderDTO> markOrderPrinted(@PathVariable Long id, @RequestParam String operator) {
        return ResponseEntity.ok(comercialService.markOrderPrinted(id, operator));
    }

    // ─── Recibos ─────────────────────────────────────────────────────────────
    @GetMapping("/receipts")
    public ResponseEntity<List<ReceiptDTO>> getReceipts(@RequestParam Long companyId) {
        return ResponseEntity.ok(comercialService.getReceiptsByCompany(companyId));
    }

    @PostMapping("/receipts")
    public ResponseEntity<ReceiptDTO> createReceipt(@RequestBody @Valid CreateReceiptRequest request) {
        return ResponseEntity.ok(comercialService.createReceipt(
                request.invoiceId(), request.treasuryAccountId(), request.paymentMethod(), request.amountPaid()));
    }

    @PostMapping("/receipts/{id}/cancel")
    public ResponseEntity<Void> cancelReceipt(@PathVariable Long id, @RequestBody CancelReasonRequest request) {
        comercialService.cancelReceipt(id, request.reason());
        return ResponseEntity.noContent().build();
    }
}
