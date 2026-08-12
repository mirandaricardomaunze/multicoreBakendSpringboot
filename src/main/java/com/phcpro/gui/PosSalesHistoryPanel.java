package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.*;
import com.phcpro.modules.comercial.dto.InvoiceDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Histórico pesquisável das vendas do POS. */
final class PosSalesHistoryPanel {
    private final POSPanel owner;
    private JLabel salesHistorySummary;
    PosSalesHistoryPanel(POSPanel owner) { this.owner = owner; }

    public JPanel buildPanel() {
        String[] cols = {"ID", "Nº Venda", "Data", "Operador", "Cliente", "Total", "Estado"};
        owner.salesHistoryModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.salesHistoryTable = new JTable(owner.salesHistoryModel);
        UIHelper.styleTable(owner.salesHistoryTable);
        owner.salesHistoryTable.getColumnModel().getColumn(5)
                .setCellRenderer(com.phcpro.gui.components.TableCellRenderers.money());
        owner.salesHistoryTable.getColumnModel().getColumn(6)
                .setCellRenderer(com.phcpro.gui.components.TableCellRenderers.status());
        owner.salesHistoryTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        owner.salesHistoryTable.setFillsViewportHeight(true);
        // Esconder coluna ID
        owner.salesHistoryTable.getColumnModel().getColumn(0).setMinWidth(0);
        owner.salesHistoryTable.getColumnModel().getColumn(0).setMaxWidth(0);
        owner.salesHistoryTable.getColumnModel().getColumn(0).setWidth(0);
        // Larguras proporcionais
        owner.salesHistoryTable.getColumnModel().getColumn(1).setPreferredWidth(140);  // Nº Venda
        owner.salesHistoryTable.getColumnModel().getColumn(2).setPreferredWidth(120);  // Data
        owner.salesHistoryTable.getColumnModel().getColumn(3).setPreferredWidth(100);  // Operador
        owner.salesHistoryTable.getColumnModel().getColumn(4).setPreferredWidth(160);  // Cliente
        owner.salesHistoryTable.getColumnModel().getColumn(5).setPreferredWidth(100);  // Total
        owner.salesHistoryTable.getColumnModel().getColumn(6).setPreferredWidth(80);   // Estado

        JScrollPane scroll = new JScrollPane(owner.salesHistoryTable);
        UIHelper.styleScrollPane(scroll);

        salesHistorySummary = new JLabel(" ");
        salesHistorySummary.setForeground(UIHelper.TEXT_LIGHT);
        salesHistorySummary.setBorder(new EmptyBorder(8, 8, 8, 8));

        ModernButton reprintBtn = UIHelper.createPrimaryButton("Reimprimir Recibo");
        reprintBtn.setIcon(UIHelper.icon("fas-print", 14));
        reprintBtn.addActionListener(e -> {
            int row = TableFilter.selectedModelRow(owner.salesHistoryTable);
            if (row < 0) {
                JOptionPane.showMessageDialog(owner, "Selecione uma venda primeiro.",
                        "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Long invoiceId = (Long) owner.salesHistoryModel.getValueAt(row, 0);
            String invNum = String.valueOf(owner.salesHistoryModel.getValueAt(row, 1));
            UIHelper.runWithProgress(owner, "A gerar recibo…",
                    () -> { com.phcpro.modules.printing.PdfFileSaver.saveAndOpen(
                                owner.posApiClient.renderReceipt(invoiceId), "recibo-" + invNum);
                            return null; },
                    ok -> { },
                    ex -> JOptionPane.showMessageDialog(owner,
                            "Erro ao gerar recibo: " + ex.getMessage(),
                            "Erro", JOptionPane.ERROR_MESSAGE));
        });

        ModernButton returnBtn = UIHelper.createSecondaryButton("Devolver / Trocar");
        returnBtn.setIcon(UIHelper.icon("fas-undo", 14));
        returnBtn.addActionListener(e -> owner.showReturnDialog());

        ModernButton refreshBtn = UIHelper.createSecondaryButton("Atualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> refresh());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        buttons.setOpaque(false);
        buttons.add(refreshBtn);
        buttons.add(returnBtn);
        buttons.add(reprintBtn);

        JTextField shSearch = TableFilter.searchField("Nº venda, operador ou cliente…");
        JComboBox<String> shEstado = TableFilter.combo("Todos os estados",
                "PAID", "APPROVED", "PARTIALLY_PAID", "CANCELLED");
        JComboBox<String> shPeriodo = TableFilter.periodCombo();
        TableFilter.install(owner.salesHistoryTable, shSearch,
                java.util.List.of(new TableFilter.ColumnFilter(shEstado, 6)),
                java.util.List.of(new TableFilter.PeriodFilter(shPeriodo, 2)));
        JPanel shBar = TableFilter.bar(shSearch,
                TableFilter.label("Estado:"), shEstado,
                TableFilter.label("Data:", "fas-calendar-alt"), shPeriodo);
        shBar.setBorder(new EmptyBorder(0, 0, 8, 0));

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(salesHistorySummary, BorderLayout.NORTH);
        north.add(shBar, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(15, 5, 5, 5));
        content.add(north, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        return content;
    }

    public void refresh() {
        if (owner.salesHistoryModel == null || salesHistorySummary == null) return;

        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> owner.comercialApiClient.getPOSSalesByCompany(companyId),
                this::applySalesHistory, error -> owner.showPosLoadError("histórico de vendas", error));
    }

    private void applySalesHistory(java.util.List<InvoiceDTO> loaded) {
        owner.salesHistoryList = loaded;
        java.time.format.DateTimeFormatter dtf =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        owner.salesHistoryModel.setRowCount(0);
        for (var inv : owner.salesHistoryList) {
            owner.salesHistoryModel.addRow(new Object[]{
                    inv.id(),
                    inv.invoiceNumber(),
                    inv.createdAt() != null ? inv.createdAt().format(dtf) : "—",
                    inv.createdBy() != null ? inv.createdBy() : "—",
                    inv.clientName() != null ? inv.clientName() : "—",
                    inv.totalAmount(),
                    inv.status() != null ? inv.status().name() : "—"
            });
        }
        BigDecimal total = owner.salesHistoryList.stream()
                .map(InvoiceDTO::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        salesHistorySummary.setText(String.format(
                "<html><b>%d</b> vendas POS — total <b>%,.2f MT</b></html>",
                owner.salesHistoryList.size(), total));
    }

}
