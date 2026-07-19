package com.phcpro.desktop.client;

import com.phcpro.modules.comercial.dto.ClientDTO;
import com.phcpro.modules.comercial.dto.CreateProductRequest;
import com.phcpro.modules.comercial.dto.InvoiceDTO;
import com.phcpro.modules.comercial.dto.ProductCategoryDTO;
import com.phcpro.modules.comercial.dto.ProductDTO;
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

    record SaveClientRequest(String name, String taxId, String email, String address) {}
}
