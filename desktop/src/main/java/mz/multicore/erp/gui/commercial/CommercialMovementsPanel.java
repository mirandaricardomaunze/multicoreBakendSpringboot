package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.MovimentosApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.gui.components.TableExportAction;
import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableQuickPeekController;
import mz.multicore.erp.gui.components.ClientTablePagination;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.movimentos.dto.MovimentoDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/** Aba autónoma da vista unificada de movimentos e documentos comerciais/operacionais. */
public final class CommercialMovementsPanel extends JPanel {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final MovimentosApiClient apiClient;
    private final PrintApiClient printApiClient;
    private final InventoryApiClient inventoryApiClient;
    private final DefaultTableModel model;
    private final JTable table;
    private final JTextField search;
    private final JComboBox<String> productFilter;
    private final JComboBox<String> warehouseFilter;
    private final JComboBox<String> typeFilter;
    private final DateField fromField;
    private final DateField toField;
    private final JLabel footer;
    private List<MovimentoDTO> data = List.of();
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    public CommercialMovementsPanel(MovimentosApiClient apiClient) {
        this(apiClient, null, null);
    }

    public CommercialMovementsPanel(MovimentosApiClient apiClient, PrintApiClient printApiClient) {
        this(apiClient, printApiClient, null);
    }

    public CommercialMovementsPanel(MovimentosApiClient apiClient, PrintApiClient printApiClient, InventoryApiClient inventoryApiClient) {
        this.apiClient = apiClient;
        this.printApiClient = printApiClient;
        this.inventoryApiClient = inventoryApiClient;
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(15, 5, 5, 5));

        ModernButton refresh = UIHelper.createSecondaryButton("Actualizar");
        refresh.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refresh.addActionListener(e -> refresh());
        add(feedback, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        model = new DefaultTableModel(new String[]{"Tipo", "Nº", "Cliente / Destino", "Artigos", "Qtd", "Armazém", "Data", "Estado", "Total"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.quantity());
        table.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.status());
        table.getColumnModel().getColumn(8).setCellRenderer(TableCellRenderers.money());
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        search = TableFilter.searchField("Nº, cliente, artigo ou armazém…");
        productFilter = TableFilter.combo("Todos os artigos");
        warehouseFilter = TableFilter.combo("Todos os armazéns");
        typeFilter = TableFilter.combo("Todos os tipos",
                "Fatura", "Venda POS", "Cotação", "Encomenda", "Guia de Remessa",
                "Nota de Crédito", "Nota de Débito", "Recibo", "Transferência");
        fromField = new DateField(null);
        toField = new DateField(null);

        UIHelper.styleComboBox(productFilter);
        UIHelper.styleComboBox(warehouseFilter);
        UIHelper.styleComboBox(typeFilter);

        search.setPreferredSize(new Dimension(160, UIHelper.FORM_CONTROL_HEIGHT));
        productFilter.setPreferredSize(new Dimension(145, UIHelper.FORM_CONTROL_HEIGHT));
        warehouseFilter.setPreferredSize(new Dimension(140, UIHelper.FORM_CONTROL_HEIGHT));
        typeFilter.setPreferredSize(new Dimension(115, UIHelper.FORM_CONTROL_HEIGHT));
        fromField.setPreferredSize(new Dimension(95, UIHelper.FORM_CONTROL_HEIGHT));
        toField.setPreferredSize(new Dimension(95, UIHelper.FORM_CONTROL_HEIGHT));
        fromField.putClientProperty("JTextField.placeholderText", "aaaa-mm-dd");
        toField.putClientProperty("JTextField.placeholderText", "aaaa-mm-dd");

        TableFilter.install(table, search,
                List.of(new TableFilter.ColumnFilter(typeFilter, 0)),
                List.of(),
                List.of(new TableFilter.DateRangeFilter(fromField, toField, 6)),
                List.of(
                        new TableFilter.ContainsColumnFilter(productFilter, 3),
                        new TableFilter.ContainsColumnFilter(warehouseFilter, 5)
                ));

        UIHelper.onTextChange(search, this::updateFooter);
        productFilter.addActionListener(e -> updateFooter());
        warehouseFilter.addActionListener(e -> updateFooter());
        typeFilter.addActionListener(e -> updateFooter());
        UIHelper.onTextChange(fromField, this::updateFooter);
        UIHelper.onTextChange(toField, this::updateFooter);

        UIHelper.ensureHeadersFit(table);
        JPanel filterBar = TableFilter.bar(
                search,
                TableFilter.label("Artigo:"), productFilter,
                TableFilter.label("Armazém:"), warehouseFilter,
                TableFilter.label("Tipo:"), typeFilter,
                TableFilter.label("De:"), fromField,
                TableFilter.label("Até:"), toField
        );
        filterBar.setBorder(new EmptyBorder(0, 0, 10, 0));

        TableQuickPeekController peekController = TableQuickPeekController.install(table, card, (peek, mr) -> {
            int row = table.convertRowIndexToModel(mr);
            peek.setHeaderIcon("fas-exchange-alt", UIHelper.ACCENT_BLUE);
            peek.setTitle(String.valueOf(model.getValueAt(row, 0)) + " " + model.getValueAt(row, 1));
            peek.setSubtitle("Entidade: " + model.getValueAt(row, 2) + " • " + model.getValueAt(row, 6));
            peek.setStatus(UIHelper.humanStatus(String.valueOf(model.getValueAt(row, 7))), UIHelper.ACCENT_BLUE);
            List<mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem> items = new ArrayList<>();
            items.add(new mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem("Tipo Documento", String.valueOf(model.getValueAt(row, 0)), false));
            items.add(new mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem("Número", String.valueOf(model.getValueAt(row, 1)), false));
            items.add(new mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem("Cliente / Destino", String.valueOf(model.getValueAt(row, 2)), false));
            items.add(new mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem("Artigos", String.valueOf(model.getValueAt(row, 3)), false));
            items.add(new mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem("Qtd Movimentada", String.valueOf(model.getValueAt(row, 4)), true));
            items.add(new mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem("Armazém", String.valueOf(model.getValueAt(row, 5)), false));
            items.add(new mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem("Data Emissão", String.valueOf(model.getValueAt(row, 6)), false));
            items.add(new mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem("Total Geral", String.valueOf(model.getValueAt(row, 8)), true));
            peek.setItems(items);
        });

        ModernButton printBtn = UIHelper.createSecondaryButton("Imprimir movimentos");
        printBtn.setIcon(UIHelper.icon("fas-print", 14, UIHelper.ACCENT_BLUE));
        printBtn.addActionListener(e -> {
            StringBuilder title = new StringBuilder("Movimentos Globais");
            if (warehouseFilter.getSelectedIndex() > 0) {
                title.append(" — Armazém: ").append(warehouseFilter.getSelectedItem());
            }
            if (productFilter.getSelectedIndex() > 0) {
                title.append(" — Artigo: ").append(productFilter.getSelectedItem());
            }
            if (typeFilter.getSelectedIndex() > 0) {
                title.append(" — Tipo: ").append(typeFilter.getSelectedItem());
            }
            if (fromField.getDate() != null || toField.getDate() != null) {
                String dFrom = fromField.getDate() != null ? fromField.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
                String dTo = toField.getDate() != null ? toField.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
                if (!dFrom.isEmpty() && !dTo.isEmpty()) {
                    title.append(" (").append(dFrom).append(" a ").append(dTo).append(")");
                } else if (!dFrom.isEmpty()) {
                    title.append(" (desde ").append(dFrom).append(")");
                } else {
                    title.append(" (até ").append(dTo).append(")");
                }
            }
            TableExportAction.export(this, printApiClient, table, title.toString(), "movimentos-globais");
        });

        card.add(UIHelper.tableCardTop("Movimentos — Todos os Documentos Comerciais & Operacionais", filterBar,
                refresh, printBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        footer = new JLabel(" ");
        footer.setForeground(UIHelper.TEXT_MUTED);
        footer.setBorder(new EmptyBorder(8, 4, 0, 4));
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setOpaque(false);
        southPanel.add(ClientTablePagination.install(table), BorderLayout.NORTH);
        southPanel.add(footer, BorderLayout.SOUTH);
        card.add(southPanel, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    public void refresh() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        loadWarehouses(companyId);
        UIHelper.loadAsync(this, () -> apiClient.listar(companyId, "", null, null), this::apply,
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar movimentos comerciais",
                        error.getMessage(), "Tentar novamente", this::refresh));
    }

    private void loadWarehouses(Long companyId) {
        if (inventoryApiClient == null || companyId == null) return;
        UIHelper.loadAsync(this, () -> inventoryApiClient.getWarehousesByCompany(companyId), warehouses -> {
            if (warehouses == null) return;
            Object currentSel = warehouseFilter.getSelectedItem();
            TreeSet<String> whNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            for (WarehouseDTO w : warehouses) {
                if (w.name() != null && !w.name().isBlank()) {
                    whNames.add(w.name());
                }
            }
            for (MovimentoDTO m : data) {
                if (m.armazem() != null && !m.armazem().isBlank() && !m.armazem().equals("—")) {
                    if (m.armazem().contains("→")) {
                        for (String part : m.armazem().split("→")) {
                            String c = part.trim();
                            if (!c.isEmpty() && !c.equals("-")) whNames.add(c);
                        }
                    } else {
                        whNames.add(m.armazem().trim());
                    }
                }
            }
            warehouseFilter.removeAllItems();
            warehouseFilter.addItem("Todos os armazéns");
            whNames.forEach(warehouseFilter::addItem);
            if (currentSel != null) {
                warehouseFilter.setSelectedItem(currentSel);
            }
        }, error -> {
            // Silencioso; manterá armazéns dos movimentos
        });
    }

    private void apply(List<MovimentoDTO> loaded) {
        data = loaded;
        model.setRowCount(0);

        Object selectedProduct = productFilter == null ? null : productFilter.getSelectedItem();
        Object selectedWh = warehouseFilter == null ? null : warehouseFilter.getSelectedItem();
        TreeSet<String> productNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        TreeSet<String> whNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (MovimentoDTO movement : data) {
            String artigos = movement.artigos() != null && !movement.artigos().isBlank()
                    ? movement.artigos()
                    : "—";

            if (movement.artigos() != null && !movement.artigos().isBlank()) {
                String[] parts = movement.artigos().split(",");
                for (String p : parts) {
                    String clean = p.trim();
                    if (!clean.isEmpty()) {
                        productNames.add(clean);
                    }
                }
            }

            String armazem = movement.armazem() != null && !movement.armazem().isBlank()
                    ? movement.armazem()
                    : "—";

            if (!armazem.equals("—")) {
                if (armazem.contains("→")) {
                    for (String part : armazem.split("→")) {
                        String c = part.trim();
                        if (!c.isEmpty() && !c.equals("-")) whNames.add(c);
                    }
                } else {
                    whNames.add(armazem.trim());
                }
            }

            BigDecimal qty = movement.quantidade() != null ? movement.quantidade() : BigDecimal.ZERO;

            model.addRow(new Object[]{
                    movement.tipo().getLabel(),
                    movement.numero() == null ? "-" : movement.numero(),
                    movement.cliente() == null ? "-" : movement.cliente(),
                    artigos,
                    qty,
                    armazem,
                    movement.data() == null ? "-" : movement.data().format(DATE_TIME),
                    UIHelper.humanStatus(movement.estado()),
                    movement.total() == null ? BigDecimal.ZERO : movement.total()
            });
        }

        if (productFilter != null) {
            productFilter.removeAllItems();
            productFilter.addItem("Todos os artigos");
            productNames.forEach(productFilter::addItem);
            if (selectedProduct != null) {
                productFilter.setSelectedItem(selectedProduct);
            }
        }

        if (warehouseFilter != null && (warehouseFilter.getItemCount() <= 1 || inventoryApiClient == null)) {
            warehouseFilter.removeAllItems();
            warehouseFilter.addItem("Todos os armazéns");
            whNames.forEach(warehouseFilter::addItem);
            if (selectedWh != null) {
                warehouseFilter.setSelectedItem(selectedWh);
            }
        }

        updateFooter();
    }

    private void updateFooter() {
        String query = search.getText();
        String selectedProduct = productFilter.getSelectedIndex() > 0 ? String.valueOf(productFilter.getSelectedItem()) : null;
        String selectedWarehouse = warehouseFilter.getSelectedIndex() > 0 ? String.valueOf(warehouseFilter.getSelectedItem()) : null;
        String selectedType = typeFilter.getSelectedIndex() > 0 ? String.valueOf(typeFilter.getSelectedItem()) : null;
        Map<Integer, String> exact = selectedType != null ? Map.of(0, selectedType) : Map.of();
        LocalDate fromDate = fromField.getDate();
        LocalDate toDate = toField.getDate();

        int count = 0;
        BigDecimal sumQty = BigDecimal.ZERO;
        BigDecimal sumVal = BigDecimal.ZERO;

        for (MovimentoDTO movement : data) {
            BigDecimal total = movement.total() == null ? BigDecimal.ZERO : movement.total();
            BigDecimal qty = movement.quantidade() != null ? movement.quantidade() : BigDecimal.ZERO;
            String date = movement.data() == null ? "-" : movement.data().format(DATE_TIME);
            String artigos = movement.artigos() != null ? movement.artigos() : "";
            String armazem = movement.armazem() != null ? movement.armazem() : "";

            List<String> cells = List.of(
                    movement.tipo().getLabel(),
                    movement.numero() == null ? "-" : movement.numero(),
                    movement.cliente() == null ? "" : movement.cliente(),
                    artigos,
                    qty.toPlainString(),
                    armazem,
                    date,
                    movement.estado() == null ? "" : movement.estado(),
                    total.toPlainString()
            );

            if (!TableFilter.rowMatches(cells, query, exact)) continue;

            if (selectedProduct != null && !artigos.toLowerCase(Locale.ROOT).contains(selectedProduct.toLowerCase(Locale.ROOT))) {
                continue;
            }

            if (selectedWarehouse != null && !armazem.toLowerCase(Locale.ROOT).contains(selectedWarehouse.toLowerCase(Locale.ROOT))) {
                continue;
            }

            if (!TableFilter.matchesDateRange(TableFilter.parseCellDate(date), fromDate, toDate)) {
                continue;
            }

            count++;
            sumQty = sumQty.add(qty);
            sumVal = sumVal.add(total);
        }
        footer.setText(String.format("%d documento(s) · Qtd Total: %,.2f · Valor Total: %,.2f MT", count, sumQty, sumVal));
    }
}
