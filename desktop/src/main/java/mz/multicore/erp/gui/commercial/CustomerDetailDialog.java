package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Ficha executiva do cliente (Customer Profile View).
 * Implementada através do componente canónico {@link ExecutiveDetailDialog}.
 */
public class CustomerDetailDialog {

    private final Component parent;
    private final ClientDTO client;
    private final Runnable onEdit;
    private final Runnable onViewStatement;

    public CustomerDetailDialog(Component parent, ClientDTO client, Runnable onEdit, Runnable onViewStatement) {
        this.parent = parent;
        this.client = client;
        this.onEdit = onEdit;
        this.onViewStatement = onViewStatement;
    }

    public void show() {
        ExecutiveDetailDialog dialog = ExecutiveDetailDialog.create(parent, "Ficha do Cliente — " + client.name());

        // Iniciais para o avatar
        String initials = extractInitials(client.name());
        dialog.setTitle(client.name())
                .setSubtitle("Código: " + (client.code() != null ? client.code() : "CLI-" + client.id()) + " | NUIT: " + (client.taxId() != null ? client.taxId() : "—"))
                .setHeaderAvatar(initials, UIHelper.ACCENT_BLUE);

        // Status badge
        dialog.setStatusBadge("ATIVO", ExecutiveDetailDialog.StatusSeverity.SUCCESS);

        // KPIs
        String limitStr = client.creditLimit() != null ? String.format("%,.2f MT", client.creditLimit()) : "Sem limite";
        dialog.addKpi("Limite de Crédito", limitStr, "Teto autorizado", UIHelper.APPROVED_GREEN, "fas-hand-holding-usd");

        String termsStr = client.paymentTermsDays() > 0 ? client.paymentTermsDays() + " dias" : "Pronto Pagamento";
        dialog.addKpi("Condições de Pagamento", termsStr, "Prazo comercial", UIHelper.ACCENT_CYAN, "fas-calendar-check");

        BigDecimal pts = client.loyaltyPoints() != null ? client.loyaltyPoints() : BigDecimal.ZERO;
        dialog.addKpi("Pontos de Fidelização", pts.toPlainString() + " pts", "Saldo acumulado", UIHelper.ACCENT_ORANGE, "fas-star");

        String refStr = client.code() != null && !client.code().isBlank() ? client.code() : "—";
        dialog.addKpi("Ref. Fidelidade", refStr, "Cartão / Identificador", UIHelper.ACCENT, "fas-id-card");

        // Abas
        dialog.addTab("Dados Cadastrais & Fiscais", "fas-id-badge", UIHelper.ACCENT_BLUE, buildInfoTab());
        dialog.addTab("Condições Comerciais", "fas-file-invoice-dollar", UIHelper.APPROVED_GREEN, buildCommercialTab());

        // Ações à esquerda
        if (onEdit != null) {
            ModernButton editBtn = new ModernButton("Editar Cliente", UIHelper.ACCENT, UIHelper.ACCENT_HOVER);
            editBtn.setIcon(UIHelper.icon("fas-edit", 14, Color.WHITE));
            editBtn.addActionListener(e -> {
                dialog.dispose();
                onEdit.run();
            });
            dialog.addLeftAction(editBtn);
        }

        if (onViewStatement != null) {
            ModernButton stmtBtn = new ModernButton("Conta Corrente", UIHelper.ACCENT_CYAN, UIHelper.ACCENT_CYAN.darker());
            stmtBtn.setIcon(UIHelper.icon("fas-file-invoice", 14, Color.WHITE));
            stmtBtn.addActionListener(e -> {
                dialog.dispose();
                onViewStatement.run();
            });
            dialog.addLeftAction(stmtBtn);
        }

        dialog.showDialog();
    }

    private JComponent buildInfoTab() {
        java.util.List<RecordDetailsDialog.PropertyItem> items = new java.util.ArrayList<>();
        items.add(new RecordDetailsDialog.PropertyItem("Nome / Razão Social", client.name()));
        items.add(new RecordDetailsDialog.PropertyItem("NUIT / NIF", client.taxId() != null ? client.taxId() : "Consumidor Final"));
        items.add(new RecordDetailsDialog.PropertyItem("Correio Electrónico (E-mail)", client.email() != null && !client.email().isBlank() ? client.email() : "Não registado"));
        items.add(new RecordDetailsDialog.PropertyItem("Endereço / Sede", client.address() != null && !client.address().isBlank() ? client.address() : "Não especificado"));
        items.add(new RecordDetailsDialog.PropertyItem("Código de Registo", "#" + client.id()));
        return RecordDetailsDialog.buildPropertySheet(items);
    }

    private JComponent buildCommercialTab() {
        java.util.List<RecordDetailsDialog.PropertyItem> items = new java.util.ArrayList<>();
        items.add(new RecordDetailsDialog.PropertyItem("Prazo de Pagamento Acordado", client.paymentTermsDays() > 0 ? client.paymentTermsDays() + " dias" : "Pronto Pagamento"));
        String limit = client.creditLimit() != null ? String.format("%,.2f MT", client.creditLimit()) : "Crédito Livre";
        items.add(new RecordDetailsDialog.PropertyItem("Limite de Crédito Atribuído", limit));
        BigDecimal pts = client.loyaltyPoints() != null ? client.loyaltyPoints() : BigDecimal.ZERO;
        items.add(new RecordDetailsDialog.PropertyItem("Pontos de Fidelização", pts.toPlainString() + " pontos"));
        items.add(new RecordDetailsDialog.PropertyItem("Código do Cartão / Referência", client.code() != null && !client.code().isBlank() ? client.code() : "Sem cartão associado"));
        return RecordDetailsDialog.buildPropertySheet(items);
    }

    private String extractInitials(String name) {
        if (name == null || name.isBlank()) return "CL";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
