package com.phcpro.gui.commercial;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.desktop.client.ComercialApiClient;
import com.phcpro.desktop.client.FinanceApiClient;
import com.phcpro.desktop.client.POSApiClient;
import com.phcpro.gui.components.*;
import com.phcpro.modules.comercial.dto.AgingBucketTotalDTO;
import com.phcpro.modules.comercial.dto.AgingSummaryDTO;
import com.phcpro.modules.comercial.dto.InvoiceDTO;
import com.phcpro.modules.comercial.model.AgingBucket;
import com.phcpro.modules.financeira.dto.TreasuryAccountDTO;
import com.phcpro.modules.pos.dto.PosPaymentRequest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Caso de uso autónomo de contas correntes e recebimentos tardios. */
public final class OutstandingAccountsPanel extends JPanel {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ComercialApiClient comercialApiClient;
    private final FinanceApiClient financeApiClient;
    private final POSApiClient posApiClient;
    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel agingSummary;
    private List<InvoiceDTO> invoices = new ArrayList<>();

    public OutstandingAccountsPanel(ComercialApiClient comercialApiClient,
                                    FinanceApiClient financeApiClient,
                                    POSApiClient posApiClient) {
        this.comercialApiClient = comercialApiClient;
        this.financeApiClient = financeApiClient;
        this.posApiClient = posApiClient;
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Contas Correntes — Faturas com Saldo em Dívida"), BorderLayout.WEST);
        ModernButton pay = UIHelper.createSuccessButton("Receber Pagamento");
        pay.setIcon(UIHelper.icon("fas-money-bill-wave", 14));
        pay.addActionListener(e -> receivePayment());
        ModernButton refresh = UIHelper.createSecondaryButton("Atualizar");
        refresh.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refresh.addActionListener(e -> refresh());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(refresh);
        actions.add(pay);
        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        model = new DefaultTableModel(
                new String[]{"Nº Fatura", "Data", "Cliente", "NUIT", "Total", "Pago", "Em Dívida",
                        "Vencimento", "Dias em Atraso", "Antiguidade", "Estado"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        for (int column : new int[]{4, 5, 6}) table.getColumnModel().getColumn(column).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(10).setCellRenderer(TableCellRenderers.status());
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        JTextField search = TableFilter.searchField("Nº fatura, cliente ou NUIT…");
        JComboBox<String> status = TableFilter.combo("Todos os estados", "APPROVED", "PARTIALLY_PAID");
        JComboBox<String> aging = TableFilter.combo(agingLabels());
        JComboBox<String> period = TableFilter.periodCombo();
        TableFilter.install(table, search,
                List.of(new TableFilter.ColumnFilter(status, 10), new TableFilter.ColumnFilter(aging, 9)),
                List.of(new TableFilter.PeriodFilter(period, 1)));
        JPanel filters = TableFilter.bar(search, TableFilter.label("Estado:"), status,
                TableFilter.label("Antiguidade:", "fas-hourglass-half"), aging,
                TableFilter.label("Data:", "fas-calendar-alt"), period);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(filters, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        agingSummary = new JLabel(" ");
        agingSummary.setForeground(UIHelper.TEXT_LIGHT);
        agingSummary.setBorder(new EmptyBorder(10, 2, 0, 2));
        card.add(agingSummary, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    /** Opções do dropdown: índice 0 é "sem filtro" (contrato do {@link TableFilter.ColumnFilter}). */
    private static String[] agingLabels() {
        AgingBucket[] values = AgingBucket.values();
        String[] labels = new String[values.length + 1];
        labels[0] = "Toda a antiguidade";
        for (int i = 0; i < values.length; i++) labels[i + 1] = values[i].label();
        return labels;
    }

    public void refresh() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> comercialApiClient.getOutstandingInvoicesByCompany(companyId), this::apply,
                error -> showError("carregar contas correntes", error));
        // O resumo por escalão vem do servidor (mesma regra, uma só porta) e é acessório: se
        // falhar, a tabela continua utilizável — só fica sem a linha de totais.
        UIHelper.loadAsync(this, comercialApiClient::getReceivablesAging, this::applyAging,
                error -> agingSummary.setText(" "));
    }

    private void apply(List<InvoiceDTO> loaded) {
        invoices = loaded;
        model.setRowCount(0);
        for (InvoiceDTO invoice : invoices) {
            BigDecimal paid = invoice.amountPaid() == null ? BigDecimal.ZERO : invoice.amountPaid();
            model.addRow(new Object[]{invoice.invoiceNumber(),
                    invoice.createdAt() == null ? "-" : invoice.createdAt().format(DATE_TIME),
                    invoice.clientName(), invoice.clientTaxId(), invoice.totalAmount(), paid,
                    invoice.outstandingAmount(),
                    invoice.dueDate() == null ? "-" : invoice.dueDate().format(DATE),
                    invoice.daysOverdue() == 0 ? "—" : invoice.daysOverdue(),
                    invoice.agingBucket() == null ? "-" : invoice.agingBucket().label(),
                    invoice.status().name()});
        }
    }

    private void applyAging(AgingSummaryDTO summary) {
        if (summary == null) { agingSummary.setText(" "); return; }
        StringBuilder text = new StringBuilder("<html><b>Antiguidade a ")
                .append(summary.referenceDate().format(DATE)).append(":</b> ");
        for (AgingBucketTotalDTO bucket : summary.buckets()) {
            text.append(String.format("&nbsp;%s: <b>%,.2f MT</b> (%d)&nbsp;·", bucket.label(),
                    bucket.amount(), bucket.invoiceCount()));
        }
        text.append(String.format("&nbsp;&nbsp;<b>Em atraso: %,.2f MT</b> de %,.2f MT",
                summary.overdueTotal(), summary.total()));
        agingSummary.setText(text.append("</html>").toString());
    }

    private InvoiceDTO selected() {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0 || row >= invoices.size()) {
            JOptionPane.showMessageDialog(this, "Selecione uma fatura na tabela.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return invoices.get(row);
    }

    private void receivePayment() {
        InvoiceDTO invoice = selected();
        if (invoice == null) return;
        UIHelper.loadAsync(this, financeApiClient::getAllAccounts,
                accounts -> showPaymentDialog(invoice, accounts), error -> showError("carregar contas de tesouraria", error));
    }

    private void showPaymentDialog(InvoiceDTO invoice, List<TreasuryAccountDTO> accounts) {
        BigDecimal paid = invoice.amountPaid() == null ? BigDecimal.ZERO : invoice.amountPaid();
        BigDecimal outstanding = invoice.totalAmount().subtract(paid);
        JComboBox<String> method = new JComboBox<>(new String[]{"CASH", "CARD", "BANK_TRANSFER"});
        UIHelper.styleComboBox(method);
        JComboBox<String> account = new JComboBox<>();
        for (TreasuryAccountDTO item : accounts) account.addItem(item.name());
        UIHelper.styleComboBox(account);
        MoneyField amount = new MoneyField(outstanding.toPlainString());
        JTextField reference = new JTextField();
        UIHelper.styleTextField(reference);
        JLabel info = new JLabel(String.format(
                "<html><b>Fatura:</b> %s · <b>Cliente:</b> %s<br><b>Total:</b> %,.2f MT &nbsp; " +
                        "<b>Pago:</b> %,.2f MT &nbsp; <b>Em dívida:</b> %,.2f MT</html>",
                invoice.invoiceNumber(), invoice.clientName(), invoice.totalAmount(), paid, outstanding));
        info.setForeground(UIHelper.TEXT_LIGHT);
        JPanel form = UIHelper.createDialogForm("Resumo:", info, "Método:", method,
                "Conta de Tesouraria:", account, "Valor a Receber (MT):", amount,
                "Referência (Nº recibo/transação):", reference);
        if (!new ModernFormDialog(UIHelper.mainWindow, "Receber Pagamento — " + invoice.invoiceNumber(),
                "fas-money-bill-wave", "Liquidação de fatura em dívida", form)
                .setConfirmButton("Receber", "fas-money-bill-wave").showDialog()) return;
        try {
            BigDecimal value = amount.value();
            if (value.signum() <= 0) throw new IllegalArgumentException("O valor deve ser maior que zero.");
            Long accountId = accounts.isEmpty() ? null : accounts.get(account.getSelectedIndex()).id();
            PosPaymentRequest request = new PosPaymentRequest(String.valueOf(method.getSelectedItem()), value, value,
                    reference.getText().trim().isEmpty() ? null : reference.getText().trim(), accountId);
            UIHelper.runWithProgress(this, "A registar pagamento…", () -> {
                posApiClient.registerLatePayment(invoice.id(), request);
                return null;
            }, ignored -> {
                JOptionPane.showMessageDialog(this, "Pagamento registado com sucesso.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                refresh();
            }, error -> showError("registar pagamento", error));
        } catch (IllegalArgumentException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showError(String action, Throwable error) {
        JOptionPane.showMessageDialog(this, "Não foi possível " + action + ": " + error.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
