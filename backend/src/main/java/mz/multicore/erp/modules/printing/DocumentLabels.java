package mz.multicore.erp.modules.printing;

import java.util.Map;

/** Textos em português para os valores internos que aparecem nos documentos impressos. */
final class DocumentLabels {

    private static final Map<String, String> STATUS = Map.ofEntries(
            Map.entry("DRAFT", "Rascunho"),
            Map.entry("PENDING_APPROVAL", "Pendente de aprovação"),
            Map.entry("PENDING_DISCOUNT_APPROVAL", "Desconto por aprovar"),
            Map.entry("APPROVED", "Aprovada"),
            Map.entry("PARTIALLY_PAID", "Parcialmente paga"),
            Map.entry("PAID", "Paga"),
            Map.entry("REJECTED", "Rejeitada"),
            Map.entry("CANCELLED", "Anulada"));

    private static final Map<String, String> REASON = Map.of(
            "RETURN", "Devolução de mercadoria",
            "DISCOUNT", "Desconto comercial",
            "ERROR", "Correcção de fatura",
            "FREIGHT", "Frete / portes",
            "SURCHARGE", "Encargos adicionais",
            "CORRECTION", "Correcção de valor",
            "OTHER", "Outro");

    private DocumentLabels() {}

    static String status(Enum<?> status) {
        return status == null ? "—" : STATUS.getOrDefault(status.name(), status.name());
    }

    static String reason(Enum<?> reason) {
        return reason == null ? "—" : REASON.getOrDefault(reason.name(), reason.name());
    }
}
