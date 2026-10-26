package mz.multicore.erp.gui.platform;

import mz.multicore.erp.desktop.client.PlatformApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.support.dto.SupportMessageDTO;
import mz.multicore.erp.modules.support.dto.SupportTicketDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Vista de gestão de pedidos de assistência e suporte ao cliente (superadmin).
 */
public class PlatformSupportTicketsView extends JPanel {

    private final PlatformApiClient platformApiClient;
    private final PlatformFeedbackHandler feedbackHandler;

    private DefaultTableModel ticketsModel;
    private JTable ticketsTable;
    private List<SupportTicketDTO> tickets = new ArrayList<>();

    public PlatformSupportTicketsView(PlatformApiClient platformApiClient, PlatformFeedbackHandler feedbackHandler) {
        this.platformApiClient = platformApiClient;
        this.feedbackHandler = feedbackHandler;

        setLayout(new BorderLayout(0, 12));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        ModernButton openBtn = UIHelper.createPrimaryButton("Abrir / Responder");
        openBtn.setIcon(UIHelper.icon("fas-comments", 14));
        openBtn.addActionListener(e -> openTicketConversation());

        ModernButton statusBtn = UIHelper.createSecondaryButton("Mudar Estado");
        statusBtn.setIcon(UIHelper.icon("fas-tasks", 14));
        statusBtn.addActionListener(e -> changeTicketStatus());

        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> loadTickets());

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout());
        listCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"#", "Empresa", "Assunto", "Prioridade", "Estado", "Responsável", "Mensagens"};
        ticketsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        ticketsTable = new JTable(ticketsModel);
        UIHelper.styleTable(ticketsTable);
        UIHelper.ensureHeadersFit(ticketsTable);
        ticketsTable.putClientProperty("noRowInspector", Boolean.TRUE);
        ticketsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openTicketConversation();
            }
        });
        JScrollPane scroll = new JScrollPane(ticketsTable);
        UIHelper.styleScrollPane(scroll);

        JTextField tSearch = TableFilter.searchField("Empresa ou assunto…");
        JComboBox<String> tEstado = TableFilter.combo("Todos os estados", "Aberto", "Em curso", "Resolvido", "Fechado");
        JComboBox<String> tPrio = TableFilter.combo("Todas as prioridades", "Baixa", "Normal", "Alta", "Urgente");
        TableFilter.install(ticketsTable, tSearch,
                new TableFilter.ColumnFilter(tEstado, 4), new TableFilter.ColumnFilter(tPrio, 3));
        JPanel tBar = TableFilter.bar(tSearch, TableFilter.label("Estado:"), tEstado, TableFilter.label("Prioridade:"), tPrio);
        tBar.setBorder(new EmptyBorder(0, 0, 10, 0));

        listCard.add(UIHelper.tableCardTop("Pedidos de Assistência", tBar, refreshBtn, statusBtn, openBtn), BorderLayout.NORTH);
        listCard.add(scroll, BorderLayout.CENTER);

        add(listCard, BorderLayout.CENTER);
    }

    public void loadTickets() {
        UIHelper.loadAsync(this, platformApiClient::listAllTickets, loaded -> {
            tickets = loaded;
            ticketsModel.setRowCount(0);
            for (SupportTicketDTO t : tickets) {
                ticketsModel.addRow(new Object[]{
                        t.id(), t.companyName(), t.subject(), t.priorityLabel(), t.statusLabel(),
                        t.assignee() == null ? "—" : t.assignee(), t.messageCount()
                });
            }
        }, error -> feedbackHandler.showError("pedidos de assistência", error, this::loadTickets));
    }

    private SupportTicketDTO selectedTicket() {
        int row = TableFilter.selectedModelRow(ticketsTable);
        if (row < 0 || row >= tickets.size()) {
            feedbackHandler.showNotice(FeedbackType.WARNING, "Seleccione um pedido", "Escolha um pedido de assistência na lista.");
            return null;
        }
        return tickets.get(row);
    }

    private void openTicketConversation() {
        SupportTicketDTO ticket = selectedTicket();
        if (ticket == null) return;
        UIHelper.loadAsync(this, () -> platformApiClient.listPlatformMessages(ticket.id()),
                messages -> showTicketConversation(ticket, renderThread(messages)),
                error -> feedbackHandler.showError("conversa de assistência", error, this::openTicketConversation));
    }

    private void showTicketConversation(SupportTicketDTO ticket, String renderedThread) {
        JTextArea thread = new JTextArea(renderedThread);
        thread.setEditable(false);
        thread.setLineWrap(true);
        thread.setWrapStyleWord(true);
        JScrollPane threadScroll = new JScrollPane(thread);
        threadScroll.setMinimumSize(new Dimension(460, 200));

        JTextArea reply = new JTextArea(3, 40);
        reply.setLineWrap(true);
        reply.setWrapStyleWord(true);
        JPanel form = new JPanel(new BorderLayout(0, 8));
        form.setOpaque(false);
        form.add(threadScroll, BorderLayout.CENTER);
        JPanel replyBox = new JPanel(new BorderLayout(0, 4));
        replyBox.setOpaque(false);
        JLabel lbl = new JLabel("Resposta (deixe vazio para só consultar):");
        lbl.setForeground(UIHelper.TEXT_MUTED);
        replyBox.add(lbl, BorderLayout.NORTH);
        replyBox.add(new JScrollPane(reply), BorderLayout.CENTER);
        form.add(replyBox, BorderLayout.SOUTH);

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Pedido #" + ticket.id(),
                "fas-comments", ticket.subject(), form)
                .setConfirmButton("Enviar", "fas-paper-plane");
        dlg.setSize(540, 420);
        if (dlg.showDialog() && !reply.getText().trim().isEmpty()) {
            String message = reply.getText().trim();
            UIHelper.runWithProgress(this, "A enviar resposta…",
                    () -> platformApiClient.addSuperAdminReply(ticket.id(), message), ignored -> loadTickets(),
                    error -> feedbackHandler.showError("resposta de assistência", error, this::loadTickets));
        }
    }

    private void changeTicketStatus() {
        SupportTicketDTO ticket = selectedTicket();
        if (ticket == null) return;
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"});
        UIHelper.styleComboBox(statusCombo);
        statusCombo.setSelectedItem(ticket.status());
        JPanel form = UIHelper.createDialogForm("Estado:", statusCombo);
        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Mudar Estado",
                "fas-tasks", "Pedido #" + ticket.id(), form).setConfirmButton("Guardar", "fas-check");
        dlg.setOnSaveAsync(() -> {
            String status = (String) statusCombo.getSelectedItem();
            return () -> platformApiClient.changeTicketStatus(ticket.id(), status);
        });
        if (dlg.showDialog()) {
            loadTickets();
        }
    }

    public static String renderThread(List<SupportMessageDTO> messages) {
        if (messages.isEmpty()) return "(sem mensagens)";
        StringBuilder sb = new StringBuilder();
        for (SupportMessageDTO m : messages) {
            String who = m.fromSuperAdmin() ? "Suporte" : "Empresa";
            sb.append("[").append(who).append(" · ").append(m.author()).append("]\n");
            sb.append(m.body()).append("\n\n");
        }
        return sb.toString().trim();
    }
}
