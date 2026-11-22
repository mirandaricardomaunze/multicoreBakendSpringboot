package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.modules.comercial.dto.CreateOpeningReceivableRequest;
import mz.multicore.erp.modules.comercial.service.OpeningReceivableCommandService;
import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import org.springframework.stereotype.Component;

/**
 * Grava as dívidas de clientes classificadas pelo {@link OpeningReceivableImportService} (IMPORTACAO_DADOS_SPEC §5.2).
 * Tudo ou nada: com algum {@code ERRO} não grava nenhuma linha. Cada linha {@code NOVO} vira um documento "Saldo
 * Inicial" (série SI) pelo serviço do comercial, dono das facturas. A transacção é a da confirmação que o chama.
 */
@Component
public class OpeningReceivableWriter {

    private final OpeningReceivableCommandService commandService;

    public OpeningReceivableWriter(OpeningReceivableCommandService commandService) {
        this.commandService = commandService;
    }

    /** Recusa o ficheiro com erros e grava as linhas pela ordem do ficheiro; devolve a classificação gravada. */
    ImportPreviewDTO write(ImportPlan<CreateOpeningReceivableRequest> plan) {
        plan.requireNoErrors().toWrite().forEach(commandService::create);
        return plan.preview();
    }
}
