package mz.multicore.erp.modules.performance.dto;

import mz.multicore.erp.modules.performance.model.BonusStatus;

import java.math.BigDecimal;

public record SalesGoalBonusDTO(
        Long id,
        Long goalId,
        String goalName,
        Long employeeId,
        String employeeName,
        Long companyId,
        BigDecimal calculatedAmount,
        BigDecimal approvedAmount,
        String justification,
        BonusStatus status,
        Long payslipId,
        String createdBy,
        String approvedBy
) {}
