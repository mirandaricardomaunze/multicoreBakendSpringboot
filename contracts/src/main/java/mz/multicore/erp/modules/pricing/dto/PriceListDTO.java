package mz.multicore.erp.modules.pricing.dto;

import java.time.LocalDate;

/** Company price list (SA-C03). */
public record PriceListDTO(
        Long id,
        Long companyId,
        String code,
        String name,
        String description,
        LocalDate validFrom,
        LocalDate validTo,
        boolean active,
        long itemCount,
        long clientCount
) {}
