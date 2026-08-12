package com.phcpro.gui;

import com.phcpro.architecture.security.PermissionGuard;
import com.phcpro.gui.components.*;
import com.phcpro.modules.support.dto.SupportTicketDTO;
import com.phcpro.modules.support.dto.CreateTicketRequest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Gestão e conversação de pedidos de suporte. */
final class ConfigSupportPanel {
    private static final String[] TICKET_PRIORITIES = {"LOW", "NORMAL", "HIGH", "URGENT"};
    private final ConfigPanel owner;
    ConfigSupportPanel(ConfigPanel owner) { this.owner = owner; }

    public JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createHeading("Pedidos de Assistência à Plataforma"), BorderLayout.WEST);

        ModernButton newBtn = UIHelper.createSuccessButton("Novo Pedido");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        ModernButton viewBtn = UIHelper.createPrimaryButton("Ver / Responder");
        viewBtn.setIcon(UIHelper.icon("fas-comments", 14));
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Atualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(refreshBtn);
        actions.add(viewBtn);
        actions.add(newBtn);
        header.add(actions, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout());
        listCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"#", "Assunto", "Prioridade", "Estado", "Mensagens"};
        owner.supportModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.supportTable = new JTable(owner.supportModel);
        UIHelper.styleTable(owner.supportTable);
        owner.supportTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) viewSupportTicket();
            }
        });
        JScrollPane scroll = new JScrollPane(owner.supportTable);
        UIHelper.styleScrollPane(scroll);

        JTextField supSearch = TableFilter.searchField("Assunto, prioridade ou estado…");
        TableFilter.install(owner.supportTable, supSearch);
        JPanel supBar = TableFilter.bar(supSearch);
        supBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        listCard.add(supBar, BorderLayout.NORTH);
        listCard.add(scroll, BorderLayout.CENTER);
        panel.add(listCard, BorderLayout.CENTER);

        newBtn.addActionListener(e -> newSupportTicket());
        viewBtn.addActionListener(e -> viewSupportTicket());
        refreshBtn.addActionListener(e -> refresh());

        return panel;
    }

    public void refresh() {
        if (!PermissionGuard.isManagerOrAdmin()) {
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

    private void newSupportTicket() {
        JTextField subjectField = new JTextField();
        JComboBox<String> priorityCombo = new JComboBox<>(TICKET_PRIORITIES);
        priorityCombo.setSelectedItem("NORMAL");
        JTextArea descArea = new JTextArea(4, 30);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        UIHelper.styleTextField(subjectField);
        UIHelper.styleComboBox(priorityCombo);

        JPanel form = UIHelper.createDialogForm(
                "Assunto:", subjectField,
                "Prioridade:", priorityCombo,
                "Descrição:", new JScrollPane(descArea)
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Novo Pedido de Assistência",
                "fas-headset", "Contactar o suporte da plataforma", form).setConfirmButton("Enviar", "fas-paper-plane");
        dlg.setOnSaveAsync(() -> {
            if (subjectField.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("O assunto é obrigatório.");
            }
            CreateTicketRequest request = new CreateTicketRequest(subjectField.getText().trim(),
                    descArea.getText().trim(), (String) priorityCombo.getSelectedItem());
            return () -> owner.supportApiClient.openTicket(request);
        });

        if (dlg.showDialog()) {
            JOptionPane.showMessageDialog(owner, "Pedido enviado ao suporte.", "Sucesso",
                    JOptionPane.INFORMATION_MESSAGE);
            refresh();
        }
    }

    private void viewSupportTicket() {
        int row = TableFilter.selectedModelRow(owner.supportTable);
        if (row < 0 || row >= owner.supportTickets.size()) {
            JOptionPane.showMessageDialog(owner, "Selecione um pedido.", "Suporte", JOptionPane.WARNING_MESSAGE);
            return;
        }
        SupportTicketDTO ticket = owner.supportTickets.get(row);
        UIHelper.loadAsync(owner, () -> owner.supportApiClient.listCompanyMessages(ticket.id()),
                messages -> showSupportThread(ticket, PlataformaPanel.renderThread(messages)),
                error -> owner.showConfigError("conversa de suporte", error));
    }

    private void showSupportThread(SupportTicketDTO ticket, String renderedThread) {
        JTextArea thread = new JTextArea(renderedThread);
        thread.setEditable(false);
        thread.setLineWrap(true);
        thread.setWrapStyleWord(true);
        JScrollPane threadScroll = new JScrollPane(thread);
        threadScroll.setMinimumSize(new Dimension(420, 180));

        JTextArea reply = new JTextArea(3, 40);
        reply.setLineWrap(true);
        reply.setWrapStyleWord(true);
        JPanel form = new JPanel(new BorderLayout(0, 8));
        form.setOpaque(false);
        form.add(threadScroll, BorderLayout.CENTER);
        JPanel replyBox = new JPanel(new BorderLayout(0, 4));
        replyBox.setOpaque(false);
        JLabel lbl = new JLabel("Responder (deixe vazio para só consultar):");
        lbl.setForeground(UIHelper.TEXT_MUTED);
        replyBox.add(lbl, BorderLayout.NORTH);
        replyBox.add(new JScrollPane(reply), BorderLayout.CENTER);
        form.add(replyBox, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(owner, form,
                "Pedido #" + ticket.id() + " — " + ticket.subject(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION && !reply.getText().trim().isEmpty()) {
            String message = reply.getText().trim();
            UIHelper.runWithProgress(owner, "A enviar resposta…", () -> {
                owner.supportApiClient.addCompanyMessage(ticket.id(), message);
                return null;
            }, ignored -> refresh(), error -> owner.showConfigError("resposta ao suporte", error));
        }
    }

    // ------------------------------------------------------------- TAB 6: A Minha Assinatura

}
