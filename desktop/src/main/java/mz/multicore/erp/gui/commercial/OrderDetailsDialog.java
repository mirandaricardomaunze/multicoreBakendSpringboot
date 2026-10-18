package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
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
    private final PrintApiClient printApiClient;
    private final Runnable afterPrint;
    private boolean loadingOrOpen = false;

    public OrderDetailsDialog(JComponent owner, ComercialApiClient apiClient, Runnable afterPrint) {
        this(owner, apiClient, null, afterPrint);
    }

    public OrderDetailsDialog(JComponent owner, ComercialApiClient apiClient,
                              PrintApiClient printApiClient, Runnable afterPrint) {
        this.owner = owner;
        this.apiClient = apiClient;
        this.printApiClient = printApiClient;
        this.afterPrint = afterPrint;
    }

    public void open(Long orderId) {
        if (loadingOrOpen) return;
        loadingOrOpen = true;
        UIHelper.loadAsync(owner, () -> apiClient.getOrderById(orderId), order -> {
            try {
                show(order);
            } finally {
                loadingOrOpen = false;
            }
        }, error -> {
            loadingOrOpen = false;
            showError("Não foi possível carregar os detalhes da encomenda", error);
        });
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
            case "BILLED", "INVOICED", "FATURADO", "FACTURADO" -> ExecutiveDetailDialog.StatusSeverity.SUCCESS;
            case "CONFIRMED", "APPROVED" -> ExecutiveDetailDialog.StatusSeverity.INFO;
            case "PENDING" -> ExecutiveDetailDialog.StatusSeverity.WARNING;
            case "CANCELLED" -> ExecutiveDetailDialog.StatusSeverity.DANGER;
            default -> ExecutiveDetailDialog.StatusSeverity.NEUTRAL;
        };
        String rawStatus = order.statusLabel() != null && !order.statusLabel().isBlank()
                ? order.statusLabel()
                : order.status();
        dialog.setStatusBadge(UIHelper.humanStatus(rawStatus), severity);

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
        UIHelper.ensureHeadersFit(table);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JComponent buildLogisticsTab(OrderDTO order) {
        String deliveryDateText = order.expectedDeliveryDate() != null
                ? order.expectedDeliveryDate().format(DATE_ONLY) : "Não agendada";
        if (order.deliveryOverdue()) {
            deliveryDateText += " [EM ATRASO]";
        }

        String buyer = (order.walkInName() != null && !order.walkInName().isBlank())
                ? order.walkInName() : "Não aplicável";

        String quotation = (order.quotationNumber() != null && !order.quotationNumber().isBlank())
                ? order.quotationNumber() : "Venda Direta";

        java.util.List<RecordDetailsDialog.PropertyItem> items = java.util.List.of(
                new RecordDetailsDialog.PropertyItem("Termos de Pagamento", order.paymentTerms() != null ? order.paymentTerms() : "Pronto Pagamento"),
                new RecordDetailsDialog.PropertyItem("Condições de Entrega Acordadas", order.deliveryTerms() != null ? order.deliveryTerms() : "Entrega standard"),
                new RecordDetailsDialog.PropertyItem("Data de Entrega Prevista", deliveryDateText),
                new RecordDetailsDialog.PropertyItem("Cliente Titular", order.clientName() != null ? order.clientName() : "—"),
                new RecordDetailsDialog.PropertyItem("Destinatário / Comprador no Balcão", buyer),
                new RecordDetailsDialog.PropertyItem("Cotação de Origem", quotation)
        );

        return RecordDetailsDialog.buildPropertySheet(items);
    }

    private JComponent buildPrintAuditTab(OrderDTO order) {
        String printStatus = order.printCount() > 0
                ? "Documento impresso " + order.printCount() + " vez(es)"
                : "Sem registo de emissão em papel";

        String lastPrint = order.printedAt() != null
                ? order.printedAt().format(DATE_TIME)
                : "—";

        java.util.List<RecordDetailsDialog.PropertyItem> items = java.util.List.of(
                new RecordDetailsDialog.PropertyItem("Estado de Emissão Física", printStatus),
                new RecordDetailsDialog.PropertyItem("Contador de Impressões Oficiais", String.valueOf(order.printCount())),
                new RecordDetailsDialog.PropertyItem("Data da Última Impressão", lastPrint),
                new RecordDetailsDialog.PropertyItem("Último Utilizador a Imprimir", order.lastPrintedBy() != null ? order.lastPrintedBy() : "—"),
                new RecordDetailsDialog.PropertyItem("Armazém de Expedição", order.warehouseName() != null ? order.warehouseName() : "—"),
                new RecordDetailsDialog.PropertyItem("Via / Natureza do Documento", order.kindLabel() != null ? order.kindLabel() : "Encomenda Formal")
        );

        return RecordDetailsDialog.buildPropertySheet(items);
    }

    private void printWithConfirmation(OrderDTO order) {
        if (printApiClient == null) {
            UIHelper.runWithProgress(owner, "A gerar encomenda em PDF…", () -> apiClient.renderOrder(order.id()),
                    pdf -> PrintPreviewDialog.show(owner, pdf, "encomenda-" + order.orderNumber()),
                    error -> showError("Não foi possível gerar a encomenda em PDF", error));
            return;
        }
        SensitivePrintCoordinator.print(owner, printApiClient,
                mz.multicore.erp.modules.printing.dto.SensitiveDocumentType.ORDER,
                order.id(), "Encomenda " + order.orderNumber(),
                "encomenda-" + order.orderNumber(), () -> apiClient.renderOrder(order.id()), afterPrint);
    }

    private void showError(String action, Throwable error) {
        if (owner instanceof ComercialPanel panel) panel.showCommercialNotice(FeedbackType.ERROR, action, error.getMessage());
    }
}
