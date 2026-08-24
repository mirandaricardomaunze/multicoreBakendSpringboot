package mz.multicore.erp.modules.hr.dto;

import java.util.List;

/**
 * Os dois avisos de contrato, numa só resposta. Ver docs/RH_COMPLETO_SPEC.md §B1.
 *
 * <p>Vêm juntos de propósito: o sino recarrega com frequência e já faz quatro chamadas por
 * actualização. Dois endpoints separados para duas listas que são sempre pedidas ao mesmo tempo
 * seriam uma ida ao servidor a mais, todas as vezes.
 */
public record ContractAlertsDTO(
        /** Contratos vigentes a terminar dentro da janela de aviso (30 dias). */
        List<EmploymentContractDTO> endingSoon,
        /** Contratos cujo período experimental termina dentro da janela curta (7 dias). */
        List<EmploymentContractDTO> probationEndingSoon
) {}
