package com.phcpro.desktop.client;

import com.phcpro.modules.comercial.dto.CancelReasonRequest;
import com.phcpro.modules.comercial.dto.ClientDTO;
import com.phcpro.modules.comercial.dto.CreateInvoiceRequest;
import com.phcpro.modules.comercial.dto.CreateOrderRequest;
import com.phcpro.modules.comercial.dto.CreateProductRequest;
import com.phcpro.modules.comercial.dto.CreateReceiptRequest;
import com.phcpro.modules.comercial.dto.InvoiceDTO;
import com.phcpro.modules.comercial.dto.OrderDTO;
import com.phcpro.modules.comercial.dto.ProductCategoryDTO;
import com.phcpro.modules.comercial.dto.ProductDTO;
import com.phcpro.modules.comercial.dto.ReceiptDTO;
import com.phcpro.modules.fiscal.dto.TaxRateDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Profile("desktop")
public class ComercialApiClient {

    private final DesktopClientFactory clientFactory;

    public ComercialApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<ClientDTO> getClients() {
        return clientFactory.authenticatedClient().getList("/api/comercial/clients", ClientDTO.class);
    }

    public ClientDTO createClient(String name, String taxId, String email, String address) {
        return clientFactory.authenticatedClient().post(
                "/api/comercial/clients", new SaveClientRequest(name, taxId, email, address), ClientDTO.class);
    }

    public ClientDTO updateClient(Long id, String name, String taxId, String email, String address) {
        return clientFactory.authenticatedClient().put(
                "/api/comercial/clients/" + id, new SaveClientRequest(name, taxId, email, address), ClientDTO.class);
    }

    public void deleteClient(Long id) {
        clientFactory.authenticatedClient().delete("/api/comercial/clients/" + id);
    }

    public List<InvoiceDTO> getAllInvoices() {
        return clientFactory.authenticatedClient().getList("/api/comercial/invoices", InvoiceDTO.class);
    }

    public List<ProductDTO> getAllProducts() {
        return clientFactory.authenticatedClient().getList("/api/comercial/products", ProductDTO.class);
    }

    public List<ClientDTO> getAllClients() {
        return getClients();
    }

    /** Produtos vendáveis no POS (stock rastreado + activos). */
    public List<ProductDTO> getSellableProducts() {
        return clientFactory.authenticatedClient().getList("/api/comercial/products/sellable", ProductDTO.class);
    }

    /** Produto pelo código de barras (leitor do POS); {@code null} se não existir. */
    public ProductDTO findProductByBarcode(String barcode) {
        String enc = URLEncoder.encode(barcode == null ? "" : barcode, StandardCharsets.UTF_8);
        return clientFactory.authenticatedClient()
                .get("/api/comercial/products/by-barcode?barcode=" + enc, ProductDTO.class);
    }

    /** Histórico de vendas de balcão (POS) da empresa. */
    public List<InvoiceDTO> getPOSSalesByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/comercial/pos-sales?companyId=" + companyId, InvoiceDTO.class);
    }

    public List<ProductCategoryDTO> getActiveCategories() {
        return clientFactory.authenticatedClient()
                .getList("/api/product-categories?onlyActive=true", ProductCategoryDTO.class);
    }

    public List<TaxRateDTO> getActiveVatRates() {
        return clientFactory.authenticatedClient().getList("/api/comercial/vat-rates", TaxRateDTO.class);
    }

    public ProductDTO createProduct(String sku, String reference, String barcode, String name, BigDecimal unitPrice,
            BigDecimal purchasePrice, BigDecimal minStock, int unitsPerBox, Long categoryId, String saleType,
            boolean stockTracked, Long taxRateId, String description, BigDecimal wholesalePrice,
            BigDecimal wholesaleMinQty) {
        return clientFactory.authenticatedClient().post("/api/comercial/products",
                new CreateProductRequest(sku, reference, barcode, name, unitPrice, purchasePrice, minStock,
                        unitsPerBox, categoryId, saleType, stockTracked, taxRateId, description, wholesalePrice,
                        wholesaleMinQty), ProductDTO.class);
    }

    public ProductDTO updateProduct(Long id, String reference, String barcode, String name, BigDecimal unitPrice,
            BigDecimal purchasePrice, BigDecimal minStock, int unitsPerBox, Long categoryId, String saleType,
            boolean stockTracked, Long taxRateId, String description, BigDecimal wholesalePrice,
            BigDecimal wholesaleMinQty) {
        return clientFactory.authenticatedClient().put("/api/comercial/products/" + id,
                new CreateProductRequest(null, reference, barcode, name, unitPrice, purchasePrice, minStock,
                        unitsPerBox, categoryId, saleType, stockTracked, taxRateId, description, wholesalePrice,
                        wholesaleMinQty), ProductDTO.class);
    }

    public void updateProductImage(Long productId, byte[] imageData) {
        clientFactory.authenticatedClient().postBytes("/api/comercial/products/" + productId + "/image", imageData);
    }

    // ─── Faturas ─────────────────────────────────────────────────────────────
    public InvoiceDTO createInvoice(CreateInvoiceRequest request) {
        return clientFactory.authenticatedClient().post("/api/comercial/invoices", request, InvoiceDTO.class);
    }

    public List<InvoiceDTO> getInvoicesByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/comercial/invoices?companyId=" + companyId, InvoiceDTO.class);
    }

    public List<InvoiceDTO> searchInvoices(String query) {
        return clientFactory.authenticatedClient()
                .getList("/api/comercial/invoices/search?query=" + enc(query), InvoiceDTO.class);
    }

    public List<InvoiceDTO> getOutstandingInvoicesByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/comercial/invoices/outstanding?companyId=" + companyId, InvoiceDTO.class);
    }

    public void cancelInvoice(Long invoiceId, String reason) {
        clientFactory.authenticatedClient()
                .post("/api/comercial/invoices/" + invoiceId + "/cancel", new CancelReasonRequest(reason));
    }

    // ─── Encomendas ──────────────────────────────────────────────────────────
    public List<OrderDTO> getOrdersByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/comercial/orders?companyId=" + companyId, OrderDTO.class);
    }

    public List<OrderDTO> getPendingOrdersByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/comercial/orders/pending?companyId=" + companyId, OrderDTO.class);
    }

    public List<OrderDTO> searchPendingOrders(String query) {
        return clientFactory.authenticatedClient()
                .getList("/api/comercial/orders/pending/search?query=" + enc(query), OrderDTO.class);
    }

    public List<OrderDTO> searchCancellableOrders(String query) {
        return clientFactory.authenticatedClient()
                .getList("/api/comercial/orders/cancellable/search?query=" + enc(query), OrderDTO.class);
    }

    public OrderDTO getOrderById(Long orderId) {
        return clientFactory.authenticatedClient().get("/api/comercial/orders/" + orderId, OrderDTO.class);
    }

    public OrderDTO createOrder(CreateOrderRequest request) {
        return clientFactory.authenticatedClient().post("/api/comercial/orders", request, OrderDTO.class);
    }

    public InvoiceDTO billOrder(Long orderId) {
        return clientFactory.authenticatedClient()
                .post("/api/comercial/orders/" + orderId + "/bill", null, InvoiceDTO.class);
    }

    public void cancelOrder(Long orderId, String reason) {
        clientFactory.authenticatedClient()
                .post("/api/comercial/orders/" + orderId + "/cancel", new CancelReasonRequest(reason));
    }

    public OrderDTO markOrderPrinted(Long orderId, String operator) {
        return clientFactory.authenticatedClient().post(
                "/api/comercial/orders/" + orderId + "/print?operator=" + enc(operator), null, OrderDTO.class);
    }

    // ─── Recibos ─────────────────────────────────────────────────────────────
    public List<ReceiptDTO> getReceiptsByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/comercial/receipts?companyId=" + companyId, ReceiptDTO.class);
    }

    public ReceiptDTO createReceipt(Long invoiceId, Long treasuryAccountId, String paymentMethod,
                                    BigDecimal amountPaid) {
        return clientFactory.authenticatedClient().post("/api/comercial/receipts",
                new CreateReceiptRequest(invoiceId, treasuryAccountId, paymentMethod, amountPaid), ReceiptDTO.class);
    }

    public void cancelReceipt(Long receiptId, String reason) {
        clientFactory.authenticatedClient()
                .post("/api/comercial/receipts/" + receiptId + "/cancel", new CancelReasonRequest(reason));
    }

    // ─── Impressões (PDF) ────────────────────────────────────────────────────
    public byte[] renderInvoice(Long invoiceId) {
        return clientFactory.authenticatedClient().getBytes("/api/print/invoice/" + invoiceId);
    }

    public byte[] renderOrder(Long orderId) {
        return clientFactory.authenticatedClient().getBytes("/api/print/order/" + orderId);
    }

    public byte[] renderGuide(Long invoiceId) {
        return clientFactory.authenticatedClient().getBytes("/api/print/guide/" + invoiceId);
    }

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    record SaveClientRequest(String name, String taxId, String email, String address) {}
}
