package com.phcpro.modules.movimentos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Projecção read-only de um documento comercial para a vista unificada de movimentos.
 * Nunca expõe a entidade JPA de origem — só os campos necessários à listagem.
 */
public record MovimentoDTO(
        MovimentoTipo tipo,
        Long documentId,
        String numero,
        String cliente,
        LocalDateTime data,
        String estado,
        BigDecimal total
) {
}
