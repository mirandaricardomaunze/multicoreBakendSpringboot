package mz.multicore.erp.desktop.client;

import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.modules.comercial.dto.*;
import mz.multicore.erp.modules.fiscal.dto.TaxRateDTO;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Fachada canónica de cliente HTTP para o módulo Comercial no Desktop.
 * Delega para sub-clientes especializados por subdomínio comercial, preservando 100%
 * de compatibilidade retrocompatível com todos os ecrãs e testes.
 */
@Component
@Profile("desktop")
public class ComercialApiClient {

    private final CommercialClientApiClient clientApiClient;
    private final CommercialProductApiClient productApiClient;
    private final CommercialOrderApiClient orderApiClient;
    private final CommercialQuotationApiClient quotationApiClient;
    private final CommercialDeliveryGuideApiClient deliveryGuideApiClient;
    private final CommercialInvoiceApiClient invoiceApiClient;

    public ComercialApiClient(DesktopClientFactory clientFactory) {
        this(clientFactory,
             new CommercialClientApiClient(clientFactory),
             new CommercialProductApiClient(clientFactory),
             new CommercialOrderApiClient(clientFactory),
             new CommercialQuotationApiClient(clientFactory),
             new CommercialDeliveryGuideApiClient(clientFactory),
             new CommercialInvoiceApiClient(clientFactory));
    }

    @Autowired
    public ComercialApiClient(DesktopClientFactory clientFactory,
                              CommercialClientApiClient clientApiClient,
                              CommercialProductApiClient productApiClient,
                              CommercialOrderApiClient orderApiClient,
                              CommercialQuotationApiClient quotationApiClient,
                              CommercialDeliveryGuideApiClient deliveryGuideApiClient,
                              CommercialInvoiceApiClient invoiceApiClient) {
        this.clientApiClient = clientApiClient;
        this.productApiClient = productApiClient;
        this.orderApiClient = orderApiClient;
        this.quotationApiClient = quotationApiClient;
        this.deliveryGuideApiClient = deliveryGuideApiClient;
        this.invoiceApiClient = invoiceApiClient;
    }

    // ─── Clientes & Crédito ──────────────────────────────────────────────────
    public List<ClientDTO> getClients() { return clientApiClient.getClients(); }
    public List<ClientDTO> getAllClients() { return clientApiClient.getClients(); }
    public ClientCreditEligibilityDTO getClientCreditEligibility(Long clientId) { return clientApiClient.getClientCreditEligibility(clientId); }
    public ClientDTO createClient(String name, String taxId, String email, String address) { return createClient(name, taxId, email, address, 0, null); }
    public ClientDTO createClient(String name, String taxId, String email, String address, int paymentTermsDays, BigDecimal creditLimit) {
        return clientApiClient.createClient(name, taxId, email, address, paymentTermsDays, creditLimit);
    }
    public ClientDTO updateClient(Long id, String name, String taxId, String email, String address) { return updateClient(id, name, taxId, email, address, 0, null); }
    public ClientDTO updateClient(Long id, String name, String taxId, String email, String address, int paymentTermsDays, BigDecimal creditLimit) {
        return clientApiClient.updateClient(id, name, taxId, email, address, paymentTermsDays, creditLimit);
    }
    public AgingSummaryDTO getReceivablesAging() { return clientApiClient.getReceivablesAging(); }
    public void deleteClient(Long id) { clientApiClient.deleteClient(id); }

    // ─── Artigos & Catálogo POS ──────────────────────────────────────────────
    public List<ProductDTO> getAllProducts() { return productApiClient.getAllProducts(); }
    public PageResponse<ProductDTO> getProductsPage(String query, Long categoryId, int page, int size) {
        return productApiClient.getProductsPage(query, categoryId, page, size);
    }
    public List<ProductDTO> getSellableProducts() { return productApiClient.getSellableProducts(); }
    public PageResponse<POSCatalogItemDTO> getPOSCatalogPage(String query, boolean availableOnly, int page, int size) {
        return productApiClient.getPOSCatalogPage(query, availableOnly, null, page, size);
    }
    public PageResponse<POSCatalogItemDTO> getPOSCatalogPage(String query, boolean availableOnly, Long warehouseId, int page, int size) {
        return productApiClient.getPOSCatalogPage(query, availableOnly, warehouseId, page, size);
    }
    public POSCatalogItemDTO findPOSCatalogItemByBarcode(String barcode) { return productApiClient.findPOSCatalogItemByBarcode(barcode, null); }
    public POSCatalogItemDTO findPOSCatalogItemByBarcode(String barcode, Long warehouseId) { return productApiClient.findPOSCatalogItemByBarcode(barcode, warehouseId); }
    public ProductDTO findProductByBarcode(String barcode) { return productApiClient.findProductByBarcode(barcode); }
    public List<ProductCategoryDTO> getActiveCategories() { return productApiClient.getActiveCategories(); }
    public List<TaxRateDTO> getActiveVatRates() { return productApiClient.getActiveVatRates(); }
    public ProductDTO createProduct(String sku, String reference, String barcode, String name, BigDecimal unitPrice,
            BigDecimal purchasePrice, BigDecimal minStock, int unitsPerBox, int packagesPerBox, int unitsPerPackage,
            Long categoryId, String saleType, boolean stockTracked, Long taxRateId, String description,
            BigDecimal wholesalePrice, BigDecimal wholesaleMinQty, BigDecimal netUnitWeightKg, BigDecimal grossUnitWeightKg) {
        return createProduct(sku, reference, barcode, name, unitPrice, purchasePrice, minStock, unitsPerBox,
                packagesPerBox, unitsPerPackage, categoryId, saleType, stockTracked, taxRateId, description,
                wholesalePrice, wholesaleMinQty, netUnitWeightKg, grossUnitWeightKg, null, null);
    }
    public ProductDTO createProduct(String sku, String reference, String barcode, String name, BigDecimal unitPrice,
            BigDecimal purchasePrice, BigDecimal minStock, int unitsPerBox, int packagesPerBox, int unitsPerPackage,
            Long categoryId, String saleType, boolean stockTracked, Long taxRateId, String description,
            BigDecimal wholesalePrice, BigDecimal wholesaleMinQty, BigDecimal netUnitWeightKg, BigDecimal grossUnitWeightKg,
            Long businessUnitId, Boolean tracksExpiry) {
        return productApiClient.createProduct(new CreateProductRequest(sku, reference, barcode, name, unitPrice, purchasePrice,
                minStock, unitsPerBox, packagesPerBox, unitsPerPackage, categoryId, saleType, stockTracked, taxRateId,
                description, wholesalePrice, wholesaleMinQty, netUnitWeightKg, grossUnitWeightKg, businessUnitId, tracksExpiry));
    }
    public ProductDTO updateProduct(Long id, String reference, String barcode, String name, BigDecimal unitPrice,
            BigDecimal purchasePrice, BigDecimal minStock, int unitsPerBox, int packagesPerBox, int unitsPerPackage,
            Long categoryId, String saleType, boolean stockTracked, Long taxRateId, String description,
            BigDecimal wholesalePrice, BigDecimal wholesaleMinQty, BigDecimal netUnitWeightKg, BigDecimal grossUnitWeightKg) {
        return updateProduct(id, reference, barcode, name, unitPrice, purchasePrice, minStock, unitsPerBox,
                packagesPerBox, unitsPerPackage, categoryId, saleType, stockTracked, taxRateId, description,
                wholesalePrice, wholesaleMinQty, netUnitWeightKg, grossUnitWeightKg, null, null);
    }
    public ProductDTO updateProduct(Long id, String reference, String barcode, String name, BigDecimal unitPrice,
            BigDecimal purchasePrice, BigDecimal minStock, int unitsPerBox, int packagesPerBox, int unitsPerPackage,
            Long categoryId, String saleType, boolean stockTracked, Long taxRateId, String description,
            BigDecimal wholesalePrice, BigDecimal wholesaleMinQty, BigDecimal netUnitWeightKg, BigDecimal grossUnitWeightKg,
            Long businessUnitId, Boolean tracksExpiry) {
        return productApiClient.updateProduct(id, new CreateProductRequest(null, reference, barcode, name, unitPrice,
                purchasePrice, minStock, unitsPerBox, packagesPerBox, unitsPerPackage, categoryId, saleType,
                stockTracked, taxRateId, description, wholesalePrice, wholesaleMinQty, netUnitWeightKg,
                grossUnitWeightKg, businessUnitId, tracksExpiry));
    }
    public void updateProductImage(Long productId, byte[] imageData) { productApiClient.updateProductImage(productId, imageData); }

    // ─── Encomendas ──────────────────────────────────────────────────────────
    public List<OrderDTO> getOrdersByCompany(Long companyId) { return orderApiClient.getOrdersByCompany(companyId); }
    // Delega para CommercialOrderApiClient: /api/comercial/orders/page
    public PageResponse<OrderDTO> getOrdersPage(Long companyId, int page, int size) { return orderApiClient.getOrdersPage(companyId, page, size); }
    public List<OrderDTO> getPendingOrdersByCompany(Long companyId) { return orderApiClient.getPendingOrdersByCompany(companyId); }
    public List<OrderDTO> searchPendingOrders(String query) { return orderApiClient.searchPendingOrders(query); }
    public List<OrderDTO> searchCancellableOrders(String query) { return orderApiClient.searchCancellableOrders(query); }
    public OrderDTO getOrderById(Long orderId) { return orderApiClient.getOrderById(orderId); }
    public OrderDTO createOrder(CreateOrderRequest request) { return orderApiClient.createOrder(request); }
    public OrderDTO updateOrder(Long id, UpdateOrderRequest request) {
        // Delega para CommercialOrderApiClient .put("/api/comercial/orders/" + id
        return orderApiClient.updateOrder(id, request);
    }
    public OrderDTO submitFulfillmentOrder(CreateOrderRequest req, String k, String t) { return orderApiClient.submitFulfillmentOrder(req, k, t); }
    public byte[] printPicking(Long id, String t) { return orderApiClient.printPicking(id, t); }
    public byte[] reprintPicking(Long id, String u, String p, String r, String t) { return orderApiClient.reprintPicking(id, u, p, r, t); }
    public OrderPickingDetailsDTO getOrderPickingDetails(Long id) { return orderApiClient.getOrderPickingDetails(id); }
    public byte[] printPickingA4(Long id, String t) { return orderApiClient.printPickingA4(id, t); }
    public OrderDTO confirmOrderPicking(Long id, CompleteOrderPickingRequest r) { return orderApiClient.confirmOrderPicking(id, r); }
    public OrderDTO completeSeparation(Long id, String t) { return orderApiClient.completeSeparation(id, t); }
    public InvoiceDTO billFulfillmentOrder(Long id, String t) { return orderApiClient.billFulfillmentOrder(id, t); }
    public List<OrderEventDTO> getOrderEvents(Long id) { return orderApiClient.getOrderEvents(id); }
    public InvoiceDTO billOrder(Long orderId) { return orderApiClient.billOrder(orderId); }
    public void cancelOrder(Long orderId, String reason) { orderApiClient.cancelOrder(orderId, reason); }
    public OrderDTO approveOrder(Long orderId, String comments) { return orderApiClient.approveOrder(orderId, comments); }
    public OrderDTO rejectOrder(Long orderId, String reason) { return orderApiClient.rejectOrder(orderId, reason); }
    public OrderDTO markOrderPrinted(Long orderId, String operator) { return orderApiClient.markOrderPrinted(orderId, operator); }

    // ─── Cotações ────────────────────────────────────────────────────────────
    public List<QuotationDTO> getQuotationsByCompany(Long companyId) { return quotationApiClient.getQuotationsByCompany(companyId); }
    public QuotationDTO getQuotationById(Long id) { return quotationApiClient.getQuotationById(id); }
    public QuotationDTO createQuotation(CreateQuotationRequest req) { return quotationApiClient.createQuotation(req); }
    public QuotationDTO updateQuotation(Long id, UpdateQuotationRequest req) { return quotationApiClient.updateQuotation(id, req); }
    public QuotationDTO sendQuotation(Long id) { return quotationApiClient.sendQuotation(id); }
    public QuotationDTO acceptQuotation(Long id) { return quotationApiClient.acceptQuotation(id); }
    public QuotationDTO rejectQuotation(Long id, String reason) { return quotationApiClient.rejectQuotation(id, reason); }
    public QuotationDTO cancelQuotation(Long id) { return quotationApiClient.cancelQuotation(id); }
    public QuotationDTO extendQuotationValidity(Long id, LocalDate validUntil) { return quotationApiClient.extendQuotationValidity(id, validUntil); }
    public OrderDTO convertQuotation(Long id) { return quotationApiClient.convertQuotation(id); }
    public InvoiceDTO convertQuotationToInvoice(Long id) { return quotationApiClient.convertQuotationToInvoice(id); }
    public List<QuotationDTO> getOpenQuotationsByCompany(Long companyId) { return quotationApiClient.getOpenQuotationsByCompany(companyId); }
    public byte[] renderQuotation(Long id) { return quotationApiClient.renderQuotation(id); }
    public byte[] renderQuotationThermal(Long id) { return quotationApiClient.renderQuotationThermal(id); }

    // ─── Guias de Remessa ────────────────────────────────────────────────────
    public List<DeliveryGuideDTO> getDeliveryGuidesByCompany(Long companyId) { return deliveryGuideApiClient.getDeliveryGuidesByCompany(companyId); }
    public DeliveryGuideDTO getDeliveryGuideById(Long id) { return deliveryGuideApiClient.getDeliveryGuideById(id); }
    public mz.multicore.erp.modules.inventory.dto.StockTransferDTO convertOrderToTransfer(Long orderId, String resp, String veh, String notes) {
        return convertOrderToTransfer(orderId, resp, veh, notes, null, null);
    }
    public mz.multicore.erp.modules.inventory.dto.StockTransferDTO convertOrderToTransfer(
            Long orderId, String resp, String veh, String notes, String driver, String plate) {
        return deliveryGuideApiClient.convertOrderToTransfer(orderId, resp, veh, notes, driver, plate);
    }
    public DeliveryGuideDTO createDeliveryGuide(Long orderId, String resp, String veh, String notes) {
        return deliveryGuideApiClient.createDeliveryGuide(orderId, resp, veh, notes);
    }
    public DeliveryGuideDTO createStandaloneDeliveryGuide(CreateStandaloneDeliveryGuideRequest req) { return deliveryGuideApiClient.createStandaloneDeliveryGuide(req); }
    public DeliveryGuideDTO approveDeliveryGuide(Long id) { return deliveryGuideApiClient.approveDeliveryGuide(id); }
    public InvoiceDTO billDeliveryGuide(Long id) { return deliveryGuideApiClient.billDeliveryGuide(id); }
    public DeliveryGuideDTO rejectDeliveryGuide(Long id, String reason) { return deliveryGuideApiClient.rejectDeliveryGuide(id, reason); }
    public DeliveryGuideDTO cancelDeliveryGuide(Long id) { return deliveryGuideApiClient.cancelDeliveryGuide(id); }
    public byte[] renderDeliveryGuide(Long id) { return deliveryGuideApiClient.renderDeliveryGuide(id); }

    // ─── Faturas, Recibos & Cobrança ─────────────────────────────────────────
    public List<InvoiceDTO> getAllInvoices() { return invoiceApiClient.getAllInvoices(); }
    public PageResponse<InvoiceDTO> getInvoicePage(Long companyId, int page, int size) { return invoiceApiClient.getInvoicePage(companyId, page, size); }
    public PageResponse<InvoiceDTO> getPOSSalesPage(Long companyId, int page, int size) { return invoiceApiClient.getPOSSalesPage(companyId, page, size, null, null); }
    public PageResponse<InvoiceDTO> getPOSSalesPage(Long companyId, int page, int size, LocalDate from, LocalDate to) {
        return invoiceApiClient.getPOSSalesPage(companyId, page, size, from, to);
    }
    public POSSalesSummaryDTO getPOSSalesSummary(Long companyId, LocalDate from, LocalDate to) { return invoiceApiClient.getPOSSalesSummary(companyId, from, to); }
    public List<InvoiceDTO> getPOSSalesByCompany(Long companyId) { return invoiceApiClient.getPOSSalesByCompany(companyId); }
    public InvoiceDTO createInvoice(CreateInvoiceRequest request) { return invoiceApiClient.createInvoice(request); }
    public List<InvoiceDTO> getInvoicesByCompany(Long companyId) { return invoiceApiClient.getInvoicesByCompany(companyId); }
    public List<InvoiceDTO> searchInvoices(String query) { return invoiceApiClient.searchInvoices(query); }
    public List<InvoiceDTO> getOutstandingInvoicesByCompany(Long companyId) { return invoiceApiClient.getOutstandingInvoicesByCompany(companyId); }
    public void cancelInvoice(Long invoiceId, String reason) { invoiceApiClient.cancelInvoice(invoiceId, reason); }
    public List<ReceiptDTO> getReceiptsByCompany(Long companyId) { return invoiceApiClient.getReceiptsByCompany(companyId); }
    public ReceiptDTO createReceipt(Long invId, Long accId, String method, BigDecimal amount) { return invoiceApiClient.createReceipt(invId, accId, method, amount); }
    public void cancelReceipt(Long receiptId, String reason) { invoiceApiClient.cancelReceipt(receiptId, reason); }
    public byte[] renderInvoice(Long invoiceId) { return invoiceApiClient.renderInvoice(invoiceId); }
    public byte[] renderInvoiceThermal(Long invoiceId) { return invoiceApiClient.renderInvoiceThermal(invoiceId); }
    public byte[] renderOrder(Long orderId) { return invoiceApiClient.renderOrder(orderId); }
    public byte[] renderGuide(Long invoiceId) { return invoiceApiClient.renderGuide(invoiceId); }
    public List<BusinessUnitDTO> getActiveBusinessUnits(Long companyId) { return invoiceApiClient.getActiveBusinessUnits(companyId); }
    public List<InvoiceInstallmentDTO> createInvoiceInstallmentPlan(Long invoiceId, CreateInstallmentPlanRequest req) { return invoiceApiClient.createInvoiceInstallmentPlan(invoiceId, req); }
    public List<InvoiceInstallmentDTO> getInvoiceInstallments(Long invoiceId) { return invoiceApiClient.getInvoiceInstallments(invoiceId); }
    public List<InvoiceInstallmentDTO> payInvoiceInstallment(Long invoiceId, BigDecimal amount, Long financeAccountId) { return invoiceApiClient.payInvoiceInstallment(invoiceId, amount, financeAccountId); }
    public List<mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO> treasuryAccounts() { return invoiceApiClient.treasuryAccounts(); }
    public CollectionRadarSummaryDTO getCollectionRadarSummary(Long companyId) { return invoiceApiClient.getCollectionRadarSummary(companyId); }
    public List<CollectionRadarItemDTO> getCollectionRadarItems(Long companyId, String bucket) { return invoiceApiClient.getCollectionRadarItems(companyId, bucket); }
    public CollectionReminderTemplateDTO getCollectionReminderTemplate(Long invoiceId, String channel) { return invoiceApiClient.getCollectionReminderTemplate(invoiceId, channel); }
    public List<ReturnableInvoiceLineDTO> getReturnableInvoiceLines(Long invoiceId) { return invoiceApiClient.getReturnableInvoiceLines(invoiceId); }
    public SalesReturnWizardResultDTO processSalesReturn(Long invoiceId, SalesReturnWizardRequest req) { return invoiceApiClient.processSalesReturn(invoiceId, req); }

    // Compatibilidade retrocompatível de classes internas
    public static record SaveClientRequest(String name, String taxId, String email, String address, int paymentTermsDays, BigDecimal creditLimit) {}
    public static record RejectGuideRequest(String rejectionReason) {}
}
