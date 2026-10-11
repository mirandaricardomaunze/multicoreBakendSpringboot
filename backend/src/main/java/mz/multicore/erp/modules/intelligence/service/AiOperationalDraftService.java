package mz.multicore.erp.modules.intelligence.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.intelligence.dto.AiOperationalDraftRequest;
import mz.multicore.erp.modules.intelligence.dto.AiOperationalDraftResponse;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class AiOperationalDraftService {
    private final AuditLogService auditLogService;

    public AiOperationalDraftService(AuditLogService auditLogService) { this.auditLogService = auditLogService; }

    public AiOperationalDraftResponse prepare(AiOperationalDraftRequest request) {
        PermissionGuard.requireManagerOrAdmin("preparar rascunhos operacionais com IA");
        if (CurrentUserContext.getCurrentCompanyId() == null) throw new BusinessRuleException("Seleccione uma empresa.");
        String text = request.request().trim();
        DraftKind kind = classify(text);
        List<String> explicit = Arrays.stream(text.split("[;\n]"))
                .map(String::trim).filter(value -> value.contains(":") && value.length() <= 180).limit(12).toList();
        boolean ready = explicit.size() >= kind.minimumExplicitFields;
        auditLogService.logCurrent("AI_OPERATIONAL_DRAFT_PREPARED",
                "Tipo=" + kind.label + "; camposExplicitos=" + explicit.size() + "; pronto=" + ready);
        return new AiOperationalDraftResponse(kind.label, kind.module,
                "Rascunho de " + kind.label.toLowerCase(Locale.ROOT) + " preparado sem gravar dados.",
                explicit, ready ? List.of() : kind.missing, ready,
                "Reveja todos os valores no módulo responsável. A IA não gravou nem emitiu o documento.");
    }

    private DraftKind classify(String text) {
        String value = text.toLowerCase(Locale.ROOT);
        if (value.contains("cotação") || value.contains("orcamento") || value.contains("orçamento")) return DraftKind.QUOTATION;
        if (value.contains("fornecedor") || value.contains("compra")) return DraftKind.PURCHASE;
        if (value.contains("cobrança") || value.contains("divida") || value.contains("dívida")) return DraftKind.COLLECTION;
        if (value.contains("reposição") || value.contains("repor") || value.contains("stock")) return DraftKind.REPLENISHMENT;
        if (value.contains("proposta")) return DraftKind.PROPOSAL;
        return DraftKind.CUSTOMER_ORDER;
    }

    private enum DraftKind {
        QUOTATION("Cotação", "comercial", 3, List.of("cliente", "produto/serviço", "quantidade")),
        PURCHASE("Encomenda a fornecedor", "compras", 3, List.of("fornecedor", "produto", "quantidade")),
        CUSTOMER_ORDER("Pedido de cliente", "comercial", 3, List.of("cliente", "produto", "quantidade")),
        COLLECTION("Memorando de cobrança", "financeiro", 2, List.of("cliente", "referência da dívida")),
        REPLENISHMENT("Plano de reposição", "stock", 3, List.of("armazém", "produto", "quantidade")),
        PROPOSAL("Proposta comercial", "comercial", 2, List.of("destinatário", "objectivo"));
        final String label; final String module; final int minimumExplicitFields; final List<String> missing;
        DraftKind(String label, String module, int minimumExplicitFields, List<String> missing) {
            this.label = label; this.module = module; this.minimumExplicitFields = minimumExplicitFields; this.missing = missing;
        }
    }
}
