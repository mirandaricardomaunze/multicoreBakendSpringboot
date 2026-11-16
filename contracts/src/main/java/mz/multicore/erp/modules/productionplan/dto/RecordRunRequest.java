package mz.multicore.erp.modules.productionplan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Record a batch made for a plan line. {@code requestKey} makes a retry return the same run. */
public record RecordRunRequest(
        @NotNull @Positive Long companyId,
        @NotNull @Positive Long planLineId,
        @NotNull @Positive BigDecimal quantity,
        LocalDate expirationDate,
        @NotBlank @Size(max = 64) String requestKey
) {}
