package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ApprovalApiClient;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.StockTransferApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.gui.components.ClientTablePagination;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.PrintPreviewDialog;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.SensitivePrintCoordinator;
import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.dto.OrderLineDTO;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.dto.StockTransferLineDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Modal Executivo de Decisão de Aprovação Corporativa.
 * Permite aos Diretores e Gestores inspecionar detalhadamente cada documento
 * (linhas de artigos, quantidades, preços, armazéns, clientes, PDF oficial)
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
            Window owner,
            ApprovalRequestDTO request,
            ApprovalApiClient approvalApiClient,
            StockTransferApiClient stockTransferApiClient,
            ComercialApiClient comercialApiClient,
            Runnable onDecisionCompleted
    ) {
        show(owner, request, approvalApiClient, stockTransferApiClient, comercialApiClient,
                null, onDecisionCompleted);
    }

    public static void show(
            Window owner,
            ApprovalRequestDTO request,
            ApprovalApiClient approvalApiClient,
            StockTransferApiClient stockTransferApiClient,
            ComercialApiClient comercialApiClient,
            PrintApiClient printApiClient,
            Runnable onDecisionCompleted
    ) {
        ApprovalDetailsDialog dialog = new ApprovalDetailsDialog(
                owner, request, approvalApiClient, stockTransferApiClient, comercialApiClient,
                printApiClient, onDecisionCompleted);
        dialog.setVisible(true);
    }

    private ApprovalDetailsDialog(
            Window owner,
            ApprovalRequestDTO request,
            ApprovalApiClient approvalApiClient,
            StockTransferApiClient stockTransferApiClient,
            ComercialApiClient comercialApiClient,
            PrintApiClient printApiClient,
            Runnable onDecisionCompleted
    ) {
        super(owner, "Dossiê Executivo de Decisão de Aprovação", ModalityType.APPLICATION_MODAL);
        this.request = request;
        this.approvalApiClient = approvalApiClient;
        this.stockTransferApiClient = stockTransferApiClient;
        this.comercialApiClient = comercialApiClient;
        this.printApiClient = printApiClient;
        this.onDecisionCompleted = onDecisionCompleted;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(UIHelper.BG_DARK);
        UIHelper.applyStandardModalBounds(this, 920, 610);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(UIHelper.BG_DARK);
        root.setBorder(new EmptyBorder(16, 20, 16, 20));

        root.add(buildHeaderCard(), BorderLayout.NORTH);
        root.add(buildContentTabs(), BorderLayout.CENTER);
        root.add(buildActionsBar(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel buildHeaderCard() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(new EmptyBorder(14, 18, 14, 18));

        String iconCode = resolveIcon(request.documentType());
        Color iconColor = resolveColor(request.documentType());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        JLabel iconLbl = new JLabel(UIHelper.icon(iconCode, 32, iconColor));
        left.add(iconLbl);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        JLabel titleLbl = new JLabel(humanType(request.documentType()) + " #" + request.documentId());
        titleLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 18));
        titleLbl.setForeground(UIHelper.TEXT_LIGHT);

        String subText = "Pedido de Aprovação #" + request.id() + " • Submetido por " + request.submitter();
        JLabel subLbl = new JLabel(subText);
        subLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        subLbl.setForeground(UIHelper.TEXT_MUTED);

        info.add(titleLbl);
        info.add(Box.createVerticalStrut(2));
        info.add(subLbl);
        left.add(info);

        card.add(left, BorderLayout.WEST);

        // Painel direito: Montante financeiro e perfil requerido
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        JLabel amountLbl = new JLabel(String.format("%,.2f MT", request.amount() != null ? request.amount() : BigDecimal.ZERO));
        amountLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 22));
        amountLbl.setForeground(UIHelper.APPROVED_GREEN);
        amountLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel roleChip = new JLabel("Perfil Requerido: " + (request.requiredRole() != null ? request.requiredRole() : "MANAGER"));
        roleChip.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        roleChip.setForeground(UIHelper.ACCENT_BLUE);
        roleChip.setAlignmentX(Component.RIGHT_ALIGNMENT);

        right.add(amountLbl);
        right.add(Box.createVerticalStrut(4));
        right.add(roleChip);

        card.add(right, BorderLayout.EAST);
        return card;
    }

    private JComponent buildContentTabs() {
        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);

        tabs.addTab("Artigos & Documento", UIHelper.icon("fas-boxes", 16, UIHelper.ACCENT_BLUE), buildDocumentDetailsTab());
        tabs.addTab("Parecer & Justificação", UIHelper.icon("fas-align-left", 16, UIHelper.PENDING_YELLOW), buildJustificationTab());

        return tabs;
    }

    private JPanel buildDocumentDetailsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 0, 0, 0));

        String docType = request.documentType() != null ? request.documentType().toUpperCase(java.util.Locale.ROOT) : "";

        if (("STOCK_TRANSFER".equals(docType) || "TRANSFER".equals(docType)) && stockTransferApiClient != null) {
            populateTransferDetails(panel);
        } else if ("ORDER".equals(docType) && comercialApiClient != null) {
            populateOrderDetails(panel);
        } else {
            populateGenericDetails(panel);
        }

        return panel;
    }

    private void populateTransferDetails(JPanel container) {
        JPanel summaryBox = new ModernPanel(12);
        summaryBox.setLayout(new GridLayout(2, 3, 14, 6));
        summaryBox.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel origVal = new JLabel("A carregar…");
        origVal.setForeground(UIHelper.TEXT_LIGHT);
        JLabel destVal = new JLabel("A carregar…");
        destVal.setForeground(UIHelper.TEXT_LIGHT);
        JLabel transpVal = new JLabel("A carregar…");
        transpVal.setForeground(UIHelper.TEXT_LIGHT);
        JLabel statusVal = new JLabel("A carregar…");
        statusVal.setForeground(UIHelper.PENDING_YELLOW);

        summaryBox.add(metaLabel("Armazém Origem"));
        summaryBox.add(metaLabel("Armazém Destino"));
        summaryBox.add(metaLabel("Motorista / Viatura"));
        summaryBox.add(origVal);
        summaryBox.add(destVal);
        summaryBox.add(transpVal);

        String[] cols = {"Artigo / Produto", "SKU / Código", "Quantidade", "Lote Atribuído", "Notas / Volumes"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        ModernPanel tableCard = new ModernPanel(14);
        tableCard.setLayout(new BorderLayout(0, 8));
        tableCard.setBorder(new EmptyBorder(10, 12, 10, 12));

        ModernButton pdfBtn = UIHelper.createSecondaryButton("Ver Guia em PDF");
        pdfBtn.setIcon(UIHelper.icon("fas-file-pdf", 14, UIHelper.ACCENT_BLUE));
        pdfBtn.setEnabled(false);

        tableCard.add(UIHelper.tableCardTop("Linhas da Guia de Transferência", null, pdfBtn), BorderLayout.NORTH);
        tableCard.add(scroll, BorderLayout.CENTER);

        container.add(summaryBox, BorderLayout.NORTH);
        container.add(tableCard, BorderLayout.CENTER);

        // Carregamento assíncrono dos dados completos da transferência
        UIHelper.loadAsync(container, () -> stockTransferApiClient.findById(request.documentId()), transfer -> {
            origVal.setText(transfer.originWarehouseName() != null ? transfer.originWarehouseName() : "—");
            destVal.setText(transfer.destinationWarehouseName() != null ? transfer.destinationWarehouseName() : "—");
            String driver = transfer.driverName() != null && !transfer.driverName().isBlank() ? transfer.driverName() : transfer.responsible();
            String plate = transfer.vehiclePlate() != null && !transfer.vehiclePlate().isBlank() ? " (" + transfer.vehiclePlate() + ")" : "";
            transpVal.setText((driver != null ? driver : "Não especificado") + plate);

            model.setRowCount(0);
            if (transfer.lines() != null) {
                for (StockTransferLineDTO l : transfer.lines()) {
                    model.addRow(new Object[]{
                            l.productName() != null ? l.productName() : "Artigo #" + l.productId(),
                            l.productSku() != null ? l.productSku() : "—",
                            l.quantity() != null ? l.quantity().toString() : "0",
                            l.batchNumber() != null ? l.batchNumber() : "Automático (FEFO)",
                            String.format("%d cx / %d un", l.safePackagesPerBox(), l.safeUnitsPerPackage())
                    });
                }
            }
            pdfBtn.setEnabled(true);
            pdfBtn.addActionListener(e -> {
                SensitivePrintCoordinator.print(container, printApiClient,
                        mz.multicore.erp.modules.printing.dto.SensitiveDocumentType.STOCK_TRANSFER,
                        transfer.id(), "Guia de transferência " + transfer.transferNumber(),
                        "transferencia-" + transfer.transferNumber(),
                        () -> stockTransferApiClient.renderTransfer(transfer.id()), null);
            });
        }, err -> {
            origVal.setText("Erro ao carregar");
            destVal.setText("Erro ao carregar");
            ToastManager.show(container, FeedbackType.ERROR, "Erro ao obter detalhes da transferência: " + err.getMessage());
        });
    }

    private void populateOrderDetails(JPanel container) {
        JPanel summaryBox = new ModernPanel(12);
        summaryBox.setLayout(new GridLayout(2, 3, 14, 6));
        summaryBox.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel clientVal = new JLabel("A carregar…");
        clientVal.setForeground(UIHelper.TEXT_LIGHT);
        JLabel numVal = new JLabel("A carregar…");
        numVal.setForeground(UIHelper.TEXT_LIGHT);
        JLabel termsVal = new JLabel("A carregar…");
        termsVal.setForeground(UIHelper.TEXT_LIGHT);

        summaryBox.add(metaLabel("Cliente"));
        summaryBox.add(metaLabel("Número Encomenda"));
        summaryBox.add(metaLabel("Condição de Pagamento"));
        summaryBox.add(clientVal);
        summaryBox.add(numVal);
        summaryBox.add(termsVal);

        String[] cols = {"Produto", "Qtd", "Preço Unit.", "Desconto", "Imposto", "Total Linha"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        table.getColumnModel().getColumn(2).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.money());
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        ModernPanel tableCard = new ModernPanel(14);
        tableCard.setLayout(new BorderLayout(0, 8));
        tableCard.setBorder(new EmptyBorder(10, 12, 10, 12));
        tableCard.add(UIHelper.tableCardTop("Itens da Encomenda", null), BorderLayout.NORTH);
        tableCard.add(scroll, BorderLayout.CENTER);

        container.add(summaryBox, BorderLayout.NORTH);
        container.add(tableCard, BorderLayout.CENTER);

        UIHelper.loadAsync(container, () -> comercialApiClient.getOrderById(request.documentId()), order -> {
            clientVal.setText(order.clientName() != null ? order.clientName() : "Cliente #" + order.clientId());
            numVal.setText(order.orderNumber() != null ? order.orderNumber() : "#" + order.id());
            termsVal.setText(order.paymentTerms() != null ? order.paymentTerms() : "Pronto Pagamento");

            model.setRowCount(0);
            if (order.lines() != null) {
                for (OrderLineDTO l : order.lines()) {
                    model.addRow(new Object[]{
                            l.productName() != null ? l.productName() : "Produto #" + l.productId(),
                            l.quantity() != null ? l.quantity().toString() : "0",
                            l.unitPrice(),
                            (l.discountPercentage() != null ? l.discountPercentage() : BigDecimal.ZERO) + "%",
                            (l.taxRate() != null ? l.taxRate() : BigDecimal.ZERO) + "%",
                            l.lineTotal()
                    });
                }
            }
        }, err -> {
            clientVal.setText("Erro ao carregar");
            ToastManager.show(container, FeedbackType.ERROR, "Erro ao obter detalhes da encomenda: " + err.getMessage());
        });
    }

    private void populateGenericDetails(JPanel container) {
        ModernPanel card = new ModernPanel(14);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));

        JTextArea area = new JTextArea(request.description() != null ? request.description() : "Sem detalhes adicionais.");
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        UIHelper.styleTextArea(area);
        card.add(UIHelper.tableCardTop("Especificações do Documento", null), BorderLayout.NORTH);
        card.add(new JScrollPane(area), BorderLayout.CENTER);

        container.add(card, BorderLayout.CENTER);
    }

    private JPanel buildJustificationTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(12, 0, 0, 0));

        ModernPanel card = new ModernPanel(14);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(14, 16, 14, 16));

        JTextArea descA = new JTextArea(request.description() != null ? request.description() : "Sem justificação registada.");
        descA.setEditable(false);
        descA.setLineWrap(true);
        descA.setWrapStyleWord(true);
        descA.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        UIHelper.styleTextArea(descA);

        JScrollPane scroll = new JScrollPane(descA);
        scroll.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER, 1));

        card.add(UIHelper.tableCardTop("Descrição do Solicitante e Parecer do Workflow", null), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActionsBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(6, 0, 0, 0));

        ModernButton closeBtn = UIHelper.createSecondaryButton("Fechar");
        closeBtn.setIcon(UIHelper.icon("fas-times", 14));
        closeBtn.addActionListener(e -> dispose());

        ModernButton rejectBtn = UIHelper.createDangerButton("Rejeitar");
        rejectBtn.setIcon(UIHelper.icon("fas-times-circle", 14));
        rejectBtn.addActionListener(e -> handleReject(rejectBtn));

        ModernButton approveBtn = UIHelper.createPrimaryButton("Aprovar Documento");
        approveBtn.setIcon(UIHelper.icon("fas-check-circle", 14));
        approveBtn.addActionListener(e -> handleApprove(approveBtn));

        bar.add(closeBtn);
        bar.add(rejectBtn);
        bar.add(approveBtn);
        return bar;
    }

    private void handleReject(ModernButton trigger) {
        String reason = UIHelper.promptRequiredText("Rejeitar Documento", "fas-times-circle",
                humanType(request.documentType()) + " #" + request.documentId(), "Motivo da rejeição:");
        if (reason == null || reason.isBlank()) return;

        UIHelper.submitAsync(trigger, () -> {
            approvalApiClient.rejectRequest(request.id(), reason);
            return null;
        }, ignored -> {
            decisionTaken = true;
            ToastManager.success(this, "Documento rejeitado com sucesso.");
            dispose();
            if (onDecisionCompleted != null) onDecisionCompleted.run();
        }, error -> ToastManager.show(this, FeedbackType.ERROR, "Não foi possível rejeitar: " + error.getMessage()));
    }

    private void handleApprove(ModernButton trigger) {
        boolean confirm = mz.multicore.erp.gui.components.ModernMessageDialog.confirm(
                this,
                FeedbackType.INFO,
                "Confirmar Aprovação",
                "Tem a certeza de que deseja aprovar o documento " + humanType(request.documentType())
                        + " #" + request.documentId() + " no montante de "
                        + String.format("%,.2f MT", request.amount()) + "?\n\n"
                        + "Esta decisão é autoritativa e aplicará os movimentos correspondentes no sistema.",
                "Aprovar Agora"
        );
        if (!confirm) return;

        UIHelper.submitAsync(trigger, () -> {
            approvalApiClient.approveRequest(request.id(), "Aprovado via Central de Aprovações.");
            return null;
        }, ignored -> {
            decisionTaken = true;
            ToastManager.success(this, "Documento aprovado com sucesso!");
            dispose();
            if (onDecisionCompleted != null) onDecisionCompleted.run();
        }, error -> ToastManager.show(this, FeedbackType.ERROR, "Não foi possível aprovar: " + error.getMessage()));
    }

    private static JLabel metaLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        l.setForeground(UIHelper.TEXT_MUTED);
        return l;
    }

    private static String humanType(String documentType) {
        if (documentType == null) return "Documento";
        return switch (documentType.toUpperCase(java.util.Locale.ROOT)) {
            case "ORDER" -> "Encomenda";
            case "INVOICE" -> "Fatura";
            case "EXPENSE", "EXPENSE_CLAIM" -> "Despesa";
            case "STOCK_TRANSFER", "TRANSFER" -> "Transferência de Stock";
            case "STOCK_WASTE", "WASTE" -> "Quebra de Stock";
            case "PURCHASE_ORDER", "PURCHASE" -> "Ordem de Compra";
            default -> documentType;
        };
    }

    private static String resolveIcon(String documentType) {
        if (documentType == null) return "fas-file-alt";
        return switch (documentType.toUpperCase(java.util.Locale.ROOT)) {
            case "ORDER" -> "fas-shopping-cart";
            case "INVOICE" -> "fas-file-invoice";
            case "EXPENSE", "EXPENSE_CLAIM" -> "fas-receipt";
            case "STOCK_TRANSFER", "TRANSFER" -> "fas-truck";
            case "STOCK_WASTE", "WASTE" -> "fas-trash-alt";
            case "PURCHASE_ORDER", "PURCHASE" -> "fas-shopping-bag";
            default -> "fas-file-alt";
        };
    }

    private static Color resolveColor(String documentType) {
        if (documentType == null) return UIHelper.ACCENT_BLUE;
        return switch (documentType.toUpperCase(java.util.Locale.ROOT)) {
            case "ORDER" -> UIHelper.PENDING_YELLOW;
            case "INVOICE" -> UIHelper.APPROVED_GREEN;
            case "EXPENSE", "EXPENSE_CLAIM" -> UIHelper.ACCENT_BLUE;
            case "STOCK_TRANSFER", "TRANSFER" -> UIHelper.ACCENT_BLUE;
            case "STOCK_WASTE", "WASTE" -> UIHelper.REJECTED_RED;
            default -> UIHelper.ACCENT_BLUE;
        };
    }
}
