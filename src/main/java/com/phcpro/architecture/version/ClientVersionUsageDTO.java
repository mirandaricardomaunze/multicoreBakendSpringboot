package com.phcpro.architecture.version;

import java.time.LocalDateTime;

/**
 * Uma empresa vista numa versão do programa.
 *
 * @param lastUsername último utilizador visto nesta versão — para se saber a quem telefonar
 */
public record ClientVersionUsageDTO(
        Long companyId,
        String clientVersion,
        String lastUsername,
        LocalDateTime firstSeenAt,
        LocalDateTime lastSeenAt
) {}
