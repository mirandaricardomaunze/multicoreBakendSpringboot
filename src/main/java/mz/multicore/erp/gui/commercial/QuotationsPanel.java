package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.ActionMenuButton;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.comercial.dto.QuotationDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.printing.PdfFileSaver;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

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

        setLayout(new BorderLayout(0, 15));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.add(UIHelper.createHeading("Cotações"), BorderLayout.WEST);
        ModernButton newBtn = UIHelper.createPrimaryButton("Nova Cotação");
        newBtn.setIcon(UIHelper.icon("fas-file-signature", 14));
        newBtn.addActionListener(e -> openEditor());
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerActions.setOpaque(false);
        headerActions.add(newBtn);
        header.add(headerActions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        model = new DefaultTableModel(new String[]{
                "ID", "Nº Cotação", "Data", "Cliente", "NUIT", "Total", "Estado", "Validade", "Encomenda"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(COL_STATUS).setCellRenderer(TableCellRenderers.status());
        hideColumn(COL_ID);

        JTextField search = TableFilter.searchField("Nº cotação, cliente ou NUIT…");
        JComboBox<String> status = TableFilter.combo("Todos os estados",
                "Rascunho", "Enviada", "Aceite", "Recusada", "Convertida", "Cancelada");
        TableFilter.install(table, search, new TableFilter.ColumnFilter(status, COL_STATUS));
        JPanel filters = TableFilter.bar(search, TableFilter.label("Estado:"), status);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(filters, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        ActionMenuButton more = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Imprimir", UIHelper.icon("fas-print", 14), this::print)
                .addAction("Ver linhas", UIHelper.icon("fas-list", 14), this::showLines)
                .addAction("Marcar como enviada", UIHelper.icon("fas-paper-plane", 14), this::send)
                .addAction("Estender validade", UIHelper.icon("fas-calendar-plus", 14), this::extendValidity)
                .addAction("Cancelar cotação", UIHelper.icon("fas-ban", 14), this::cancel);
        actions.add(UIHelper.createRefreshButton(this::refresh));
        actions.add(more);
        actions.add(button("Recusada pelo cliente", "fas-times", UIHelper.createDangerButton(""), this::reject));
        actions.add(button("Aceite pelo cliente", "fas-check", UIHelper.createSuccessButton(""), this::accept));
        actions.add(button("Converter em Encomenda", "fas-exchange-alt",
                UIHelper.createPrimaryButton(""), this::convert));
        card.add(actions, BorderLayout.SOUTH);

        add(card, BorderLayout.CENTER);
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
                        blank(q.orderNumber())});
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
        QuotationDTO created = new QuotationEditorDialog(this, apiClient,
                clients.get(), products.get(), warehouses.get()).open();
        if (created == null) return;
        JOptionPane.showMessageDialog(this,
                "Cotação " + created.quotationNumber() + " emitida.\n"
                        + "Total: " + created.totalAmount() + " MT.\n"
                        + "Válida até " + created.validUntil().format(DATE) + ".",
                "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        refresh();
    }

    private void send() {
        int row = selected("marcar como enviada");
        if (row < 0) return;
        Long id = (Long) model.getValueAt(row, COL_ID);
        String number = String.valueOf(model.getValueAt(row, COL_NUMBER));
        UIHelper.runWithProgress(this, "A registar envio…", () -> apiClient.sendQuotation(id), ignored -> {
            JOptionPane.showMessageDialog(this, "Cotação " + number + " marcada como enviada ao cliente.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            refresh();
        }, error -> showError("marcar a cotação como enviada", error));
    }

    private void accept() {
        int row = selected("registar a aceitação");
        if (row < 0) return;
        Long id = (Long) model.getValueAt(row, COL_ID);
        String number = String.valueOf(model.getValueAt(row, COL_NUMBER));
        UIHelper.runWithProgress(this, "A registar aceitação…", () -> apiClient.acceptQuotation(id), ignored -> {
            JOptionPane.showMessageDialog(this,
                    "Cotação " + number + " marcada como aceite.\nPode agora convertê-la em encomenda.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
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
                    JOptionPane.showMessageDialog(this, "Cotação " + number + " registada como recusada.",
                            "Sucesso", JOptionPane.INFORMATION_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "Cotação " + number + " cancelada.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
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

        String input = JOptionPane.showInputDialog(this,
                "Cotação " + number + " — validade actual: " + current + ".\n\n"
                        + "Estender a validade volta a garantir ao cliente os preços desta proposta.\n"
                        + "Nova data de validade (aaaa-mm-dd):",
                "Estender Validade", JOptionPane.QUESTION_MESSAGE);
        if (input == null || input.isBlank()) return;

        LocalDate newValidUntil;
        try {
            newValidUntil = LocalDate.parse(input.trim());
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Data inválida. Use o formato aaaa-mm-dd (ex.: 2026-09-30).",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        UIHelper.runWithProgress(this, "A estender validade…",
                () -> apiClient.extendQuotationValidity(id, newValidUntil), updated -> {
                    JOptionPane.showMessageDialog(this, "Cotação " + number + " válida até "
                                    + updated.validUntil().format(DATE) + ".",
                            "Sucesso", JOptionPane.INFORMATION_MESSAGE);
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
                    JOptionPane.showMessageDialog(this,
                            "Cotação " + number + " convertida na encomenda " + order.orderNumber() + ".\n"
                                    + "Total: " + order.totalAmount() + " MT.\n"
                                    + "A encomenda aguarda aprovação antes de poder ser facturada.",
                            "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                    refresh();
                    ordersRefresh.run();
                }, error -> showError("converter a cotação", error));
    }

    private void print() {
        int row = selected("imprimir");
        if (row < 0) return;
        Long id = (Long) model.getValueAt(row, COL_ID);
        String number = String.valueOf(model.getValueAt(row, COL_NUMBER));
        UIHelper.runWithProgress(this, "A gerar cotação em PDF…", () -> apiClient.renderQuotation(id),
                pdf -> PdfFileSaver.saveAndOpen(pdf, "cotacao-" + number),
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
            JOptionPane.showMessageDialog(this, "Selecione uma cotação na tabela para " + action + ".",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
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
        JOptionPane.showMessageDialog(this, "Não foi possível " + action + ": " + error.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
