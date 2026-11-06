package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.QuickPeekPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.crm.dto.SupportTicketDTO;
import mz.multicore.erp.modules.crm.dto.WorkSheetDTO;

import javax.swing.table.DefaultTableModel;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordenador de mapeamento de dados, formatação e Quick Peek do CRM.
 */
public final class CrmViewCoordinator {

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private CrmViewCoordinator() {}

    public static void populateTicketPeek(QuickPeekPanel peek, DefaultTableModel model, int row, Runnable onOpen) {
        if (model == null || row < 0 || row >= model.getRowCount()) return;
        peek.setHeaderIcon("fas-headset", UIHelper.ACCENT_BLUE);
        peek.setTitle(String.valueOf(model.getValueAt(row, 3)));
        peek.setSubtitle("Ticket #" + model.getValueAt(row, 0) + " • " + model.getValueAt(row, 2));
        peek.setStatus(String.valueOf(model.getValueAt(row, 6)), UIHelper.ACCENT_BLUE);

        List<QuickPeekPanel.PeekItem> items = new ArrayList<>();
        items.add(new QuickPeekPanel.PeekItem("Data de Abertura", peek.cell(model.getValueAt(row, 1), 1), false));
        items.add(new QuickPeekPanel.PeekItem("Cliente", peek.cell(model.getValueAt(row, 2), 2), false));
        items.add(new QuickPeekPanel.PeekItem("Prioridade", peek.cell(model.getValueAt(row, 4), 4), true));
        items.add(new QuickPeekPanel.PeekItem("Técnico Responsável", peek.cell(model.getValueAt(row, 5), 5), false));
        peek.setItems(items);
        peek.getOpenFullButton().setText("Abrir Pedido");
        peek.setOnOpenFullAction(v -> { if (onOpen != null) onOpen.run(); });
    }

    public static void populateTicketsModel(DefaultTableModel model, List<SupportTicketDTO> tickets) {
        model.setRowCount(0);
        for (SupportTicketDTO ticket : tickets) {
            model.addRow(new Object[]{
                    ticket.id(),
                    ticket.createdAt().format(DATE_FMT),
                    ticket.clientName(),
                    ticket.subject(),
                    ticket.priorityLabel(),
                    ticket.assignedTechnician() == null ? "—" : ticket.assignedTechnician(),
                    ticket.statusLabel()
            });
        }
    }

    public static void populateWorksheetsModel(DefaultTableModel model, List<WorkSheetDTO> worksheets) {
        model.setRowCount(0);
        for (WorkSheetDTO ws : worksheets) {
            model.addRow(new Object[]{
                    ws.id(),
                    ws.createdAt().format(DATE_FMT),
                    ws.clientName(),
                    ws.technicianName(),
                    ws.hoursWorked() + " h",
                    ws.hourlyRate(),
                    ws.totalValue(),
                    ws.statusLabel()
            });
        }
    }

    public static List<SupportTicketDTO> filterOpenTickets(List<SupportTicketDTO> tickets) {
        List<SupportTicketDTO> open = new ArrayList<>();
        for (SupportTicketDTO ticket : tickets) {
            if ("OPEN".equals(ticket.status()) || "IN_PROGRESS".equals(ticket.status())) {
                open.add(ticket);
            }
        }
        return open;
    }
}
