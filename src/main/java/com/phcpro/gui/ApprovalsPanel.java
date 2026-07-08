package com.phcpro.gui;

import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernFormDialog;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.TableFilter;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.approvals.dto.ApprovalRequestDTO;
import com.phcpro.modules.approvals.model.ApprovalStatus;
import com.phcpro.modules.approvals.service.ApprovalService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ApprovalsPanel extends JPanel {

    private final ApprovalService approvalService;

    // GUI Tables
    private DefaultTableModel pendingModel;
    private JTable pendingTable;

    private DefaultTableModel historyModel;
    private JTable historyTable;

    // Toolbar
    private ModernButton openBtn;

    // Data lists
    private List<ApprovalRequestDTO> pendingList = new ArrayList<>();
    private List<ApprovalRequestDTO> historyList = new ArrayList<>();

    public ApprovalsPanel(ApprovalService approvalService) {
        this.approvalService = approvalService;

        setLayout(new BorderLayout(0, 20));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(25, 25, 25, 25));

        // HEADER
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(UIHelper.createHeading("Fila de Aprovações de Documentos"), BorderLayout.NORTH);
        
        JLabel sub = new JLabel("Validação e controlo administrativo de limites de compras e vendas.");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(UIHelper.TEXT_MUTED);
        headerPanel.add(sub, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // Cada tabela na sua aba, para ganhar espaço vertical em vez de ficarem apertadas juntas.
        JTabbedPane tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPane(tabbedPane);
        tabbedPane.addTab("Pendentes", UIHelper.icon("fas-hourglass-half", 16, UIHelper.TEXT_LIGHT),
                createPendingTab());
        tabbedPane.addTab("Histórico", UIHelper.icon("fas-clipboard-check", 16, UIHelper.TEXT_LIGHT),
                createHistoryTab());
        add(tabbedPane, BorderLayout.CENTER);

        // LISTENERS
        pendingTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                openBtn.setEnabled(pendingTable.getSelectedRow() >= 0);
            }
        });
        pendingTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent ev) {
                if (ev.getClickCount() == 2) openDecisionForSelected();
            }
        });

        refreshData();
    }

    public void refreshData() {
        loadPendingTable();
        loadHistoryTable();
        if (openBtn != null) {
            openBtn.setEnabled(false);
            pendingTable.clearSelection();
        }
    }

    /** Aba dos pedidos pendentes (cabeçalho com acção + tabela a ocupar toda a aba). */
    private JPanel createPendingTab() {
        JPanel pendingPanel = new JPanel(new BorderLayout(0, 10));
        pendingPanel.setOpaque(false);
        pendingPanel.setBorder(new EmptyBorder(15, 0, 0, 0));

        JPanel pendHeader = new JPanel(new BorderLayout());
        pendHeader.setOpaque(false);
        pendHeader.add(UIHelper.createSubheading("Pedidos a Aguardar Decisão"), BorderLayout.WEST);
        openBtn = UIHelper.createPrimaryButton("Abrir / Decidir");
        openBtn.setIcon(UIHelper.icon("fas-gavel", 14));
        openBtn.setEnabled(false);
        openBtn.addActionListener(e -> openDecisionForSelected());
        JPanel pendActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pendActions.setOpaque(false);
        pendActions.add(openBtn);
        pendHeader.add(pendActions, BorderLayout.EAST);
        pendingPanel.add(pendHeader, BorderLayout.NORTH);

        ModernPanel pendingCard = new ModernPanel(16);
        pendingCard.setLayout(new BorderLayout());
        pendingCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] pendingCols = {"ID", "Documento", "Submissor", "Valor", "Perfil Requerido"};
        pendingModel = new DefaultTableModel(pendingCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        pendingTable = new JTable(pendingModel);
        UIHelper.styleTable(pendingTable);
        // Duplo-clique abre o modal de decisão (não o inspector genérico do styleTable).
        pendingTable.putClientProperty("noRowInspector", Boolean.TRUE);
        JScrollPane pendingScroll = new JScrollPane(pendingTable);
        UIHelper.styleScrollPane(pendingScroll);
        JTextField pSearch = TableFilter.searchField("Documento ou submissor…");
        JComboBox<String> pPerfil = TableFilter.combo("Todos os perfis", "ADMIN", "MANAGER");
        TableFilter.install(pendingTable, pSearch, new TableFilter.ColumnFilter(pPerfil, 4));
        JPanel pBar = TableFilter.bar(pSearch, TableFilter.label("Perfil:"), pPerfil);
        pBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        pendingCard.add(pBar, BorderLayout.NORTH);
        pendingCard.add(pendingScroll, BorderLayout.CENTER);
        pendingPanel.add(pendingCard, BorderLayout.CENTER);
        return pendingPanel;
    }

    /** Aba do histórico e auditoria (tabela ocupa toda a aba). */
    private JPanel createHistoryTab() {
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 10));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(15, 0, 0, 0));
        bottomPanel.add(UIHelper.createSubheading("Histórico e Auditoria de Aprovações"), BorderLayout.NORTH);

        ModernPanel historyCard = new ModernPanel(16);
        historyCard.setLayout(new BorderLayout());
        historyCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] historyCols = {"Data", "Tipo Doc", "Submissor", "Valor", "Estado Final", "Motivo / Comentário"};
        historyModel = new DefaultTableModel(historyCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(historyModel);
        UIHelper.styleTable(historyTable);
        JScrollPane historyScroll = new JScrollPane(historyTable);
        UIHelper.styleScrollPane(historyScroll);
        JTextField hSearch = TableFilter.searchField("Documento ou submissor…");
        JComboBox<String> hEstado = TableFilter.combo("Todos os estados", "APPROVED", "REJECTED");
        JComboBox<String> hPeriodo = TableFilter.periodCombo();
        TableFilter.install(historyTable, hSearch,
                java.util.List.of(new TableFilter.ColumnFilter(hEstado, 4)),
                java.util.List.of(new TableFilter.PeriodFilter(hPeriodo, 0)));
        JPanel hBar = TableFilter.bar(hSearch, TableFilter.label("Estado final:"), hEstado,
                TableFilter.label("Data:", "fas-calendar-alt"), hPeriodo);
        hBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        historyCard.add(hBar, BorderLayout.NORTH);
        historyCard.add(historyScroll, BorderLayout.CENTER);
        bottomPanel.add(historyCard, BorderLayout.CENTER);
        return bottomPanel;
    }

    /** Tipo de documento em PT para a área de aprovação. Desconhecidos ficam como estão. */
    private static String humanType(String documentType) {
        if (documentType == null) return "";
        return switch (documentType.toUpperCase(java.util.Locale.ROOT)) {
            case "ORDER" -> "Encomenda";
            case "INVOICE" -> "Fatura";
            case "EXPENSE", "EXPENSE_CLAIM" -> "Despesa";
            default -> documentType;
        };
    }

    private void loadPendingTable() {
        pendingModel.setRowCount(0);
        pendingList = approvalService.getPendingRequests();
        for (ApprovalRequestDTO req : pendingList) {
            pendingModel.addRow(new Object[]{
                    req.id(),
                    humanType(req.documentType()) + " #" + req.documentId(),
                    req.submitter(),
                    String.format("%,.2f MT", req.amount()),
                    req.requiredRole()
            });
        }
    }

    private void loadHistoryTable() {
        historyModel.setRowCount(0);
        historyList = approvalService.getAllRequests();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (ApprovalRequestDTO req : historyList) {
            if (req.status() != ApprovalStatus.PENDING) {
                historyModel.addRow(new Object[]{
                        req.createdAt().format(formatter),
                        humanType(req.documentType()) + " #" + req.documentId(),
                        req.submitter(),
                        String.format("%,.2f MT", req.amount()),
                        req.status().name(),
                        req.status() == ApprovalStatus.REJECTED ? req.rejectionReason() : "Validado administrativo"
                });
            }
        }
    }

    private void openDecisionForSelected() {
        int row = TableFilter.selectedModelRow(pendingTable);
        if (row < 0 || row >= pendingList.size()) return;
        openDecisionDialog(pendingList.get(row));
    }

    /**
     * Modal de decisão profissional ({@link ModernFormDialog}): mostra os detalhes do pedido
     * (só-leitura) e oferece Aprovar / Rejeitar / Fechar no rodapé. Substitui o inspector inline.
     */
    private void openDecisionDialog(ApprovalRequestDTO req) {
        JTextArea descA = new JTextArea(req.description() == null ? "" : req.description());
        descA.setEditable(false);
        descA.setLineWrap(true);
        descA.setWrapStyleWord(true);
        UIHelper.styleTextArea(descA);
        JScrollPane descScroll = new JScrollPane(descA);
        descScroll.setPreferredSize(new Dimension(380, 96));
        descScroll.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER, 1));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.gridx = 0;
        g.insets = new Insets(5, 8, 5, 8);
        int y = 0;
        y = addFormRow(form, g, y, "Tipo de Documento:", readOnly(humanType(req.documentType())));
        y = addFormRow(form, g, y, "ID Documento:", readOnly("#" + req.documentId()));
        y = addFormRow(form, g, y, "Solicitante:", readOnly(req.submitter()));
        y = addFormRow(form, g, y, "Valor do Documento:", readOnly(String.format("%,.2f MT", req.amount())));
        y = addFormRow(form, g, y, "Perfil Requerido:", readOnly(req.requiredRole()));
        addFormRow(form, g, y, "Descrição / Justificação:", descScroll);

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Decisão de Aprovação",
                "fas-clipboard-check", humanType(req.documentType()) + " #" + req.documentId(), form);

        ModernButton rejectBtn = UIHelper.createDangerButton("Rejeitar");
        rejectBtn.setIcon(UIHelper.icon("fas-times", 14));
        rejectBtn.addActionListener(e -> {
            String reason = JOptionPane.showInputDialog(this,
                    "Introduza o motivo de rejeição (obrigatório):", "Rejeitar Documento",
                    JOptionPane.WARNING_MESSAGE);
            if (reason == null) return;
            if (reason.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "É obrigatório indicar um motivo para a rejeição.",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                approvalService.rejectRequest(req.id(), reason.trim());
                dlg.close();
                JOptionPane.showMessageDialog(this, "Documento rejeitado com sucesso.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                refreshData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao rejeitar: " + ex.getMessage(), "Erro de Autorização", JOptionPane.ERROR_MESSAGE);
            }
        });
        dlg.addActionButton(rejectBtn);
        dlg.setConfirmButton("Aprovar", "fas-check");
        dlg.setOnSave(() -> approvalService.approveRequest(req.id(), "Aprovado via interface Swing."));

        boolean approved = dlg.showDialog();
        if (approved) {
            JOptionPane.showMessageDialog(this, "Documento aprovado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        }
        refreshData();
    }

    private static JTextField readOnly(String value) {
        JTextField f = new JTextField(value == null ? "" : value);
        f.setEditable(false);
        UIHelper.styleTextField(f);
        return f;
    }

    /** Adiciona um par etiqueta (acento) → componente, empilhados, e devolve o próximo {@code gridy}. */
    private static int addFormRow(JPanel form, GridBagConstraints g, int y, String label, JComponent comp) {
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(UIHelper.ACCENT);
        g.gridy = y;
        g.insets = new Insets(8, 8, 2, 8);
        form.add(l, g);
        g.gridy = y + 1;
        g.insets = new Insets(0, 8, 6, 8);
        form.add(comp, g);
        return y + 2;
    }

    public void onPanelSelected() {
        refreshData();
    }
}
