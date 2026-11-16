package mz.multicore.erp.modules.consignment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

/** Goods left at a reseller today. {@code requestKey} makes a retry return the same delivery. */
public record DeliverConsignmentRequest(
        @NotNull @Positive Long companyId,
        @NotNull @Positive Long clientId,
        @NotNull @Positive Long warehouseId,
        @NotNull LocalDate deliveryDate,
        @Size(max = 80) String route,
        @NotEmpty @Valid List<DeliverLineRequest> lines,
        @NotBlank @Size(max = 64) String requestKey
) {}
