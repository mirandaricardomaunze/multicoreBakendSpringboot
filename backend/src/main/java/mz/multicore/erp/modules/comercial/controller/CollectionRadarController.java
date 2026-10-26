package mz.multicore.erp.modules.comercial.controller;

import mz.multicore.erp.modules.comercial.dto.CollectionRadarItemDTO;
import mz.multicore.erp.modules.comercial.dto.CollectionRadarSummaryDTO;
import mz.multicore.erp.modules.comercial.dto.CollectionReminderTemplateDTO;
import mz.multicore.erp.modules.comercial.model.CollectionRadarBucket;
import mz.multicore.erp.modules.comercial.service.CollectionRadarService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Controller REST para o Radar Inteligente de Cobranças e Lembretes Preventivos.
 */
@RestController
@RequestMapping("/api/comercial/radar")
public class CollectionRadarController {

    private final CollectionRadarService radarService;

    public CollectionRadarController(CollectionRadarService radarService) {
        this.radarService = Objects.requireNonNull(radarService, "radarService obrigatório");
    }

    @GetMapping("/summary")
    public ResponseEntity<CollectionRadarSummaryDTO> getSummary(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate) {
        return ResponseEntity.ok(radarService.getRadarSummary(companyId, referenceDate));
    }

    @GetMapping("/items")
    public ResponseEntity<List<CollectionRadarItemDTO>> getItems(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) CollectionRadarBucket bucket,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate) {
        return ResponseEntity.ok(radarService.getRadarItems(companyId, bucket, referenceDate));
    }

    @GetMapping("/invoices/{id}/reminder")
    public ResponseEntity<CollectionReminderTemplateDTO> getReminderTemplate(
            @PathVariable Long id,
            @RequestParam(defaultValue = "WHATSAPP") String channel) {
        return ResponseEntity.ok(radarService.generateReminderTemplate(id, channel));
    }
}
