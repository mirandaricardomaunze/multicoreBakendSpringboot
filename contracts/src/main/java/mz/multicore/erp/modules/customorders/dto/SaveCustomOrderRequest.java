package mz.multicore.erp.modules.customorders.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import mz.multicore.erp.modules.customorders.model.DeliveryMode;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Delivery and personalisation of an order. {@code imageBase64} replaces the reference photo (JPEG or PNG, up to
 * 2 MB); {@code removeImage} clears it; both empty keep the current photo.
 */
public record SaveCustomOrderRequest(
        @NotNull @Positive Long companyId,
        @NotNull LocalDate deliveryDate,
        LocalTime deliveryTime,
        @NotNull DeliveryMode mode,
        @Size(max = 300) String deliveryAddress,
        @Size(max = 40) String contactPhone,
        @Size(max = 1000) String personalization,
        @Size(max = 2_900_000) String imageBase64,
        boolean removeImage
) {}
