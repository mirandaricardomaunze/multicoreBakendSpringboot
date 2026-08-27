package mz.multicore.erp.modules.comercial.controller;

import mz.multicore.erp.modules.comercial.dto.AgingSummaryDTO;
import mz.multicore.erp.modules.comercial.service.ReceivablesService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Contas a receber — antiguidade de saldos. Só HTTP; a regra vive no serviço e no domínio. */
@RestController
@RequestMapping("/api/comercial/receivables")
public class ReceivablesController {

    private final ReceivablesService receivablesService;

    public ReceivablesController(ReceivablesService receivablesService) {
        this.receivablesService = receivablesService;
    }

    /**
     * Mapa de antiguidade da empresa activa.
     *
     * @param reference data de referência (opcional, {@code yyyy-MM-dd}); omitida = hoje
     */
    @GetMapping("/aging")
    public ResponseEntity<AgingSummaryDTO> getAging(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reference) {
        return ResponseEntity.ok(receivablesService.getAging(reference));
    }
}
