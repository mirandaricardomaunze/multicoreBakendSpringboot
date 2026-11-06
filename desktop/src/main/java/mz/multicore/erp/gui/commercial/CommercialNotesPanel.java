package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.CreditNoteApiClient;
import mz.multicore.erp.desktop.client.DebitNoteApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.*;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.printing.dto.SensitiveDocumentType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Casos de uso de emissão e aprovação de notas de crédito e débito. */
public final class CommercialNotesPanel {

    private static final String[] CREDIT_REASONS = {"RETURN", "DISCOUNT", "ERROR", "CANCELLATION"};
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ComercialApiClient comercialApiClient;
    private final CreditNoteApiClient creditApiClient;
    private final DebitNoteApiClient debitApiClient;
    private final Supplier<List<WarehouseDTO>> warehouses;
    private final PrintApiClient printApiClient;
    private final JPanel creditTab;
    private final JPanel debitTab;
    private final DefaultTableModel creditModel;
    private final DefaultTableModel debitModel;
    private final JTable creditTable;
    private final JTable debitTable;
    private final InlineFeedbackPanel creditFeedback = new InlineFeedbackPanel();
    private final InlineFeedbackPanel debitFeedback = new InlineFeedbackPanel();
    private List<CreditNoteDTO> creditNotes = new ArrayList<>();
    private List<DebitNoteDTO> debitNotes = new ArrayList<>();

    public CommercialNotesPanel(ComercialApiClient comercialApiClient, CreditNoteApiClient creditApiClient,
                                DebitNoteApiClient debitApiClient, Supplier<List<WarehouseDTO>> warehouses) {
        this(comercialApiClient, creditApiClient, debitApiClient, warehouses, null);
    }

    public CommercialNotesPanel(ComercialApiClient comercialApiClient, CreditNoteApiClient creditApiClient,
                                DebitNoteApiClient debitApiClient, Supplier<List<WarehouseDTO>> warehouses,
                                PrintApiClient printApiClient) {
        this.comercialApiClient = comercialApiClient;
        this.creditApiClient = creditApiClient;
        this.debitApiClient = debitApiClient;
        this.warehouses = warehouses;
        this.printApiClient = printApiClient;
        creditModel = readOnlyModel("Nº Nota", "Data", "Fatura", "Cliente", "Motivo", "Armazém", "Total", "Estado", "Impressões", "Negócio");
        debitModel = readOnlyModel("Nº Nota", "Data", "Fatura", "Cliente", "Motivo", "Total", "Estado", "Impressões", "Negócio");
        creditTable = configuredTable(creditModel, 6, 7);
        debitTable = configuredTable(debitModel, 5, 6);
        creditTab = buildCreditTab();
        debitTab = buildDebitTab();
    }

    public JPanel creditTab() { return creditTab; }
    public JPanel debitTab() { return debitTab; }
    public void refresh() { loadCredits(); loadDebits(); }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private static JTable configuredTable(DefaultTableModel model, int moneyColumn, int statusColumn) {
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(moneyColumn).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(statusColumn).setCellRenderer(TableCellRenderers.status());
        return table;
    }

    private JPanel buildCreditTab() {
        ModernButton create = UIHelper.createSuccessButton("Emitir Nota");
        create.setIcon(UIHelper.icon("fas-plus", 14));
        create.addActionListener(e -> loadInvoices(this::showCreditDialog));
        ActionMenuButton more = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Imprimir PDF", UIHelper.icon("fas-print", 14, UIHelper.ACCENT_BLUE), this::printCredit)
                .addAction("Rejeitar", UIHelper.icon("fas-times", 14, UIHelper.REJECTED_RED), this::rejectCredit)
                .addAction("Aprovar", UIHelper.icon("fas-check", 14, UIHelper.APPROVED_GREEN), this::approveCredit);
        return buildTab("Notas de Crédito", creditTable,
                CREDIT_REASONS, 4, 7,
                UIHelper.createRefreshButton(this::loadCredits), more, create);
    }

    private JPanel buildDebitTab() {
        ModernButton create = UIHelper.createSuccessButton("Emitir Nota");
        create.setIcon(UIHelper.icon("fas-plus", 14));
        create.addActionListener(e -> loadInvoices(invoices ->
                DebitNoteFormDialog.show(debitTab, debitApiClient, invoices, this::loadDebits,
                        err -> showError(debitTab, "emitir nota de débito", err))));
        ActionMenuButton more = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Imprimir PDF", UIHelper.icon("fas-print", 14, UIHelper.ACCENT_BLUE), this::printDebit)
                .addAction("Rejeitar", UIHelper.icon("fas-times", 14, UIHelper.REJECTED_RED), this::rejectDebit)
                .addAction("Aprovar", UIHelper.icon("fas-check", 14, UIHelper.APPROVED_GREEN), this::approveDebit);
        return buildTab("Notas de Débito", debitTable,
                new String[]{"FREIGHT", "SURCHARGE", "CORRECTION", "OTHER"}, 4, 6,
                UIHelper.createRefreshButton(this::loadDebits), more, create);
    }

    private JPanel buildTab(String title, JTable table, String[] reasons, int reasonColumn, int statusColumn,
                            ModernButton... buttons) {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));
        tab.add(table == creditTable ? creditFeedback : debitFeedback, BorderLayout.NORTH);
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        JTextField search = TableFilter.searchField("Nº nota, fatura ou cliente…");
        JComboBox<String> reason = TableFilter.combo(withFirst("Todos os motivos", reasons));
        UIHelper.styleComboBox(reason);
        reason.setPreferredSize(new Dimension(180, UIHelper.FORM_CONTROL_HEIGHT));

        JComboBox<String> status = TableFilter.combo("Todos os estados", "Pendente", "Aprovado", "Rejeitado");
        UIHelper.styleComboBox(status);
        status.setPreferredSize(new Dimension(160, UIHelper.FORM_CONTROL_HEIGHT));

        JComboBox<String> period = TableFilter.periodCombo();
        UIHelper.styleComboBox(period);
        period.setPreferredSize(new Dimension(160, UIHelper.FORM_CONTROL_HEIGHT));

        TableFilter.install(table, search,
                List.of(new TableFilter.ColumnFilter(reason, reasonColumn), new TableFilter.ColumnFilter(status, statusColumn)),
                List.of(new TableFilter.PeriodFilter(period, 1)));

        UIHelper.ensureHeadersFit(creditTable);
        UIHelper.ensureHeadersFit(debitTable);
        JPanel filters = TableFilter.bar(search,
                TableFilter.label("Motivo:"), reason,
                TableFilter.label("Estado:"), status,
                TableFilter.label("Período:"), period);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        TableQuickPeekController.install(table, card, (peek, mr) -> {
            int row = mr; // o controlador já entrega a linha do modelo
            peek.setHeaderIcon("fas-file-invoice", UIHelper.ACCENT_BLUE);
            peek.setTitle(String.valueOf(table.getModel().getValueAt(row, 0)));
            peek.setSubtitle("Cliente: " + table.getModel().getValueAt(row, 2) + " • Data: " + table.getModel().getValueAt(row, 1));
            peek.setStatus(String.valueOf(table.getModel().getValueAt(row, statusColumn)), UIHelper.ACCENT_BLUE);
            List<QuickPeekPanel.PeekItem> items = new ArrayList<>();
            items.add(new QuickPeekPanel.PeekItem("Fatura Associada", peek.cell(table.getModel().getValueAt(row, 2), 2), false));
            items.add(new QuickPeekPanel.PeekItem("Motivo", peek.cell(table.getModel().getValueAt(row, reasonColumn), reasonColumn), false));
            int moneyCol = table == creditTable ? 6 : 5;
            items.add(new QuickPeekPanel.PeekItem("Valor Total", peek.cell(table.getModel().getValueAt(row, moneyCol), moneyCol), true));
            int printCol = table == creditTable ? 8 : 7;
            if (printCol < table.getModel().getColumnCount()) {
                items.add(new QuickPeekPanel.PeekItem("Vias Impressas", peek.cell(table.getModel().getValueAt(row, printCol), printCol), false));
            }
            peek.setItems(items);
        });

        card.add(UIHelper.tableCardTop(title, filters, buttons), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(table), BorderLayout.SOUTH);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadCredits() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(creditTab, () -> creditApiClient.findByCompany(companyId), loaded -> {
            creditNotes = loaded;
            creditModel.setRowCount(0);
            for (CreditNoteDTO note : loaded) {
                String bu = note.businessUnitName() != null && !note.businessUnitName().isBlank() ? note.businessUnitName() : "—";
                creditModel.addRow(new Object[]{note.noteNumber(), note.issueDate().format(DATE_TIME),
                        note.invoiceNumber(), note.clientName(), note.reason(),
                        note.warehouseName() == null ? "-" : note.warehouseName(), note.totalAmount(), note.status(),
                        printApiClient == null ? "—" : "A verificar…", bu});
            }
            UIHelper.ensureHeadersFit(creditTable);
            if (printApiClient != null && !loaded.isEmpty()) {
                List<Long> ids = loaded.stream().map(CreditNoteDTO::id).toList();
                UIHelper.loadAsync(creditTab, () -> printApiClient.getPrintStatuses(SensitiveDocumentType.CREDIT_NOTE, ids),
                        st -> CommercialNotesOperationsCoordinator.applyPrintStatuses(creditNotes, creditModel, 8, st, CreditNoteDTO::id),
                        err -> { for (int r = 0; r < creditModel.getRowCount(); r++) creditModel.setValueAt("Indisponível", r, 8); });
            }
        }, error -> showError(creditTab, "carregar notas de crédito", error));
    }

    private void loadDebits() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(debitTab, () -> debitApiClient.findByCompany(companyId), loaded -> {
            debitNotes = loaded;
            debitModel.setRowCount(0);
            for (DebitNoteDTO note : loaded) debitModel.addRow(new Object[]{note.noteNumber(),
                    note.issueDate().format(DATE_TIME), note.invoiceNumber(), note.clientName(), note.reason(),
                    note.totalAmount(), note.status(), printApiClient == null ? "—" : "A verificar…"});
            UIHelper.ensureHeadersFit(debitTable);
            if (printApiClient != null && !loaded.isEmpty()) {
                List<Long> ids = loaded.stream().map(DebitNoteDTO::id).toList();
                UIHelper.loadAsync(debitTab, () -> printApiClient.getPrintStatuses(SensitiveDocumentType.DEBIT_NOTE, ids),
                        st -> CommercialNotesOperationsCoordinator.applyPrintStatuses(debitNotes, debitModel, 7, st, DebitNoteDTO::id),
                        err -> { for (int r = 0; r < debitModel.getRowCount(); r++) debitModel.setValueAt("Indisponível", r, 7); });
            }
        }, error -> showError(debitTab, "carregar notas de débito", error));
    }

    private CreditNoteDTO selectedCredit() {
        int row = TableFilter.selectedModelRow(creditTable);
        if (row < 0 || row >= creditNotes.size()) { warn(creditTab); return null; }
        return creditNotes.get(row);
    }

    private DebitNoteDTO selectedDebit() {
        int row = TableFilter.selectedModelRow(debitTable);
        if (row < 0 || row >= debitNotes.size()) { warn(debitTab); return null; }
        return debitNotes.get(row);
    }

    private void warn(Component owner) {
        showNotice(owner, FeedbackType.WARNING, "Seleccione uma nota", "Escolha uma nota na tabela para continuar.");
    }

    private void approveCredit() {
        CommercialNotesOperationsCoordinator.approveCredit(creditTab, selectedCredit(), creditApiClient,
                this::loadCredits, err -> showError(creditTab, "aprovar nota de crédito", err));
    }

    private void rejectCredit() {
        CommercialNotesOperationsCoordinator.rejectCredit(creditTab, selectedCredit(), creditApiClient,
                this::loadCredits, err -> showError(creditTab, "rejeitar nota de crédito", err));
    }

    private void printCredit() {
        CreditNoteDTO selected = selectedCredit();
        if (selected == null) return;
        sensitivePrint(creditTab, SensitiveDocumentType.CREDIT_NOTE, selected.id(),
                "Nota de crédito " + selected.noteNumber(), "nota-credito-" + selected.noteNumber(),
                () -> creditApiClient.renderCreditNote(selected.id()), this::loadCredits);
    }

    private void approveDebit() {
        CommercialNotesOperationsCoordinator.approveDebit(debitTab, selectedDebit(), debitApiClient,
                this::loadDebits, err -> showError(debitTab, "aprovar nota de débito", err));
    }

    private void rejectDebit() {
        CommercialNotesOperationsCoordinator.rejectDebit(debitTab, selectedDebit(), debitApiClient,
                this::loadDebits, err -> showError(debitTab, "rejeitar nota de débito", err));
    }

    private void printDebit() {
        DebitNoteDTO selected = selectedDebit();
        if (selected == null) return;
        sensitivePrint(debitTab, SensitiveDocumentType.DEBIT_NOTE, selected.id(),
                "Nota de débito " + selected.noteNumber(), "nota-debito-" + selected.noteNumber(),
                () -> debitApiClient.renderDebitNote(selected.id()), this::loadDebits);
    }

    private void sensitivePrint(JComponent owner, SensitiveDocumentType type, Long id,
                                String label, String fileName,
                                java.util.concurrent.Callable<byte[]> renderer, Runnable refresh) {
        if (printApiClient == null) {
            UIHelper.runWithProgress(owner, "A gerar documento…", renderer,
                    pdf -> PrintPreviewDialog.show(owner, pdf, fileName),
                    error -> showError(owner, "gerar documento", error));
            return;
        }
        SensitivePrintCoordinator.print(owner, printApiClient, type, id, label, fileName, renderer, refresh);
    }

    private void loadInvoices(java.util.function.Consumer<List<InvoiceDTO>> consumer) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(creditTab, () -> comercialApiClient.getInvoicesByCompany(companyId), loaded -> {
            if (loaded.isEmpty()) showNotice(creditTab, FeedbackType.WARNING,
                    "Sem faturas", "Registe pelo menos uma fatura antes de emitir uma nota.");
            else consumer.accept(new ArrayList<>(loaded));
        }, error -> showError(creditTab, "carregar faturas", error));
    }

    private void showCreditDialog(List<InvoiceDTO> invoices) {
        JComboBox<String> invoice = new JComboBox<>();
        for (InvoiceDTO inv : invoices) invoice.addItem(inv.invoiceNumber() + " — " + inv.buyerName());
        UIHelper.styleComboBox(invoice);
        JComboBox<String> reason = new JComboBox<>(CREDIT_REASONS);
        UIHelper.styleComboBox(reason);
        JComboBox<String> warehouse = new JComboBox<>();
        for (WarehouseDTO item : warehouses.get()) warehouse.addItem(item.name());
        UIHelper.styleComboBox(warehouse);
        JTextField description = new JTextField();
        UIHelper.styleTextField(description);
        DefaultTableModel lines = new DefaultTableModel(
                new String[]{"#linhaId", "Produto", "Lote", "Vendida", "Devolvida", "Restante", "A devolver"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return column == 6; }
        };
        JTable lineTable = new JTable(lines);
        UIHelper.styleTable(lineTable);
        lineTable.getColumnModel().getColumn(0).setMinWidth(0);
        lineTable.getColumnModel().getColumn(0).setMaxWidth(0);
        Runnable populate = () -> populateCreditLines(invoices.get(invoice.getSelectedIndex()), lines);
        invoice.addActionListener(e -> { if (invoice.getSelectedIndex() >= 0) populate.run(); });
        populate.run();
        JScrollPane lineScroll = new JScrollPane(lineTable);
        UIHelper.styleScrollPane(lineScroll);
        JPanel form = UIHelper.createDialogForm("Fatura:", invoice, "Motivo:", reason,
                "Armazém (devolução):", warehouse, "Descrição:", description);
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.add(form, BorderLayout.NORTH);
        content.add(lineScroll, BorderLayout.CENTER);
        if (!new ModernFormDialog(UIHelper.mainWindow, "Emitir Nota de Crédito", "fas-file-invoice-dollar",
                "Crédito sobre fatura (devolução/correção)", content).setConfirmButton("Emitir", "fas-check").showDialog()) return;
        if (lineTable.isEditing()) lineTable.getCellEditor().stopCellEditing();
        try {
            List<CreateCreditNoteLineRequest> requestedLines = new ArrayList<>();
            for (int row = 0; row < lines.getRowCount(); row++) {
                BigDecimal quantity = new BigDecimal(String.valueOf(lines.getValueAt(row, 6)).trim().replace(',', '.'));
                if (quantity.signum() > 0) requestedLines.add(new CreateCreditNoteLineRequest((Long) lines.getValueAt(row, 0), quantity));
            }
            if (requestedLines.isEmpty()) throw new IllegalArgumentException("Indique uma quantidade a devolver em pelo menos uma linha.");
            List<WarehouseDTO> available = warehouses.get();
            String desc = description.getText() == null || description.getText().isBlank() ? null : description.getText().trim();
            CreateCreditNoteRequest request = new CreateCreditNoteRequest(invoices.get(invoice.getSelectedIndex()).id(),
                    String.valueOf(reason.getSelectedItem()), available.isEmpty() ? null : available.get(warehouse.getSelectedIndex()).id(),
                    desc, requestedLines);
            UIHelper.runWithProgress(creditTab, "A emitir nota de crédito…", () -> creditApiClient.create(request), created -> {
                ToastManager.success(creditTab, "Nota " + created.noteNumber() + " emitida; pendente de aprovação.");
                loadCredits();
            }, error -> showError(creditTab, "emitir nota de crédito", error));
        } catch (RuntimeException error) {
            showNotice(creditTab, FeedbackType.ERROR, "Dados da nota de crédito inválidos", error.getMessage());
        }
    }

    private void populateCreditLines(InvoiceDTO invoice, DefaultTableModel model) {
        model.setRowCount(0);
        UIHelper.loadAsync(creditTab, () -> creditApiClient.getReturnedQuantitiesByInvoiceLine(invoice.id()), returned -> {
            model.setRowCount(0);
            for (InvoiceLineDTO line : invoice.lines()) {
                BigDecimal sold = line.quantity();
                BigDecimal already = returned.getOrDefault(line.id(), BigDecimal.ZERO);
                model.addRow(new Object[]{line.id(), line.productName(), line.batchNumber() == null ? "—" : line.batchNumber(),
                        sold, already, sold.subtract(already), BigDecimal.ZERO});
            }
        }, error -> showError(creditTab, "carregar devoluções", error));
    }

    private static String[] withFirst(String first, String[] values) {
        String[] combined = new String[values.length + 1];
        combined[0] = first;
        System.arraycopy(values, 0, combined, 1, values.length);
        return combined;
    }

    private void showError(Component owner, String action, Throwable error) {
        showNotice(owner, FeedbackType.ERROR, "Não foi possível " + action, error.getMessage());
    }

    private void showNotice(Component owner, FeedbackType type, String title, String message) {
        (owner == debitTab ? debitFeedback : creditFeedback).show(type, title, message, null, null);
    }
}
