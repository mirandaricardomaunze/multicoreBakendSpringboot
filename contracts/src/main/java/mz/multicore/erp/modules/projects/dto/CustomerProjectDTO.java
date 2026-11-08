package mz.multicore.erp.modules.projects.dto;

import java.math.BigDecimal;

/** A client's construction site or project (SA-C09) with the totals of its linked orders. */
public record CustomerProjectDTO(
        Long id,
        Long clientId,
        String clientName,
        String code,
        String name,
        String address,
        String contact,
        boolean active,
        int orderCount,
        BigDecimal orderedTotal,
        BigDecimal outstanding
) {}
