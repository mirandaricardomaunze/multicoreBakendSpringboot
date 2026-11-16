package mz.multicore.erp.modules.returnables.dto;

import mz.multicore.erp.modules.returnables.model.ReturnableMovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReturnableMovementDTO(Long id, LocalDateTime createdAt, Long clientId, String clientName, Long itemId,
                                    String itemName, ReturnableMovementType type, BigDecimal quantity, String reference,
                                    String invoiceNumber, String notes, String createdBy) {}
