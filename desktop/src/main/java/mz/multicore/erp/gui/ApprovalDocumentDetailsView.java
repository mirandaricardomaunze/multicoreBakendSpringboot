package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.desktop.client.StockTransferApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;
import mz.multicore.erp.modules.comercial.dto.OrderLineDTO;
import mz.multicore.erp.modules.inventory.dto.StockTransferLineDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Visualizador de detalhes documentais para o diálogo executivo de aprovação.
 */
final class ApprovalDocumentDetailsView {

    private ApprovalDocumentDetailsView() {}

    static JPanel buildTab(
            ApprovalRequestDTO request,
            StockTransferApiClient stockTransferApiClient,
            ComercialApiClient comercialApiClient,
            PrintApiClient printApiClient
    ) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false); panel.setBorder(new EmptyBorder(10, 0, 0, 0));

        String docType = request.documentType() != null ? request.documentType().toUpperCase(java.util.Locale.ROOT) : "";
        if (("STOCK_TRANSFER".equals(docType) || "TRANSFER".equals(docType)) && stockTransferApiClient != null) {
            populateTransferDetails(panel, request, stockTransferApiClient, printApiClient);
        } else if ("ORDER".equals(docType) && comercialApiClient != null) {
            populateOrderDetails(panel, request, comercialApiClient);
        } else {
            populateGenericDetails(panel, request);
        }
        return panel;
    }

    private static void populateTransferDetails(
            JPanel container,
            ApprovalRequestDTO request,
            StockTransferApiClient stockTransferApiClient,
            PrintApiClient printApiClient
    ) {
        JPanel summaryBox = new ModernPanel(12); summaryBox.setLayout(new GridLayout(2, 3, 14, 6));
        summaryBox.setBorder(new EmptyBorder(12, 14, 12, 14));
        JLabel origVal = loadingLabel(), destVal = loadingLabel(), transpVal = loadingLabel();

        summaryBox.add(metaLabel("Armazém Origem")); summaryBox.add(metaLabel("Armazém Destino"));
        summaryBox.add(metaLabel("Motorista / Viatura"));
        summaryBox.add(origVal); summaryBox.add(destVal); summaryBox.add(transpVal);

        String[] cols = {"Artigo / Produto", "SKU / Código", "Quantidade", "Lote Atribuído", "Notas / Volumes"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(table); UIHelper.ensureHeadersFit(table);
        JScrollPane scroll = new JScrollPane(table); UIHelper.styleScrollPane(scroll);

        ModernPanel tableCard = new ModernPanel(14); tableCard.setLayout(new BorderLayout(0, 8));
        tableCard.setBorder(new EmptyBorder(10, 12, 10, 12));

        ModernButton pdfBtn = UIHelper.createSecondaryButton("Ver Guia em PDF");
        pdfBtn.setIcon(UIHelper.icon("fas-file-pdf", 14, UIHelper.ACCENT_BLUE));
        pdfBtn.setEnabled(false);

        tableCard.add(UIHelper.tableCardTop("Linhas da Guia de Transferência", null, pdfBtn), BorderLayout.NORTH);
        tableCard.add(scroll, BorderLayout.CENTER);
        container.add(summaryBox, BorderLayout.NORTH); container.add(tableCard, BorderLayout.CENTER);

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
            pdfBtn.addActionListener(e -> SensitivePrintCoordinator.print(container, printApiClient,
                    mz.multicore.erp.modules.printing.dto.SensitiveDocumentType.STOCK_TRANSFER,
                    transfer.id(), "Guia de transferência " + transfer.transferNumber(),
                    "transferencia-" + transfer.transferNumber(),
                    () -> stockTransferApiClient.renderTransfer(transfer.id()), null));
        }, err -> {
            origVal.setText("Erro ao carregar"); destVal.setText("Erro ao carregar");
            ToastManager.show(container, FeedbackType.ERROR, "Erro ao obter detalhes da transferência: " + err.getMessage());
        });
    }

    private static void populateOrderDetails(JPanel container, ApprovalRequestDTO request, ComercialApiClient comercialApiClient) {
        JPanel summaryBox = new ModernPanel(12); summaryBox.setLayout(new GridLayout(2, 3, 14, 6));
        summaryBox.setBorder(new EmptyBorder(12, 14, 12, 14));
        JLabel clientVal = loadingLabel(), numVal = loadingLabel(), termsVal = loadingLabel();

        summaryBox.add(metaLabel("Cliente")); summaryBox.add(metaLabel("Número Encomenda"));
        summaryBox.add(metaLabel("Condição de Pagamento"));
        summaryBox.add(clientVal); summaryBox.add(numVal); summaryBox.add(termsVal);

        String[] cols = {"Produto", "Qtd", "Preço Unit.", "Desconto", "Imposto", "Total Linha"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(table); UIHelper.ensureHeadersFit(table);
        table.getColumnModel().getColumn(2).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.money());
        JScrollPane scroll = new JScrollPane(table); UIHelper.styleScrollPane(scroll);

        ModernPanel tableCard = new ModernPanel(14); tableCard.setLayout(new BorderLayout(0, 8));
        tableCard.setBorder(new EmptyBorder(10, 12, 10, 12));
        tableCard.add(UIHelper.tableCardTop("Itens da Encomenda", null), BorderLayout.NORTH);
        tableCard.add(scroll, BorderLayout.CENTER);
        container.add(summaryBox, BorderLayout.NORTH); container.add(tableCard, BorderLayout.CENTER);

        UIHelper.loadAsync(container, () -> comercialApiClient.getOrderById(request.documentId()), order -> {
            clientVal.setText(order.clientName() != null ? order.clientName() : "Cliente #" + order.clientId());
            numVal.setText(order.orderNumber() != null ? order.orderNumber() : "#" + order.id());
            termsVal.setText(order.paymentTerms() != null ? order.paymentTerms() : "Pronto Pagamento");

            model.setRowCount(0);
            if (order.lines() != null) {
                for (OrderLineDTO l : order.lines()) {
                    model.addRow(new Object[]{
                            l.productName() != null ? l.productName() : "Produto #" + l.productId(),
                            l.quantity() != null ? l.quantity().toString() : "0", l.unitPrice(),
                            (l.discountPercentage() != null ? l.discountPercentage() : BigDecimal.ZERO) + "%",
                            (l.taxRate() != null ? l.taxRate() : BigDecimal.ZERO) + "%", l.lineTotal()
                    });
                }
            }
        }, err -> {
            clientVal.setText("Erro ao carregar");
            ToastManager.show(container, FeedbackType.ERROR, "Erro ao obter detalhes da encomenda: " + err.getMessage());
        });
    }

    private static void populateGenericDetails(JPanel container, ApprovalRequestDTO request) {
        ModernPanel card = new ModernPanel(14); card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        JTextArea area = new JTextArea(request.description() != null ? request.description() : "Sem detalhes adicionais.");
        area.setEditable(false); area.setLineWrap(true); area.setWrapStyleWord(true);
        UIHelper.styleTextArea(area);
        card.add(UIHelper.tableCardTop("Especificações do Documento", null), BorderLayout.NORTH);
        card.add(new JScrollPane(area), BorderLayout.CENTER);
        container.add(card, BorderLayout.CENTER);
    }

    private static JLabel loadingLabel() {
        JLabel l = new JLabel("A carregar…"); l.setForeground(UIHelper.TEXT_LIGHT); return l;
    }

    private static JLabel metaLabel(String text) {
        JLabel l = new JLabel(text); l.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        l.setForeground(UIHelper.TEXT_MUTED); return l;
    }
}
