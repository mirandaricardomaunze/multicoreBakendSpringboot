package mz.multicore.erp.modules.intelligence.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.intelligence.dto.*;
import mz.multicore.erp.modules.intelligence.service.AiDocumentLifecycleService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/intelligence/documents")
public class AiDocumentLifecycleController {
    private final AiDocumentLifecycleService service;

    public AiDocumentLifecycleController(AiDocumentLifecycleService service) {
        this.service = service;
    }

    @GetMapping
    public List<AiManagedDocumentDTO> search(@RequestParam(defaultValue = "") String query) {
        return service.search(query);
    }

    @PostMapping
    public AiManagedDocumentDTO create(@Valid @RequestBody AiProfessionalDocumentRequest request) {
        return service.create(request);
    }

    @PostMapping("/{id}/versions")
    public AiManagedDocumentDTO createVersion(@PathVariable Long id,
                                               @Valid @RequestBody AiProfessionalDocumentRequest request) {
        return service.createVersion(id, request);
    }

    @GetMapping("/{id}/versions")
    public List<AiDocumentVersionDTO> versions(@PathVariable Long id) {
        return service.versions(id);
    }

    @PostMapping("/{id}/submit")
    public AiManagedDocumentDTO submit(@PathVariable Long id) {
        return service.submit(id);
    }

    @PostMapping("/{id}/approve")
    public AiManagedDocumentDTO approve(@PathVariable Long id) {
        return service.approve(id);
    }

    @PostMapping("/{id}/reject")
    public AiManagedDocumentDTO reject(@PathVariable Long id, @Valid @RequestBody AiDocumentReviewRequest request) {
        return service.reject(id, request);
    }

    @PostMapping("/{id}/archive")
    public AiManagedDocumentDTO archive(@PathVariable Long id) {
        return service.archive(id);
    }

    @GetMapping(value = "/{id}/versions/{version}/document.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable Long id, @PathVariable int version) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=documento-ia-" + id + "-v" + version + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(service.pdf(id, version));
    }
}
