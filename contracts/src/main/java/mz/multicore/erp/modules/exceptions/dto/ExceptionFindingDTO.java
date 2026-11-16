package mz.multicore.erp.modules.exceptions.dto;

import mz.multicore.erp.modules.exceptions.model.ExceptionSeverity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One exception: which sector and control raised it, how serious, what happened, who and when, and the document
 * to open. {@code amount} is in MT when the control is about money, otherwise in the unit the detail names.
 */
public record ExceptionFindingDTO(String sector, String code, ExceptionSeverity severity, String title, String detail,
                                  String operator, LocalDate date, String reference, BigDecimal amount) {}
