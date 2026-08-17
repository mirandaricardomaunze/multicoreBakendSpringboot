package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.printing.PdfFileSaver;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.Dimension;

final class CustomerOrderFulfillmentActions {
    private CustomerOrderFulfillmentActions() {}

    static void printSelected(ComercialPanel owner, ComercialApiClient api) {
        int row = TableFilter.selectedModelRow(owner.ordersTable);
        if (row < 0) { warn(owner); return; }
        Long id = (Long) owner.ordersTableModel.getValueAt(row, 0);
        String status = String.valueOf(owner.ordersTableModel.getValueAt(row, 3));
        if ("AWAITING_SEPARATION".equals(status)) {
            UIHelper.runWithProgress(owner, "A gerar guia térmica de separação…",
                    () -> api.printPicking(id, terminalName()),
                    pdf -> { PdfFileSaver.saveAndOpen(pdf, "separacao-" + id); owner.loadOrdersTable(); },
                    error -> showError(owner, "imprimir guia de separação", error));
        } else if ("IN_SEPARATION".equals(status)) reprint(owner, api, id);
        else UIHelper.runWithProgress(owner, "A gerar encomenda em PDF…", () -> api.renderOrder(id),
                pdf -> PdfFileSaver.saveAndOpen(pdf, "encomenda-" + id),
                error -> showError(owner, "imprimir encomenda", error));
    }

    static void completeSeparation(ComercialPanel owner, ComercialApiClient api) {
        int row = TableFilter.selectedModelRow(owner.ordersTable);
        if (row < 0) { warn(owner); return; }
        Long id = (Long) owner.ordersTableModel.getValueAt(row, 0);
        String status = String.valueOf(owner.ordersTableModel.getValueAt(row, 3));

        // O estado já está na tabela: explicar aqui evita mandar o operador a um erro do
        // servidor para descobrir que lhe falta um passo. A regra continua a ser do backend —
        // isto é só cortesia, não é a guarda.
        String impedimento = whyCannotSeparate(status);
        if (impedimento != null) {
            JOptionPane.showMessageDialog(owner, impedimento, "Separação", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        UIHelper.runWithProgress(owner, "A concluir separação…", () -> api.completeSeparation(id, terminalName()),
                ignored -> { owner.loadOrdersTable(); JOptionPane.showMessageDialog(owner, "Pedido marcado como separado."); },
                error -> showError(owner, "concluir separação", error));
    }

    /**
     * Porque é que esta encomenda não pode ser marcada como separada — ou {@code null} se puder.
     *
     * <p>Diz sempre o passo seguinte. "Estado inválido" é verdade e não ajuda ninguém que esteja
     * ao balcão com um cliente à espera.
     */
    private static String whyCannotSeparate(String status) {
        return switch (status) {
            case "IN_SEPARATION" -> null;
            case "AWAITING_SEPARATION" -> "Esta encomenda ainda não entrou em separação.\n\n"
                    + "Use primeiro \"Imprimir PDF\" para emitir a lista de separação — é isso que "
                    + "dá início ao trabalho no armazém. Depois de separada, volte aqui.";
            case "SEPARATED" -> "Esta encomenda já está separada.\n\nO passo seguinte é "
                    + "\"Faturar Encomenda\".";
            case "INVOICED" -> "Esta encomenda já foi facturada.";
            case "CANCELLED" -> "Esta encomenda foi cancelada.";
            default -> "Esta encomenda não faz parte do circuito de separação — está em \""
                    + status + "\" e fatura-se directamente.\n\n"
                    + "A separação aplica-se apenas às encomendas criadas para expedição.";
        };
    }

    static void showEvents(ComercialPanel owner, ComercialApiClient api) {
        int row = TableFilter.selectedModelRow(owner.ordersTable);
        if (row < 0) { warn(owner); return; }
        Long id = (Long) owner.ordersTableModel.getValueAt(row, 0);
        UIHelper.runWithProgress(owner, "A carregar histórico operacional…", () -> api.getOrderEvents(id), events -> {
            DefaultTableModel model = new DefaultTableModel(new Object[]{"Data", "Evento", "Estado anterior",
                    "Novo estado", "Utilizador", "Perfil", "Terminal", "Detalhes"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            events.forEach(e -> model.addRow(new Object[]{e.occurredAt(), e.eventType(), e.previousStatus(),
                    e.newStatus(), e.actor(), e.actorRole(), e.terminalName(), e.details()}));
            JTable table = new JTable(model); UIHelper.styleTable(table);
            JScrollPane scroll = new JScrollPane(table); UIHelper.styleScrollPane(scroll);
            scroll.setPreferredSize(new Dimension(1000, 420));
            JOptionPane.showMessageDialog(owner, scroll, "Histórico operacional do pedido", JOptionPane.PLAIN_MESSAGE);
        }, error -> showError(owner, "carregar histórico operacional", error));
    }

    private static void reprint(ComercialPanel owner, ComercialApiClient api, Long id) {
        JTextField user = new JTextField(); JPasswordField password = new JPasswordField(); JTextField reason = new JTextField();
        Object[] fields = {"Outro gerente/administrador:", user, "Senha:", password, "Motivo:", reason};
        if (JOptionPane.showConfirmDialog(owner, fields, "Autorizar reimpressão", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        String secret = new String(password.getPassword());
        UIHelper.runWithProgress(owner, "A autorizar reimpressão…",
                () -> api.reprintPicking(id, user.getText(), secret, reason.getText(), terminalName()),
                pdf -> { PdfFileSaver.saveAndOpen(pdf, "reimpressao-separacao-" + id); owner.loadOrdersTable(); },
                error -> showError(owner, "reimprimir guia", error));
    }

    static String terminalName() {
        String name = System.getenv("COMPUTERNAME");
        return name == null || name.isBlank() ? "POSTO-DESKTOP" : name;
    }

    private static void warn(ComercialPanel owner) {
        JOptionPane.showMessageDialog(owner, "Seleccione um pedido.", "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    private static void showError(ComercialPanel owner, String action, Throwable error) {
        JOptionPane.showMessageDialog(owner, "Não foi possível " + action + ": " + error.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
