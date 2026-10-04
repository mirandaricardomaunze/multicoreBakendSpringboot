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
        JPanel p = new JPanel(new GridLayout(2, 2, 14, 14));
        p.setBackground(UIHelper.BG_DARK);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        p.add(buildCard("Razão Social / Nome da Entidade", "fas-building", UIHelper.APPROVED_GREEN, supplier.name()));
        p.add(buildCard("NUIT / NIF", "fas-passport", UIHelper.ACCENT_CYAN, supplier.taxId() != null ? supplier.taxId() : "—"));
        p.add(buildCard("Pessoa de Contacto", "fas-user-tie", UIHelper.ACCENT_BLUE, supplier.contactPerson() != null && !supplier.contactPerson().isBlank() ? supplier.contactPerson() : "Departamento Comercial"));
        p.add(buildCard("Estado do Fornecedor", "fas-check-circle", supplier.active() ? UIHelper.APPROVED_GREEN : UIHelper.REJECTED_RED, supplier.active() ? "Fornecedor Habilitado para Compras" : "Bloqueado / Inativo"));

        return p;
    }

    private JComponent buildContactTab() {
        JPanel p = new JPanel(new GridLayout(2, 2, 14, 14));
        p.setBackground(UIHelper.BG_DARK);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        p.add(buildCard("Telefone de Contacto", "fas-phone", UIHelper.APPROVED_GREEN, supplier.phone() != null && !supplier.phone().isBlank() ? supplier.phone() : "Não especificado"));
        p.add(buildCard("Correio Eletrónico (E-mail)", "fas-envelope", UIHelper.ACCENT_CYAN, supplier.email() != null && !supplier.email().isBlank() ? supplier.email() : "Não especificado"));
        p.add(buildCard("Endereço Físico / Instalações", "fas-map-marker-alt", UIHelper.ACCENT_ORANGE, supplier.address() != null && !supplier.address().isBlank() ? supplier.address() : "Não especificado"));
        p.add(buildCard("Canal de Aquisição", "fas-truck-loading", UIHelper.ACCENT, "Logística e Distribuição Comercial"));

        return p;
    }

    private ModernPanel buildCard(String title, String icon, Color iconColor, String content) {
        ModernPanel p = new ModernPanel(12);
        p.setLayout(new BorderLayout(0, 6));
        p.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        top.add(new JLabel(UIHelper.icon(icon, 15, iconColor)));
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.setForeground(UIHelper.TEXT_MUTED);
        top.add(t);

        JLabel c = new JLabel("<html><body style='width:240px;'>" + content + "</body></html>");
        c.setFont(new Font("Segoe UI", Font.BOLD, 13));
        c.setForeground(UIHelper.TEXT_LIGHT);

        p.add(top, BorderLayout.NORTH);
        p.add(c, BorderLayout.CENTER);
        return p;
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
