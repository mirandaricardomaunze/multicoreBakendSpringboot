package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.CreditNoteApiClient;
import mz.multicore.erp.desktop.client.DebitNoteApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.*;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.printing.PdfFileSaver;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Casos de uso de emissão e aprovação de notas de crédito e débito. */
public final class CommercialNotesPanel {

    private static final String[] CREDIT_REASONS = {"RETURN", "DISCOUNT", "ERROR", "CANCELLATION"};
    private static final String[] DEBIT_REASONS = {"FREIGHT", "SURCHARGE", "CORRECTION", "OTHER"};
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ComercialApiClient comercialApiClient;
    private final CreditNoteApiClient creditApiClient;
    private final DebitNoteApiClient debitApiClient;
    private final Supplier<List<WarehouseDTO>> warehouses;
    private final JPanel creditTab;
    private final JPanel debitTab;
    private final DefaultTableModel creditModel;
    private final DefaultTableModel debitModel;
    private final JTable creditTable;
    private final JTable debitTable;
    private List<CreditNoteDTO> creditNotes = new ArrayList<>();
    private List<DebitNoteDTO> debitNotes = new ArrayList<>();

    public CommercialNotesPanel(ComercialApiClient comercialApiClient, CreditNoteApiClient creditApiClient,
                                DebitNoteApiClient debitApiClient, Supplier<List<WarehouseDTO>> warehouses) {
        this.comercialApiClient = comercialApiClient;
        this.creditApiClient = creditApiClient;
        this.debitApiClient = debitApiClient;
        this.warehouses = warehouses;
        creditModel = readOnlyModel("Nº Nota", "Data", "Fatura", "Cliente", "Motivo", "Armazém", "Total", "Estado");
        debitModel = readOnlyModel("Nº Nota", "Data", "Fatura", "Cliente", "Motivo", "Total", "Estado");
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
        ModernButton approve = UIHelper.createSuccessButton("Aprovar");
        approve.setIcon(UIHelper.icon("fas-check", 14));
        approve.addActionListener(e -> approveCredit());
        ModernButton reject = UIHelper.createDangerButton("Rejeitar");
        reject.setIcon(UIHelper.icon("fas-times", 14));
        reject.addActionListener(e -> rejectCredit());
        ActionMenuButton more = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Imprimir PDF", UIHelper.icon("fas-print", 14), this::printCredit)
                .addAction("Actualizar", UIHelper.icon("fas-sync-alt", 14), this::loadCredits);
        return buildTab("Notas de Crédito", creditTable,
                new String[]{"RETURN", "DISCOUNT", "ERROR", "CANCELLATION"}, 4, 7,
                more, reject, approve, create);
    }

    private JPanel buildDebitTab() {
        ModernButton create = UIHelper.createSuccessButton("Emitir Nota");
        create.setIcon(UIHelper.icon("fas-plus", 14));
        create.addActionListener(e -> loadInvoices(this::showDebitDialog));
        ModernButton approve = UIHelper.createSuccessButton("Aprovar");
        approve.setIcon(UIHelper.icon("fas-check", 14));
        approve.addActionListener(e -> approveDebit());
        ModernButton reject = UIHelper.createDangerButton("Rejeitar");
        reject.setIcon(UIHelper.icon("fas-times", 14));
        reject.addActionListener(e -> rejectDebit());
        ActionMenuButton more = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Imprimir PDF", UIHelper.icon("fas-print", 14), this::printDebit)
                .addAction("Actualizar", UIHelper.icon("fas-sync-alt", 14), this::loadDebits);
        return buildTab("Notas de Débito", debitTable,
                new String[]{"FREIGHT", "SURCHARGE", "CORRECTION", "OTHER"}, 4, 6,
                more, reject, approve, create);
    }

    private JPanel buildTab(String title, JTable table, String[] reasons, int reasonColumn, int statusColumn,
                            ModernButton... buttons) {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading(title), BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        for (ModernButton button : buttons) actions.add(button);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        JTextField search = TableFilter.searchField("Nº nota, fatura ou cliente…");
        JComboBox<String> reason = TableFilter.combo(withFirst("Todos os motivos", reasons));
        JComboBox<String> status = TableFilter.combo("Todos os estados", "PENDING", "APPROVED", "REJECTED");
        JComboBox<String> period = TableFilter.periodCombo();
        TableFilter.install(table, search,
                List.of(new TableFilter.ColumnFilter(reason, reasonColumn), new TableFilter.ColumnFilter(status, statusColumn)),
                List.of(new TableFilter.PeriodFilter(period, 1)));
        JPanel filters = TableFilter.bar(search, TableFilter.label("Motivo:"), reason,
                TableFilter.label("Estado:"), status, TableFilter.label("Data:", "fas-calendar-alt"), period);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        card.add(filters, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadCredits() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(creditTab, () -> creditApiClient.findByCompany(companyId), loaded -> {
            creditNotes = loaded;
            creditModel.setRowCount(0);
            for (CreditNoteDTO note : loaded) creditModel.addRow(new Object[]{note.noteNumber(),
                    note.issueDate().format(DATE_TIME), note.invoiceNumber(), note.clientName(), note.reason(),
                    note.warehouseName() == null ? "-" : note.warehouseName(), note.totalAmount(), note.status()});
        }, error -> showError(creditTab, "carregar notas de crédito", error));
    }

    private void loadDebits() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(debitTab, () -> debitApiClient.findByCompany(companyId), loaded -> {
            debitNotes = loaded;
            debitModel.setRowCount(0);
            for (DebitNoteDTO note : loaded) debitModel.addRow(new Object[]{note.noteNumber(),
                    note.issueDate().format(DATE_TIME), note.invoiceNumber(), note.clientName(), note.reason(),
                    note.totalAmount(), note.status()});
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

    private static void warn(Component owner) {
        JOptionPane.showMessageDialog(owner, "Selecione uma nota na tabela.", "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    private void approveCredit() {
        CreditNoteDTO selected = selectedCredit();
        if (selected == null) return;
        UIHelper.runWithProgress(creditTab, "A aprovar nota de crédito…", () -> creditApiClient.approve(selected.id()), approved -> {
            String message = "Nota " + approved.noteNumber() + " aprovada.";
            if ("RETURN".equals(approved.reason())) message += "\nStock devolvido ao armazém " + approved.warehouseName() + ".";
            JOptionPane.showMessageDialog(creditTab, message, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadCredits();
        }, error -> showError(creditTab, "aprovar nota de crédito", error));
    }

    private void rejectCredit() {
        CreditNoteDTO selected = selectedCredit();
        if (selected == null) return;
        String reason = UIHelper.promptRequiredText("Rejeitar Nota de Crédito", "fas-times-circle",
                "Indique o motivo da rejeição", "Motivo da rejeição:");
        if (reason == null) return;
        UIHelper.runWithProgress(creditTab, "A rejeitar nota de crédito…", () -> creditApiClient.reject(selected.id(), reason),
                ignored -> loadCredits(), error -> showError(creditTab, "rejeitar nota de crédito", error));
    }

    private void printCredit() {
        CreditNoteDTO selected = selectedCredit();
        if (selected == null) return;
        UIHelper.runWithProgress(creditTab, "A gerar nota de crédito…", () -> creditApiClient.renderCreditNote(selected.id()),
                pdf -> PdfFileSaver.saveAndOpen(pdf, "nota-credito-" + selected.noteNumber()),
                error -> showError(creditTab, "gerar nota de crédito", error));
    }

    private void approveDebit() {
        DebitNoteDTO selected = selectedDebit();
        if (selected == null) return;
        UIHelper.runWithProgress(debitTab, "A aprovar nota de débito…", () -> debitApiClient.approve(selected.id()), ignored -> {
            JOptionPane.showMessageDialog(debitTab, "Nota " + selected.noteNumber() + " aprovada.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadDebits();
        }, error -> showError(debitTab, "aprovar nota de débito", error));
    }

    private void rejectDebit() {
        DebitNoteDTO selected = selectedDebit();
        if (selected == null) return;
        String reason = UIHelper.promptRequiredText("Rejeitar Nota de Débito", "fas-times-circle",
                "Indique o motivo da rejeição", "Motivo da rejeição:");
        if (reason == null) return;
        UIHelper.runWithProgress(debitTab, "A rejeitar nota de débito…", () -> debitApiClient.reject(selected.id(), reason),
                ignored -> loadDebits(), error -> showError(debitTab, "rejeitar nota de débito", error));
    }

    private void printDebit() {
        DebitNoteDTO selected = selectedDebit();
        if (selected == null) return;
        UIHelper.runWithProgress(debitTab, "A gerar nota de débito…", () -> debitApiClient.renderDebitNote(selected.id()),
                pdf -> PdfFileSaver.saveAndOpen(pdf, "nota-debito-" + selected.noteNumber()),
                error -> showError(debitTab, "gerar nota de débito", error));
    }

    private void loadInvoices(java.util.function.Consumer<List<InvoiceDTO>> consumer) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(creditTab, () -> comercialApiClient.getInvoicesByCompany(companyId), loaded -> {
            if (loaded.isEmpty()) JOptionPane.showMessageDialog(creditTab,
                    "Precisa de pelo menos uma fatura cadastrada.", "Aviso", JOptionPane.WARNING_MESSAGE);
            else consumer.accept(new ArrayList<>(loaded));
        }, error -> showError(creditTab, "carregar faturas", error));
    }

    private void showCreditDialog(List<InvoiceDTO> invoices) {
        JComboBox<String> invoice = invoiceCombo(invoices);
        JComboBox<String> reason = styledCombo(CREDIT_REASONS);
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
            CreateCreditNoteRequest request = new CreateCreditNoteRequest(invoices.get(invoice.getSelectedIndex()).id(),
                    String.valueOf(reason.getSelectedItem()), available.isEmpty() ? null : available.get(warehouse.getSelectedIndex()).id(),
                    blank(description.getText()), requestedLines);
            UIHelper.runWithProgress(creditTab, "A emitir nota de crédito…", () -> creditApiClient.create(request), created -> {
                JOptionPane.showMessageDialog(creditTab, "Nota " + created.noteNumber() + " emitida (pendente de aprovação).",
                        "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                loadCredits();
            }, error -> showError(creditTab, "emitir nota de crédito", error));
        } catch (RuntimeException error) {
            JOptionPane.showMessageDialog(creditTab, error.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
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

    private void showDebitDialog(List<InvoiceDTO> invoices) {
        JComboBox<String> invoice = invoiceCombo(invoices);
        JComboBox<String> reason = styledCombo(DEBIT_REASONS);
        JTextField description = new JTextField();
        UIHelper.styleTextField(description);
        DefaultTableModel lines = new DefaultTableModel(new String[]{"Descrição", "Valor", "IVA (0.16)"}, 0);
        lines.addRow(new Object[]{"Frete adicional", "0", "0"});
        JTable lineTable = new JTable(lines);
        UIHelper.styleTable(lineTable);
        ModernButton add = UIHelper.createAddLineButton();
        add.addActionListener(e -> lines.addRow(new Object[]{"", "0", "0"}));
        ModernButton remove = UIHelper.createDangerButton("Remover");
        remove.setIcon(UIHelper.icon("fas-minus", 14));
        remove.addActionListener(e -> { if (lineTable.getSelectedRow() >= 0) lines.removeRow(lineTable.getSelectedRow()); });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(add); buttons.add(remove);
        JScrollPane scroll = new JScrollPane(lineTable);
        UIHelper.styleScrollPane(scroll);
        JPanel tablePanel = new JPanel(new BorderLayout(0, 6));
        tablePanel.setOpaque(false);
        tablePanel.add(scroll, BorderLayout.CENTER); tablePanel.add(buttons, BorderLayout.SOUTH);
        JPanel form = UIHelper.createDialogForm("Fatura:", invoice, "Motivo:", reason, "Descrição:", description);
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false); content.add(form, BorderLayout.NORTH); content.add(tablePanel, BorderLayout.CENTER);
        if (!new ModernFormDialog(UIHelper.mainWindow, "Emitir Nota de Débito", "fas-file-invoice-dollar",
                "Débito adicional sobre fatura", content).setConfirmButton("Emitir", "fas-check").showDialog()) return;
        if (lineTable.isEditing()) lineTable.getCellEditor().stopCellEditing();
        try {
            List<CreateDebitNoteLineRequest> requestedLines = new ArrayList<>();
            for (int row = 0; row < lines.getRowCount(); row++) {
                String text = String.valueOf(lines.getValueAt(row, 0)).trim();
                if (text.isEmpty()) throw new IllegalArgumentException("Descrição da linha não pode estar vazia.");
                requestedLines.add(new CreateDebitNoteLineRequest(text,
                        new BigDecimal(String.valueOf(lines.getValueAt(row, 1)).trim().replace(',', '.')),
                        new BigDecimal(String.valueOf(lines.getValueAt(row, 2)).trim().replace(',', '.'))));
            }
            if (requestedLines.isEmpty()) throw new IllegalArgumentException("Adicione pelo menos uma linha.");
            CreateDebitNoteRequest request = new CreateDebitNoteRequest(invoices.get(invoice.getSelectedIndex()).id(),
                    String.valueOf(reason.getSelectedItem()), blank(description.getText()), requestedLines);
            UIHelper.runWithProgress(debitTab, "A emitir nota de débito…", () -> debitApiClient.create(request), created -> {
                JOptionPane.showMessageDialog(debitTab, "Nota " + created.noteNumber() + " emitida (pendente de aprovação).",
                        "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                loadDebits();
            }, error -> showError(debitTab, "emitir nota de débito", error));
        } catch (RuntimeException error) {
            JOptionPane.showMessageDialog(debitTab, error.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static JComboBox<String> invoiceCombo(List<InvoiceDTO> invoices) {
        JComboBox<String> combo = new JComboBox<>();
        for (InvoiceDTO invoice : invoices) combo.addItem(invoice.invoiceNumber() + " — " + invoice.clientName());
        UIHelper.styleComboBox(combo);
        return combo;
    }

    private static JComboBox<String> styledCombo(String[] values) {
        JComboBox<String> combo = new JComboBox<>(values);
        UIHelper.styleComboBox(combo);
        return combo;
    }

    private static String[] withFirst(String first, String[] values) {
        String[] combined = new String[values.length + 1];
        combined[0] = first;
        System.arraycopy(values, 0, combined, 1, values.length);
        return combined;
    }

    private static String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private static void showError(Component owner, String action, Throwable error) {
        JOptionPane.showMessageDialog(owner, "Não foi possível " + action + ": " + error.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
