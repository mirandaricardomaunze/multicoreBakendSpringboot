package mz.multicore.erp.architecture.events;

import java.time.LocalDate;
import java.util.Set;

/**
 * Um lançamento com custos (classe 6) foi gravado pela porta única do diário.
 *
 * <p><b>Porquê um evento e não uma chamada directa:</b> os alertas de orçamento lêem o real através do diário; se o
 * diário chamasse os alertas fechar-se-ia um ciclo entre os dois serviços.
 *
 * <p>Os {@code @EventListener} do Spring correm de forma síncrona e na <b>mesma transacção</b>: o alerta de 90%/100%
 * nasce com o lançamento que o provocou (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.3, OR-05).
 *
 * @param companyId     empresa do lançamento
 * @param entryDate     data do lançamento (ano e mês avaliados)
 * @param costCenterIds centros das partidas de classe 6
 */
public record JournalEntryPostedEvent(Long companyId, LocalDate entryDate, Set<Long> costCenterIds) {

    public JournalEntryPostedEvent {
        costCenterIds = Set.copyOf(costCenterIds);
    }
}
