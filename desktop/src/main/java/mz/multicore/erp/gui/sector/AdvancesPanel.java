package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.AdvancesApiClient;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.advances.dto.CustomerAdvanceDTO;
import mz.multicore.erp.modules.advances.dto.ReceiveAdvanceRequest;
import mz.multicore.erp.modules.advances.model.CustomerAdvanceStatus;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Customer advances (SA-C10, {@code docs/ADIANTAMENTOS_SPEC.md}): a cake with 50% down, meat for a party paid in
 * part, a contractor paying before the work. "Receber Sinal" issues the advance invoice with VAT and its receipt;
 * "Aplicar na Fatura" regularises it with a credit note and offsets it on the final invoice, without new cash.
 */
public class AdvancesPanel extends JPanel {

    static final String OPEN_LABEL = "EM ABERTO";
    static final String APPLIED_LABEL = "APLICADO";
    private static final String[] COLUMNS = {"Data", "Cliente", "Valor", "Estado", "Fatura do sinal", "Recibo",
            "Fatura final", "Nota de crédito", "Observações"};
    private static final String[][] METHODS = {{"CASH", "Numerário"}, {"MPESA", "M-Pesa"}, {"EMOLA", "E-Mola"},
            {"CARD", "Cartão"}, {"BANK_TRANSFER", "Transferência"}};

    private final AdvancesApiClient api;
    private final ComercialApiClient comercialApi;
    private final FinanceApiClient financeApi;
    private final InventoryApiClient inventoryApi;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final ModernButton receiveBtn = UIHelper.createSuccessButton("Receber Sinal");
    private final ModernButton applyBtn = UIHelper.createPrimaryButton("Aplicar na Fatura");
    private final ModernButton printBtn = UIHelper.createSecondaryButton("Imprimir Fatura");
    private final List<CustomerAdvanceDTO> advances = new ArrayList<>();
    private Consumer<List<CustomerAdvanceDTO>> onLoaded = list -> { };

    public AdvancesPanel(AdvancesApiClient api, ComercialApiClient comercialApi, FinanceApiClient financeApi,
                         InventoryApiClient inventoryApi) {
        this.api = api;
        this.comercialApi = comercialApi;
        this.financeApi = financeApi;
        this.inventoryApi = inventoryApi;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        add(feedback, BorderLayout.NORTH);
        add(buildList(), BorderLayout.CENTER);
        updateActions();
    }

    /** Receives every loaded list, so the module page can update its indicators. */
    public void onLoaded(Consumer<List<CustomerAdvanceDTO>> listener) {
        this.onLoaded = listener == null ? list -> { } : listener;
    }

    private JComponent buildList() {
        UIHelper.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(2).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.status());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateActions();
        });
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);

        JTextField search = TableFilter.searchField("Cliente, fatura ou recibo…");
        search.setPreferredSize(new Dimension(210, UIHelper.FORM_CONTROL_HEIGHT));
        JComboBox<String> state = TableFilter.combo("Todos os estados", OPEN_LABEL, APPLIED_LABEL);
        UIHelper.styleComboBox(state);
        state.setPreferredSize(new Dimension(150, UIHelper.FORM_CONTROL_HEIGHT));
        state.setSelectedItem(OPEN_LABEL);
        TableFilter.install(table, search, new TableFilter.ColumnFilter(state, 3));
        JPanel filters = TableFilter.bar(search, TableFilter.label("Estado:"), state);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));

        receiveBtn.setIcon(UIHelper.icon("fas-hand-holding-usd", 14));
        receiveBtn.setToolTipText("Emite a fatura de adiantamento (com IVA) e o recibo do valor recebido");
        receiveBtn.addActionListener(e -> openReceive());
        applyBtn.setIcon(UIHelper.icon("fas-link", 14));
        applyBtn.setToolTipText("Desconta o sinal na fatura final do mesmo cliente, sem nova entrada de caixa");
        applyBtn.addActionListener(e -> openApply());
        printBtn.setIcon(UIHelper.icon("fas-print", 14));
        printBtn.addActionListener(e -> printAdvanceInvoice());
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::reload);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.add(UIHelper.tableCardTop("Adiantamentos e Sinais", filters, refreshBtn, printBtn, applyBtn, receiveBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(table), BorderLayout.SOUTH);
        return card;
    }

    public void reload() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null || api == null) return;
        Long keep = selected() == null ? null : selected().id();
        UIHelper.loadAsync(this, () -> api.list(companyId), loaded -> apply(loaded, keep),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar os adiantamentos", error.getMessage(),
                        "Tentar novamente", this::reload));
    }

    private void apply(List<CustomerAdvanceDTO> loaded, Long keep) {
        feedback.clear();
        advances.clear();
        advances.addAll(loaded);
        model.setRowCount(0);
        for (CustomerAdvanceDTO a : advances) {
            model.addRow(new Object[]{a.createdAt() == null ? "—" : UIHelper.formatDate(a.createdAt().toLocalDate()),
                    a.clientName(), a.amount(), statusLabel(a.status()), a.advanceInvoiceNumber(), a.receiptNumber(),
                    a.appliedInvoiceNumber() == null ? "—" : a.appliedInvoiceNumber(),
                    a.creditNoteNumber() == null ? "—" : a.creditNoteNumber(), a.notes() == null ? "" : a.notes()});
        }
        if (keep != null) {
            for (int i = 0; i < advances.size(); i++) {
                if (!advances.get(i).id().equals(keep)) continue;
                int view = table.convertRowIndexToView(i);
                if (view >= 0) table.setRowSelectionInterval(view, view);
            }
        }
        updateActions();
        onLoaded.accept(List.copyOf(advances));
    }

    static String statusLabel(CustomerAdvanceStatus status) {
        return status == CustomerAdvanceStatus.APPLIED ? APPLIED_LABEL : OPEN_LABEL;
    }

    private void updateActions() {
        CustomerAdvanceDTO selected = selected();
        applyBtn.setEnabled(selected != null && selected.status() == CustomerAdvanceStatus.OPEN);
        printBtn.setEnabled(selected != null);
    }

    private CustomerAdvanceDTO selected() {
        int row = TableFilter.selectedModelRow(table);
        return row < 0 || row >= advances.size() ? null : advances.get(row);
    }

    // ─── Receive ────────────────────────────────────────────────────────────────

    private record ReceiveData(List<ClientDTO> clients, List<TreasuryAccountDTO> accounts, List<WarehouseDTO> warehouses) {}

    private void openReceive() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        UIHelper.loadAsync(this, () -> new ReceiveData(comercialApi.getAllClients(), financeApi.getAllAccounts(),
                        inventoryApi.getSalesWarehousesByCompany(companyId)),
                data -> showReceiveDialog(companyId, data),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível preparar o recebimento", error.getMessage(), null, null));
    }

    private void showReceiveDialog(Long companyId, ReceiveData data) {
        if (data.accounts().isEmpty() || data.warehouses().isEmpty()) {
            feedback.show(FeedbackType.WARNING, "Falta configuração",
                    "É preciso pelo menos uma conta de tesouraria e um armazém de vendas para emitir a fatura do sinal.", null, null);
            return;
        }
        SearchableComboBox<ClientDTO> client = new SearchableComboBox<>("Pesquisar cliente por nome ou NUIT",
                c -> c.name() + (c.taxId() == null ? "" : "  ·  NUIT " + c.taxId()), c -> c.name() + " " + c.taxId());
        client.setItems(data.clients().stream().filter(c -> !"000000000".equals(c.taxId())).toList());
        MoneyField amount = new MoneyField("");
        JComboBox<String> method = new JComboBox<>(java.util.Arrays.stream(METHODS).map(m -> m[1]).toArray(String[]::new));
        UIHelper.styleComboBox(method);
        JComboBox<String> account = new JComboBox<>(data.accounts().stream().map(TreasuryAccountDTO::name).toArray(String[]::new));
        UIHelper.styleComboBox(account);
        JTextField notes = new JTextField();
        UIHelper.styleTextField(notes);
        JLabel info = new JLabel("<html><div style='width:360px'>Emite uma <b>fatura de adiantamento com IVA</b> e o "
                + "<b>recibo</b> do valor recebido. O valor indicado já inclui IVA. Mais tarde o sinal é descontado na "
                + "fatura final, sem nova entrada de caixa.</div></html>");
        info.setForeground(UIHelper.TEXT_MUTED);
        FormField clientForm = new FormField("Cliente", client, true, "Só clientes identificados (não o Consumidor Final)");
        FormField amountForm = new FormField("Valor recebido (MT, com IVA)", amount, true, null);
        JPanel form = UIHelper.createDialogForm(1, new Object[]{"", info, "", clientForm, "", amountForm,
                "Forma de pagamento:", method, "Conta de tesouraria:", account, "Observações:", notes});
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Receber Sinal", "fas-hand-holding-usd",
                "Fatura de adiantamento e recibo", form).setConfirmButton("Receber e Emitir", "fas-check");
        String requestKey = "ADV-" + UUID.randomUUID();
        CustomerAdvanceDTO[] result = new CustomerAdvanceDTO[1];
        dialog.setOnSaveAsync(() -> {
            ClientDTO c = client.selectedValue();
            if (c == null) {
                clientForm.setError("Escolha o cliente.");
                throw new IllegalArgumentException("Escolha o cliente.");
            }
            BigDecimal value = amount.value();
            if (value == null || value.signum() <= 0) {
                amountForm.setError("Indique o valor recebido.");
                throw new IllegalArgumentException("Indique o valor recebido.");
            }
            ReceiveAdvanceRequest request = new ReceiveAdvanceRequest(companyId, c.id(), data.warehouses().get(0).id(),
                    data.accounts().get(account.getSelectedIndex()).id(), METHODS[method.getSelectedIndex()][0], value,
                    null, notes.getText().trim(), requestKey);
            return () -> { result[0] = api.receive(request); return null; };
        });
        if (!dialog.showDialog() || result[0] == null) return;
        CustomerAdvanceDTO saved = result[0];
        ToastManager.success(this, "Sinal de " + UIHelper.formatMzn(saved.amount()) + " recebido: fatura "
                + saved.advanceInvoiceNumber() + " e recibo " + saved.receiptNumber() + ".");
        UIHelper.loadAsync(this, () -> api.list(companyId), loaded -> apply(loaded, saved.id()), error -> reload());
        print(saved);
    }

    // ─── Apply ──────────────────────────────────────────────────────────────────

    private void openApply() {
        CustomerAdvanceDTO advance = selected();
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (advance == null || companyId == null || advance.status() != CustomerAdvanceStatus.OPEN) return;
        Set<Long> advanceInvoices = advances.stream().map(CustomerAdvanceDTO::advanceInvoiceId).collect(Collectors.toSet());
        UIHelper.loadAsync(this, () -> comercialApi.getOutstandingInvoicesByCompany(companyId).stream()
                        .filter(i -> advance.clientId().equals(i.clientId()) && !advanceInvoices.contains(i.id()))
                        .toList(),
                invoices -> showApplyDialog(companyId, advance, invoices),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar as faturas do cliente", error.getMessage(), null, null));
    }

    private void showApplyDialog(Long companyId, CustomerAdvanceDTO advance, List<InvoiceDTO> invoices) {
        List<InvoiceDTO> eligible = invoices.stream().filter(i -> i.outstandingAmount().compareTo(advance.amount()) >= 0).toList();
        if (eligible.isEmpty()) {
            feedback.show(FeedbackType.INFO, "Sem fatura final para aplicar",
                    advance.clientName() + " não tem fatura por cobrar com pelo menos " + UIHelper.formatMzn(advance.amount())
                            + " em dívida. Emita primeiro a fatura final da encomenda.", null, null);
            return;
        }
        JComboBox<String> invoice = new JComboBox<>(eligible.stream().map(i -> i.invoiceNumber() + "  ·  em dívida "
                + UIHelper.formatMzn(i.outstandingAmount())).toArray(String[]::new));
        UIHelper.styleComboBox(invoice);
        JLabel info = new JLabel("<html><div style='width:360px'>Sinal <b>" + advance.advanceInvoiceNumber() + "</b> de <b>"
                + UIHelper.formatMzn(advance.amount()) + "</b> de " + advance.clientName() + ".<br><br>"
                + "O sistema emite e aprova a <b>nota de crédito</b> da fatura do sinal (estorna o IVA) e um <b>recibo de "
                + "compensação</b> na fatura final. A caixa não mexe: o dinheiro já entrou quando o sinal foi recebido."
                + "</div></html>");
        info.setForeground(UIHelper.TEXT_LIGHT);
        JPanel form = UIHelper.createDialogForm(1, new Object[]{"", info, "Fatura final:", invoice});
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Aplicar Adiantamento", "fas-link",
                "Desconto do sinal na fatura final", form).setConfirmButton("Aplicar", "fas-check");
        CustomerAdvanceDTO[] result = new CustomerAdvanceDTO[1];
        dialog.setOnSaveAsync(() -> {
            Long invoiceId = eligible.get(invoice.getSelectedIndex()).id();
            return () -> { result[0] = api.apply(companyId, advance.id(), invoiceId); return null; };
        });
        if (!dialog.showDialog() || result[0] == null) return;
        CustomerAdvanceDTO applied = result[0];
        ToastManager.success(this, "Sinal aplicado na fatura " + applied.appliedInvoiceNumber() + " (nota de crédito "
                + applied.creditNoteNumber() + ", recibo " + applied.offsetReceiptNumber() + ").");
        UIHelper.loadAsync(this, () -> api.list(companyId), loaded -> apply(loaded, applied.id()), error -> reload());
    }

    // ─── Print ──────────────────────────────────────────────────────────────────

    private void printAdvanceInvoice() {
        CustomerAdvanceDTO advance = selected();
        if (advance != null) print(advance);
    }

    private void print(CustomerAdvanceDTO advance) {
        UIHelper.runWithProgress(this, "A gerar a fatura do sinal…", () -> comercialApi.renderInvoice(advance.advanceInvoiceId()),
                pdf -> PrintPreviewDialog.show(this, pdf, "adiantamento-" + advance.advanceInvoiceNumber().replace('/', '-')),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível gerar o PDF", error.getMessage(), null, null));
    }
}
