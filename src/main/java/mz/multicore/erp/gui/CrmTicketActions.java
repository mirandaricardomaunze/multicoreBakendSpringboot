package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.CRMApiClient;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.crm.dto.ChangeTicketStatusRequest;
import mz.multicore.erp.modules.crm.dto.CreateTicketRequest;
import mz.multicore.erp.modules.crm.dto.SupportTicketDTO;
import mz.multicore.erp.modules.crm.dto.UpdateTicketRequest;

import javax.swing.*;
import java.util.List;

/**
 * Acções sobre pedidos de assistência: abrir, atribuir, assumir, resolver, anular e reabrir.
 *
 * <p>Vive fora do {@link CRMPanel} pela mesma razão que o {@code StockTransferActions} — o painel
 * trata do layout, os casos de uso tratam-se aqui.
 */
final class CrmTicketActions {

    /** Rótulos PT-MZ ↔ nomes do enum {@code TicketPriority} do backend. */
    private static final String[] PRIORITY_LABELS = {"Baixa", "Normal", "Alta", "Urgente"};
    private static final String[] PRIORITY_NAMES = {"LOW", "NORMAL", "HIGH", "URGENT"};

    private final CRMPanel owner;
    private final CRMApiClient crmApiClient;
    private final ComercialApiClient comercialApiClient;

    CrmTicketActions(CRMPanel owner, CRMApiClient crmApiClient, ComercialApiClient comercialApiClient) {
        this.owner = owner;
        this.crmApiClient = crmApiClient;
        this.comercialApiClient = comercialApiClient;
    }

    /**
     * Abre um pedido de assistência. O endpoint existia desde sempre; o que faltava era isto — não
     * havia como registar um pedido a partir da aplicação, só os que vinham do povoamento.
     */
    void createTicket() {
        UIHelper.runWithProgress(owner, "A carregar clientes…",
                comercialApiClient::getClients,
                this::showNewTicketDialog,
                error -> JOptionPane.showMessageDialog(owner,
                        "Não foi possível carregar os clientes: " + error.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE));
    }

    private void showNewTicketDialog(List<ClientDTO> clients) {
        if (clients == null || clients.isEmpty()) {
            JOptionPane.showMessageDialog(owner,
                    "Não há clientes registados. Registe o cliente antes de abrir o pedido.",
                    "Informação", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<String> clientCombo = new JComboBox<>();
        UIHelper.styleComboBox(clientCombo);
        for (ClientDTO c : clients) {
            clientCombo.addItem(c.name() + (c.taxId() == null || c.taxId().isBlank() ? "" : " — " + c.taxId()));
        }

        JComboBox<String> priorityCombo = new JComboBox<>(PRIORITY_LABELS);
        UIHelper.styleComboBox(priorityCombo);
        priorityCombo.setSelectedItem("Normal");

        JTextField subjectField = new JTextField();
        JTextField technicianField = new JTextField();
        JTextArea descArea = new JTextArea(5, 20);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        UIHelper.styleTextArea(descArea);
        JScrollPane descScroll = new JScrollPane(descArea);
        UIHelper.styleScrollPane(descScroll);

        JPanel form = UIHelper.createDialogForm(
                "Cliente:", clientCombo,
                "Prioridade:", priorityCombo,
                "Assunto:", subjectField,
                "Técnico responsável (opcional):", technicianField,
                "Descrição da avaria / pedido:", descScroll);

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Novo Pedido de Assistência",
                "fas-headset", "Registo de um pedido do cliente", form)
                .setConfirmButton("Abrir Pedido", "fas-save");
        dlg.setOnSaveAsync(() -> {
            ClientDTO client = clients.get(Math.max(0, clientCombo.getSelectedIndex()));
            String subject = subjectField.getText().trim();
            if (subject.isEmpty()) throw new IllegalArgumentException("O assunto é obrigatório.");
            String description = descArea.getText().trim();
            if (description.isEmpty()) throw new IllegalArgumentException("A descrição é obrigatória.");

            CreateTicketRequest request = new CreateTicketRequest(
                    client.id(), subject, description,
                    priorityName(priorityCombo), technicianField.getText().trim());
            return () -> crmApiClient.createTicket(request);
        });

        if (dlg.showDialog()) {
            JOptionPane.showMessageDialog(owner, "Pedido de assistência aberto.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            owner.refreshData();
        }
    }

    void openSelectedTicket() {
        int row = TableFilter.selectedModelRow(owner.ticketsTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(owner, "Selecione um pedido na tabela.",
                    "Informação", JOptionPane.WARNING_MESSAGE);
            return;
        }
        showTicketDialog(owner.ticketsList.get(row));
    }

    /**
     * Ficha do pedido: o que se pode alterar (prioridade, técnico) fica editável; o ciclo de vida
     * fica nos botões do rodapé, porque cada transição tem regras próprias no servidor.
     */
    private void showTicketDialog(SupportTicketDTO ticket) {
        JComboBox<String> priorityCombo = new JComboBox<>(PRIORITY_LABELS);
        UIHelper.styleComboBox(priorityCombo);
        priorityCombo.setSelectedItem(ticket.priorityLabel());

        JTextField technicianField = new JTextField(
                ticket.assignedTechnician() == null ? "" : ticket.assignedTechnician());

        JTextArea descArea = new JTextArea(ticket.description(), 5, 20);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        UIHelper.styleTextArea(descArea);
        UIHelper.setReadOnly(descArea, true);
        JScrollPane descScroll = new JScrollPane(descArea);
        UIHelper.styleScrollPane(descScroll);

        JPanel form = UIHelper.createDialogForm(
                "Cliente:", readOnly(ticket.clientName()),
                "Aberto em:", readOnly(ticket.createdAt().format(CRMPanel.DATE_FMT)),
                "Assunto:", readOnly(ticket.subject()),
                "Estado:", readOnly(ticket.statusLabel()),
                "Prioridade:", priorityCombo,
                "Técnico responsável:", technicianField,
                "Fechado em:", readOnly(ticket.resolvedAt() == null
                        ? "—" : ticket.resolvedAt().format(CRMPanel.DATE_FMT)),
                "Nota de fecho:", readOnly(ticket.closingNote() == null ? "—" : ticket.closingNote()),
                "Descrição:", descScroll);

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow,
                "Pedido #" + ticket.id(), "fas-headset", ticket.subject(), form)
                .setConfirmButton("Gravar Atribuição", "fas-save");

        boolean terminal = "RESOLVED".equals(ticket.status()) || "CANCELLED".equals(ticket.status());
        if ("OPEN".equals(ticket.status())) {
            dlg.addActionButton(statusButton(dlg, ticket, "Assumir", "fas-user-check",
                    "IN_PROGRESS", false, UIHelper.createSecondaryButton("Assumir")));
        }
        if (!terminal) {
            dlg.addActionButton(statusButton(dlg, ticket, "Resolver", "fas-check",
                    "RESOLVED", false, UIHelper.createSuccessButton("Resolver")));
            dlg.addActionButton(statusButton(dlg, ticket, "Anular", "fas-ban",
                    "CANCELLED", true, UIHelper.createDangerButton("Anular")));
        } else {
            dlg.addActionButton(statusButton(dlg, ticket, "Reabrir", "fas-undo",
                    "OPEN", false, UIHelper.createSecondaryButton("Reabrir")));
        }

        dlg.setOnSaveAsync(() -> {
            UpdateTicketRequest request = new UpdateTicketRequest(
                    priorityName(priorityCombo), technicianField.getText().trim());
            return () -> crmApiClient.updateTicket(ticket.id(), request);
        });

        if (dlg.showDialog()) {
            owner.refreshData();
        }
    }

    /**
     * Botão de transição de estado. {@code needsNote} pede o motivo antes de disparar — anular sem
     * motivo é recusado pelo servidor, e é melhor perguntar do que mostrar o erro depois.
     */
    private ModernButton statusButton(ModernFormDialog dlg, SupportTicketDTO ticket, String label,
                                      String icon, String targetStatus, boolean needsNote,
                                      ModernButton button) {
        button.setIcon(UIHelper.icon(icon, 14));
        button.addActionListener(e -> {
            String note = null;
            if (needsNote) {
                note = JOptionPane.showInputDialog(UIHelper.mainWindow,
                        "Motivo da anulação do pedido #" + ticket.id() + ":", label,
                        JOptionPane.QUESTION_MESSAGE);
                if (note == null) return;                       // cancelou
                if (note.isBlank()) {
                    JOptionPane.showMessageDialog(UIHelper.mainWindow,
                            "É obrigatório indicar o motivo da anulação.", "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else if ("RESOLVED".equals(targetStatus)) {
                note = JOptionPane.showInputDialog(UIHelper.mainWindow,
                        "Nota de fecho (opcional):", label, JOptionPane.QUESTION_MESSAGE);
                if (note == null) return;                       // cancelou
            }

            ChangeTicketStatusRequest request = new ChangeTicketStatusRequest(targetStatus, note);
            UIHelper.runWithProgress(owner, "A actualizar o pedido…",
                    () -> crmApiClient.changeTicketStatus(ticket.id(), request),
                    updated -> {
                        dlg.close();
                        JOptionPane.showMessageDialog(owner,
                                "Pedido #" + ticket.id() + " ficou em '" + updated.statusLabel() + "'.",
                                "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                        owner.refreshData();
                    },
                    error -> JOptionPane.showMessageDialog(owner, error.getMessage(),
                            "Erro", JOptionPane.ERROR_MESSAGE));
        });
        return button;
    }

    private String priorityName(JComboBox<String> combo) {
        int index = Math.max(0, combo.getSelectedIndex());
        return PRIORITY_NAMES[index];
    }

    private static JTextField readOnly(String value) {
        JTextField f = new JTextField(value == null ? "" : value);
        UIHelper.styleTextField(f);
        UIHelper.setReadOnly(f, true);
        return f;
    }
}
