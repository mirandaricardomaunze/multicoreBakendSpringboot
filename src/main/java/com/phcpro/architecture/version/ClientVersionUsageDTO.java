package com.phcpro.architecture.version;

import java.time.LocalDateTime;

/**
 * Uma empresa vista numa versão do programa.
 *
 * @param companyName  nome resolvido no <b>servidor</b>. O desktop não deve andar a cruzar ids
 *                     com outra listagem só para escrever um nome numa tabela — foi o incómodo
 *                     que já existiu no painel de Compras com o armazém.
 * @param lastUsername último utilizador visto nesta versão — para se saber a quem telefonar
 */
public record ClientVersionUsageDTO(
        Long companyId,
        String companyName,
        String clientVersion,
        String lastUsername,
        LocalDateTime firstSeenAt,
        LocalDateTime lastSeenAt
) {}
