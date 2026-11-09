package mz.multicore.erp.modules.exceptions.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Exception report of a period, most serious first. */
public record ExceptionReportDTO(LocalDate from, LocalDate to, LocalDateTime generatedAt, long high, long medium, long low,
                                 List<ExceptionFindingDTO> findings) {}
