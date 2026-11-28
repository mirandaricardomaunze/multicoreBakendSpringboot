package mz.multicore.erp.modules.publicapi.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.publicapi.dto.ApiKeyDTO;
import mz.multicore.erp.modules.publicapi.dto.CreateApiKeyRequest;
import mz.multicore.erp.modules.publicapi.dto.CreatedApiKeyDTO;
import mz.multicore.erp.modules.publicapi.service.ApiKeyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Internal management of the active company's public API keys (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.1.1, API-02):
 * user login only, ADMIN only ({@code ApiRolePolicy} and the Service guard). The secret leaves only in the 201.
 */
@RestController
@RequestMapping("/api/integrations/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    public List<ApiKeyDTO> list() {
        return apiKeyService.list();
    }

    @PostMapping
    public ResponseEntity<CreatedApiKeyDTO> create(@Valid @RequestBody CreateApiKeyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(apiKeyService.create(request));
    }

    @PostMapping("/{id}/revoke")
    public ApiKeyDTO revoke(@PathVariable Long id) {
        return apiKeyService.revoke(id);
    }
}
