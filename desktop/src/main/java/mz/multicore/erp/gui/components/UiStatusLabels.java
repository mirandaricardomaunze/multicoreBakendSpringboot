package mz.multicore.erp.gui.components;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.ListCellRenderer;
import java.awt.Component;
import java.util.Locale;

/** Tradução central de códigos técnicos (estados, perfis) para rótulos em pt-MZ. */
final class UiStatusLabels {

    private UiStatusLabels() {}

    static String humanRole(String role) {
        if (role == null || role.isBlank()) return "—";
        return switch (role.trim().toUpperCase()) {
            case "ADMIN" -> "Administrador";
            case "MANAGER" -> "Gestor";
            case "SELLER" -> "Vendedor";
            case "EMPLOYEE" -> "Funcionário";
            case "SUPERADMIN" -> "Administrador da Plataforma";
            default -> role;
        };
    }

    static void humanizeRoleCombo(JComboBox<String> combo) {
        @SuppressWarnings("unchecked")
        ListCellRenderer<? super String> original = (ListCellRenderer<? super String>) combo.getRenderer();
        combo.setRenderer((list, value, index, selected, focus) -> {
            Component rendered = original.getListCellRendererComponent(list, value, index, selected, focus);
            if (rendered instanceof JLabel label) label.setText(humanRole(value));
            return rendered;
        });
    }

    static String humanStatus(String status) {
        if (status == null || status.isBlank()) return "—";
        String s = status.trim().toUpperCase(Locale.ROOT);
        return switch (s) {
            case "ACTIVE", "ACTIVO" -> "Activo";
            case "ACTIVA" -> "Activa";
            case "INACTIVE", "INACTIVO" -> "Inactivo";
            case "INACTIVA" -> "Inactiva";
            case "OPEN", "ABERTO" -> "Aberto";
            case "IN_PROGRESS", "EM_CURSO", "EM CURSO" -> "Em curso";
            case "RESOLVED", "RESOLVIDO" -> "Resolvido";
            case "CLOSED", "FECHADO" -> "Fechado";
            case "DRAFT", "RASCUNHO" -> "Rascunho";
            case "PENDING", "PENDENTE" -> "Pendente";
            case "PENDING_APPROVAL", "PENDENTE_APROVACAO", "PENDENTE DE APROVAÇÃO", "PENDENTE DE APROVACAO" -> "Pendente de aprovação";
            case "PENDING_DISCOUNT_APPROVAL" -> "Aprovação de desconto";
            case "POR_FACTURAR", "POR_FATURAR", "POR FACTURAR", "POR FATURAR" -> "Por faturar";
            case "AWAITING_SEPARATION", "AGUARDA_SEPARACAO", "AGUARDA SEPARAÇÃO", "AGUARDA SEPARACAO" -> "Aguarda separação";
            case "IN_SEPARATION", "EM_SEPARACAO", "EM SEPARAÇÃO", "EM SEPARACAO" -> "Em separação";
            case "SEPARATED", "SEPARADO" -> "Separado";
            case "GUIDE_PENDING", "GUIA_POR_APROVAR", "GUIA POR APROVAR" -> "Guia por aprovar";
            case "GUIDED", "EXPEDIDO_POR_GUIA", "EXPEDIDO POR GUIA" -> "Expedido por guia";
            case "TRANSFER_PENDING", "TRANSFERENCIA_POR_APROVAR", "TRANSFERÊNCIA POR APROVAR" -> "Transferência por aprovar";
            case "TRANSFERRED", "TRANSFERIDO", "TRANSFERIDO PARA A LOJA" -> "Transferido para a loja";
            case "BILLED", "INVOICED", "FACTURADO", "FACTURADA", "FATURADO", "FATURADA" -> "Faturado";
            case "APPROVED", "APROVADO", "APROVADA" -> "Aprovado";
            case "REJECTED", "REJEITADO", "REJEITADA" -> "Rejeitado";
            case "CANCELLED", "CANCELADO", "CANCELADA", "ANULADO", "ANULADA" -> "Cancelado";
            case "PAID", "PAGA", "PAGO" -> "Paga";
            case "PARTIALLY_PAID", "PARCIALMENTE_PAGA", "PARCIALMENTE PAGA" -> "Parcialmente paga";
            case "OVERDUE", "EM_ATRASO", "EM ATRASO" -> "Em atraso";
            case "ORDERED", "ENCOMENDADO" -> "Encomendado";
            case "PARTIALLY_RECEIVED", "PARCIALMENTE_RECEBIDO", "PARCIALMENTE RECEBIDO" -> "Parcialmente recebido";
            case "RECEIVED", "RECEBIDO" -> "Recebido";
            case "RECEIVED_WITH_DISCREPANCY", "RECEBIDO C/ DIVERGÊNCIA", "RECEBIDO C/ DIVERGENCIA" -> "Recebido c/ divergência";
            case "COMPLETED", "CONCLUIDO", "CONCLUÍDO", "CONCLUIDA", "CONCLUÍDA" -> "Concluído";
            case "SUSPENDED", "SUSPENSO", "SUSPENSA" -> "Suspenso";
            case "EXPIRED", "EXPIRADO", "EXPIRADA" -> "Expirado";
            case "OPERATIONAL", "OPERACIONAL" -> "Operacional";
            case "MAINTENANCE", "MANUTENCAO", "MANUTENÇÃO" -> "Manutenção";
            case "INSPECTION", "INSPECCAO", "INSPECÇÃO" -> "Inspecção";
            case "OUT_OF_SERVICE", "FORA_DE_SERVICO", "FORA DE SERVIÇO" -> "Fora de serviço";
            case "SOLD", "VENDIDO" -> "Vendido";
            case "PLANEADA" -> "Planeada";
            case "EM_PRODUCAO", "EM PRODUÇÃO", "EM PRODUCAO" -> "Em produção";
            case "ISSUED", "EMITIDA", "EMITIDO" -> "Emitida";
            case "SHIPPED", "ENVIADO", "EM TRANSITO", "EM TRÂNSITO", "IN_TRANSIT" -> "Em trânsito";
            case "DELIVERED", "ENTREGUE" -> "Entregue";
            case "MATCHED", "CONCILIADO" -> "Conciliado";
            case "UNMATCHED", "POR_CONCILIAR", "POR CONCILIAR" -> "Por conciliar";
            // Tipos de falta (§B2)
            case "PENDING_JUSTIFICATION", "POR JUSTIFICAR" -> "Por justificar";
            case "JUSTIFIED", "JUSTIFICADA" -> "Justificada";
            case "UNJUSTIFIED", "INJUSTIFICADA" -> "Injustificada";
            case "SICK", "BAIXA MEDICA", "BAIXA MÉDICA" -> "Baixa médica";
            case "MATERNITY", "MATERNIDADE" -> "Maternidade";
            case "UNPAID_LEAVE", "LICENCA SEM VENCIMENTO", "LICENÇA SEM VENCIMENTO" -> "Licença sem vencimento";
            // Tipos de movimento de stock
            case "PURCHASE", "COMPRA" -> "Compra";
            case "ENTRY", "ENTRADA" -> "Entrada";
            case "SALE", "VENDA" -> "Venda";
            case "TRANSFER", "TRANSFERENCIA", "TRANSFERÊNCIA" -> "Transferência";
            case "ADJUSTMENT", "AJUSTE" -> "Ajuste";
            case "RETURN", "DEVOLUCAO", "DEVOLUÇÃO" -> "Devolução";
            case "REVERSAL", "ESTORNO" -> "Estorno";
            case "ESGOTADO" -> "Esgotado";
            case "CRITICO", "CRÍTICO" -> "Crítico";
            case "BAIXO" -> "Baixo";
            case "TERMINATED", "RESCINDIDO", "DEMITIDO" -> "Rescindido";
            case "SUPERADMIN" -> "SuperAdmin";
            case "DEBIT", "DEBITO", "DÉBITO" -> "Débito";
            case "CREDIT", "CREDITO", "CRÉDITO" -> "Crédito";
            // Métodos de pagamento
            case "CASH", "DINHEIRO" -> "Dinheiro";
            case "BANK_TRANSFER" -> "Transferência";
            case "CARD", "CARTAO", "CARTÃO" -> "Cartão";
            case "MPESA", "M-PESA" -> "M-Pesa";
            case "EMOLA", "E-MOLA" -> "E-Mola";
            default -> status.trim().replace('_', ' ');
        };
    }

    /** Tipo de imposto em linguagem corrente; {@code null} quando não é um tipo fiscal. */
    static String taxType(String type) {
        return switch (type == null ? "" : type) {
            case "IVA_STANDARD" -> "IVA normal";
            case "IVA_REDUCED" -> "IVA reduzido";
            case "IVA_ZERO" -> "IVA zero";
            case "IVA_EXEMPT" -> "IVA isento";
            case "WITHHOLDING" -> "Retenção na fonte";
            case "CORPORATE_INCOME" -> "IRPC";
            case "EXCISE" -> "Consumo específico";
            default -> null;
        };
    }
}
