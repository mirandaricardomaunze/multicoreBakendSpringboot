package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderPickingDetailsDTO(
    Long orderId,
    String orderNumber,
    String clientName,
    String warehouseName,
    String status,
    List<OrderPickingLineDTO> lines,
    int totalItems,
    BigDecimal totalBoxes
) {}
