package com.phcpro.modules.comercial.controller;

import com.phcpro.modules.comercial.dto.CreateProductCategoryRequest;
import com.phcpro.modules.comercial.dto.ProductCategoryDTO;
import com.phcpro.modules.comercial.service.ProductCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-categories")
public class ProductCategoryController {

    private final ProductCategoryService service;

    public ProductCategoryController(ProductCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ProductCategoryDTO>> list(
            @RequestParam(required = false, defaultValue = "false") boolean onlyActive) {
        return ResponseEntity.ok(onlyActive ? service.getActive() : service.getAll());
    }

    @PostMapping
    public ResponseEntity<ProductCategoryDTO> create(@RequestBody @Valid CreateProductCategoryRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductCategoryDTO> update(@PathVariable Long id,
                                                     @RequestBody @Valid CreateProductCategoryRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        service.setActive(id, true);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.setActive(id, false);
        return ResponseEntity.noContent().build();
    }
}
