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
    private JComboBox<String> businessUnit;
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

        ModernButton pay = UIHelper.createSuccessButton("Receber Pagamento");
        pay.setIcon(UIHelper.icon("fas-money-bill-wave", 14));
        pay.addActionListener(e -> receivePayment());
        ModernButton refresh = UIHelper.createSecondaryButton("Actualizar");
        refresh.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refresh.addActionListener(e -> refresh());
        add(feedback, BorderLayout.NORTH);
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        model = new DefaultTableModel(
                new String[]{"Nº Fatura", "Data", "Negócio", "Cliente", "NUIT", "Total", "Pago", "Em Dívida",
                        "Vencimento", "Dias em Atraso", "Antiguidade", "Estado"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        // O rotulo mais longo da antiguidade — "Corrente (por vencer)" — nao cabia e saia
        // "Corrente (por …", que e o mesmo que nao dizer nada: todos os escaloes comecam por
        // "Corrente" ou por um numero.
        UIHelper.ensureColumnFits(table, 10, "Corrente (por vencer)");
        // E o estado, pela mesma razao: "Parcialmente paga" e o rotulo mais longo, e distingui-lo
        // de "Paga" e o que a coluna existe para fazer. O espaco sai do Cliente, que e texto livre
        // e vai truncar de qualquer maneira — ali a ficha do cliente resolve; aqui nao ha para onde ir.
        UIHelper.ensureColumnFits(table, 11, "Parcialmente paga");
        for (int column : new int[]{5, 6, 7}) table.getColumnModel().getColumn(column).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(11).setCellRenderer(TableCellRenderers.status());
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        JTextField search = TableFilter.searchField("Nº fatura, cliente ou NUIT…");
        search.setPreferredSize(new Dimension(160, UIHelper.FORM_CONTROL_HEIGHT));

        businessUnit = TableFilter.combo("Todos os negócios");
        UIHelper.styleComboBox(businessUnit);
        businessUnit.setPreferredSize(new Dimension(135, UIHelper.FORM_CONTROL_HEIGHT));

        JComboBox<String> status = TableFilter.combo("Todos os estados", "Aprovado", "Parcialmente paga");
        UIHelper.styleComboBox(status);
        status.setPreferredSize(new Dimension(130, UIHelper.FORM_CONTROL_HEIGHT));

        JComboBox<String> aging = TableFilter.combo(agingLabels());
        UIHelper.styleComboBox(aging);
        aging.setPreferredSize(new Dimension(145, UIHelper.FORM_CONTROL_HEIGHT));

        JComboBox<String> period = TableFilter.periodCombo();
        UIHelper.styleComboBox(period);
        period.setPreferredSize(new Dimension(120, UIHelper.FORM_CONTROL_HEIGHT));

        TableFilter.install(table, search,
                List.of(
                        new TableFilter.ColumnFilter(businessUnit, 2),
                        new TableFilter.ColumnFilter(status, 11),
                        new TableFilter.ColumnFilter(aging, 10)
                ),
                List.of(new TableFilter.PeriodFilter(period, 1)));

        ModernButton peekToggleBtn = UIHelper.createSecondaryButton("");
        peekToggleBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekToggleBtn.setToolTipText("Espreitar detalhes da conta (Espaço)");
        peekToggleBtn.getAccessibleContext().setAccessibleName("Espreitar conta");
        peekToggleBtn.setPreferredSize(new Dimension(30, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel filters = TableFilter.bar(
                search,
                TableFilter.label("Negócio:"), businessUnit,
                TableFilter.label("Estado:"), status,
                TableFilter.label("Antiguidade:"), aging,
                TableFilter.label("Período:"), period,
                peekToggleBtn
        );
        filters.setBorder(new EmptyBorder(0, 0, 8, 0));
        agingSummary = new JLabel(" ");
        agingSummary.setForeground(UIHelper.TEXT_LIGHT);
        agingSummary.setBorder(new EmptyBorder(4, 2, 0, 2));
        JPanel filterHeader = new JPanel(new BorderLayout(0, 4));
        filterHeader.setOpaque(false);
        filterHeader.add(filters, BorderLayout.NORTH);
        filterHeader.add(agingSummary, BorderLayout.SOUTH);

        UIHelper.ensureHeadersFit(table);
        TableQuickPeekController peekController = TableQuickPeekController.install(table, card, (peek, mr) -> {
            if (mr < 0 || mr >= model.getRowCount()) return;
            String num = String.valueOf(model.getValueAt(mr, 0));
            String date = String.valueOf(model.getValueAt(mr, 1));
            String bu = String.valueOf(model.getValueAt(mr, 2));
            String client = String.valueOf(model.getValueAt(mr, 3));
            String nuit = String.valueOf(model.getValueAt(mr, 4));
            String total = String.valueOf(model.getValueAt(mr, 5));
            String paid = String.valueOf(model.getValueAt(mr, 6));
            String balance = String.valueOf(model.getValueAt(mr, 7));
            String due = String.valueOf(model.getValueAt(mr, 8));
            String days = String.valueOf(model.getValueAt(mr, 9));
            String bucket = String.valueOf(model.getValueAt(mr, 10));
            String state = String.valueOf(model.getValueAt(mr, 11));

            peek.setTitle("Fatura " + num);
            peek.setSubtitle(client + " (NUIT " + nuit + ")");
            peek.setItems(java.util.List.of(
                    new QuickPeekPanel.PeekItem("Data de Emissão", date, false),
                    new QuickPeekPanel.PeekItem("Linha de Negócio", bu != null && !"-".equals(bu) ? bu : "Geral", false),
                    new QuickPeekPanel.PeekItem("Vencimento", due, false),
                    new QuickPeekPanel.PeekItem("Total Fatura", total, false),
                    new QuickPeekPanel.PeekItem("Valor Pago", paid, false),
                    new QuickPeekPanel.PeekItem("Saldo Devedor", balance, true),
                    new QuickPeekPanel.PeekItem("Dias Atraso", days, false),
                    new QuickPeekPanel.PeekItem("Antiguidade", bucket, false),
                    new QuickPeekPanel.PeekItem("Estado", state, false)
            ));
            peek.getOpenFullButton().setText("Liquidar Fatura");
            peek.setOnOpenFullAction(v -> pay.doClick());
        });
        peekToggleBtn.addActionListener(e -> peekController.toggle());

        card.add(UIHelper.tableCardTop("Contas Correntes — Faturas com Saldo em Dívida", filterHeader,
                refresh, pay), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(table), BorderLayout.SOUTH);
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
        java.util.Set<String> bus = new java.util.TreeSet<>();
        for (InvoiceDTO invoice : invoices) {
            BigDecimal paid = invoice.amountPaid() == null ? BigDecimal.ZERO : invoice.amountPaid();
            String bu = invoice.businessUnitName() == null || invoice.businessUnitName().isBlank() ? "-" : invoice.businessUnitName();
            if (!"-".equals(bu)) bus.add(bu);
            model.addRow(new Object[]{invoice.invoiceNumber(),
                    // Data e nao data-hora: a coluna nao tem largura para as duas e via-se
                    // "29/08/2026 22…" — a hora a comer a data numa tabela de dividas, onde a hora
                    // de emissao nao decide nada e a data decide tudo.
                    invoice.createdAt() == null ? "-" : invoice.createdAt().format(DATE),
                    bu,
                    invoice.clientName(), invoice.clientTaxId(), invoice.totalAmount(), paid,
                    invoice.outstandingAmount(),
                    invoice.dueDate() == null ? "-" : invoice.dueDate().format(DATE),
                    invoice.daysOverdue() == 0 ? "—" : invoice.daysOverdue(),
                    invoice.agingBucket() == null ? "-" : invoice.agingBucket().label(),
                    invoice.status().name()});
        }
        if (businessUnit != null) {
            String selectedBu = (String) businessUnit.getSelectedItem();
            businessUnit.removeAllItems();
            businessUnit.addItem("Todos os negócios");
            for (String b : bus) businessUnit.addItem(b);
            if (selectedBu != null) businessUnit.setSelectedItem(selectedBu);
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
    private JLabel filterLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(UIHelper.TEXT_MUTED);
        return label;
    }
}
