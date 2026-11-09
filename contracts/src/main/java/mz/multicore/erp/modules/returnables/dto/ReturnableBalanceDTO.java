package mz.multicore.erp.modules.returnables.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** What a client still holds of one returnable, its value and how long since the oldest open delivery. */
public record ReturnableBalanceDTO(Long clientId, String clientName, Long itemId, String itemName, BigDecimal balance,
                                   BigDecimal value, LocalDate lastMovement, long daysOut) {}
