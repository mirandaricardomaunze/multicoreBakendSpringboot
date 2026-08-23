package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/** Apresentação e impressão de uma encomenda, sem regras de negócio locais. */
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

    private void show(OrderDTO order) {
        StringBuilder header = new StringBuilder("<html><body style='font-family:sans-serif;'>")
                .append("<b>Nº Encomenda:</b> ").append(order.orderNumber()).append("<br>")
                .append("<b>Cliente:</b> ").append(order.clientName());
        if (order.walkInName() != null && !order.walkInName().isBlank()) {
            header.append(" <i>(comprador: ").append(order.walkInName()).append(")</i>");
        }
        header.append("<br><b>Data:</b> ")
                .append(order.createdAt() != null ? order.createdAt().format(DATE_TIME) : "—")
                // Rótulo PT-MZ vindo do servidor — nunca a constante interna.
                .append("<br><b>Estado:</b> ").append(order.statusLabel())
                .append("<br><b>Total:</b> ").append(order.totalAmount()).append(" MT");
        if (order.quotationNumber() != null && !order.quotationNumber().isBlank()) {
            header.append("<br><b>Origem:</b> Cotação ").append(order.quotationNumber());
        }
        if (order.paymentTerms() != null && !order.paymentTerms().isBlank()) {
            header.append("<br><b>Pagamento:</b> ").append(order.paymentTerms());
        }
        if (order.deliveryTerms() != null && !order.deliveryTerms().isBlank()) {
            header.append("<br><b>Prazo de entrega:</b> ").append(order.deliveryTerms());
        }
        if (order.expectedDeliveryDate() != null) {
            header.append("<br><b>Entrega prevista:</b> ")
                    .append(order.expectedDeliveryDate().format(DATE_ONLY));
            if (order.deliveryOverdue()) {
                header.append(" <b>(em atraso)</b>");
            }
        }
        header.append("</body></html>");

        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Produto", "Lote", "Qtd / Caixas", "Peso kg", "% Qtd", "% Peso", "Preço", "Total"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        for (var line : order.lines()) {
            model.addRow(new Object[]{line.productName(), line.batchNumber() == null ? "—" : line.batchNumber(),
                    line.quantity() + " (" + mz.multicore.erp.architecture.quantity.PackageQuantity
                            .label(line.quantity(), line.unitsPerBox()) + ")",
                    line.lineGrossWeightKg(), line.quantityPercentage() + "%", line.weightPercentage() + "%",
                    line.unitPrice() + " MT", line.lineTotal() + " MT"});
        }
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(660, 200));

        JLabel printStatus = order.printCount() > 0
                ? new JLabel(String.format("<html><body style='color:#d97706;font-weight:bold;'>Já impressa %d vez(es). Última: %s%s</body></html>",
                        order.printCount(), order.printedAt() != null ? order.printedAt().format(DATE_TIME) : "—",
                        order.lastPrintedBy() != null ? " por " + order.lastPrintedBy() : ""))
                : new JLabel("<html><body style='color:#16a34a;'>Ainda não foi impressa.</body></html>");

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.add(new JLabel(header.toString()), BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        content.add(printStatus, BorderLayout.SOUTH);
        int choice = JOptionPane.showOptionDialog(owner, content, "Detalhes da Encomenda " + order.orderNumber(),
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new String[]{"Imprimir", "Fechar"}, "Fechar");
        if (choice == 0) printWithConfirmation(order);
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
            mz.multicore.erp.modules.printing.PdfFileSaver.saveAndOpen(pdf, "encomenda-" + order.orderNumber());
            afterPrint.run();
        }, error -> showError("Não foi possível gerar a encomenda em PDF", error));
    }

    private void showError(String action, Throwable error) {
        JOptionPane.showMessageDialog(owner, action + ": " + error.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
