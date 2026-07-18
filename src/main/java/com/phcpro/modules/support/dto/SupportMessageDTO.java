package com.phcpro.modules.support.dto;

import java.time.LocalDateTime;

public record SupportMessageDTO(
        Long id,
        String author,
        boolean fromSuperAdmin,
        String body,
        LocalDateTime createdAt
) {}
