package mz.multicore.erp.modules.hr.dto;

import java.util.List;

/**
 * Resultado do processamento da folha mensal: o que foi gerado <b>e quem ficou de fora, e porquê</b>.
 *
 * <p>Devolver só a lista de recibos era o problema: quem processa a folha via "12 recibos gerados" e
 * não tinha como saber que o 13º colaborador foi saltado por o contrato ter terminado. Um silêncio
 * desses só se descobre no fim do mês, quando alguém reclama o ordenado.
 * Ver docs/RH_COMPLETO_SPEC.md §B1.
 */
public record PayrollRunDTO(
        List<PayslipDTO> generated,
        List<PayrollSkipDTO> skipped
) {
    /** Um colaborador que a folha não processou, e a razão em PT-MZ. */
    public record PayrollSkipDTO(Long employeeId, String employeeName, String reason) {}

    /**
     * O que dizer a quem carregou em "Processar". <b>Fonte única</b> desta frase — o painel não a
     * remonta, e assim quem for saltado aparece na mesma caixa, em vez de num sítio que ninguém abre.
     */
    public String summaryMessage() {
        StringBuilder message = new StringBuilder()
                .append(generated.size()).append(" recibo(s) processado(s).");
        if (!skipped.isEmpty()) {
            message.append("\n\n").append(skipped.size())
                    .append(" colaborador(es) não processado(s):");
            for (PayrollSkipDTO skip : skipped) {
                message.append("\n  • ").append(skip.employeeName()).append(" — ").append(skip.reason());
            }
        }
        return message.toString();
    }
}
