package com.phcpro.gui.commercial;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.desktop.client.ComercialApiClient;
import com.phcpro.gui.components.*;
import com.phcpro.modules.comercial.dto.ReceiptDTO;
import com.phcpro.modules.financeira.dto.TreasuryAccountDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Lista e anulação de recibos comerciais. */
public final class ReceiptsPanel extends JPanel {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ComercialApiClient apiClient;
    private final Runnable invoicesRefresh;
    private final DefaultTableModel model;
    private final JTable table;

    public ReceiptsPanel(ComercialApiClient apiClient, Runnable invoicesRefresh) {
        this.apiClient = apiClient;
        this.invoicesRefresh = invoicesRefresh;
        setLayout(new BorderLayout(0, 15));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(15, 15, 15, 15));
        add(UIHelper.createHeading("Recibos Emitidos (Liquidações)"), BorderLayout.NORTH);
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));
        model = new DefaultTableModel(
                new String[]{"ID", "Nº Recibo", "Fatura", "Cliente", "Montante Pago", "Método Pag.", "Estado", "Data"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.status());
        hideColumn(0);
        JTextField search = TableFilter.searchField("Nº recibo, fatura ou cliente…");
        JComboBox<String> method = TableFilter.combo("Todos os métodos", "CASH", "BANK_TRANSFER", "CARD");
        JComboBox<String> status = TableFilter.combo("Todos os estados", "COMPLETED", "CANCELLED");
        JComboBox<String> period = TableFilter.periodCombo();
        TableFilter.install(table, search,
                List.of(new TableFilter.ColumnFilter(method, 5), new TableFilter.ColumnFilter(status, 6)),
                List.of(new TableFilter.PeriodFilter(period, 7)));
        JPanel filters = TableFilter.bar(search, TableFilter.label("Método:"), method,
                TableFilter.label("Estado:"), status, TableFilter.label("Data:", "fas-calendar-alt"), period);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(filters, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        ModernButton cancel = UIHelper.createDangerButton("Anular Recibo");
        cancel.setIcon(UIHelper.icon("fas-ban", 14));
        cancel.addActionListener(e -> cancelSelected());
        ModernButton refresh = UIHelper.createSecondaryButton("Atualizar");
        refresh.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refresh.addActionListener(e -> refresh());
        actions.add(cancel); actions.add(refresh);
        card.add(actions, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    public void openPayment(Long invoiceId, String invoiceNumber, BigDecimal invoiceTotal,
                            List<TreasuryAccountDTO> accounts) {
        if (accounts.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Não existem contas de tesouraria registadas.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JComboBox<String> accountCombo = new JComboBox<>();
        UIHelper.styleComboBox(accountCombo);
        accounts.forEach(account -> accountCombo.addItem(account.name() + " (" + account.balance() + " MT)"));
        JComboBox<String> methodCombo = new JComboBox<>(
                new String[]{"DINHEIRO", "TRANSFERÊNCIA", "M-PESA", "CARTÃO"});
        UIHelper.styleComboBox(methodCombo);
        MoneyField amountField = new MoneyField(invoiceTotal.toString());
        JLabel invoiceLabel = new JLabel(invoiceNumber);
        invoiceLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        invoiceLabel.setForeground(UIHelper.TEXT_LIGHT);
        JPanel form = UIHelper.createDialogForm("Fatura:", invoiceLabel,
                "Conta de Tesouraria:", accountCombo, "Método de Pagamento:", methodCombo,
                "Montante a Receber (MT):", amountField);
        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow,
                "Registar Recebimento (Emitir Recibo)", "fas-receipt",
                "Recebimento de cliente e emissão de recibo", form)
                .setConfirmButton("Receber", "fas-money-bill-wave").showDialog();
        if (!confirmed || accountCombo.getSelectedIndex() < 0) return;
        try {
            BigDecimal amount = amountField.value();
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                JOptionPane.showMessageDialog(this, "O valor pago deve ser maior que zero.",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Long accountId = accounts.get(accountCombo.getSelectedIndex()).id();
            String method = String.valueOf(methodCombo.getSelectedItem());
            UIHelper.runWithProgress(this, "A emitir recibo…",
                    () -> apiClient.createReceipt(invoiceId, accountId, method, amount), ignored -> {
                        BigDecimal remaining = invoiceTotal.subtract(amount);
                        String message = remaining.compareTo(BigDecimal.ZERO) > 0
                                ? "Recibo emitido. Continuam por receber " + remaining + " MT desta fatura."
                                : "Fatura liquidada com sucesso! Recibo emitido.";
                        JOptionPane.showMessageDialog(this, message, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                        invoicesRefresh.run();
                        refresh();
                    }, error -> JOptionPane.showMessageDialog(this,
                            "Não foi possível emitir o recibo: " + error.getMessage(),
                            "Erro", JOptionPane.ERROR_MESSAGE));
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refresh() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> apiClient.getReceiptsByCompany(companyId), this::apply,
                error -> showError("carregar recibos", error));
    }

    private void apply(List<ReceiptDTO> receipts) {
        model.setRowCount(0);
        for (ReceiptDTO receipt : receipts) model.addRow(new Object[]{receipt.id(), receipt.receiptNumber(),
                receipt.invoiceNumber(), receipt.clientName(), receipt.amountPaid(), receipt.paymentMethod(),
                receipt.status(), receipt.receiptDate().format(DATE_TIME)});
    }

    private void cancelSelected() {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um recibo na tabela para anular.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long id = (Long) model.getValueAt(row, 0);
        String number = String.valueOf(model.getValueAt(row, 1));
        String reason = UIHelper.promptRequiredText("Anular Recibo", "fas-ban", "Recibo " + number,
                "Motivo da anulação:");
        if (reason == null) return;
        UIHelper.runWithProgress(this, "A anular recibo…", () -> {
            apiClient.cancelReceipt(id, reason);
            return null;
        }, ignored -> {
            JOptionPane.showMessageDialog(this,
                    "Recibo " + number + " anulado com sucesso. O estado da fatura foi actualizado.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            invoicesRefresh.run();
            refresh();
        }, error -> showError("anular recibo", error));
    }

    private void hideColumn(int index) {
        table.getColumnModel().getColumn(index).setMinWidth(0);
        table.getColumnModel().getColumn(index).setMaxWidth(0);
        table.getColumnModel().getColumn(index).setWidth(0);
    }

    private void showError(String action, Throwable error) {
        JOptionPane.showMessageDialog(this, "Não foi possível " + action + ": " + error.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
