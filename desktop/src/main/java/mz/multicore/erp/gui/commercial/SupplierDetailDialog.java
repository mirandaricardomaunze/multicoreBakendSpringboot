package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.purchases.dto.SupplierDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Ficha executiva do fornecedor (Supplier Profile View).
 * Implementada através do componente canónico {@link ExecutiveDetailDialog}.
 */
public class SupplierDetailDialog {

    private final Component parent;
    private final SupplierDTO supplier;
    private final Runnable onEdit;
    private final Runnable onViewPayables;

    public SupplierDetailDialog(Component parent, SupplierDTO supplier, Runnable onEdit, Runnable onViewPayables) {
        this.parent = parent;
        this.supplier = supplier;
        this.onEdit = onEdit;
        this.onViewPayables = onViewPayables;
    }

    public void show() {
        ExecutiveDetailDialog dialog = ExecutiveDetailDialog.create(parent, "Ficha do Fornecedor — " + supplier.name());

        String initials = extractInitials(supplier.name());
        dialog.setTitle(supplier.name())
                .setSubtitle("Código: FORN-" + supplier.id() + " | NUIT: " + (supplier.taxId() != null ? supplier.taxId() : "—"))
                .setHeaderAvatar(initials, UIHelper.APPROVED_GREEN);

        // Status badge
        if (supplier.active()) {
            dialog.setStatusBadge("ATIVO", ExecutiveDetailDialog.StatusSeverity.SUCCESS);
        } else {
            dialog.setStatusBadge("INATIVO", ExecutiveDetailDialog.StatusSeverity.DANGER);
        }

        // KPIs
        String phoneStr = supplier.phone() != null && !supplier.phone().isBlank() ? supplier.phone() : "Sem telefone";
        dialog.addKpi("Linha Direta", phoneStr, "Telefone / Central", UIHelper.APPROVED_GREEN, "fas-phone-alt");

        String contactStr = supplier.contactPerson() != null && !supplier.contactPerson().isBlank() ? supplier.contactPerson() : "Geral";
        dialog.addKpi("Pessoa de Contacto", contactStr, "Representante comercial", UIHelper.ACCENT_BLUE, "fas-user-tie");

        String emailStr = supplier.email() != null && !supplier.email().isBlank() ? supplier.email() : "Não registado";
        dialog.addKpi("Correio Eletrónico", emailStr, "Comunicação oficial", UIHelper.ACCENT_CYAN, "fas-envelope");

        String idStr = "FORN-" + supplier.id();
        dialog.addKpi("Identificador", idStr, "Registo de Compras", UIHelper.ACCENT_ORANGE, "fas-hashtag");

        // Abas
        dialog.addTab("Dados Cadastrais & Empresa", "fas-id-card", UIHelper.APPROVED_GREEN, buildCompanyInfoTab());
        dialog.addTab("Contactos & Localização", "fas-map-marked-alt", UIHelper.ACCENT_BLUE, buildContactTab());

        // Ações
        if (onEdit != null) {
            ModernButton editBtn = new ModernButton("Editar Fornecedor", UIHelper.ACCENT, UIHelper.ACCENT_HOVER);
            editBtn.setIcon(UIHelper.icon("fas-edit", 14, Color.WHITE));
            editBtn.addActionListener(e -> {
                dialog.dispose();
                onEdit.run();
            });
            dialog.addLeftAction(editBtn);
        }

        if (onViewPayables != null) {
            ModernButton payablesBtn = new ModernButton("Contas a Pagar", UIHelper.PENDING_YELLOW, UIHelper.PENDING_YELLOW.darker());
            payablesBtn.setIcon(UIHelper.icon("fas-file-invoice-dollar", 14, Color.BLACK));
            payablesBtn.addActionListener(e -> {
                dialog.dispose();
                onViewPayables.run();
            });
            dialog.addLeftAction(payablesBtn);
        }

        dialog.showDialog();
    }

    private JComponent buildCompanyInfoTab() {
        java.util.List<RecordDetailsDialog.PropertyItem> items = new java.util.ArrayList<>();
        items.add(new RecordDetailsDialog.PropertyItem("Razão Social / Entidade", supplier.name()));
        items.add(new RecordDetailsDialog.PropertyItem("NUIT / NIF", supplier.taxId() != null ? supplier.taxId() : "—"));
        items.add(new RecordDetailsDialog.PropertyItem("Pessoa de Contacto", supplier.contactPerson() != null && !supplier.contactPerson().isBlank() ? supplier.contactPerson() : "Departamento Comercial"));
        items.add(new RecordDetailsDialog.PropertyItem("Estado do Fornecedor", supplier.active() ? "Habilitado para Compras" : "Bloqueado / Inativo"));
        items.add(new RecordDetailsDialog.PropertyItem("Identificador Único", "#" + supplier.id()));
        return RecordDetailsDialog.buildPropertySheet(items);
    }

    private JComponent buildContactTab() {
        java.util.List<RecordDetailsDialog.PropertyItem> items = new java.util.ArrayList<>();
        items.add(new RecordDetailsDialog.PropertyItem("Telefone de Contacto", supplier.phone() != null && !supplier.phone().isBlank() ? supplier.phone() : "Não especificado"));
        items.add(new RecordDetailsDialog.PropertyItem("Correio Electrónico (E-mail)", supplier.email() != null && !supplier.email().isBlank() ? supplier.email() : "Não especificado"));
        items.add(new RecordDetailsDialog.PropertyItem("Endereço Físico / Sede", supplier.address() != null && !supplier.address().isBlank() ? supplier.address() : "Não especificado"));
        items.add(new RecordDetailsDialog.PropertyItem("Canal de Aquisição", "Logística e Distribuição Comercial"));
        return RecordDetailsDialog.buildPropertySheet(items);
    }

    private String extractInitials(String name) {
        if (name == null || name.isBlank()) return "FO";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
