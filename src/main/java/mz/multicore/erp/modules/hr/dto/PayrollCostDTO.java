package mz.multicore.erp.modules.hr.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * <b>Custo total do trabalhador</b> — base + subsídios + horas extra + INSS patronal.
 * Ver docs/RH_COMPLETO_SPEC.md §B5 (RHC-55).
 *
 * <p>Existe porque o INSS patronal <b>é custo da empresa</b> e não aparecia em relatório nenhum: o
 * mapa fiscal imprimia-o e mais nada. Quem olhava para a folha via o ilíquido e pensava que era o
 * que a empresa gasta — e não é.
 *
 * @param grossPay      o que o colaborador ganha (base + subsídios + extra)
 * @param employerInss  o que a empresa paga por cima disso
 * @param totalCost     o que a empresa gasta de facto
 * @param netPay        o que o colaborador recebe à mão
 */
public record PayrollCostDTO(
        int year,
        int month,
        BigDecimal grossPay,
        BigDecimal employerInss,
        BigDecimal totalCost,
        BigDecimal netPay,
        List<PayrollCostLineDTO> lines
) {
    public record PayrollCostLineDTO(
            Long employeeId,
            String employeeNumber,
            String employeeName,
            BigDecimal baseSalary,
            BigDecimal allowances,
            BigDecimal overtime,
            BigDecimal grossPay,
            BigDecimal employerInss,
            BigDecimal totalCost,
            BigDecimal netPay
    ) {}
}
