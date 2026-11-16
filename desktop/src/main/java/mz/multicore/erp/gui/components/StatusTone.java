package mz.multicore.erp.gui.components;

import java.awt.Color;
import java.util.Locale;
import java.util.Set;

/**
 * One status vocabulary for every table: green done, red cancelled or late, amber waiting, blue in progress,
 * grey anything else. Both the shared cell renderer and {@code TableCellRenderers.status()} read it, so the
 * same word has the same colour everywhere. Statuses are coloured text, not button-like badges
 * (decision of 2026-11-05, see TableStatusPtMzHarnessTest).
 */
public final class StatusTone {

    public enum Tone { GOOD, BAD, WARN, INFO, NEUTRAL }

    private static final Set<String> GOOD = Set.of(
            "ACTIVE", "ACTIVA", "ACTIVO", "ATIVA", "ATIVO", "APPROVED", "APROVADO", "APROVADA", "PAID", "PAGA", "PAGO",
            "OPEN", "ABERTO", "EM STOCK", "DISPONÍVEL", "RESOLVED", "RESOLVIDO", "FATURADA", "FATURADO", "FACTURADO",
            "FACTURADA", "BILLED", "INVOICED", "MATCHED", "CONCILIADO", "COMPLETED", "CONCLUÍDO", "CONCLUIDO",
            "CONCLUÍDA", "CONCLUIDA", "RECEIVED", "RECEBIDO", "RECEBIDA", "DELIVERED", "ENTREGUE", "OPERATIONAL",
            "OPERACIONAL", "OK");
    private static final Set<String> BAD = Set.of(
            "INACTIVE", "INACTIVA", "INACTIVO", "INATIVO", "INATIVA", "REJECTED", "REJEITADO", "REJEITADA", "CANCELLED",
            "CANCELADO", "CANCELADA", "ANULADO", "ANULADA", "OVERDUE", "EM ATRASO", "IGNORED", "IGNORADO", "ESGOTADO",
            "SEM STOCK", "OUT_OF_SERVICE", "FORA DE SERVIÇO", "FORA DE SERVICO", "SOLD", "VENDIDO", "EXPIRED",
            "EXPIRADO", "TERMINATED", "RESCINDIDO", "DEMITIDO");
    private static final Set<String> WARN = Set.of(
            "PENDING", "PENDING_APPROVAL", "PENDING_DISCOUNT_APPROVAL", "PARTIALLY_PAID", "PENDENTE",
            "PENDENTE DE APROVAÇÃO", "PENDENTE DE APROVACAO", "APROVAÇÃO DE DESCONTO", "IN_PROGRESS", "EM CURSO",
            "POR FATURAR", "POR FACTURAR", "AWAITING_SEPARATION", "AGUARDA SEPARAÇÃO", "AGUARDA SEPARACAO",
            "IN_SEPARATION", "EM SEPARAÇÃO", "EM SEPARACAO", "GUIDE_PENDING", "GUIA POR APROVAR", "TRANSFER_PENDING",
            "TRANSFERÊNCIA POR APROVAR", "ORDERED", "ENCOMENDADO", "PARTIALLY_RECEIVED", "PARCIALMENTE RECEBIDO",
            "RECEIVED_WITH_DISCREPANCY", "RECEBIDO C/ DIVERGÊNCIA", "RECEBIDO C/ DIVERGENCIA", "MAINTENANCE",
            "MANUTENÇÃO", "MANUTENCAO", "INSPECTION", "INSPECÇÃO", "INSPECCAO", "UNMATCHED", "POR CONCILIAR",
            "CRÍTICO", "CRITICO", "BAIXO", "STOCK BAIXO");
    private static final Set<String> INFO = Set.of(
            "PROCESSING", "EM PROCESSAMENTO", "SHIPPED", "ENVIADO", "EM TRÂNSITO", "EM TRANSITO", "IN_TRANSIT",
            "SCHEDULED", "AGENDADO", "DRAFT", "RASCUNHO", "SEPARATED", "SEPARADO", "GUIDED", "EXPEDIDO POR GUIA",
            "TRANSFERRED", "TRANSFERIDO", "TRANSFERIDO PARA A LOJA", "PLANEADA", "EM PRODUÇÃO", "EM_PRODUCAO",
            "EM PRODUCAO", "SUSPENDED", "SUSPENSO", "ISSUED", "EMITIDA", "EMITIDO");

    private StatusTone() {}

    public static Tone toneOf(String text) {
        if (text == null || text.isBlank()) return Tone.NEUTRAL;
        String s = text.trim().toUpperCase(Locale.ROOT);
        if (GOOD.contains(s)) return Tone.GOOD;
        if (BAD.contains(s) || s.startsWith("VENCIDO") || s.startsWith("VENCIDA")) return Tone.BAD;
        if (WARN.contains(s) || s.contains("PENDENTE") || s.contains("PENDING") || s.startsWith("VENCE")
                || s.contains("DÍVIDA") || s.contains("DIVIDA") || s.contains("PARCIAL") || s.contains("PARTIALLY")
                || s.contains("DIVERG") || s.contains("DISCREPANCY") || s.startsWith("AGUARDA")) return Tone.WARN;
        if (INFO.contains(s)) return Tone.INFO;
        return Tone.NEUTRAL;
    }

    public static Color colorOf(Tone tone) {
        return switch (tone) {
            case GOOD -> UIHelper.APPROVED_GREEN;
            case BAD -> UIHelper.REJECTED_RED;
            case WARN -> UIHelper.PENDING_YELLOW;
            case INFO -> UIHelper.ACCENT_BLUE;
            case NEUTRAL -> UIHelper.TEXT_MUTED;
        };
    }
}
