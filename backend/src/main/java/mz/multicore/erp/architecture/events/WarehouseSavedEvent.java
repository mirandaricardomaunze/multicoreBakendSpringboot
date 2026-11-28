package mz.multicore.erp.architecture.events;

/**
 * Um armazém foi criado ou alterado (nome, número, estado).
 *
 * <p><b>Porquê um evento e não uma chamada directa:</b> a contabilidade já depende do inventário (valorização do
 * stock); se o inventário chamasse a contabilidade para criar o centro de custo do armazém fecharia um ciclo entre os
 * dois módulos. Mesmo padrão do {@link SaleRegisteredEvent}.
 *
 * <p>Os {@code @EventListener} do Spring correm de forma síncrona e na <b>mesma transacção</b>: o armazém não fica
 * gravado sem o seu centro de custo (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.1, CC-02).
 *
 * @param warehouseId     armazém gravado
 * @param companyId       empresa dona do armazém
 * @param warehouseNumber número/código do armazém (pode faltar)
 * @param name            nome do armazém
 * @param active          estado do armazém
 */
public record WarehouseSavedEvent(Long warehouseId, Long companyId, String warehouseNumber, String name,
                                  boolean active) {}
