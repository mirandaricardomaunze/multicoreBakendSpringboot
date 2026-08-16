package com.phcpro.desktop.client;

import com.phcpro.architecture.paging.PageResponse;
import com.phcpro.modules.comercial.dto.AgingSummaryDTO;
import com.phcpro.modules.comercial.dto.CancelReasonRequest;
import com.phcpro.modules.comercial.dto.ClientDTO;
import com.phcpro.modules.comercial.dto.CreateInvoiceRequest;
import com.phcpro.modules.comercial.dto.CreateOrderRequest;
import com.phcpro.modules.comercial.dto.CreateDeliveryGuideRequest;
import com.phcpro.modules.comercial.dto.CreateProductRequest;
import com.phcpro.modules.comercial.dto.CreateReceiptRequest;
import com.phcpro.modules.comercial.dto.DeliveryGuideDTO;
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
        return createClient(name, taxId, email, address, 0, null);
    }

    public ClientDTO createClient(String name, String taxId, String email, String address,
                                  int paymentTermsDays, BigDecimal creditLimit) {
        return clientFactory.authenticatedClient().post("/api/comercial/clients",
                new SaveClientRequest(name, taxId, email, address, paymentTermsDays, creditLimit), ClientDTO.class);
    }

    public ClientDTO updateClient(Long id, String name, String taxId, String email, String address) {
        return updateClient(id, name, taxId, email, address, 0, null);
    }

    public ClientDTO updateClient(Long id, String name, String taxId, String email, String address,
                                  int paymentTermsDays, BigDecimal creditLimit) {
        return clientFactory.authenticatedClient().put("/api/comercial/clients/" + id,
                new SaveClientRequest(name, taxId, email, address, paymentTermsDays, creditLimit), ClientDTO.class);
    }

    /** Mapa de antiguidade de saldos (contas a receber) à data de hoje no servidor. */
    public AgingSummaryDTO getReceivablesAging() {
        return clientFactory.authenticatedClient()
                .get("/api/comercial/receivables/aging", AgingSummaryDTO.class);
    }

    public void deleteClient(Long id) {
        clientFactory.authenticatedClient().delete("/api/comercial/clients/" + id);
    }

    public List<InvoiceDTO> getAllInvoices() {
        return clientFactory.authenticatedClient().getList("/api/comercial/invoices", InvoiceDTO.class);
    }

    /** Página de faturas (a listagem completa fica para os ecrãs ainda não migrados). */
    @SuppressWarnings("unchecked")
    public PageResponse<InvoiceDTO> getInvoicePage(Long companyId, int page, int size) {
        return clientFactory.authenticatedClient().getGeneric(
                "/api/comercial/invoices/page?companyId=" + companyId + "&page=" + page + "&size=" + size,
                PageResponse.class, InvoiceDTO.class);
    }

    /** Página do histórico de vendas do POS. */
    @SuppressWarnings("unchecked")
    public PageResponse<InvoiceDTO> getPOSSalesPage(Long companyId, int page, int size) {
        return clientFactory.authenticatedClient().getGeneric(
                "/api/comercial/pos-sales/page?companyId=" + companyId + "&page=" + page + "&size=" + size,
                PageResponse.class, InvoiceDTO.class);
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

    public PageResponse<com.phcpro.modules.comercial.dto.POSCatalogItemDTO> getPOSCatalogPage(
            String query, boolean availableOnly, int page, int size) {
        String encoded = URLEncoder.encode(query == null ? "" : query, StandardCharsets.UTF_8);
        String path = "/api/comercial/products/pos-catalog/page?query=" + encoded
                + "&availableOnly=" + availableOnly + "&page=" + page + "&size=" + size;
        return clientFactory.authenticatedClient().getGeneric(path, PageResponse.class,
                com.phcpro.modules.comercial.dto.POSCatalogItemDTO.class);
    }

    public com.phcpro.modules.comercial.dto.POSCatalogItemDTO findPOSCatalogItemByBarcode(String barcode) {
        String encoded = URLEncoder.encode(barcode == null ? "" : barcode, StandardCharsets.UTF_8);
        return clientFactory.authenticatedClient().get(
                "/api/comercial/products/pos-catalog/by-barcode?barcode=" + encoded,
                com.phcpro.modules.comercial.dto.POSCatalogItemDTO.class);
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

    // ─── Guias de Remessa (GR) ───────────────────────────────────────────────
    public List<DeliveryGuideDTO> getDeliveryGuidesByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/comercial/delivery-guides?companyId=" + companyId, DeliveryGuideDTO.class);
    }

    public DeliveryGuideDTO getDeliveryGuideById(Long id) {
        return clientFactory.authenticatedClient()
                .get("/api/comercial/delivery-guides/" + id, DeliveryGuideDTO.class);
    }

    public DeliveryGuideDTO createDeliveryGuide(Long orderId, String responsible, String vehicle, String notes) {
        return clientFactory.authenticatedClient().post("/api/comercial/delivery-guides",
                new CreateDeliveryGuideRequest(orderId, responsible, vehicle, notes), DeliveryGuideDTO.class);
    }

    public DeliveryGuideDTO approveDeliveryGuide(Long id) {
        return clientFactory.authenticatedClient()
                .post("/api/comercial/delivery-guides/" + id + "/approve", null, DeliveryGuideDTO.class);
    }

    public DeliveryGuideDTO rejectDeliveryGuide(Long id, String reason) {
        return clientFactory.authenticatedClient().post(
                "/api/comercial/delivery-guides/" + id + "/reject", new RejectGuideRequest(reason), DeliveryGuideDTO.class);
    }

    public DeliveryGuideDTO cancelDeliveryGuide(Long id) {
        return clientFactory.authenticatedClient()
                .post("/api/comercial/delivery-guides/" + id + "/cancel", null, DeliveryGuideDTO.class);
    }

    public byte[] renderDeliveryGuide(Long id) {
        return clientFactory.authenticatedClient().getBytes("/api/print/delivery-guide/" + id);
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

    /**
     * Espelha o corpo esperado pelo controller. {@code paymentTermsDays} = prazo acordado, em
     * dias; {@code creditLimit} nulo = sem limite de crédito.
     */
    record SaveClientRequest(String name, String taxId, String email, String address,
                             int paymentTermsDays, BigDecimal creditLimit) {}

    /** Corpo do reject da guia — chave "rejectionReason" espelha o esperado pelo controller. */
    record RejectGuideRequest(String rejectionReason) {}
}
