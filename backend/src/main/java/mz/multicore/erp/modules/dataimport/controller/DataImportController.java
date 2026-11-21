package mz.multicore.erp.modules.dataimport.controller;

import mz.multicore.erp.modules.dataimport.dto.ImportCommitResultDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportTemplateDTO;
import mz.multicore.erp.modules.dataimport.service.DataImportService;
import mz.multicore.erp.modules.dataimport.service.DataImportTemplateService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Importação de cadastros por CSV (IMPORTACAO_DADOS_SPEC §3.1); só ADMIN (ver {@code ApiRolePolicy}).
 * O ficheiro chega como corpo binário ({@code application/octet-stream} ou {@code text/csv}).
 */
@RestController
@RequestMapping("/api/data-import")
public class DataImportController {

    private static final MediaType TEXT_CSV = MediaType.parseMediaType("text/csv; charset=UTF-8");
    private static final String CSV_UPLOAD = "text/csv";

    private final DataImportTemplateService templateService;
    private final DataImportService importService;

    public DataImportController(DataImportTemplateService templateService, DataImportService importService) {
        this.templateService = templateService;
        this.importService = importService;
    }

    /** Modelo CSV da entidade ({@code clients}, {@code suppliers}, {@code products}). */
    @GetMapping("/{entity}/template")
    public ResponseEntity<byte[]> template(@PathVariable String entity) {
        ImportTemplateDTO template = templateService.template(entity);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + template.fileName() + "\"")
                .contentType(TEXT_CSV)
                .body(template.content());
    }

    /** Classifica cada linha do ficheiro sem gravar nada. */
    @PostMapping(value = "/{entity}/preview", consumes = {MediaType.APPLICATION_OCTET_STREAM_VALUE, CSV_UPLOAD})
    public ResponseEntity<ImportPreviewDTO> preview(@PathVariable String entity, @RequestBody byte[] content) {
        return ResponseEntity.ok(importService.preview(entity, content));
    }

    /** Revalida e grava tudo numa transacção; com qualquer erro recusa e não grava nada. */
    @PostMapping(value = "/{entity}/commit", consumes = {MediaType.APPLICATION_OCTET_STREAM_VALUE, CSV_UPLOAD})
    public ResponseEntity<ImportCommitResultDTO> commit(@PathVariable String entity, @RequestBody byte[] content) {
        return ResponseEntity.ok(importService.commit(entity, content));
    }
}
