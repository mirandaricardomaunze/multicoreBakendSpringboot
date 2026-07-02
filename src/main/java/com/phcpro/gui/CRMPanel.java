package com.phcpro.gui;

import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernFormDialog;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.crm.dto.*;
import com.phcpro.modules.crm.service.CRMService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CRMPanel extends JPanel {

    private final CRMService crmService;

    // Support Tickets Table
    private DefaultTableModel ticketsModel;
    private JTable ticketsTable;

    // WorkSheets Table
    private DefaultTableModel worksheetsModel;
    private JTable worksheetsTable;

    private List<SupportTicketDTO> ticketsList = new ArrayList<>();
    private List<WorkSheetDTO> worksheetsList = new ArrayList<>();

    public CRMPanel(CRMService crmService) {
        this.crmService = crmService;

        setLayout(new BorderLayout(0, 15));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(25, 25, 25, 25));

        // TOP HALF: SUPPORT TICKETS LIST
        JPanel topPanel = new JPanel(new BorderLayout(0, 10));
        topPanel.setOpaque(false);
        topPanel.add(UIHelper.createHeading("CRM & Pedidos de Assistência"), BorderLayout.NORTH);

        ModernPanel ticketsCard = new ModernPanel(16);
        ticketsCard.setLayout(new BorderLayout());
        ticketsCard.setBorder(new EmptyBorder(15, 15, 15, 15));
        ticketsCard.setPreferredSize(new Dimension(800, 200));

        String[] ticketCols = {"ID", "Data", "Cliente", "Assunto", "Descrição", "Estado"};
        ticketsModel = new DefaultTableModel(ticketCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        ticketsTable = new JTable(ticketsModel);
        UIHelper.styleTable(ticketsTable);
        JScrollPane tScroll = new JScrollPane(ticketsTable);
        UIHelper.styleScrollPane(tScroll);
        ticketsCard.add(tScroll, BorderLayout.CENTER);
        topPanel.add(ticketsCard, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // BOTTOM HALF: FOLHAS DE OBRA (a largura total) — registo via modal.
        JPanel wsPanel = new JPanel(new BorderLayout(0, 10));
        wsPanel.setOpaque(false);

        JPanel wsHeader = new JPanel(new BorderLayout());
        wsHeader.setOpaque(false);
        wsHeader.add(UIHelper.createSubheading("Folhas de Obra Registadas"), BorderLayout.WEST);
        ModernButton newWsBtn = UIHelper.createSuccessButton("Registar Folha de Obra");
        newWsBtn.setIcon(UIHelper.icon("fas-tools", 14));
        ModernButton billBtn = UIHelper.createPrimaryButton("Faturar Folha de Obra");
        billBtn.setIcon(UIHelper.icon("fas-file-invoice-dollar", 14));
        JPanel wsActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        wsActions.setOpaque(false);
        wsActions.add(newWsBtn);
        wsActions.add(billBtn);
        wsHeader.add(wsActions, BorderLayout.EAST);
        wsPanel.add(wsHeader, BorderLayout.NORTH);

        ModernPanel wsCard = new ModernPanel(16);
        wsCard.setLayout(new BorderLayout());
        wsCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] wsCols = {"ID", "Cliente", "Técnico", "Horas", "Total Valor", "Faturado"};
        worksheetsModel = new DefaultTableModel(wsCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        worksheetsTable = new JTable(worksheetsModel);
        UIHelper.styleTable(worksheetsTable);
        JScrollPane wsScroll = new JScrollPane(worksheetsTable);
        UIHelper.styleScrollPane(wsScroll);
        wsCard.add(wsScroll, BorderLayout.CENTER);
        wsPanel.add(wsCard, BorderLayout.CENTER);
        add(wsPanel, BorderLayout.CENTER);

        // Action Listeners
        newWsBtn.addActionListener(e -> registerWorkSheet());
        billBtn.addActionListener(e -> billWorkSheet());

        refreshData();
    }

    public void refreshData() {
        loadTicketsTable();
        loadWorkSheetsTable();
        loadOpenTickets();
    }

    private void loadTicketsTable() {
        ticketsModel.setRowCount(0);
        List<SupportTicketDTO> tickets = crmService.getAllTickets();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (SupportTicketDTO ticket : tickets) {
            ticketsModel.addRow(new Object[]{
                    ticket.id(),
                    ticket.createdAt().format(dtf),
                    ticket.clientName(),
                    ticket.subject(),
                    ticket.description(),
                    ticket.status()
            });
        }
    }

    private void loadWorkSheetsTable() {
        worksheetsModel.setRowCount(0);
        worksheetsList = crmService.getAllWorkSheets();
        for (WorkSheetDTO ws : worksheetsList) {
            worksheetsModel.addRow(new Object[]{
                    ws.id(),
                    ws.clientName(),
                    ws.technicianName(),
                    ws.hoursWorked() + " h",
                    ws.totalValue() + " MT",
                    ws.isBilled() ? "SIM" : "NÃO"
            });
        }
    }

    private void loadOpenTickets() {
        ticketsList.clear();
        for (SupportTicketDTO ticket : crmService.getAllTickets()) {
            if ("OPEN".equalsIgnoreCase(ticket.status())) {
                ticketsList.add(ticket);
            }
        }
    }

    /** Registo de folha de obra em modal profissional (fecho de ticket). */
    private void registerWorkSheet() {
        if (ticketsList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Não existem tickets em aberto para registar trabalho.", "Informação", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<String> ticketCombo = new JComboBox<>();
        UIHelper.styleComboBox(ticketCombo);
        for (SupportTicketDTO t : ticketsList) {
            ticketCombo.addItem("#" + t.id() + " - " + t.clientName() + ": " + t.subject());
        }
        JTextField technicianField = new JTextField();
        JTextField hoursField = new JTextField("1.0");
        JTextField descField = new JTextField();
        JTextField partsField = new JTextField();
        JTextField partsCostField = new JTextField("0.00");

        JPanel form = UIHelper.createDialogForm(
                "Ticket Associado:", ticketCombo,
                "Técnico:", technicianField,
                "Horas Executadas:", hoursField,
                "Descrição do Serviço:", descField,
                "Peças Substituídas:", partsField,
                "Custo Peças (MT):", partsCostField
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Registar Folha de Obra",
                "fas-tools", "Fecho de ticket de assistência", form).setConfirmButton("Gravar", "fas-save");
        dlg.setOnSave(() -> {
            SupportTicketDTO ticket = ticketsList.get(Math.max(0, ticketCombo.getSelectedIndex()));
            String tech = technicianField.getText().trim();
            if (tech.isEmpty()) throw new IllegalArgumentException("O nome do técnico é obrigatório.");

            BigDecimal hours;
            try {
                hours = new BigDecimal(hoursField.getText().trim());
                if (hours.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("As horas devem ser um número positivo.");
            }

            String desc = descField.getText().trim();
            if (desc.isEmpty()) throw new IllegalArgumentException("A descrição do serviço é obrigatória.");

            BigDecimal partsCost = BigDecimal.ZERO;
            String partsCostStr = partsCostField.getText().trim();
            if (!partsCostStr.isEmpty()) {
                try {
                    partsCost = new BigDecimal(partsCostStr);
                    if (partsCost.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("O custo das peças não pode ser negativo.");
                }
            }

            crmService.createWorkSheet(new CreateWorkSheetRequest(
                    ticket.id(), tech, hours, desc, partsField.getText().trim(), partsCost));
        });

        if (dlg.showDialog()) {
            JOptionPane.showMessageDialog(this, "Folha de Obra gravada com sucesso!\n"
                    + "O ticket foi marcado como RESOLVIDO.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        }
    }

    private void billWorkSheet() {
        int selectedRow = worksheetsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma folha de obra na tabela para faturar.", "Informação", JOptionPane.WARNING_MESSAGE);
            return;
        }

        WorkSheetDTO ws = worksheetsList.get(selectedRow);
        if (ws.isBilled()) {
            JOptionPane.showMessageDialog(this, "Esta folha de obra já foi faturada.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            crmService.billWorkSheet(ws.id());
            JOptionPane.showMessageDialog(this, "Folha de obra faturada com sucesso!\n" +
                    "Uma fatura comercial foi gerada e submetida para aprovação.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao faturar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void onPanelSelected() {
        refreshData();
    }
}
