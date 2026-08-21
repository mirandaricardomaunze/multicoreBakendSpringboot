package mz.multicore.erp.architecture.events;

/**
 * Uma transferência entre armazéns foi decidida — aprovada, rejeitada ou cancelada.
 *
 * <p><b>Porquê um evento e não uma chamada directa:</b> o inventário não pode passar a conhecer o
 * comercial só para lhe dizer que a transferência avançou. A dependência já existe no outro
 * sentido (o comercial chama o inventário para mover stock); invertê-la também aqui fecharia um
 * ciclo entre os dois módulos. Mesmo padrão do {@link SaleRegisteredEvent} com a contabilidade.
 *
 * <p>Só é emitido quando a transferência veio de uma encomenda de reposição
 * ({@code orderId != null}) — quem transfere directamente não tem encomenda a actualizar.
 *
 * <p>Os {@code @EventListener} do Spring correm de forma síncrona e na <b>mesma transacção</b>: se
 * a actualização da encomenda falhar, o stock não fica movido com a encomenda por actualizar.
 *
 * @param orderId encomenda de reposição que originou a transferência
 * @param outcome o que foi decidido
 */
public record StockTransferResolvedEvent(Long orderId, Outcome outcome) {

    public enum Outcome {
        /** Stock movido: a encomenda cumpriu-se e fica terminal. */
        APPROVED,
        /** Não moveu nada: a encomenda volta a poder ser corrigida e convertida de novo. */
        REJECTED,
        CANCELLED;

        /** Rejeitar e cancelar libertam a encomenda; ambos deixam o stock onde estava. */
        public boolean releasesOrder() {
            return this != APPROVED;
        }
    }
}
