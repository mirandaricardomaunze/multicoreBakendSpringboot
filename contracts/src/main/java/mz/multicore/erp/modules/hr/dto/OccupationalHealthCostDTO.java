package mz.multicore.erp.modules.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * O que a saúde ocupacional custou à empresa num intervalo, e a quem.
 *
 * <p>O custo do exame de aptidão é <b>encargo do empregador</b> — nunca desconto ao trabalhador
 * (ver docs/CONFORMIDADE_LEGAL_MZ_SPEC.md §3). Este relatório existe para que esse encargo tenha
 * um número: sem ele, o dinheiro sai da tesouraria misturado com tudo o resto.
 */
public record OccupationalHealthCostDTO(
        LocalDate from, LocalDate to,
        long examCount, BigDecimal total, BigDecimal paid, BigDecimal pending,
        List<OccupationalHealthProviderCostDTO> byProvider
) {}
