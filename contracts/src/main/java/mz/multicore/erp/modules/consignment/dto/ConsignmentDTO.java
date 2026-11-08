package mz.multicore.erp.modules.consignment.dto;

import mz.multicore.erp.modules.consignment.model.ConsignmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** A consignment delivery (guia CG) to a reseller, and its settlement. */
public record ConsignmentDTO(Long id, String number, LocalDate deliveryDate, Long clientId, String clientName, String route,
                             Long sourceWarehouseId, String sourceWarehouseName, ConsignmentStatus status,
                             Long invoiceId, String invoiceNumber, BigDecimal invoiceTotal, String createdBy, String settledBy,
                             List<ConsignmentLineDTO> lines) {}
