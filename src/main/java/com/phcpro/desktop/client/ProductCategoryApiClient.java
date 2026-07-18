package com.phcpro.desktop.client;

import com.phcpro.modules.comercial.dto.CreateProductCategoryRequest;
import com.phcpro.modules.comercial.dto.ProductCategoryDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/** Cliente HTTP para categorias de produto ({@code /api/product-categories}). */
@Component
@Profile("desktop")
public class ProductCategoryApiClient {

    private final DesktopClientFactory clientFactory;

    public ProductCategoryApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<ProductCategoryDTO> getAll() {
        return clientFactory.authenticatedClient()
                .getList("/api/product-categories", ProductCategoryDTO.class);
    }

    public ProductCategoryDTO create(CreateProductCategoryRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/product-categories", request, ProductCategoryDTO.class);
    }

    public ProductCategoryDTO update(Long id, CreateProductCategoryRequest request) {
        return clientFactory.authenticatedClient()
                .put("/api/product-categories/" + id, request, ProductCategoryDTO.class);
    }

    public void setActive(Long id, boolean active) {
        String action = active ? "activate" : "deactivate";
        clientFactory.authenticatedClient()
                .post("/api/product-categories/" + id + "/" + action, null);
    }
}
