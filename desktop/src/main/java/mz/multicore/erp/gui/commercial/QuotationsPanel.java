package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.ActionMenuButton;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.DocumentEditorHost;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.comercial.dto.QuotationDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;
import mz.multicore.erp.gui.components.PrintPreviewDialog;

/**
 * Cotações ao cliente: listagem, decisão e conversão em encomenda.
 *
 * <p>A caducidade que este ecrã mostra vem <b>calculada do servidor</b> ({@code expired},
 * {@code daysUntilExpiry} no DTO). O painel não compara datas: se comparasse, a regra de "o preço
 * ainda é para honrar?" passava a existir em dois sítios, e o relógio do posto de trabalho podia
 * discordar do do servidor. Ver docs/COTACAO_SPEC.md §4.
 */
public final class QuotationsPanel extends JPanel {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final int COL_ID = 0;
    private static final int COL_NUMBER = 1;
    private static final int COL_STATUS = 6;
    private static final int COL_VALIDITY = 7;

    private final ComercialApiClient apiClient;
    private final Supplier<List<ClientDTO>> clients;
    private final Supplier<List<ProductDTO>> products;
    private final Supplier<List<WarehouseDTO>> warehouses;
    private final Runnable ordersRefresh;
    private final DefaultTableModel model;
    private final JTable table;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final CardLayout pages = new CardLayout();
    private final QuotationEditorForm editorForm;
    private final DocumentEditorHost editorHost;

    public QuotationsPanel(ComercialApiClient apiClient,
                           Supplier<List<ClientDTO>> clients,
                           Supplier<List<ProductDTO>> products,
                           Supplier<List<WarehouseDTO>> warehouses,
                           Runnable ordersRefresh) {
        this.apiClient = apiClient;
        this.clients = clients;
        this.products = products;
        this.warehouses = warehouses;
        this.ordersRefresh = ordersRefresh;

        setLayout(pages);
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel listPage = new JPanel(new BorderLayout(0, 15));
        listPage.setOpaque(false);

        ModernButton newBtn = UIHelper.createPrimaryButton("Nova Cotação");
        newBtn.setIcon(UIHelper.icon("fas-file-signature", 14));
        newBtn.addActionListener(e -> openEditor());
        listPage.add(feedback, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        model = new DefaultTableModel(new String[]{
                "ID", "Nº Cotação", "Data", "Cliente", "NUIT", "Total", "Estado", "Validade", "Encomenda"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openSelectedEditor();
            }
        });
        table.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(COL_STATUS).setCellRenderer(TableCellRenderers.status());
        hideColumn(COL_ID);

        JTextField search = TableFilter.searchField("Nº cotação, cliente ou NUIT…");
        JComboBox<String> status = TableFilter.combo("Todos os estados",
                "Rascunho", "Enviada", "Aceite", "Recusada", "Convertida", "Cancelada");
        JComboBox<String> periodo = TableFilter.periodCombo();
        TableFilter.install(table, search,
                java.util.List.of(new TableFilter.ColumnFilter(status, COL_STATUS)),
                java.util.List.of(new TableFilter.PeriodFilter(periodo, 2)));
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refresh);
        ActionMenuButton more = UIHelper.createActionMenuButton("Operações")
                .addAction("Imprimir", UIHelper.icon("fas-print", 14), this::print)
                .addAction("Editar / Consultar", UIHelper.icon("fas-edit", 14, UIHelper.ACCENT_BLUE), this::openSelectedEditor)
                .addAction("Marcar como enviada", UIHelper.icon("fas-paper-plane", 14), this::send)
                .addAction("Estender validade", UIHelper.icon("fas-calendar-plus", 14), this::extendValidity);
        ActionMenuButton decision = UIHelper.createActionMenuButton("Decisão")
                .addAction("Recusada pelo cliente", UIHelper.icon("fas-times", 14), this::reject)
                .addAction("Aceite pelo cliente", UIHelper.icon("fas-check", 14), this::accept)
                .addAction("Cancelar cotação", UIHelper.icon("fas-ban", 14), this::cancel)
                .addAction("Converter em Encomenda", UIHelper.icon("fas-exchange-alt", 14, UIHelper.ACCENT_BLUE), this::convert)
                .addAction("Converter em Factura", UIHelper.icon("fas-file-invoice-dollar", 14, UIHelper.APPROVED_GREEN), this::convertToInvoice);
        JPanel filters = UIHelper.filterBar(
                new JComponent[]{search, TableFilter.label("Estado:"), status,
                        TableFilter.label("Data:", "fas-calendar-alt"), periodo},
                null);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(UIHelper.tableCardTop("Cotações", filters, refreshBtn, more, decision, newBtn), BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);

        listPage.add(card, BorderLayout.CENTER);

        editorForm = new QuotationEditorForm(this);
        editorHost = new DocumentEditorHost("Nova Cotação", editorForm.component(),
                this::saveEditor, this::showList, editorForm::isDirty);
        add(listPage, "LIST");
        add(editorHost, "EDITOR");
        pages.show(this, "LIST");
    }

    private ModernButton button(String text, String icon, ModernButton button, Runnable action) {
        button.setText(text);
        button.setIcon(UIHelper.icon(icon, 14));
        button.addActionListener(e -> action.run());
        return button;
    }

    public void refresh() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> apiClient.getQuotationsByCompany(companyId), quotations -> {
            model.setRowCount(0);
            for (QuotationDTO q : quotations) {
                model.addRow(new Object[]{
                        q.id(),
                        q.quotationNumber(),
                        q.quotationDate() == null ? "—" : q.quotationDate().format(DATE_TIME),
                        q.clientName(),
                        blank(q.clientTaxId()),
                        q.totalAmount(),
                        q.statusLabel(),
                        validityLabel(q),
                        blank(q.invoiceNumber() != null ? q.invoiceNumber() : q.orderNumber())});
            }
        }, error -> showError("carregar cotações", error));
    }

    /**
     * A validade dita como o operador a lê: caducada, a caducar hoje, ou os dias que faltam. Sai do
     * que o servidor calculou — este método só escolhe as palavras.
     */
    private static String validityLabel(QuotationDTO q) {
        String until = q.validUntil() == null ? "—" : q.validUntil().format(DATE);
        if (q.expired()) {
            return "Caducada em " + until;
        }
        if (q.daysUntilExpiry() == 0) {
            return "Caduca hoje (" + until + ")";
        }
        return until + " (faltam " + q.daysUntilExpiry() + " dias)";
    }

    private void openEditor() {
        if (warehouses.get().isEmpty() || products.get().isEmpty()) {
            showNotice(FeedbackType.WARNING, "Dados necessários",
                    "Registe um armazém e pelo menos um produto antes de criar a cotação.");
            return;
        }
        editorForm.setOptions(clients.get(), products.get(), warehouses.get());
        editorForm.prepareCreate();
        editorHost.setEditorTitle("Nova Cotação");
        editorHost.setSaveText("Emitir Cotação");
        editorHost.setSaveEnabled(true);
        pages.show(this, "EDITOR");
    }

    private void openSelectedEditor() {
        int row = selected("editar ou consultar");
        if (row < 0) return;
        Long id = (Long) model.getValueAt(row, COL_ID);
        UIHelper.loadAsync(this, () -> apiClient.getQuotationById(id), quotation -> {
            editorForm.setOptions(clients.get(), products.get(), warehouses.get());
            editorForm.load(quotation);
            boolean editable = "DRAFT".equals(quotation.status());
            editorHost.setEditorTitle((editable ? "Editar Cotação " : "Consultar Cotação ")
                    + quotation.quotationNumber());
            editorHost.setSaveText("Guardar alterações");
            editorHost.setSaveEnabled(editable);
            pages.show(this, "EDITOR");
            if (!editable) {
                showNotice(FeedbackType.INFO, "Cotação protegida",
                        "Depois do envio, a cotação fica apenas para consulta. Use as operações próprias do estado.");
            }
        }, error -> showError("carregar a cotação", error));
    }

    void saveEditor() {
        try {
            QuotationDTO loaded = editorForm.loaded();
            if (loaded == null) {
                var request = editorForm.createRequest();
                UIHelper.runWithProgress(this, "A emitir cotação…", () -> apiClient.createQuotation(request),
                        saved -> finishSave(saved, "emitida"), error -> showError("emitir a cotação", error));
            } else {
                var request = editorForm.updateRequest();
                UIHelper.runWithProgress(this, "A guardar alterações…",
                        () -> apiClient.updateQuotation(loaded.id(), request),
                        saved -> finishSave(saved, "actualizada"),
                        error -> showError("actualizar a cotação", error));
            }
        } catch (RuntimeException error) {
            showNotice(FeedbackType.ERROR, "Dados incompletos", error.getMessage());
        }
    }

    private void finishSave(QuotationDTO saved, String action) {
        editorForm.markClean();
        showSuccess("Cotação " + saved.quotationNumber() + " " + action + "; total "
                + saved.totalAmount() + " MT.");
        showList();
        refresh();
    }

    private void showList() {
        pages.show(this, "LIST");
    }

    private void send() {
        int row = selected("marcar como enviada");
        if (row < 0) return;
        Long id = (Long) model.getValueAt(row, COL_ID);
        String number = String.valueOf(model.getValueAt(row, COL_NUMBER));
        UIHelper.runWithProgress(this, "A registar envio…", () -> apiClient.sendQuotation(id), ignored -> {
            showSuccess("Cotação " + number + " marcada como enviada ao cliente.");
            refresh();
        }, error -> showError("marcar a cotação como enviada", error));
    }

    private void accept() {
        int row = selected("registar a aceitação");
        if (row < 0) return;
        Long id = (Long) model.getValueAt(row, COL_ID);
        String number = String.valueOf(model.getValueAt(row, COL_NUMBER));
        UIHelper.runWithProgress(this, "A registar aceitação…", () -> apiClient.acceptQuotation(id), ignored -> {
            showSuccess("Cotação " + number + " aceite; pode agora convertê-la em encomenda.");
            refresh();
        }, error -> showError("registar a aceitação", error));
    }

    private void reject() {
        int row = selected("registar a recusa");
        if (row < 0) return;
        Long id = (Long) model.getValueAt(row, COL_ID);
        String number = String.valueOf(model.getValueAt(row, COL_NUMBER));
        String reason = UIHelper.promptRequiredText("Cotação Recusada", "fas-times",
                "Cotação " + number, "Motivo da recusa do cliente:");
        if (reason == null) return;
        UIHelper.runWithProgress(this, "A registar recusa…", () -> apiClient.rejectQuotation(id, reason),
                ignored -> {
                    showSuccess("Cotação " + number + " registada como recusada.");
                    refresh();
                }, error -> showError("registar a recusa", error));
    }

    private void cancel() {
        int row = selected("cancelar");
        if (row < 0) return;
        Long id = (Long) model.getValueAt(row, COL_ID);
        String number = String.valueOf(model.getValueAt(row, COL_NUMBER));
        if (JOptionPane.showConfirmDialog(this, "Cancelar a cotação " + number + "?",
                "Confirmar Cancelamento", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        UIHelper.runWithProgress(this, "A cancelar cotação…", () -> apiClient.cancelQuotation(id), ignored -> {
            showSuccess("Cotação " + number + " cancelada.");
            refresh();
        }, error -> showError("cancelar a cotação", error));
    }

    /** Estender é conceder: o diálogo di-lo antes de o operador escolher a data. */
    private void extendValidity() {
        int row = selected("estender a validade");
        if (row < 0) return;
        Long id = (Long) model.getValueAt(row, COL_ID);
        String number = String.valueOf(model.getValueAt(row, COL_NUMBER));
        String current = String.valueOf(model.getValueAt(row, COL_VALIDITY));

        mz.multicore.erp.gui.components.DateField validity = new mz.multicore.erp.gui.components.DateField();
        JPanel form = UIHelper.createDialogForm("Validade actual:", new JLabel(current),
                "Nova validade:", validity);
        boolean confirmed = new mz.multicore.erp.gui.components.ModernFormDialog(UIHelper.mainWindow,
                "Estender Validade", "fas-calendar-alt",
                "Estender volta a garantir ao cliente os preços desta proposta", form)
                .setConfirmButton("Estender", "fas-check").showDialog();
        if (!confirmed) return;
        LocalDate newValidUntil = validity.value();

        UIHelper.runWithProgress(this, "A estender validade…",
                () -> apiClient.extendQuotationValidity(id, newValidUntil), updated -> {
                    showSuccess("Cotação " + number + " válida até " + updated.validUntil().format(DATE) + ".");
                    refresh();
                }, error -> showError("estender a validade", error));
    }

    /**
     * Converte na encomenda. O aviso diz o que vai acontecer a seguir — a encomenda gerada é formal
     * e fica pendente de aprovação, coisa que quem converte precisa de saber para não ficar à espera
     * de poder facturar já.
     */
    private void convert() {
        int row = selected("converter");
        if (row < 0) return;
        Long id = (Long) model.getValueAt(row, COL_ID);
        String number = String.valueOf(model.getValueAt(row, COL_NUMBER));
        if (JOptionPane.showConfirmDialog(this,
                "Converter a cotação " + number + " numa encomenda?\n\n"
                        + "A encomenda mantém exactamente os preços cotados e fica pendente de aprovação.\n"
                        + "A cotação não poderá ser convertida outra vez.",
                "Confirmar Conversão", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) return;

        UIHelper.runWithProgress(this, "A converter em encomenda…", () -> apiClient.convertQuotation(id),
                order -> {
                    showSuccess("Cotação " + number + " convertida na encomenda " + order.orderNumber()
                            + "; total " + order.totalAmount() + " MT, pendente de aprovação.");
                    refresh();
                    ordersRefresh.run();
                }, error -> showError("converter a cotação", error));
    }

    /**
     * Converte na factura comercial directamente. A factura mantém os preços acordados
     * na cotação, efectua baixa de stock no armazém e assume o vencimento do cliente.
     */
    private void convertToInvoice() {
        int row = selected("converter em factura");
        if (row < 0) return;
        Long id = (Long) model.getValueAt(row, COL_ID);
        String number = String.valueOf(model.getValueAt(row, COL_NUMBER));
        if (JOptionPane.showConfirmDialog(this,
                "Converter a cotação " + number + " directamente numa Factura comercial (FT)?\n\n"
                        + "A factura mantém exactamente os preços cotados, efectua a baixa de stock e cria a conta a receber.\n"
                        + "A cotação não poderá ser convertida outra vez.",
                "Confirmar Emissão de Factura", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) return;

        UIHelper.runWithProgress(this, "A converter em factura…", () -> apiClient.convertQuotationToInvoice(id),
                invoice -> {
                    showSuccess("Cotação " + number + " convertida na factura " + invoice.invoiceNumber()
                            + "; total " + invoice.totalAmount() + " MT.");
                    refresh();
                    ordersRefresh.run();
                }, error -> showError("converter a cotação em factura", error));
    }

    private void print() {
        int row = selected("imprimir");
        if (row < 0) return;
        Long id = (Long) model.getValueAt(row, COL_ID);
        String number = String.valueOf(model.getValueAt(row, COL_NUMBER));
        UIHelper.runWithProgress(this, "A gerar cotação em PDF…", () -> apiClient.renderQuotation(id),
                pdf -> PrintPreviewDialog.show(this, pdf, "cotacao-" + number),
                error -> showError("gerar a cotação em PDF", error));
    }

    private void showLines() {
        int row = selected("consultar");
        if (row < 0) return;
        Long id = (Long) model.getValueAt(row, COL_ID);
        UIHelper.loadAsync(this, () -> apiClient.getQuotationById(id), quotation -> {
            DefaultTableModel lines = new DefaultTableModel(
                    new String[]{"Produto", "SKU", "Qtd", "Preço Unit.", "IVA", "Desc.", "Total"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            quotation.lines().forEach(line -> lines.addRow(new Object[]{
                    line.productName(), line.productSku(), line.quantity(), line.unitPrice(),
                    line.taxRate(), line.discountPercentage(), line.lineTotal()}));
            JTable details = new JTable(lines);
            UIHelper.styleTable(details);
            JScrollPane scroll = new JScrollPane(details);
            UIHelper.styleScrollPane(scroll);
            scroll.setPreferredSize(new Dimension(900, 300));
            JOptionPane.showMessageDialog(this, scroll, "Linhas — " + quotation.quotationNumber(),
                    JOptionPane.PLAIN_MESSAGE);
        }, error -> showError("carregar as linhas da cotação", error));
    }

    private int selected(String action) {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0) {
            showNotice(FeedbackType.WARNING, "Seleccione uma cotação",
                    "Escolha uma cotação na tabela para " + action + ".");
        }
        return row;
    }

    private void hideColumn(int index) {
        table.getColumnModel().getColumn(index).setMinWidth(0);
        table.getColumnModel().getColumn(index).setMaxWidth(0);
        table.getColumnModel().getColumn(index).setWidth(0);
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private void showError(String action, Throwable error) {
        showNotice(FeedbackType.ERROR, "Não foi possível " + action, error.getMessage());
    }

    void showNotice(FeedbackType type, String title, String message) {
        feedback.show(type, title, message, null, null);
    }

    void showSuccess(String message) { ToastManager.success(this, message); }
}
