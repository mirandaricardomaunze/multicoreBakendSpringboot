package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.support.dto.CreateTicketRequest;
import mz.multicore.erp.modules.support.dto.SupportTicketDTO;

import javax.swing.*;
import java.awt.*;

/**
 * Diálogos modais ergonómicos para abertura de novo pedido de assistência e conversação/resposta.
 */
public final class ConfigSupportTicketDialog {
    private static final String[] TICKET_PRIORITIES = {"LOW", "NORMAL", "HIGH", "URGENT"};

    private ConfigSupportTicketDialog() {}

    public static void openNewTicket(ConfigPanel owner, Runnable onRefresh) {
        JTextField subjectField = new JTextField();
        JComboBox<String> priorityCombo = new JComboBox<>(TICKET_PRIORITIES);
        priorityCombo.setSelectedItem("NORMAL");
        JTextArea descArea = new JTextArea(4, 30);
        descArea.setLineWrap(true); descArea.setWrapStyleWord(true);
        UIHelper.styleTextField(subjectField);
        UIHelper.styleComboBox(priorityCombo);

        JPanel form = UIHelper.createDialogForm(
                "Assunto:", subjectField, "Prioridade:", priorityCombo, "Descrição:", new JScrollPane(descArea)
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
            ToastManager.success(owner, "Pedido enviado ao suporte.");
            if (onRefresh != null) onRefresh.run();
        }
    }

    public static void viewTicket(ConfigPanel owner, SupportTicketDTO ticket, Runnable onRefresh) {
        UIHelper.loadAsync(owner, () -> owner.supportApiClient.listCompanyMessages(ticket.id()),
                messages -> showSupportThread(owner, ticket, PlataformaPanel.renderThread(messages), onRefresh),
                error -> owner.showConfigError("conversa de suporte", error));
    }

    private static void showSupportThread(ConfigPanel owner, SupportTicketDTO ticket, String renderedThread, Runnable onRefresh) {
        JTextArea thread = new JTextArea(renderedThread);
        thread.setEditable(false); thread.setLineWrap(true); thread.setWrapStyleWord(true);
        JScrollPane threadScroll = new JScrollPane(thread);
        threadScroll.setMinimumSize(new Dimension(420, 180));

        JTextArea reply = new JTextArea(3, 40);
        reply.setLineWrap(true); reply.setWrapStyleWord(true);
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

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow,
                "Pedido #" + ticket.id() + " — " + ticket.subject(),
                "fas-comments", "Histórico e resposta ao suporte", form)
                .setConfirmButton("Enviar Resposta", "fas-paper-plane");
        if (dialog.showDialog() && !reply.getText().trim().isEmpty()) {
            String message = reply.getText().trim();
            UIHelper.runWithProgress(owner, "A enviar resposta…", () -> {
                owner.supportApiClient.addCompanyMessage(ticket.id(), message);
                return null;
            }, ignored -> { if (onRefresh != null) onRefresh.run(); },
            error -> owner.showConfigError("resposta ao suporte", error));
        }
    }
}
