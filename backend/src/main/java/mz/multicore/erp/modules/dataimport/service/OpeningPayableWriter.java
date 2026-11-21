package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import mz.multicore.erp.modules.purchases.dto.CreateOpeningPayableRequest;
import mz.multicore.erp.modules.purchases.service.OpeningPayableCommandService;
import org.springframework.stereotype.Component;

/**
 * Grava as dívidas a fornecedores classificadas pelo {@link OpeningPayableImportService} (IMPORTACAO_DADOS_SPEC §5.3).
 * Tudo ou nada: com algum {@code ERRO} não grava nenhuma linha. Cada linha {@code NOVO} vira um documento "Saldo
 * Inicial Fornecedor" (série SIF) pelo serviço das compras, dono das contas a pagar. A transacção é a da confirmação
 * que o chama.
 */
@Component
public class OpeningPayableWriter {

    private final OpeningPayableCommandService commandService;

    public OpeningPayableWriter(OpeningPayableCommandService commandService) {
        this.commandService = commandService;
    }

    /** Recusa o ficheiro com erros e grava as linhas pela ordem do ficheiro; devolve a classificação gravada. */
    ImportPreviewDTO write(ImportPlan<CreateOpeningPayableRequest> plan) {
        plan.requireNoErrors().toWrite().forEach(commandService::create);
        return plan.preview();
    }
}
