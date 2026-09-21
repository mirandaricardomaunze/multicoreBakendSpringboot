package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.AgingBucketTotalDTO;
import mz.multicore.erp.modules.comercial.dto.AgingSummaryDTO;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.model.AgingBucket;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.pos.dto.PosPaymentRequest;

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
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
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
        ModernButton refresh = UIHelper.createSecondaryButton("Actualizar");
        refresh.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refresh.addActionListener(e -> refresh());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(refresh);
        actions.add(pay);
        header.add(actions, BorderLayout.EAST);
        JPanel north = new JPanel(); north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        header.setAlignmentX(Component.LEFT_ALIGNMENT); feedback.setAlignmentX(Component.LEFT_ALIGNMENT);
        north.add(header); north.add(feedback); add(north, BorderLayout.NORTH);

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
        UIHelper.ensureHeadersFit(table);
        // O rotulo mais longo da antiguidade — "Corrente (por vencer)" — nao cabia e saia
        // "Corrente (por …", que e o mesmo que nao dizer nada: todos os escaloes comecam por
        // "Corrente" ou por um numero.
        UIHelper.ensureColumnFits(table, 9, "Corrente (por vencer)");
        // E o estado, pela mesma razao: "Parcialmente paga" e o rotulo mais longo, e distingui-lo
        // de "Paga" e o que a coluna existe para fazer. O espaco sai do Cliente, que e texto livre
        // e vai truncar de qualquer maneira — ali a ficha do cliente resolve; aqui nao ha para onde ir.
        UIHelper.ensureColumnFits(table, 10, "Parcialmente paga");
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
                    // Data e nao data-hora: a coluna nao tem largura para as duas e via-se
                    // "29/08/2026 22…" — a hora a comer a data numa tabela de dividas, onde a hora
                    // de emissao nao decide nada e a data decide tudo.
                    invoice.createdAt() == null ? "-" : invoice.createdAt().format(DATE),
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
            showNotice(FeedbackType.WARNING, "Seleccione uma fatura", "Escolha uma fatura na tabela para continuar.");
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
                ToastManager.success(this, "Pagamento registado com sucesso.");
                refresh();
            }, error -> showError("registar pagamento", error));
        } catch (IllegalArgumentException error) {
            showNotice(FeedbackType.ERROR, "Dados de pagamento inválidos", error.getMessage());
        }
    }

    private void showError(String action, Throwable error) {
        showNotice(FeedbackType.ERROR, "Não foi possível " + action, error.getMessage());
    }

    private void showNotice(FeedbackType type, String title, String message) {
        feedback.show(type, title, message, null, null);
    }
}
