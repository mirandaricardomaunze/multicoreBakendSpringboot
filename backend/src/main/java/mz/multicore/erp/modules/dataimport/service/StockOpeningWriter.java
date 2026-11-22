package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import mz.multicore.erp.modules.inventory.model.StockMovementType;
import mz.multicore.erp.modules.inventory.service.AverageCostUpdater;
import mz.multicore.erp.modules.inventory.service.StockMovementService;
import org.springframework.stereotype.Component;

/**
 * Grava o stock inicial classificado pelo {@link StockOpeningImportService} (IMPORTACAO_DADOS_SPEC §5.1). Tudo ou
 * nada: com algum {@code ERRO} não grava nenhuma linha. Cada linha entra pelo ledger do inventário como um movimento
 * {@code ENTRY} com lote e validade, que sobe o saldo e o lote como a entrada manual; antes disso o custo médio do
 * artigo passa a contar com a quantidade ao custo unitário do ficheiro. {@code ENTRY} não gera lançamento
 * contabilístico. A transacção é a da confirmação que chama este escritor.
 */
@Component
public class StockOpeningWriter {

    static final String DESCRIPTION = "Stock inicial (importação)";

    private final StockMovementService movementService;
    private final AverageCostUpdater averageCost;

    public StockOpeningWriter(StockMovementService movementService, AverageCostUpdater averageCost) {
        this.movementService = movementService;
        this.averageCost = averageCost;
    }

    /** Recusa o ficheiro com erros e grava as linhas pela ordem do ficheiro; devolve a classificação gravada. */
    ImportPreviewDTO write(ImportPlan<StockOpeningImportService.StockOpeningLine> plan) {
        plan.requireNoErrors().toWrite().forEach(this::write);
        return plan.preview();
    }

    private void write(StockOpeningImportService.StockOpeningLine line) {
        // o custo médio lê o stock antes da entrada (AverageCostUpdater)
        averageCost.beforeEntry(line.product(), line.warehouse(), line.quantity(),
                line.quantity().multiply(line.unitCost()));
        movementService.registerMovement(line.product(), line.warehouse(), line.quantity(),
                StockMovementType.ENTRY.name(), line.batchNumber(), null, DESCRIPTION, line.expirationDate());
    }
}
