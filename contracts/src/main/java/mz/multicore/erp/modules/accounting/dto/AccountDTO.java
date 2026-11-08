package mz.multicore.erp.modules.accounting.dto;

import mz.multicore.erp.modules.accounting.model.AccountClass;
import mz.multicore.erp.modules.accounting.model.AccountNature;

/**
 * @param classLabel pt-MZ class label resolved on the server
 * @param postable   whether the account accepts entries (leaf account)
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
