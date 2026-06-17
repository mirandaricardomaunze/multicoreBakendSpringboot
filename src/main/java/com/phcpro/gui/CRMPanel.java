package com.phcpro.gui;

import com.phcpro.gui.components.ModernButton;
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

    // Form items
    private JComboBox<String> ticketCombo;
    private JTextField technicianField;
    private JTextField hoursField;
    private JTextField descField;
    private JTextField partsField;
    private JTextField partsCostField;

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

        // BOTTOM HALF: WORKSHEETS LIST (LEFT) & ADD WORKSHEET FORM (RIGHT)
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        bottomPanel.setOpaque(false);

        // Bottom Left: Worksheets
        JPanel wsPanel = new JPanel(new BorderLayout(0, 10));
        wsPanel.setOpaque(false);
        wsPanel.add(UIHelper.createSubheading("Folhas de Obra Registadas"), BorderLayout.NORTH);

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

        JPanel wsBtnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        wsBtnBar.setOpaque(false);
        ModernButton billBtn = UIHelper.createPrimaryButton("Faturar Folha de Obra");
        billBtn.setIcon(UIHelper.icon("fas-file-invoice-dollar", 14));
        wsBtnBar.add(billBtn);
        wsCard.add(wsBtnBar, BorderLayout.SOUTH);
        wsPanel.add(wsCard, BorderLayout.CENTER);
        bottomPanel.add(wsPanel);

        // Bottom Right: Add Worksheet Form
        JPanel addWsPanel = new JPanel(new BorderLayout(0, 10));
        addWsPanel.setOpaque(false);
        addWsPanel.add(UIHelper.createSubheading("Registar Folha de Obra (Fecho de Ticket)"), BorderLayout.NORTH);

        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setOpaque(false);
        formCard.setBorder(new EmptyBorder(4, 4, 4, 4));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Row 0: Ticket Selector
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(8, 4, 2, 4);
        JLabel ticketLbl = new JLabel("Ticket Associado:");
        ticketLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(ticketLbl, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(2, 4, 12, 4);
        ticketCombo = new JComboBox<>();
        UIHelper.styleComboBox(ticketCombo);
        formCard.add(ticketCombo, gbc);

        // Row 1: Tech name and Hours
        gbc.gridy = 2; gbc.gridwidth = 1; gbc.weightx = 0.6;
        gbc.insets = new Insets(8, 4, 2, 4);
        JLabel techLbl = new JLabel("Técnico:");
        techLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(techLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.4;
        gbc.insets = new Insets(8, 4, 2, 4);
        JLabel hoursLbl = new JLabel("Horas Executadas:");
        hoursLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(hoursLbl, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.6;
        gbc.insets = new Insets(2, 4, 12, 4);
        technicianField = new JTextField();
        UIHelper.styleTextField(technicianField);
        formCard.add(technicianField, gbc);

        gbc.gridx = 1; gbc.weightx = 0.4;
        gbc.insets = new Insets(2, 4, 12, 4);
        hoursField = new JTextField("1.0");
        UIHelper.styleTextField(hoursField);
        formCard.add(hoursField, gbc);

        // Row 2: Description
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.insets = new Insets(8, 4, 2, 4);
        JLabel descLbl = new JLabel("Descrição do Serviço:");
        descLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(descLbl, gbc);

        gbc.gridy = 5;
        gbc.insets = new Insets(2, 4, 12, 4);
        descField = new JTextField();
        UIHelper.styleTextField(descField);
        formCard.add(descField, gbc);

        // Row 3: Parts and Parts Cost
        gbc.gridy = 6; gbc.gridwidth = 1; gbc.weightx = 0.6;
        gbc.insets = new Insets(8, 4, 2, 4);
        JLabel partsLbl = new JLabel("Peças / Peças Substituídas:");
        partsLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(partsLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.4;
        gbc.insets = new Insets(8, 4, 2, 4);
        JLabel costLbl = new JLabel("Custo Peças (MT):");
        costLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(costLbl, gbc);

        gbc.gridx = 0; gbc.gridy = 7; gbc.weightx = 0.6;
        gbc.insets = new Insets(2, 4, 12, 4);
        partsField = new JTextField();
        UIHelper.styleTextField(partsField);
        formCard.add(partsField, gbc);

        gbc.gridx = 1; gbc.weightx = 0.4;
        gbc.insets = new Insets(2, 4, 12, 4);
        partsCostField = new JTextField("0.00");
        UIHelper.styleTextField(partsCostField);
        formCard.add(partsCostField, gbc);

        // Row 4: Submit Button
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.insets = new Insets(20, 4, 4, 4);
        ModernButton submitBtn = UIHelper.createSuccessButton("Gravar Folha de Obra");
        submitBtn.setIcon(UIHelper.icon("fas-save", 14));
        formCard.add(submitBtn, gbc);

        // Wrap form in a scroll pane so nothing gets clipped on small screens
        JScrollPane formScroll = new JScrollPane(formCard);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.setOpaque(false);
        formScroll.getViewport().setOpaque(false);
        formScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);

        ModernPanel formCardWrapper = new ModernPanel(16);
        formCardWrapper.setLayout(new BorderLayout());
        formCardWrapper.setBorder(new EmptyBorder(15, 15, 15, 15));
        formCardWrapper.add(formScroll, BorderLayout.CENTER);

        addWsPanel.add(formCardWrapper, BorderLayout.CENTER);
        bottomPanel.add(addWsPanel);

        add(bottomPanel, BorderLayout.CENTER);

        // Action Listeners
        submitBtn.addActionListener(e -> registerWorkSheet());
        billBtn.addActionListener(e -> billWorkSheet());

        refreshData();
    }

    public void refreshData() {
        loadTicketsTable();
        loadWorkSheetsTable();
        loadTicketCombo();
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

    private void loadTicketCombo() {
        ticketCombo.removeAllItems();
        ticketsList.clear();

        List<SupportTicketDTO> allTickets = crmService.getAllTickets();
        for (SupportTicketDTO ticket : allTickets) {
            if ("OPEN".equalsIgnoreCase(ticket.status())) {
                ticketsList.add(ticket);
                ticketCombo.addItem("#" + ticket.id() + " - " + ticket.clientName() + ": " + ticket.subject());
            }
        }
    }

    private void registerWorkSheet() {
        if (ticketsList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Não existem tickets em aberto para registar trabalho.", "Informação", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int selectedIdx = ticketCombo.getSelectedIndex();
        if (selectedIdx < 0) return;

        SupportTicketDTO ticket = ticketsList.get(selectedIdx);

        String tech = technicianField.getText().trim();
        if (tech.isEmpty()) {
            JOptionPane.showMessageDialog(this, "O nome do técnico é obrigatório.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BigDecimal hours;
        try {
            hours = new BigDecimal(hoursField.getText().trim());
            if (hours.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "As horas devem ser um número positivo.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String desc = descField.getText().trim();
        if (desc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "A descrição do serviço efetuado é obrigatória.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BigDecimal partsCost = BigDecimal.ZERO;
        try {
            String partsCostStr = partsCostField.getText().trim();
            if (!partsCostStr.isEmpty()) {
                partsCost = new BigDecimal(partsCostStr);
                if (partsCost.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "O custo das peças não pode ser negativo.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String parts = partsField.getText().trim();

        try {
            CreateWorkSheetRequest req = new CreateWorkSheetRequest(ticket.id(), tech, hours, desc, parts, partsCost);
            crmService.createWorkSheet(req);

            JOptionPane.showMessageDialog(this, "Folha de Obra gravada com sucesso!\n" +
                    "O ticket #" + ticket.id() + " foi marcado como RESOLVIDO.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);

            // Clean Form
            technicianField.setText("");
            hoursField.setText("1.0");
            descField.setText("");
            partsField.setText("");
            partsCostField.setText("0.00");

            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao gravar folha de obra: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
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
