package com.phcpro.modules.movimentos.controller;

import com.phcpro.modules.movimentos.dto.MovimentoDTO;
import com.phcpro.modules.movimentos.service.MovimentosService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Leitura agregada dos movimentos comerciais. Protegido pelo {@code SecurityInterceptor}
 * como todo o {@code /api/**} (401 sem token, 403 empresa sem acesso).
 */
@RestController
@RequestMapping("/api/movimentos")
public class MovimentosController {

    private final MovimentosService movimentosService;

    public MovimentosController(MovimentosService movimentosService) {
        this.movimentosService = movimentosService;
    }

    @GetMapping
    public ResponseEntity<List<MovimentoDTO>> listar(
            @RequestParam Long companyId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(movimentosService.listar(companyId, query, from, to));
    }
}
