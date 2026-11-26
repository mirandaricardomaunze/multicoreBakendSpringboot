package mz.multicore.erp.modules.printing;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.documents.dto.DocumentTemplateDTO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pré-visualização do modelo de documento antes de gravar (MODELOS_DOCUMENTO_SPEC §3.4). Vive no módulo de
 * impressão porque é este que depende de {@code documents}, nunca o contrário.
 */
@RestController
public class DocumentTemplatePreviewController {

    private final DocumentTemplatePreviewService previewService;

    public DocumentTemplatePreviewController(DocumentTemplatePreviewService previewService) {
        this.previewService = previewService;
    }

    @PostMapping("/api/documents/template/preview")
    public ResponseEntity<byte[]> preview(@RequestParam Long companyId, @RequestBody @Valid DocumentTemplateDTO dto) {
        byte[] pdf = previewService.preview(companyId, dto);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"modelo-exemplo.pdf\"")
                .contentLength(pdf.length)
                .body(pdf);
    }
}
