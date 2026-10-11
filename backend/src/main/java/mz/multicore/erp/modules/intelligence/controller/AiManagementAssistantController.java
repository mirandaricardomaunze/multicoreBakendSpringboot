package mz.multicore.erp.modules.intelligence.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.intelligence.dto.AiAssistantRequest;
import mz.multicore.erp.modules.intelligence.dto.AiAssistantResponse;
import mz.multicore.erp.modules.intelligence.dto.AiProfessionalDocumentRequest;
import mz.multicore.erp.modules.intelligence.dto.AiOperationalDraftRequest;
import mz.multicore.erp.modules.intelligence.dto.AiOperationalDraftResponse;
import mz.multicore.erp.modules.intelligence.service.AiManagementAssistantService;
import mz.multicore.erp.modules.intelligence.service.AiManagementReportService;
import mz.multicore.erp.modules.intelligence.service.AiOperationalDraftService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/intelligence/management-assistant")
public class AiManagementAssistantController {

    private final AiManagementAssistantService service;
    private final AiManagementReportService reportService;
    private final AiOperationalDraftService draftService;

    public AiManagementAssistantController(AiManagementAssistantService service, AiManagementReportService reportService,
                                           AiOperationalDraftService draftService) {
        this.service = service;
        this.reportService = reportService;
        this.draftService = draftService;
    }

    @PostMapping("/ask")
    public AiAssistantResponse ask(@Valid @RequestBody AiAssistantRequest request) {
        return service.ask(request);
    }

    @PostMapping(value = "/report.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> report(@RequestBody AiAssistantResponse response) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=relatorio-gestao-ia.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(reportService.render(response));
    }

    @PostMapping(value = "/document.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> document(@Valid @RequestBody AiProfessionalDocumentRequest request) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=documento-profissional-ia.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(reportService.renderProfessionalDocument(request));
    }

    @PostMapping("/operational-draft")
    public AiOperationalDraftResponse operationalDraft(@Valid @RequestBody AiOperationalDraftRequest request) {
        return draftService.prepare(request);
    }
}
