package mz.multicore.erp.modules.pos.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Autorização de gerente para um desconto no POS. */
public final class DiscountAuthorizationDTOs {

    private DiscountAuthorizationDTOs() {}

    public record Request(@NotBlank String pin,
                          @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal maxDiscountPercent) {}

    public record Response(boolean approved, String managerName, String message, String token) {}
}
