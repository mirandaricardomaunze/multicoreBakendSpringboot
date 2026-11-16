package mz.multicore.erp.modules.customorders.dto;

import mz.multicore.erp.modules.customorders.model.CustomOrderStage;
import mz.multicore.erp.modules.customorders.model.DeliveryMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * An order with a delivery date, seen as the bakery or butcher agenda shows it: when, how, what to write on the
 * cake, the reference photo, the kitchen stage and the advance already paid.
 */
public record CustomOrderDTO(Long orderId, String orderNumber, String orderStatus, Long clientId, String clientName,
                             String contactPhone, LocalDate deliveryDate, LocalTime deliveryTime, DeliveryMode mode,
                             String deliveryAddress, String personalization, boolean hasImage, CustomOrderStage stage,
                             BigDecimal total, BigDecimal advancePaid, boolean sentToPlan, List<CustomOrderLineDTO> lines) {}
