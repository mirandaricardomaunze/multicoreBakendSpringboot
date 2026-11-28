package mz.multicore.erp.modules.portal.dto;

import java.time.LocalDateTime;

/** Caixa POS aberta: operador actual e desde quando (a sessão ainda não guarda a loja, SPEC §3.3). */
public record PortalOpenTillDTO(Long sessionId, String operator, LocalDateTime openedAt) {}
