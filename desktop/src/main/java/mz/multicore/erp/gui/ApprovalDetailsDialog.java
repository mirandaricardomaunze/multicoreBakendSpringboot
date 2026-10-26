package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ApprovalApiClient;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.desktop.client.StockTransferApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Modal Executivo de Decisão de Aprovação Corporativa.
 * Permite aos Diretores e Gestores inspecionar detalhadamente cada documento
 * antes de emitir a decisão final de Aprovar ou Rejeitar.
 */
public final class ApprovalDetailsDialog extends JDialog {

    private final ApprovalRequestDTO request;
    private final ApprovalApiClient approvalApiClient;
    private final StockTransferApiClient stockTransferApiClient;
    private final ComercialApiClient comercialApiClient;
    private final PrintApiClient printApiClient;
    private final Runnable onDecisionCompleted;
    private boolean decisionTaken = false;

    public static void show(
            Window owner, ApprovalRequestDTO request, ApprovalApiClient approvalApiClient,
            StockTransferApiClient stockTransferApiClient, ComercialApiClient comercialApiClient,
            Runnable onDecisionCompleted) {
        show(owner, request, approvalApiClient, stockTransferApiClient, comercialApiClient, null, onDecisionCompleted);
    }

    public static void show(
            Window owner, ApprovalRequestDTO request, ApprovalApiClient approvalApiClient,
            StockTransferApiClient stockTransferApiClient, ComercialApiClient comercialApiClient,
            PrintApiClient printApiClient, Runnable onDecisionCompleted) {
        new ApprovalDetailsDialog(owner, request, approvalApiClient, stockTransferApiClient,
                comercialApiClient, printApiClient, onDecisionCompleted).setVisible(true);
    }

    private ApprovalDetailsDialog(
            Window owner, ApprovalRequestDTO request, ApprovalApiClient approvalApiClient,
            StockTransferApiClient stockTransferApiClient, ComercialApiClient comercialApiClient,
            PrintApiClient printApiClient, Runnable onDecisionCompleted) {
        super(owner, "Dossiê Executivo de Decisão de Aprovação", ModalityType.APPLICATION_MODAL);
        this.request = request; this.approvalApiClient = approvalApiClient;
        this.stockTransferApiClient = stockTransferApiClient; this.comercialApiClient = comercialApiClient;
        this.printApiClient = printApiClient; this.onDecisionCompleted = onDecisionCompleted;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(UIHelper.BG_DARK);
        UIHelper.applyStandardModalBounds(this, 920, 610);

        JPanel root = new JPanel(new BorderLayout(0, 12)); root.setBackground(UIHelper.BG_DARK);
        root.setBorder(new EmptyBorder(16, 20, 16, 20));
        root.add(buildHeaderCard(), BorderLayout.NORTH);
        root.add(buildContentTabs(), BorderLayout.CENTER);
        root.add(buildActionsBar(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel buildHeaderCard() {
        ModernPanel card = new ModernPanel(16); card.setLayout(new BorderLayout(16, 0)); card.setBorder(new EmptyBorder(14, 18, 14, 18));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0)); left.setOpaque(false);
        left.add(new JLabel(UIHelper.icon(resolveIcon(request.documentType()), 32, resolveColor(request.documentType()))));

        JPanel info = new JPanel(); info.setOpaque(false); info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel titleLbl = new JLabel(humanType(request.documentType()) + " #" + request.documentId());
        titleLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 18)); titleLbl.setForeground(UIHelper.TEXT_LIGHT);
        JLabel subLbl = new JLabel("Pedido de Aprovação #" + request.id() + " • Submetido por " + request.submitter());
        subLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12)); subLbl.setForeground(UIHelper.TEXT_MUTED);
        info.add(titleLbl); info.add(Box.createVerticalStrut(2)); info.add(subLbl);
        left.add(info); card.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(); right.setOpaque(false); right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JLabel amountLbl = new JLabel(String.format("%,.2f MT", request.amount() != null ? request.amount() : BigDecimal.ZERO));
        amountLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 22)); amountLbl.setForeground(UIHelper.APPROVED_GREEN);
        amountLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JLabel roleChip = new JLabel("Perfil Requerido: " + (request.requiredRole() != null ? request.requiredRole() : "MANAGER"));
        roleChip.setFont(new Font(UIHelper.FONT, Font.BOLD, 11)); roleChip.setForeground(UIHelper.ACCENT_BLUE);
        roleChip.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(amountLbl); right.add(Box.createVerticalStrut(4)); right.add(roleChip);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private JComponent buildContentTabs() {
        JTabbedPane tabs = new JTabbedPane(); UIHelper.styleTabbedPaneMulticore(tabs);
        tabs.addTab("Artigos & Documento", UIHelper.icon("fas-boxes", 16, UIHelper.ACCENT_BLUE),
                ApprovalDocumentDetailsView.buildTab(request, stockTransferApiClient, comercialApiClient, printApiClient));
        tabs.addTab("Parecer & Justificação", UIHelper.icon("fas-align-left", 16, UIHelper.PENDING_YELLOW), buildJustificationTab());
        return tabs;
    }

    private JPanel buildJustificationTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12)); panel.setOpaque(false); panel.setBorder(new EmptyBorder(12, 0, 0, 0));
        ModernPanel card = new ModernPanel(14); card.setLayout(new BorderLayout(0, 8)); card.setBorder(new EmptyBorder(14, 16, 14, 16));
        JTextArea descA = new JTextArea(request.description() != null ? request.description() : "Sem justificação registada.");
        descA.setEditable(false); descA.setLineWrap(true); descA.setWrapStyleWord(true); descA.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        UIHelper.styleTextArea(descA);
        JScrollPane scroll = new JScrollPane(descA); scroll.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER, 1));
        card.add(UIHelper.tableCardTop("Descrição do Solicitante e Parecer do Workflow", null), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER); panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActionsBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0)); bar.setOpaque(false); bar.setBorder(new EmptyBorder(6, 0, 0, 0));
        ModernButton closeBtn = UIHelper.createSecondaryButton("Fechar");
        closeBtn.setIcon(UIHelper.icon("fas-times", 14)); closeBtn.addActionListener(e -> dispose());

        ModernButton rejectBtn = UIHelper.createDangerButton("Rejeitar");
        rejectBtn.setIcon(UIHelper.icon("fas-times-circle", 14)); rejectBtn.addActionListener(e -> handleReject(rejectBtn));

        ModernButton approveBtn = UIHelper.createPrimaryButton("Aprovar Documento");
        approveBtn.setIcon(UIHelper.icon("fas-check-circle", 14)); approveBtn.addActionListener(e -> handleApprove(approveBtn));

        bar.add(closeBtn); bar.add(rejectBtn); bar.add(approveBtn);
        return bar;
    }

    private void handleReject(ModernButton trigger) {
        String reason = UIHelper.promptRequiredText("Rejeitar Documento", "fas-times-circle",
                humanType(request.documentType()) + " #" + request.documentId(), "Motivo da rejeição:");
        if (reason == null || reason.isBlank()) return;

        UIHelper.submitAsync(trigger, () -> {
            approvalApiClient.rejectRequest(request.id(), reason); return null;
        }, ignored -> {
            decisionTaken = true; ToastManager.success(this, "Documento rejeitado com sucesso.");
            dispose(); if (onDecisionCompleted != null) onDecisionCompleted.run();
        }, error -> ToastManager.show(this, FeedbackType.ERROR, "Não foi possível rejeitar: " + error.getMessage()));
    }

    private void handleApprove(ModernButton trigger) {
        boolean confirm = mz.multicore.erp.gui.components.ModernMessageDialog.confirm(
                this, FeedbackType.INFO, "Confirmar Aprovação",
                "Tem a certeza de que deseja aprovar o documento " + humanType(request.documentType())
                        + " #" + request.documentId() + " no montante de "
                        + String.format("%,.2f MT", request.amount()) + "?\n\n"
                        + "Esta decisão é autoritativa e aplicará os movimentos correspondentes no sistema.",
                "Aprovar Agora"
        );
        if (!confirm) return;

        UIHelper.submitAsync(trigger, () -> {
            approvalApiClient.approveRequest(request.id(), "Aprovado via Central de Aprovações."); return null;
        }, ignored -> {
            decisionTaken = true; ToastManager.success(this, "Documento aprovado com sucesso!");
            dispose(); if (onDecisionCompleted != null) onDecisionCompleted.run();
        }, error -> ToastManager.show(this, FeedbackType.ERROR, "Não foi possível aprovar: " + error.getMessage()));
    }

    private static String humanType(String dt) {
        if (dt == null) return "Documento";
        return switch (dt.toUpperCase(java.util.Locale.ROOT)) {
            case "ORDER" -> "Encomenda"; case "INVOICE" -> "Fatura";
            case "EXPENSE", "EXPENSE_CLAIM" -> "Despesa"; case "STOCK_TRANSFER", "TRANSFER" -> "Transferência de Stock";
            case "STOCK_WASTE", "WASTE" -> "Quebra de Stock"; case "PURCHASE_ORDER", "PURCHASE" -> "Ordem de Compra";
            default -> dt;
        };
    }

    private static String resolveIcon(String dt) {
        if (dt == null) return "fas-file-alt";
        return switch (dt.toUpperCase(java.util.Locale.ROOT)) {
            case "ORDER" -> "fas-shopping-cart"; case "INVOICE" -> "fas-file-invoice";
            case "EXPENSE", "EXPENSE_CLAIM" -> "fas-receipt"; case "STOCK_TRANSFER", "TRANSFER" -> "fas-truck";
            case "STOCK_WASTE", "WASTE" -> "fas-trash-alt"; case "PURCHASE_ORDER", "PURCHASE" -> "fas-shopping-bag";
            default -> "fas-file-alt";
        };
    }

    private static Color resolveColor(String dt) {
        if (dt == null) return UIHelper.ACCENT_BLUE;
        return switch (dt.toUpperCase(java.util.Locale.ROOT)) {
            case "ORDER" -> UIHelper.PENDING_YELLOW; case "INVOICE" -> UIHelper.APPROVED_GREEN;
            case "STOCK_WASTE", "WASTE" -> UIHelper.REJECTED_RED; default -> UIHelper.ACCENT_BLUE;
        };
    }
}
