package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.hr.dto.CreatePayrollDeductionRequest;
import mz.multicore.erp.modules.hr.model.PayrollDeductionKind;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Valida e normaliza a configuração imutável de um compromisso salarial. */
final class PayrollDeductionPolicy {
    private PayrollDeductionPolicy() { }

    static void validate(PayrollDeductionKind kind, CreatePayrollDeductionRequest request) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate()))
            throw new BusinessRuleException("O fim do desconto não pode ser anterior ao início.");
        if (kind != PayrollDeductionKind.RECORRENTE && request.principalAmount() == null)
            throw new BusinessRuleException("Um " + kind.getLabel().toLowerCase() + " tem de indicar o valor entregue ao colaborador.");
        if (kind == PayrollDeductionKind.RECORRENTE && request.principalAmount() == null && request.installmentAmount() == null)
            throw new BusinessRuleException("Um desconto recorrente tem de indicar quanto desconta em cada recibo.");
        if (kind == PayrollDeductionKind.ADIANTAMENTO && request.installments() != null && request.installments() > 1)
            throw new BusinessRuleException("Um adiantamento desconta-se de uma vez. Para prestações, registe um empréstimo.");
    }

    static BigDecimal principal(PayrollDeductionKind kind, CreatePayrollDeductionRequest request) {
        return kind == PayrollDeductionKind.RECORRENTE && request.endDate() == null ? null : request.principalAmount();
    }

    static Integer installments(PayrollDeductionKind kind, CreatePayrollDeductionRequest request) {
        return kind == PayrollDeductionKind.ADIANTAMENTO ? Integer.valueOf(1) : request.installments();
    }

    static BigDecimal installmentAmount(PayrollDeductionKind kind, CreatePayrollDeductionRequest request) {
        if (kind == PayrollDeductionKind.ADIANTAMENTO) return request.principalAmount();
        if (request.installmentAmount() != null && request.installmentAmount().signum() > 0) return request.installmentAmount();
        if (request.principalAmount() != null && request.installments() != null)
            return request.principalAmount().divide(BigDecimal.valueOf(request.installments()), 2, RoundingMode.CEILING);
        throw new BusinessRuleException("Indique o valor de cada prestação ou o número de prestações.");
    }
}
