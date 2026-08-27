package mz.multicore.erp.modules.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Criar um adiantamento, um empréstimo ou um desconto recorrente.
 *
 * <p>O {@code installmentAmount} é <b>opcional</b> num adiantamento (é o valor todo de uma vez) e
 * num empréstimo com número de prestações (deriva-se do capital). Pedi-lo sempre convidava a
 * introduzir um valor que não bate com o capital, e a diferença só apareceria na última prestação.
 */
public record CreatePayrollDeductionRequest(
        @NotNull(message = "Indique o colaborador.") Long employeeId,
        @NotBlank(message = "Indique o tipo: ADIANTAMENTO, EMPRESTIMO ou RECORRENTE.") String kind,
        @NotBlank(message = "Descreva o desconto — um desconto sem nome volta a ser um número anónimo.")
        String description,
        @DecimalMin(value = "0.01", message = "O valor tem de ser positivo.") BigDecimal principalAmount,
        BigDecimal installmentAmount,
        @Min(value = 1, message = "O número de prestações tem de ser pelo menos 1.") Integer installments,
        @NotNull(message = "Indique a partir de que data desconta.") LocalDate startDate,
        LocalDate endDate,
        String notes
) {}
