package com.phcpro.gui;

import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernPanel;
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

    // Detailed inspector labels
    private JLabel docTypeVal;
    private JLabel docIdVal;
    private JLabel submitterVal;
    private JLabel amountVal;
    private JLabel requiredRoleVal;
    private JTextArea descVal;

    // Control buttons
    private ModernButton approveBtn;
    private ModernButton rejectBtn;

    // Data lists
    private List<ApprovalRequestDTO> pendingList = new ArrayList<>();
    private List<ApprovalRequestDTO> historyList = new ArrayList<>();
    private ApprovalRequestDTO selectedRequest = null;

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

        // CENTER: PENDING (LEFT) & DETAIL INSPECTOR (RIGHT)
        JPanel centerSplit = new JPanel(new GridLayout(1, 2, 20, 0));
        centerSplit.setOpaque(false);

        // Left Card: Pending Table
        JPanel pendingPanel = new JPanel(new BorderLayout(0, 10));
        pendingPanel.setOpaque(false);
        pendingPanel.add(UIHelper.createSubheading("Pedidos a Aguardar Decisão"), BorderLayout.NORTH);

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
        JScrollPane pendingScroll = new JScrollPane(pendingTable);
        UIHelper.styleScrollPane(pendingScroll);
        pendingCard.add(pendingScroll, BorderLayout.CENTER);
        pendingPanel.add(pendingCard, BorderLayout.CENTER);
        centerSplit.add(pendingPanel);

        // Right Card: Detail View & Actions
        JPanel inspectorPanel = new JPanel(new BorderLayout(0, 10));
        inspectorPanel.setOpaque(false);
        inspectorPanel.add(UIHelper.createSubheading("Inspetor de Detalhes"), BorderLayout.NORTH);

        ModernPanel detailCard = new ModernPanel(16);
        detailCard.setLayout(new GridBagLayout());
        detailCard.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.weightx = 1.0;

        // Rows config
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        JLabel lblType = new JLabel("Tipo Documento:");
        lblType.setForeground(UIHelper.TEXT_MUTED);
        detailCard.add(lblType, gbc);

        gbc.gridx = 1;
        docTypeVal = new JLabel("Nenhum selecionado");
        docTypeVal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        docTypeVal.setForeground(UIHelper.TEXT_LIGHT);
        detailCard.add(docTypeVal, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        JLabel lblId = new JLabel("ID Documento:");
        lblId.setForeground(UIHelper.TEXT_MUTED);
        detailCard.add(lblId, gbc);

        gbc.gridx = 1;
        docIdVal = new JLabel("-");
        docIdVal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        docIdVal.setForeground(UIHelper.TEXT_LIGHT);
        detailCard.add(docIdVal, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        JLabel lblSub = new JLabel("Solicitante:");
        lblSub.setForeground(UIHelper.TEXT_MUTED);
        detailCard.add(lblSub, gbc);

        gbc.gridx = 1;
        submitterVal = new JLabel("-");
        submitterVal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        submitterVal.setForeground(UIHelper.TEXT_LIGHT);
        detailCard.add(submitterVal, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        JLabel lblAmt = new JLabel("Valor do Documento:");
        lblAmt.setForeground(UIHelper.TEXT_MUTED);
        detailCard.add(lblAmt, gbc);

        gbc.gridx = 1;
        amountVal = new JLabel("-");
        amountVal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        amountVal.setForeground(UIHelper.ACCENT_BLUE);
        detailCard.add(amountVal, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        JLabel lblRole = new JLabel("Perfil Requerido:");
        lblRole.setForeground(UIHelper.TEXT_MUTED);
        detailCard.add(lblRole, gbc);

        gbc.gridx = 1;
        requiredRoleVal = new JLabel("-");
        requiredRoleVal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        requiredRoleVal.setForeground(UIHelper.PENDING_YELLOW);
        detailCard.add(requiredRoleVal, gbc);

        // Description text area (scrollable)
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        JLabel lblDesc = new JLabel("Descrição / Justificação:");
        lblDesc.setForeground(UIHelper.TEXT_MUTED);
        detailCard.add(lblDesc, gbc);

        gbc.gridy = 6; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        descVal = new JTextArea();
        descVal.setEditable(false);
        descVal.setLineWrap(true);
        descVal.setWrapStyleWord(true);
        descVal.setBackground(UIHelper.BG_DARK);
        descVal.setForeground(UIHelper.TEXT_LIGHT);
        descVal.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descVal.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 65, 81), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        JScrollPane descScroll = new JScrollPane(descVal);
        detailCard.add(descScroll, gbc);

        // Buttons
        gbc.gridy = 7; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        btnPanel.setOpaque(false);

        approveBtn = UIHelper.createSuccessButton("Aprovar");
        approveBtn.setIcon(UIHelper.icon("fas-check", 14));
        approveBtn.setEnabled(false);

        rejectBtn = UIHelper.createDangerButton("Rejeitar");
        rejectBtn.setIcon(UIHelper.icon("fas-times", 14));
        rejectBtn.setEnabled(false);

        btnPanel.add(approveBtn);
        btnPanel.add(rejectBtn);
        detailCard.add(btnPanel, gbc);

        inspectorPanel.add(detailCard, BorderLayout.CENTER);
        centerSplit.add(inspectorPanel);
        add(centerSplit, BorderLayout.CENTER);

        // BOTTOM: HISTORY LOG
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 10));
        bottomPanel.setOpaque(false);
        bottomPanel.add(UIHelper.createSubheading("Histórico e Auditoria de Aprovacões"), BorderLayout.NORTH);

        ModernPanel historyCard = new ModernPanel(16);
        historyCard.setLayout(new BorderLayout());
        historyCard.setBorder(new EmptyBorder(15, 15, 15, 15));
        historyCard.setPreferredSize(new Dimension(800, 200));

        String[] historyCols = {"Data", "Tipo Doc", "Submissor", "Valor", "Estado Final", "Motivo / Comentário"};
        historyModel = new DefaultTableModel(historyCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(historyModel);
        UIHelper.styleTable(historyTable);
        JScrollPane historyScroll = new JScrollPane(historyTable);
        UIHelper.styleScrollPane(historyScroll);
        historyCard.add(historyScroll, BorderLayout.CENTER);
        bottomPanel.add(historyCard, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // LISTENERS
        pendingTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = pendingTable.getSelectedRow();
                if (row >= 0 && row < pendingList.size()) {
                    selectRequest(pendingList.get(row));
                } else {
                    clearSelection();
                }
            }
        });

        approveBtn.addActionListener(e -> performApproval());
        rejectBtn.addActionListener(e -> performRejection());

        refreshData();
    }

    private void selectRequest(ApprovalRequestDTO req) {
        this.selectedRequest = req;
        docTypeVal.setText(req.documentType());
        docIdVal.setText("#" + req.documentId());
        submitterVal.setText(req.submitter());
        amountVal.setText(String.format("%,.2f MT", req.amount()));
        requiredRoleVal.setText(req.requiredRole());
        descVal.setText(req.description());

        approveBtn.setEnabled(true);
        rejectBtn.setEnabled(true);
    }

    private void clearSelection() {
        this.selectedRequest = null;
        docTypeVal.setText("Nenhum selecionado");
        docIdVal.setText("-");
        submitterVal.setText("-");
        amountVal.setText("-");
        requiredRoleVal.setText("-");
        descVal.setText("");

        approveBtn.setEnabled(false);
        rejectBtn.setEnabled(false);
    }

    public void refreshData() {
        loadPendingTable();
        loadHistoryTable();
        clearSelection();
    }

    private void loadPendingTable() {
        pendingModel.setRowCount(0);
        pendingList = approvalService.getPendingRequests();
        for (ApprovalRequestDTO req : pendingList) {
            pendingModel.addRow(new Object[]{
                    req.id(),
                    req.documentType() + " #" + req.documentId(),
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
                        req.documentType() + " #" + req.documentId(),
                        req.submitter(),
                        String.format("%,.2f MT", req.amount()),
                        req.status().name(),
                        req.status() == ApprovalStatus.REJECTED ? req.rejectionReason() : "Validado administrativo"
                });
            }
        }
    }

    private void performApproval() {
        if (selectedRequest == null) return;

        int response = JOptionPane.showConfirmDialog(
                this, 
                "Tem certeza que deseja aprovar este documento?", 
                "Confirmar Aprovação", 
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.QUESTION_MESSAGE
        );

        if (response == JOptionPane.YES_OPTION) {
            try {
                approvalService.approveRequest(selectedRequest.id(), "Aprovado via interface Swing.");
                JOptionPane.showMessageDialog(this, "Documento aprovado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                refreshData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro de validação: " + ex.getMessage(), "Erro de Autorização", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void performRejection() {
        if (selectedRequest == null) return;

        String reason = JOptionPane.showInputDialog(
                this, 
                "Introduza o motivo de rejeição para este documento (obrigatório):", 
                "Rejeitar Documento", 
                JOptionPane.WARNING_MESSAGE
        );

        if (reason == null) {
            // User cancelled
            return;
        }

        if (reason.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "É obrigatório indicar um motivo para a rejeição.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            approvalService.rejectRequest(selectedRequest.id(), reason.trim());
            JOptionPane.showMessageDialog(this, "Documento rejeitado com sucesso.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao rejeitar: " + ex.getMessage(), "Erro de Autorização", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void onPanelSelected() {
        refreshData();
    }
}
