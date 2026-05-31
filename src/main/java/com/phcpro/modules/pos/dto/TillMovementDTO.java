package com.phcpro.modules.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TillMovementDTO(
        Long id,
        Long sessionId,
        String movementType,
        BigDecimal amount,
        String description,
        LocalDateTime movementDate
) {}
