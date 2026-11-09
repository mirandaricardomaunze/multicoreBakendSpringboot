package mz.multicore.erp.modules.exceptions.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.exceptions.dto.ExceptionFindingDTO;
import mz.multicore.erp.modules.exceptions.dto.ExceptionReportDTO;
import mz.multicore.erp.modules.exceptions.model.ExceptionSeverity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Anti-fraud exception report (SA-C21, {@code docs/ANTIFRAUDE_SECTORES_SPEC.md}): runs every sector check over a
 * period and returns the findings, most serious first. Managers only; it reads, never changes anything.
 */
@Service
public class ExceptionReportService {

    private static final int MAX_DAYS = 366;

    private final List<ExceptionCheck> checks;

    public ExceptionReportService(List<ExceptionCheck> checks) {
        this.checks = checks;
    }

    @Transactional(readOnly = true)
    public ExceptionReportDTO report(Long companyId, LocalDate from, LocalDate to) {
        CurrentUserContext.requireCompany(companyId);
        PermissionGuard.requireManagerOrAdmin("ver relatório de excepções");
        if (to.isBefore(from)) throw new BusinessRuleException("A data final é anterior à inicial.");
        if (from.plusDays(MAX_DAYS).isBefore(to)) throw new BusinessRuleException("Escolha um período de até um ano.");
        List<ExceptionFindingDTO> findings = new ArrayList<>();
        for (ExceptionCheck check : checks) findings.addAll(check.run(companyId, from, to));
        findings.sort(Comparator.comparing(ExceptionFindingDTO::severity)
                .thenComparing(f -> f.date() == null ? LocalDate.MIN : f.date(), Comparator.reverseOrder()));
        return new ExceptionReportDTO(from, to, LocalDateTime.now(), count(findings, ExceptionSeverity.HIGH),
                count(findings, ExceptionSeverity.MEDIUM), count(findings, ExceptionSeverity.LOW), findings);
    }

    private static long count(List<ExceptionFindingDTO> findings, ExceptionSeverity severity) {
        return findings.stream().filter(f -> f.severity() == severity).count();
    }
}
