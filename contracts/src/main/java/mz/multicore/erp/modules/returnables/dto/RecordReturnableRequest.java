package mz.multicore.erp.modules.returnables.dto;

import jakarta.validation.constraints.*;
import mz.multicore.erp.modules.returnables.model.ReturnableMovementType;

import java.math.BigDecimal;

/** Delivery (OUT) or return (IN) of returnables. {@code requestKey} makes a retry return the same movement. */
public record RecordReturnableRequest(
        @NotNull @Positive Long companyId,
        @NotNull @Positive Long clientId,
        @NotNull @Positive Long itemId,
        @NotNull @Positive Long warehouseId,
        @NotNull ReturnableMovementType type,
        @NotNull @Positive BigDecimal quantity,
        @Size(max = 80) String reference,
        @Size(max = 300) String notes,
        @NotBlank @Size(max = 64) String requestKey
) {}
