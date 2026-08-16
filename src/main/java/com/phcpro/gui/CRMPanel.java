package com.phcpro.gui;

import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernFormDialog;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.TableFilter;
import com.phcpro.gui.components.TableCellRenderers;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.desktop.client.CRMApiClient;
import com.phcpro.modules.crm.dto.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CRMPanel extends JPanel {

    private final CRMApiClient crmApiClient;

    // Support Tickets Table
    private DefaultTableModel ticketsModel;
    private JTable ticketsTable;

    // WorkSheets Table
    private DefaultTableModel worksheetsModel;
    private JTable worksheetsTable;

    private List<SupportTicketDTO> ticketsList = new ArrayList<>();
    private List<WorkSheetDTO> worksheetsList = new ArrayList<>();

    public CRMPanel(CRMApiClient crmApiClient) {
        this.crmApiClient = crmApiClient;

        setLayout(new BorderLayout(0, 15));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(25, 25, 25, 25));

        add(UIHelper.createHeading("CRM & Assistência"), BorderLayout.NORTH);

        // Cada tabela na sua aba, para ganhar espaço vertical em vez de ficarem apertadas juntas.
        JTabbedPane tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPanePHC(tabbedPane);
        tabbedPane.addTab("Pedidos de Assistência", UIHelper.icon("fas-headset", 16, UIHelper.TEXT_LIGHT),
                createTicketsTab());
        tabbedPane.addTab("Folhas de Obra", UIHelper.icon("fas-tools", 16, UIHelper.TEXT_LIGHT),
                createWorkSheetsTab());
        add(tabbedPane, BorderLayout.CENTER);

        // Carregamento preguiçoso: os dados vêm por HTTP em onPanelSelected() (via navigate), não no
        // construtor — evita uma chamada à API no arranque para quem não tem empresa activa. Mesmo
        // padrão do ClientesPanel/ApprovalsPanel.
    }

    /** Aba dos pedidos de assistência (tabela ocupa toda a aba). */
    private JPanel createTicketsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(15, 0, 0, 0));

        ModernPanel ticketsCard = new ModernPanel(16);
        ticketsCard.setLayout(new BorderLayout());
        ticketsCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] ticketCols = {"ID", "Data", "Cliente", "Assunto", "Descrição", "Estado"};
        ticketsModel = new DefaultTableModel(ticketCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        ticketsTable = new JTable(ticketsModel);
        UIHelper.styleTable(ticketsTable);
        ticketsTable.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.status());
        JScrollPane tScroll = new JScrollPane(ticketsTable);
        UIHelper.styleScrollPane(tScroll);
        JTextField tSearch = TableFilter.searchField("Cliente, assunto ou descrição…");
        JComboBox<String> tEstado = TableFilter.combo("Todos os estados", "OPEN", "RESOLVED");
        JComboBox<String> tPeriodo = TableFilter.periodCombo();
        TableFilter.install(ticketsTable, tSearch,
                java.util.List.of(new TableFilter.ColumnFilter(tEstado, 5)),
                java.util.List.of(new TableFilter.PeriodFilter(tPeriodo, 1)));
        JPanel tBar = TableFilter.bar(tSearch, TableFilter.label("Estado:"), tEstado,
                TableFilter.label("Data:", "fas-calendar-alt"), tPeriodo);
        tBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        ticketsCard.add(tBar, BorderLayout.NORTH);
        ticketsCard.add(tScroll, BorderLayout.CENTER);
        panel.add(ticketsCard, BorderLayout.CENTER);
        return panel;
    }

    /** Aba das folhas de obra (cabeçalho com acções + tabela). */
    private JPanel createWorkSheetsTab() {
        JPanel wsPanel = new JPanel(new BorderLayout(0, 10));
        wsPanel.setOpaque(false);
        wsPanel.setBorder(new EmptyBorder(15, 0, 0, 0));

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
        worksheetsTable.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.money());
        JScrollPane wsScroll = new JScrollPane(worksheetsTable);
        UIHelper.styleScrollPane(wsScroll);
        JTextField wsSearch = TableFilter.searchField("Cliente ou técnico…");
        JComboBox<String> wsFat = TableFilter.combo("Faturado: todos", "SIM", "NÃO");
        TableFilter.install(worksheetsTable, wsSearch, new TableFilter.ColumnFilter(wsFat, 5));
        JPanel wsBar = TableFilter.bar(wsSearch, wsFat);
        wsBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        wsCard.add(wsBar, BorderLayout.NORTH);
        wsCard.add(wsScroll, BorderLayout.CENTER);
        wsPanel.add(wsCard, BorderLayout.CENTER);

        newWsBtn.addActionListener(e -> registerWorkSheet());
        billBtn.addActionListener(e -> billWorkSheet());
        return wsPanel;
    }

    public void refreshData() {
        UIHelper.loadAsync(this,
                () -> new CRMData(crmApiClient.getAllTickets(), crmApiClient.getAllWorkSheets()),
                this::applyData,
                error -> JOptionPane.showMessageDialog(this,
                        "Não foi possível carregar CRM e assistência: " + error.getMessage(),
                        "Erro de ligação", JOptionPane.ERROR_MESSAGE));
    }

    private void applyData(CRMData data) {
        ticketsModel.setRowCount(0);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (SupportTicketDTO ticket : data.tickets()) {
            ticketsModel.addRow(new Object[]{
                    ticket.id(),
                    ticket.createdAt().format(dtf),
                    ticket.clientName(),
                    ticket.subject(),
                    ticket.description(),
                    ticket.status()
            });
        }
        worksheetsModel.setRowCount(0);
        worksheetsList = data.worksheets();
        for (WorkSheetDTO ws : worksheetsList) {
            worksheetsModel.addRow(new Object[]{
                    ws.id(),
                    ws.clientName(),
                    ws.technicianName(),
                    ws.hoursWorked() + " h",
                    ws.totalValue(),
                    ws.isBilled() ? "SIM" : "NÃO"
            });
        }
        ticketsList.clear();
        for (SupportTicketDTO ticket : data.tickets()) {
            if ("OPEN".equalsIgnoreCase(ticket.status())) {
                ticketsList.add(ticket);
            }
        }
    }

    private record CRMData(List<SupportTicketDTO> tickets, List<WorkSheetDTO> worksheets) {}

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
        dlg.setOnSaveAsync(() -> {
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

            CreateWorkSheetRequest request = new CreateWorkSheetRequest(
                    ticket.id(), tech, hours, desc, partsField.getText().trim(), partsCost);
            return () -> {
                crmApiClient.createWorkSheet(request);
                return null;
            };
        });

        if (dlg.showDialog()) {
            JOptionPane.showMessageDialog(this, "Folha de Obra gravada com sucesso!\n"
                    + "O ticket foi marcado como RESOLVIDO.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        }
    }

    private void billWorkSheet() {
        int selectedRow = TableFilter.selectedModelRow(worksheetsTable);
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma folha de obra na tabela para faturar.", "Informação", JOptionPane.WARNING_MESSAGE);
            return;
        }

        WorkSheetDTO ws = worksheetsList.get(selectedRow);
        if (ws.isBilled()) {
            JOptionPane.showMessageDialog(this, "Esta folha de obra já foi faturada.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        UIHelper.runWithProgress(this, "A faturar folha de obra…", () -> {
            crmApiClient.billWorkSheet(ws.id());
            return null;
        }, ignored -> {
            JOptionPane.showMessageDialog(this, "Folha de obra faturada com sucesso!\n" +
                    "Uma fatura comercial foi gerada e submetida para aprovação.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        }, error -> JOptionPane.showMessageDialog(this, "Erro ao faturar: " + error.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE));
    }

    public void onPanelSelected() {
        refreshData();
    }
}
