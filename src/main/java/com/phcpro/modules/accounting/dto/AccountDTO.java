package com.phcpro.modules.accounting.dto;

import com.phcpro.modules.accounting.model.AccountClass;
import com.phcpro.modules.accounting.model.AccountNature;

/**
 * @param classLabel rótulo da classe em PT-MZ, resolvido no servidor
 * @param postable   aceita lançamentos (conta folha)
 */
public record AccountDTO(
        Long id,
        String code,
        String name,
        AccountClass accountClass,
        String classLabel,
        AccountNature nature,
        boolean postable,
        boolean active,
        String parentCode
) {}
