package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Diálogo pesquisável para faturar uma encomenda pendente. */
public final class BillOrderDialog {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final JComponent owner;
    private final ComercialApiClient apiClient;
    private final Runnable invoicesRefresh;
    private final Runnable ordersRefresh;

    public BillOrderDialog(JComponent owner, ComercialApiClient apiClient,
                           Runnable invoicesRefresh, Runnable ordersRefresh) {
        this.owner = owner;
        this.apiClient = apiClient;
        this.invoicesRefresh = invoicesRefresh;
        this.ordersRefresh = ordersRefresh;
    }

    public void open() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> apiClient.getPendingOrdersByCompany(companyId),
                loaded -> show(new ArrayList<>(loaded)), error -> showError("carregar encomendas pendentes", error));
    }

    private void show(List<OrderDTO> orders) {
        if (orders.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Não há encomendas pendentes para faturar.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JTextField search = new JTextField();
        UIHelper.styleTextField(search);
        JComboBox<String> combo = new JComboBox<>();
        UIHelper.styleComboBox(combo);
        DefaultTableModel lines = new DefaultTableModel(
                new String[]{"Produto", "Lote", "Qtd", "Preço Unit.", "Total"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(lines);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.money());
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        JLabel number = value("—");
        JLabel client = value("—");
        JLabel date = value("—");
        JLabel total = value("0,00 MT");
        total.setFont(new Font(UIHelper.FONT, Font.BOLD, 24));
        total.setForeground(UIHelper.APPROVED_GREEN);
        Runnable preview = () -> {
            lines.setRowCount(0);
            int index = combo.getSelectedIndex();
            if (index < 0 || index >= orders.size()) return;
            OrderDTO order = orders.get(index);
            number.setText(order.orderNumber());
            String walkIn = order.walkInName() == null || order.walkInName().isBlank() ? "" : " (" + order.walkInName() + ")";
            client.setText(order.clientName() + walkIn);
            date.setText(order.createdAt() == null ? "—" : order.createdAt().format(DATE_TIME));
            total.setText(String.format("%,.2f MT", order.totalAmount()));
            order.lines().forEach(line -> lines.addRow(new Object[]{line.productName(),
                    line.batchNumber() == null ? "—" : line.batchNumber(), line.quantity(), line.unitPrice(), line.lineTotal()}));
        };
        combo.addActionListener(e -> preview.run());
        Runnable rebuild = () -> {
            combo.removeAllItems();
            for (OrderDTO order : orders) combo.addItem(order.orderNumber() + " — " + order.clientName());
            if (!orders.isEmpty()) combo.setSelectedIndex(0);
            preview.run();
        };
        UIHelper.onTextChange(search, () -> {
            String query = search.getText();
            UIHelper.loadAsync(owner, () -> apiClient.searchPendingOrders(query), loaded -> {
                orders.clear(); orders.addAll(loaded); rebuild.run();
            }, error -> showError("pesquisar encomendas", error));
        });
        rebuild.run();
        JPanel selector = UIHelper.createDialogForm("Pesquisar (nº ou cliente):", search, "Encomenda a faturar:", combo);
        ModernPanel summary = new ModernPanel(14);
        summary.setLayout(new GridLayout(2, 4, 12, 4));
        summary.setBorder(new EmptyBorder(14, 16, 14, 16));
        summary.add(label("Encomenda")); summary.add(label("Cliente")); summary.add(label("Data")); summary.add(label("Total"));
        summary.add(number); summary.add(client); summary.add(date); summary.add(total);
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBackground(UIHelper.BG_DARK);
        content.add(selector, BorderLayout.NORTH);
        content.add(summary, BorderLayout.CENTER);
        content.add(scroll, BorderLayout.SOUTH);
        int choice = JOptionPane.showConfirmDialog(owner, content, "Faturar Encomenda",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION || combo.getSelectedIndex() < 0) return;
        OrderDTO selected = orders.get(combo.getSelectedIndex());
        UIHelper.runWithProgress(owner, "A faturar encomenda…", () -> apiClient.billOrder(selected.id()),
                invoice -> success(selected, invoice), error -> showError("faturar encomenda", error));
    }

    private void success(OrderDTO order, InvoiceDTO invoice) {
        JOptionPane.showMessageDialog(owner,
                "Fatura " + invoice.invoiceNumber() + " emitida a partir da encomenda " + order.orderNumber() + ".",
                "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        invoicesRefresh.run();
        ordersRefresh.run();
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text); label.setForeground(UIHelper.TEXT_MUTED); return label;
    }
    private static JLabel value(String text) {
        JLabel label = new JLabel(text); label.setForeground(UIHelper.TEXT_LIGHT);
        label.setFont(new Font(UIHelper.FONT, Font.BOLD, 13)); return label;
    }
    private void showError(String action, Throwable error) {
        JOptionPane.showMessageDialog(owner, "Não foi possível " + action + ": " + error.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
