package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.ComercialPanel;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Ficha executiva e apresentação detalhada de uma encomenda (Order Detail View).
 * Utiliza o componente canónico {@link ExecutiveDetailDialog} para uma experiência visual uniforme.
 */
public final class OrderDetailsDialog {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JComponent owner;
    private final ComercialApiClient apiClient;
    private final Runnable afterPrint;

    public OrderDetailsDialog(JComponent owner, ComercialApiClient apiClient, Runnable afterPrint) {
        this.owner = owner;
        this.apiClient = apiClient;
        this.afterPrint = afterPrint;
    }

    public void open(Long orderId) {
        UIHelper.loadAsync(owner, () -> apiClient.getOrderById(orderId), this::show,
                error -> showError("Não foi possível carregar os detalhes da encomenda", error));
    }

    public void print(Long orderId) {
        UIHelper.loadAsync(owner, () -> apiClient.getOrderById(orderId), this::printWithConfirmation,
                error -> showError("Não foi possível carregar a encomenda", error));
    }

    public void show(OrderDTO order) {
        ExecutiveDetailDialog dialog = ExecutiveDetailDialog.create(owner, "Detalhes da Encomenda " + order.orderNumber());

        // 1. Cabeçalho
        String clientInfo = "Cliente: " + order.clientName();
        if (order.walkInName() != null && !order.walkInName().isBlank()) {
            clientInfo += " (comprador: " + order.walkInName() + ")";
        }
        if (order.createdAt() != null) {
            clientInfo += " | Emitida em " + order.createdAt().format(DATE_TIME);
        }

        dialog.setTitle("Encomenda " + order.orderNumber())
                .setSubtitle(clientInfo)
                .setHeaderIcon("fas-shopping-bag", 24, UIHelper.ACCENT_BLUE);

        // Status com severidade semântica
        ExecutiveDetailDialog.StatusSeverity severity = switch (order.status() != null ? order.status() : "") {
            case "BILLED" -> ExecutiveDetailDialog.StatusSeverity.SUCCESS;
            case "CONFIRMED", "APPROVED" -> ExecutiveDetailDialog.StatusSeverity.INFO;
            case "PENDING" -> ExecutiveDetailDialog.StatusSeverity.WARNING;
            case "CANCELLED" -> ExecutiveDetailDialog.StatusSeverity.DANGER;
            default -> ExecutiveDetailDialog.StatusSeverity.NEUTRAL;
        };
        dialog.setStatusBadge(order.statusLabel() != null ? order.statusLabel() : order.status(), severity);

        // 2. KPIs
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        if (order.lines() != null) {
            for (var l : order.lines()) {
                if (l.quantity() != null) totalQty = totalQty.add(l.quantity());
                if (l.lineGrossWeightKg() != null) totalWeight = totalWeight.add(l.lineGrossWeightKg());
            }
        }

        dialog.addKpi("Valor Total", order.totalAmount() + " MT", "Com taxas e impostos", UIHelper.APPROVED_GREEN, "fas-money-bill-wave");
        dialog.addKpi("Volume de Artigos", totalQty.toPlainString(), order.lines() != null ? order.lines().size() + " produtos distintos" : "—", UIHelper.ACCENT_BLUE, "fas-boxes");
        dialog.addKpi("Peso Bruto", totalWeight + " kg", "Massa total estimada", UIHelper.ACCENT_ORANGE, "fas-weight-hanging");

        String originText = (order.quotationNumber() != null && !order.quotationNumber().isBlank())
                ? "Cotação " + order.quotationNumber() : "Venda Direta";
        dialog.addKpi("Origem / Termos", originText, order.paymentTerms() != null ? order.paymentTerms() : "Pronto Pagamento", UIHelper.ACCENT_CYAN, "fas-file-contract");

        // 3. Abas
        dialog.addTab("Itens da Encomenda", "fas-list", UIHelper.ACCENT_BLUE, buildLinesTab(order));
        dialog.addTab("Condições & Entrega", "fas-truck", UIHelper.APPROVED_GREEN, buildLogisticsTab(order));
        dialog.addTab("Impressão & Rastreabilidade", "fas-history", UIHelper.PENDING_YELLOW, buildPrintAuditTab(order));

        // 4. Ações
        ModernButton printBtn = new ModernButton("Imprimir PDF", UIHelper.APPROVED_GREEN, UIHelper.APPROVED_GREEN_HOVER);
        printBtn.setIcon(UIHelper.icon("fas-print", 14, Color.WHITE));
        printBtn.addActionListener(e -> {
            dialog.dispose();
            printWithConfirmation(order);
        });
        dialog.addLeftAction(printBtn);

        dialog.showDialog();
    }

    private JComponent buildLinesTab(OrderDTO order) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UIHelper.BG_DARK);
        p.setBorder(new EmptyBorder(12, 12, 12, 12));

        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Produto", "Lote", "Qtd / Caixas", "Peso kg", "% Qtd", "% Peso", "Preço Unit.", "Total"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        if (order.lines() != null) {
            for (var line : order.lines()) {
                model.addRow(new Object[]{
                        line.productName(),
                        line.batchNumber() == null ? "—" : line.batchNumber(),
                        line.quantity() + " (" + mz.multicore.erp.architecture.quantity.PackageQuantity
                                .label(line.quantity(), line.unitsPerBox()) + ")",
                        line.lineGrossWeightKg(),
                        line.quantityPercentage() + "%",
                        line.weightPercentage() + "%",
                        line.unitPrice() + " MT",
                        line.lineTotal() + " MT"
                });
            }
        }

        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JComponent buildLogisticsTab(OrderDTO order) {
        JPanel p = new JPanel(new GridLayout(2, 2, 14, 14));
        p.setBackground(UIHelper.BG_DARK);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        p.add(buildInfoCard("Termos de Pagamento", "fas-credit-card", UIHelper.ACCENT_BLUE,
                order.paymentTerms() != null ? order.paymentTerms() : "Não especificado"));

        p.add(buildInfoCard("Prazo de Entrega Acordado", "fas-shipping-fast", UIHelper.APPROVED_GREEN,
                order.deliveryTerms() != null ? order.deliveryTerms() : "Entrega standard"));

        String deliveryDateText = order.expectedDeliveryDate() != null
                ? order.expectedDeliveryDate().format(DATE_ONLY) : "Não agendada";
        if (order.deliveryOverdue()) {
            deliveryDateText += " (EM ATRASO)";
        }
        p.add(buildInfoCard("Data de Entrega Prevista", "fas-calendar-alt",
                order.deliveryOverdue() ? UIHelper.REJECTED_RED : UIHelper.ACCENT_CYAN, deliveryDateText));

        String buyer = (order.walkInName() != null && !order.walkInName().isBlank())
                ? order.walkInName() : order.clientName();
        p.add(buildInfoCard("Destinatário / Comprador", "fas-user-check", UIHelper.ACCENT_ORANGE, buyer));

        return p;
    }

    private JComponent buildPrintAuditTab(OrderDTO order) {
        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setBackground(UIHelper.BG_DARK);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        ModernPanel card = new ModernPanel(14);
        card.setLayout(new GridLayout(3, 1, 0, 8));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));

        JLabel l1 = new JLabel("Histórico de Impressão Oficial:");
        l1.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l1.setForeground(UIHelper.TEXT_LIGHT);

        JLabel l2 = new JLabel(order.printCount() > 0
                ? "Esta encomenda já foi impressa " + order.printCount() + " vez(es)."
                : "Esta encomenda ainda não foi impressa.");
        l2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l2.setForeground(order.printCount() > 0 ? UIHelper.PENDING_YELLOW : UIHelper.APPROVED_GREEN);

        String printDetails = order.printedAt() != null
                ? "Última impressão: " + order.printedAt().format(DATE_TIME) + (order.lastPrintedBy() != null ? " por " + order.lastPrintedBy() : "")
                : "Sem registo de emissão em papel.";
        JLabel l3 = new JLabel(printDetails);
        l3.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l3.setForeground(UIHelper.TEXT_MUTED);

        card.add(l1);
        card.add(l2);
        card.add(l3);

        p.add(card, BorderLayout.NORTH);
        return p;
    }

    private ModernPanel buildInfoCard(String title, String icon, Color iconColor, String content) {
        ModernPanel p = new ModernPanel(12);
        p.setLayout(new BorderLayout(0, 6));
        p.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        top.add(new JLabel(UIHelper.icon(icon, 15, iconColor)));
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.setForeground(UIHelper.TEXT_MUTED);
        top.add(t);

        JLabel c = new JLabel("<html><body style='width:240px;'>" + content + "</body></html>");
        c.setFont(new Font("Segoe UI", Font.BOLD, 14));
        c.setForeground(UIHelper.TEXT_LIGHT);

        p.add(top, BorderLayout.NORTH);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private void printWithConfirmation(OrderDTO order) {
        if (order.printCount() > 0) {
            String last = order.printedAt() == null ? "—" : new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm")
                    .format(Date.from(order.printedAt().atZone(ZoneId.systemDefault()).toInstant()));
            int answer = JOptionPane.showConfirmDialog(owner,
                    String.format("Esta encomenda já foi impressa %d vez(es) (última em %s%s).%n%nTem a certeza que pretende imprimir novamente?",
                            order.printCount(), last, order.lastPrintedBy() != null ? " por " + order.lastPrintedBy() : ""),
                    "Confirmar reimpressão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) return;
        }
        String username = CurrentUserContext.getUsername();
        UIHelper.runWithProgress(owner, "A gerar encomenda em PDF…", () -> {
            byte[] pdf = apiClient.renderOrder(order.id());
            apiClient.markOrderPrinted(order.id(), username);
            return pdf;
        }, pdf -> {
            PrintPreviewDialog.show(owner, pdf, "encomenda-" + order.orderNumber());
            afterPrint.run();
        }, error -> showError("Não foi possível gerar a encomenda em PDF", error));
    }

    private void showError(String action, Throwable error) {
        if (owner instanceof ComercialPanel panel) panel.showCommercialNotice(FeedbackType.ERROR, action, error.getMessage());
    }
}
