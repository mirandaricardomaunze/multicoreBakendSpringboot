package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.session.SignedInUser;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.support.dto.SupportTicketDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Gestão e conversação de pedidos de suporte. */
final class ConfigSupportPanel {
    private final ConfigPanel owner;
    ConfigSupportPanel(ConfigPanel owner) { this.owner = owner; }

    public JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        ModernButton newBtn = UIHelper.createSuccessButton("Novo Pedido");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.addActionListener(e -> ConfigSupportTicketDialog.openNewTicket(owner, this::refresh));
        ModernButton viewBtn = UIHelper.createPrimaryButton("Ver / Responder");
        viewBtn.setIcon(UIHelper.icon("fas-comments", 14));
        viewBtn.addActionListener(e -> viewSupportTicket());
        ModernButton refreshBtn = UIHelper.createRefreshButton(() -> refresh());

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout());
        listCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"#", "Assunto", "Prioridade", "Estado", "Mensagens"};
        owner.supportModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.supportTable = new JTable(owner.supportModel);
        UIHelper.styleTable(owner.supportTable);
        UIHelper.ensureHeadersFit(owner.supportTable);
        owner.supportTable.putClientProperty("noRowInspector", Boolean.TRUE);
        owner.supportTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) viewSupportTicket();
            }
        });
        JScrollPane scroll = new JScrollPane(owner.supportTable);
        UIHelper.styleScrollPane(scroll);

        JTextField supSearch = TableFilter.searchField("Assunto, prioridade ou estado…");
        TableFilter.install(owner.supportTable, supSearch);
        JPanel supBar = TableFilter.bar(supSearch);
        supBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        listCard.add(UIHelper.tableCardTop("Pedidos de Assistência à Plataforma", supBar, refreshBtn, viewBtn, newBtn), BorderLayout.NORTH);
        listCard.add(scroll, BorderLayout.CENTER);
        TableQuickPeekController.install(owner.supportTable, listCard, (peek, row) -> {
            String id = String.valueOf(owner.supportModel.getValueAt(row, 0)), subj = String.valueOf(owner.supportModel.getValueAt(row, 1)),
                    prio = String.valueOf(owner.supportModel.getValueAt(row, 2)), status = String.valueOf(owner.supportModel.getValueAt(row, 3)),
                    msgs = String.valueOf(owner.supportModel.getValueAt(row, 4));
            peek.setHeaderIcon("fas-headset", UIHelper.ACCENT_CYAN);
            peek.setTitle(subj);
            peek.setSubtitle("Ticket #" + id);
            peek.setStatus(status, UIHelper.ACCENT_BLUE);
            peek.setItems(List.of(new QuickPeekPanel.PeekItem("Prioridade", prio, false), new QuickPeekPanel.PeekItem("Mensagens", msgs, false)));
            peek.getOpenFullButton().setText("Ver / Responder");
            peek.setOnOpenFullAction(v -> viewSupportTicket());
        });
        panel.add(listCard, BorderLayout.CENTER);
        return panel;
    }

    public void refresh() {
        if (!SignedInUser.isManagerOrAdmin()) {
            owner.supportModel.setRowCount(0);
            owner.supportModel.addRow(new Object[]{"", "Apenas gestor/administrador pode gerir pedidos.", "", "", ""});
            return;
        }
        UIHelper.loadAsync(owner, owner.supportApiClient::listCompanyTickets, loaded -> {
            owner.supportModel.setRowCount(0);
            owner.supportTickets = loaded;
            for (SupportTicketDTO t : owner.supportTickets) {
                owner.supportModel.addRow(new Object[]{
                        t.id(), t.subject(), t.priorityLabel(), t.statusLabel(), t.messageCount()
                });
            }
        }, error -> {
            owner.supportModel.setRowCount(0);
            owner.supportModel.addRow(new Object[]{"", error.getMessage(), "", "", ""});
        });
    }

    private void viewSupportTicket() {
        int row = TableFilter.selectedModelRow(owner.supportTable);
        if (row < 0 || row >= owner.supportTickets.size()) {
            ToastManager.show(owner, FeedbackType.WARNING, "Selecione um pedido.");
            return;
        }
        ConfigSupportTicketDialog.viewTicket(owner, owner.supportTickets.get(row), this::refresh);
    }
}
