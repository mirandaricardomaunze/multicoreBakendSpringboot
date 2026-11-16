package mz.multicore.erp.modules.exceptions.service;

import mz.multicore.erp.modules.exceptions.dto.ExceptionFindingDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * One family of anti-fraud controls (SA-C21, {@code docs/ANTIFRAUDE_SECTORES_SPEC.md}). Each sector has its own
 * implementation; adding a control never touches the others. Checks only read existing records.
 */
public interface ExceptionCheck {

    /** Findings for the period {@code from}..{@code to} (both inclusive). */
    List<ExceptionFindingDTO> run(Long companyId, LocalDate from, LocalDate to);
}
