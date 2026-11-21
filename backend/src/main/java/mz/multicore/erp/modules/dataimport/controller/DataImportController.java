package mz.multicore.erp.modules.dataimport.controller;

import mz.multicore.erp.modules.dataimport.dto.ImportTemplateDTO;
import mz.multicore.erp.modules.dataimport.service.DataImportTemplateService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Importação de cadastros por CSV (IMPORTACAO_DADOS_SPEC §3.1); só ADMIN (ver {@code ApiRolePolicy}). */
@RestController
@RequestMapping("/api/data-import")
public class DataImportController {

    private static final MediaType TEXT_CSV = MediaType.parseMediaType("text/csv; charset=UTF-8");

    private final DataImportTemplateService templateService;

    public DataImportController(DataImportTemplateService templateService) {
        this.templateService = templateService;
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
}
