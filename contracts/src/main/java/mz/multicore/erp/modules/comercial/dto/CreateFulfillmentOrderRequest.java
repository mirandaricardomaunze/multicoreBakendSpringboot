package mz.multicore.erp.modules.comercial.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public record CreateFulfillmentOrderRequest(@NotBlank String idempotencyKey,
 @NotNull @Valid CreateOrderRequest order, String terminalName) {}
