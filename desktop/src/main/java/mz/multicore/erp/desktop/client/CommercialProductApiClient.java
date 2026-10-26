package mz.multicore.erp.desktop.client;

import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.modules.comercial.dto.CreateProductRequest;
import mz.multicore.erp.modules.comercial.dto.POSCatalogItemDTO;
import mz.multicore.erp.modules.comercial.dto.ProductCategoryDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.fiscal.dto.TaxRateDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Cliente HTTP especializado para catálogo de artigos, preços, IVA e produtos no POS.
 */
@Component
@Profile("desktop")
public class CommercialProductApiClient {

    private final DesktopClientFactory clientFactory;

    public CommercialProductApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<ProductDTO> getAllProducts() {
        return clientFactory.authenticatedClient().getList("/api/comercial/products", ProductDTO.class);
    }

    @SuppressWarnings("unchecked")
    public PageResponse<ProductDTO> getProductsPage(String query, Long categoryId, int page, int size) {
        StringBuilder path = new StringBuilder("/api/comercial/products/page?page=").append(page)
                .append("&size=").append(size);
        if (query != null && !query.isBlank()) {
            path.append("&query=").append(URLEncoder.encode(query.trim(), StandardCharsets.UTF_8));
        }
        if (categoryId != null) {
            path.append("&categoryId=").append(categoryId);
        }
        return clientFactory.authenticatedClient().getGeneric(path.toString(), PageResponse.class, ProductDTO.class);
    }

    public List<ProductDTO> getSellableProducts() {
        return clientFactory.authenticatedClient().getList("/api/comercial/products/sellable", ProductDTO.class);
    }

    public PageResponse<POSCatalogItemDTO> getPOSCatalogPage(
            String query, boolean availableOnly, Long warehouseId, int page, int size) {
        String encoded = URLEncoder.encode(query == null ? "" : query, StandardCharsets.UTF_8);
        String path = "/api/comercial/products/pos-catalog/page?query=" + encoded
                + "&availableOnly=" + availableOnly
                + (warehouseId != null ? "&warehouseId=" + warehouseId : "")
                + "&page=" + page + "&size=" + size;
        return clientFactory.authenticatedClient().getGeneric(path, PageResponse.class, POSCatalogItemDTO.class);
    }

    public POSCatalogItemDTO findPOSCatalogItemByBarcode(String barcode, Long warehouseId) {
        String encoded = URLEncoder.encode(barcode == null ? "" : barcode, StandardCharsets.UTF_8);
        String path = "/api/comercial/products/pos-catalog/by-barcode?barcode=" + encoded
                + (warehouseId != null ? "&warehouseId=" + warehouseId : "");
        return clientFactory.authenticatedClient().get(path, POSCatalogItemDTO.class);
    }

    public ProductDTO findProductByBarcode(String barcode) {
        String enc = URLEncoder.encode(barcode == null ? "" : barcode, StandardCharsets.UTF_8);
        return clientFactory.authenticatedClient()
                .get("/api/comercial/products/by-barcode?barcode=" + enc, ProductDTO.class);
    }

    public List<ProductCategoryDTO> getActiveCategories() {
        return clientFactory.authenticatedClient()
                .getList("/api/product-categories?onlyActive=true", ProductCategoryDTO.class);
    }

    public List<TaxRateDTO> getActiveVatRates() {
        return clientFactory.authenticatedClient().getList("/api/comercial/vat-rates", TaxRateDTO.class);
    }

    public ProductDTO createProduct(CreateProductRequest req) {
        return clientFactory.authenticatedClient().post("/api/comercial/products", req, ProductDTO.class);
    }

    public ProductDTO updateProduct(Long id, CreateProductRequest req) {
        return clientFactory.authenticatedClient().put("/api/comercial/products/" + id, req, ProductDTO.class);
    }

    public void updateProductImage(Long productId, byte[] imageData) {
        clientFactory.authenticatedClient().postBytes("/api/comercial/products/" + productId + "/image", imageData);
    }
}
