package mz.multicore.erp.modules.exceptions.service;

import mz.multicore.erp.modules.exceptions.dto.ExceptionFindingDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Test access to the package-private outlier rule. */
public final class ButcheryExceptionChecksAccess {

    private ButcheryExceptionChecksAccess() {
    }

    public static List<ExceptionFindingDTO> outliers(Map<String, BigDecimal> totals, Map<String, Integer> counts) {
        return ButcheryExceptionChecks.outliers(totals, counts, "Talho", LocalDate.now());
    }
}
