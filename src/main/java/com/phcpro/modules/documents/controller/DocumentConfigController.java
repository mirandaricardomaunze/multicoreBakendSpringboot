package com.phcpro.modules.documents.controller;

import com.phcpro.modules.documents.dto.DocumentColumnsDTO;
import com.phcpro.modules.documents.service.DocumentConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class DocumentConfigController {

    private final DocumentConfigService service;

    public DocumentConfigController(DocumentConfigService service) {
        this.service = service;
    }

    @GetMapping("/columns")
    public ResponseEntity<DocumentColumnsDTO> getColumns(@RequestParam Long companyId) {
        return ResponseEntity.ok(service.getColumns(companyId));
    }

    @PutMapping("/columns")
    public ResponseEntity<DocumentColumnsDTO> saveColumns(@RequestParam Long companyId,
                                                          @RequestBody DocumentColumnsDTO dto) {
        return ResponseEntity.ok(service.save(companyId, dto));
    }
}
