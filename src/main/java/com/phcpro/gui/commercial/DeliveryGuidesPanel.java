package com.phcpro.gui.commercial;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.desktop.client.ComercialApiClient;
import com.phcpro.gui.components.*;
import com.phcpro.modules.comercial.dto.DeliveryGuideDTO;
import com.phcpro.modules.printing.PdfFileSaver;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/** Lista e fluxo de aprovação das guias de remessa. */
public final class DeliveryGuidesPanel extends JPanel {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ComercialApiClient apiClient;
    private final Runnable ordersRefresh;
    private final DefaultTableModel model;
    private final JTable table;

    public DeliveryGuidesPanel(ComercialApiClient apiClient, Runnable ordersRefresh) {
        this.apiClient = apiClient;
        this.ordersRefresh = ordersRefresh;
        setLayout(new BorderLayout(0, 15));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(15, 15, 15, 15));
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.add(UIHelper.createHeading("Guias de Remessa"), BorderLayout.WEST);
        add(header, BorderLayout.NORTH);
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));
        model = new DefaultTableModel(new String[]{"ID", "Nº Guia", "Data", "Encomenda", "Cliente", "Armazém",
                "Responsável", "Viatura", "Total", "Estado"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(8).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(9).setCellRenderer(TableCellRenderers.status());
        hideColumn(0);
        JTextField search = TableFilter.searchField("Nº guia, encomenda, cliente ou viatura…");
        JComboBox<String> status = TableFilter.combo("Todos os estados",
                "PENDING_APPROVAL", "APPROVED", "REJECTED", "CANCELLED");
        TableFilter.install(table, search, new TableFilter.ColumnFilter(status, 9));
        JPanel filters = TableFilter.bar(search, TableFilter.label("Estado:"), status);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(filters, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        ActionMenuButton more = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Imprimir", UIHelper.icon("fas-print", 14), this::print)
                .addAction("Actualizar", UIHelper.icon("fas-sync-alt", 14), this::refresh);
        actions.add(more);
        actions.add(button("Cancelar", "fas-ban", UIHelper.createDangerButton("Cancelar"), this::cancel));
        actions.add(button("Rejeitar", "fas-times", UIHelper.createDangerButton("Rejeitar"), this::reject));
        actions.add(button("Aprovar", "fas-check", UIHelper.createSuccessButton("Aprovar"), this::approve));
        card.add(actions, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    private ModernButton button(String text, String icon, ModernButton button, Runnable action) {
        button.setText(text);
        button.setIcon(UIHelper.icon(icon, 14));
        button.addActionListener(e -> action.run());
        return button;
    }

    public void refresh() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> apiClient.getDeliveryGuidesByCompany(companyId), guides -> {
            model.setRowCount(0);
            for (DeliveryGuideDTO guide : guides) model.addRow(new Object[]{guide.id(), guide.guideNumber(),
                    guide.guideDate() == null ? "—" : guide.guideDate().format(DATE_TIME), guide.orderNumber(),
                    guide.clientName(), guide.warehouseName(), blank(guide.responsible()), blank(guide.vehicle()),
                    guide.totalAmount(), guide.status()});
        }, error -> showError("carregar guias", error));
    }

    private void approve() {
        int row = selected("aprovar");
        if (row < 0 || !pending(row)) return;
        String number = String.valueOf(model.getValueAt(row, 1));
        if (JOptionPane.showConfirmDialog(this,
                "A aprovação da guia " + number + " dará saída definitiva ao stock.\n\nPretende continuar?",
                "Confirmar Aprovação", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        Long id = (Long) model.getValueAt(row, 0);
        UIHelper.runWithProgress(this, "A aprovar guia…", () -> apiClient.approveDeliveryGuide(id), ignored -> {
            JOptionPane.showMessageDialog(this, "Guia " + number + " aprovada. O stock foi atualizado.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            refreshBoth();
        }, error -> showError("aprovar guia", error));
    }

    private void reject() {
        int row = selected("rejeitar");
        if (row < 0 || !pending(row)) return;
        String number = String.valueOf(model.getValueAt(row, 1));
        String reason = UIHelper.promptRequiredText("Rejeitar Guia de Remessa", "fas-times",
                "Guia " + number, "Motivo da rejeição:");
        if (reason == null) return;
        Long id = (Long) model.getValueAt(row, 0);
        UIHelper.runWithProgress(this, "A rejeitar guia…", () -> apiClient.rejectDeliveryGuide(id, reason), ignored -> {
            JOptionPane.showMessageDialog(this, "Guia " + number + " rejeitada. A encomenda voltou a ficar disponível.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            refreshBoth();
        }, error -> showError("rejeitar guia", error));
    }

    private void cancel() {
        int row = selected("cancelar");
        if (row < 0 || !pending(row)) return;
        String number = String.valueOf(model.getValueAt(row, 1));
        if (JOptionPane.showConfirmDialog(this,
                "Cancelar a guia " + number + "?\nA encomenda voltará a ficar disponível para faturação ou nova guia.",
                "Confirmar Cancelamento", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        Long id = (Long) model.getValueAt(row, 0);
        UIHelper.runWithProgress(this, "A cancelar guia…", () -> apiClient.cancelDeliveryGuide(id), ignored -> {
            JOptionPane.showMessageDialog(this, "Guia " + number + " cancelada.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            refreshBoth();
        }, error -> showError("cancelar guia", error));
    }

    private void print() {
        int row = selected("imprimir");
        if (row < 0) return;
        Long id = (Long) model.getValueAt(row, 0);
        String number = String.valueOf(model.getValueAt(row, 1));
        UIHelper.runWithProgress(this, "A gerar guia em PDF…", () -> apiClient.renderDeliveryGuide(id),
                pdf -> PdfFileSaver.saveAndOpen(pdf, "guia-remessa-" + number), error -> showError("gerar guia", error));
    }

    private int selected(String action) {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0) JOptionPane.showMessageDialog(this, "Selecione uma guia na tabela para " + action + ".",
                "Aviso", JOptionPane.WARNING_MESSAGE);
        return row;
    }

    private boolean pending(int row) {
        if ("PENDING_APPROVAL".equals(String.valueOf(model.getValueAt(row, 9)))) return true;
        JOptionPane.showMessageDialog(this, "Esta ação só é permitida para guias pendentes de aprovação.",
                "Erro", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    private void refreshBoth() { refresh(); ordersRefresh.run(); }
    private void hideColumn(int index) {
        table.getColumnModel().getColumn(index).setMinWidth(0);
        table.getColumnModel().getColumn(index).setMaxWidth(0);
        table.getColumnModel().getColumn(index).setWidth(0);
    }
    private static String blank(String value) { return value == null || value.isBlank() ? "—" : value; }
    private void showError(String action, Throwable error) {
        JOptionPane.showMessageDialog(this, "Não foi possível " + action + ": " + error.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
