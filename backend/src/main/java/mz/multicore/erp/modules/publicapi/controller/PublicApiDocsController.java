package mz.multicore.erp.modules.publicapi.controller;

import mz.multicore.erp.modules.publicapi.service.PublicApiDocumentationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OpenAPI 3 document of the public API (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.2.2, API-11). Like every public route it
 * needs a valid key (any permission) and counts for the rate limit; both are applied before this runs.
 */
@RestController
@RequestMapping("/api/public/v1")
public class PublicApiDocsController {

    private final PublicApiDocumentationService documentationService;

    public PublicApiDocsController(PublicApiDocumentationService documentationService) {
        this.documentationService = documentationService;
    }

    @GetMapping(value = "/docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> openApi() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(documentationService.openApiJson());
    }
}
