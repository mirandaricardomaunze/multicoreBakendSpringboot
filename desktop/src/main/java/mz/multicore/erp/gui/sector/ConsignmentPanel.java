package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.ConsignmentApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.consignment.dto.*;
import mz.multicore.erp.modules.consignment.model.ConsignmentStatus;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Consignação e Rota (SA-C11, {@code docs/CONSIGNACAO_ROTA_SPEC.md}). In the morning the driver leaves bread at each
 * reseller (guia CG); the next day the leftovers come back. "Acertar" counts what returned good and what is stale,
 * and invoices the reseller only for what was sold, at the reseller price list.
 */
public class ConsignmentPanel extends JPanel {

    private static final String[] COLUMNS = {"Data", "Guia", "Revendedor", "Rota", "Artigos", "Entregue", "Vendido", "Estado", "Fatura", "Faturado"};
    private static final String[] LINE_COLUMNS = {"Artigo", "Entregue", "Voltou bom", "Estragado", "Vendido"};
    static final String OPEN = "NO REVENDEDOR";
    static final String SETTLED = "ACERTADA";

    private final ConsignmentApiClient api;
    private final ComercialApiClient comercialApi;
    private final InventoryApiClient inventoryApi;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final DefaultTableModel model = readOnly(COLUMNS);
    private final JTable table = new JTable(model);
    private final DefaultTableModel lineModel = readOnly(LINE_COLUMNS);
    private final ModernButton settleBtn = UIHelper.createPrimaryButton("Acertar");
    private final JLabel detailTitle = new JLabel(" ");
    private final JLabel detailTotals = new JLabel(" ");
    private final CardLayout cards = new CardLayout();
    private final JPanel detail = new JPanel(cards);
    private final List<ConsignmentDTO> deliveries = new ArrayList<>();
    private Consumer<List<ConsignmentDTO>> onLoaded = list -> { };

    public ConsignmentPanel(ConsignmentApiClient api, ComercialApiClient comercialApi, InventoryApiClient inventoryApi) {
        this.api = api;
        this.comercialApi = comercialApi;
        this.inventoryApi = inventoryApi;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        add(feedback, BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildList(), buildDetail());
        split.setOpaque(false);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setResizeWeight(0.62);
        split.setDividerSize(10);
        add(split, BorderLayout.CENTER);
        settleBtn.setEnabled(false);
    }

    public void onLoaded(Consumer<List<ConsignmentDTO>> listener) {
        this.onLoaded = listener == null ? list -> { } : listener;
    }

    private JComponent buildList() {
        UIHelper.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.status());
        table.getColumnModel().getColumn(9).setCellRenderer(TableCellRenderers.money());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showDetail();
        });
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);
        JTextField search = TableFilter.searchField("Revendedor, rota ou guia…");
        search.setPreferredSize(new Dimension(190, UIHelper.FORM_CONTROL_HEIGHT));
        JComboBox<String> state = TableFilter.combo("Todos os estados", OPEN, SETTLED);
        UIHelper.styleComboBox(state);
        state.setPreferredSize(new Dimension(160, UIHelper.FORM_CONTROL_HEIGHT));
        state.setSelectedItem(OPEN);
        TableFilter.install(table, search, new TableFilter.ColumnFilter(state, 7));
        JPanel filters = TableFilter.bar(search, TableFilter.label("Estado:"), state);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));
        ModernButton newBtn = UIHelper.createSuccessButton("Nova Entrega");
        newBtn.setIcon(UIHelper.icon("fas-truck-loading", 14));
        newBtn.addActionListener(e -> openDelivery());
        settleBtn.setIcon(UIHelper.icon("fas-balance-scale", 14));
        settleBtn.setToolTipText("Conta o que voltou e fatura ao revendedor só o vendido (gestão)");
        settleBtn.addActionListener(e -> openSettle());
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.add(UIHelper.tableCardTop("Guias de Consignação", filters, UIHelper.createRefreshButton(this::reload), settleBtn, newBtn),
                BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(table), BorderLayout.SOUTH);
        return card;
    }

    private JComponent buildDetail() {
        ModernPanel empty = new ModernPanel(16);
        empty.setLayout(new GridBagLayout());
        JLabel hint = new JLabel("<html><div style='text-align:center;width:280px'><b>Entregue, devolvido e vendido</b><br>Escolha "
                + "uma guia para ver cada artigo. Só o vendido é faturado ao revendedor.</div></html>",
                UIHelper.icon("fas-truck", 36, UIHelper.ACCENT_BLUE), SwingConstants.CENTER);
        hint.setHorizontalTextPosition(SwingConstants.CENTER);
        hint.setVerticalTextPosition(SwingConstants.BOTTOM);
        hint.setForeground(UIHelper.TEXT_MUTED);
        empty.add(hint);
        JTable lines = new JTable(lineModel);
        UIHelper.styleTable(lines);
        JScrollPane scroll = new JScrollPane(lines);
        UIHelper.styleScrollPane(scroll);
        detailTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        detailTitle.setForeground(UIHelper.TEXT_LIGHT);
        detailTotals.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.add(detailTitle, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(detailTotals, BorderLayout.SOUTH);
        detail.setOpaque(false);
        detail.add(empty, "empty");
        detail.add(card, "detail");
        cards.show(detail, "empty");
        return detail;
    }

    public void reload() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null || api == null) return;
        Long keep = selected() == null ? null : selected().id();
        UIHelper.loadAsync(this, () -> api.list(companyId, 30), loaded -> apply(loaded, keep),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar as consignações", error.getMessage(), "Tentar novamente", this::reload));
    }

    private void apply(List<ConsignmentDTO> loaded, Long keep) {
        feedback.clear();
        deliveries.clear();
        deliveries.addAll(loaded);
        model.setRowCount(0);
        for (ConsignmentDTO d : deliveries) {
            model.addRow(new Object[]{UIHelper.formatDate(d.deliveryDate()), d.number(), d.clientName(), d.route() == null ? "—" : d.route(),
                    d.lines().size(), qty(sum(d, true)), d.status() == ConsignmentStatus.SETTLED ? qty(sold(d)) : "—",
                    d.status() == ConsignmentStatus.SETTLED ? SETTLED : OPEN, d.invoiceNumber() == null ? "—" : d.invoiceNumber(),
                    d.invoiceTotal()});
        }
        if (keep != null) {
            for (int i = 0; i < deliveries.size(); i++) {
                if (!deliveries.get(i).id().equals(keep)) continue;
                int view = table.convertRowIndexToView(i);
                if (view >= 0) table.setRowSelectionInterval(view, view);
            }
        }
        showDetail();
        onLoaded.accept(List.copyOf(deliveries));
    }

    private void showDetail() {
        ConsignmentDTO d = selected();
        settleBtn.setEnabled(d != null && d.status() == ConsignmentStatus.OPEN);
        if (d == null) {
            cards.show(detail, "empty");
            return;
        }
        detailTitle.setText(d.number() + "  ·  " + d.clientName());
        lineModel.setRowCount(0);
        boolean settled = d.status() == ConsignmentStatus.SETTLED;
        for (ConsignmentLineDTO l : d.lines()) {
            lineModel.addRow(new Object[]{l.productName(), qty(l.delivered()), settled ? qty(l.returned()) : "—",
                    settled ? qty(l.damaged()) : "—", settled ? qty(l.sold()) : "por acertar"});
        }
        detailTotals.setText(settled ? "Faturado " + UIHelper.formatMzn(d.invoiceTotal()) + (d.invoiceNumber() == null ? "" : "  ·  " + d.invoiceNumber())
                + "  ·  acertado por " + d.settledBy() : "Mercadoria no revendedor desde " + UIHelper.formatDate(d.deliveryDate()) + "  ·  saiu de "
                + d.sourceWarehouseName());
        detailTotals.setForeground(settled ? UIHelper.APPROVED_GREEN : UIHelper.PENDING_YELLOW);
        cards.show(detail, "detail");
    }

    private void openDelivery() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        UIHelper.loadAsync(this, () -> new Object[]{comercialApi.getAllClients(), comercialApi.getAllProducts(),
                inventoryApi.getSalesWarehousesByCompany(companyId)}, data -> {
            @SuppressWarnings("unchecked") List<ClientDTO> clients = (List<ClientDTO>) data[0];
            @SuppressWarnings("unchecked") List<ProductDTO> products = (List<ProductDTO>) data[1];
            @SuppressWarnings("unchecked") List<WarehouseDTO> warehouses = (List<WarehouseDTO>) data[2];
            showDeliveryDialog(companyId, clients, products, warehouses);
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível preparar a entrega", error.getMessage(), null, null));
    }

    private void showDeliveryDialog(Long companyId, List<ClientDTO> clients, List<ProductDTO> products, List<WarehouseDTO> warehouses) {
        if (warehouses.isEmpty()) {
            feedback.show(FeedbackType.WARNING, "Sem armazém", "Crie um armazém de vendas antes de entregar em consignação.", null, null);
            return;
        }
        SearchableComboBox<ClientDTO> client = new SearchableComboBox<>("Pesquisar revendedor por nome ou NUIT",
                c -> c.name() + (c.taxId() == null ? "" : "  ·  NUIT " + c.taxId()), c -> c.name() + " " + c.taxId());
        client.setItems(clients.stream().filter(c -> !"000000000".equals(c.taxId())).toList());
        JComboBox<String> warehouse = new JComboBox<>(warehouses.stream().map(WarehouseDTO::name).toArray(String[]::new));
        UIHelper.styleComboBox(warehouse);
        DateField date = new DateField(LocalDate.now());
        JTextField route = new JTextField();
        UIHelper.styleTextField(route);
        DefaultTableModel lines = new DefaultTableModel(new String[]{"Artigo", "Quantidade"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 1; }
        };
        List<ProductDTO> chosen = new ArrayList<>();
        JTable table = new JTable(lines);
        UIHelper.styleTable(table);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(460, 180));
        SearchableComboBox<ProductDTO> product = BreakdownFormDialog.productCombo(products, "Acrescentar artigo à guia…");
        ModernButton add = UIHelper.createSecondaryButton("Acrescentar");
        add.setIcon(UIHelper.icon("fas-plus", 13));
        add.addActionListener(e -> {
            ProductDTO p = product.selectedValue();
            if (p == null || chosen.stream().anyMatch(x -> x.id().equals(p.id()))) return;
            chosen.add(p);
            lines.addRow(new Object[]{p.name(), ""});
        });
        JPanel addRow = new JPanel(new BorderLayout(8, 0));
        addRow.setOpaque(false);
        addRow.add(product, BorderLayout.CENTER);
        addRow.add(add, BorderLayout.EAST);
        JPanel box = new JPanel(new BorderLayout(0, 6));
        box.setOpaque(false);
        box.add(addRow, BorderLayout.NORTH);
        box.add(scroll, BorderLayout.CENTER);
        FormField clientForm = new FormField("Revendedor", client, true, "Paga ao preço da sua tabela de preços");
        JPanel form = UIHelper.createDialogForm(1, new Object[]{"", clientForm, "Sai do armazém:", warehouse, "Dia:", date,
                "Rota / viatura:", route, "Artigos entregues:", box});
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Nova Entrega em Consignação", "fas-truck-loading",
                "Mercadoria deixada no revendedor", form).setConfirmButton("Entregar", "fas-check");
        String key = "CG-" + UUID.randomUUID();
        ConsignmentDTO[] result = new ConsignmentDTO[1];
        dialog.setOnSaveAsync(() -> {
            if (table.isEditing()) table.getCellEditor().stopCellEditing();
            ClientDTO c = client.selectedValue();
            if (c == null) {
                clientForm.setError("Escolha o revendedor.");
                throw new IllegalArgumentException("Escolha o revendedor.");
            }
            List<DeliverLineRequest> request = new ArrayList<>();
            for (int r = 0; r < lines.getRowCount(); r++) {
                BigDecimal q = BreakdownFormDialog.parse(lines.getValueAt(r, 1));
                if (q.signum() > 0) request.add(new DeliverLineRequest(chosen.get(r).id(), q));
            }
            if (request.isEmpty()) throw new IllegalArgumentException("Indique a quantidade de pelo menos um artigo.");
            DeliverConsignmentRequest body = new DeliverConsignmentRequest(companyId, c.id(), warehouses.get(warehouse.getSelectedIndex()).id(),
                    date.getDate() == null ? LocalDate.now() : date.getDate(), route.getText().trim(), request, key);
            return () -> { result[0] = api.deliver(body); return null; };
        });
        if (!dialog.showDialog() || result[0] == null) return;
        ToastManager.success(this, "Guia " + result[0].number() + " entregue a " + result[0].clientName() + ".");
        Long id = result[0].id();
        UIHelper.loadAsync(this, () -> api.list(companyId, 30), loaded -> apply(loaded, id), error -> reload());
    }

    private void openSettle() {
        ConsignmentDTO d = selected();
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (d == null || companyId == null) return;
        DefaultTableModel lines = new DefaultTableModel(new String[]{"Artigo", "Entregue", "Voltou bom", "Estragado", "Vendido"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 2 || c == 3; }
        };
        for (ConsignmentLineDTO l : d.lines()) lines.addRow(new Object[]{l.productName(), qty(l.delivered()), "0", "0", qty(l.delivered())});
        lines.addTableModelListener(e -> {
            if (e.getColumn() != 2 && e.getColumn() != 3) return;
            for (int r = 0; r < lines.getRowCount(); r++) {
                BigDecimal sold = d.lines().get(r).delivered().subtract(BreakdownFormDialog.parse(lines.getValueAt(r, 2)))
                        .subtract(BreakdownFormDialog.parse(lines.getValueAt(r, 3)));
                String text = sold.signum() < 0 ? "a mais!" : qty(sold);
                if (!text.equals(lines.getValueAt(r, 4))) lines.setValueAt(text, r, 4);
            }
        });
        JTable table = new JTable(lines);
        UIHelper.styleTable(table);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(520, 200));
        JLabel info = new JLabel("<html><div style='width:500px'>O que <b>voltou bom</b> regressa ao armazém da loja; o "
                + "<b>estragado</b> é abatido como quebra por validade. O <b>vendido</b> (entregue menos o que voltou) é faturado a "
                + d.clientName() + " ao preço da tabela dele.</div></html>");
        info.setForeground(UIHelper.TEXT_MUTED);
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Acertar Guia " + d.number(), "fas-balance-scale",
                "Devoluções e fatura do vendido", UIHelper.createDialogForm(1, new Object[]{"", info, "Artigos:", scroll}))
                .setConfirmButton("Acertar e Faturar", "fas-file-invoice");
        ConsignmentDTO[] result = new ConsignmentDTO[1];
        dialog.setOnSaveAsync(() -> {
            if (table.isEditing()) table.getCellEditor().stopCellEditing();
            List<SettleLineRequest> request = new ArrayList<>();
            for (int r = 0; r < lines.getRowCount(); r++) {
                request.add(new SettleLineRequest(d.lines().get(r).id(), BreakdownFormDialog.parse(lines.getValueAt(r, 2)),
                        BreakdownFormDialog.parse(lines.getValueAt(r, 3))));
            }
            SettleConsignmentRequest body = new SettleConsignmentRequest(companyId, request);
            return () -> { result[0] = api.settle(d.id(), body); return null; };
        });
        if (!dialog.showDialog() || result[0] == null) return;
        ToastManager.success(this, result[0].invoiceNumber() == null ? "Guia acertada: nada vendido."
                : "Guia acertada: fatura " + result[0].invoiceNumber() + " de " + UIHelper.formatMzn(result[0].invoiceTotal()) + ".");
        Long id = result[0].id();
        UIHelper.loadAsync(this, () -> api.list(companyId, 30), loaded -> apply(loaded, id), error -> reload());
    }

    private ConsignmentDTO selected() {
        int row = TableFilter.selectedModelRow(table);
        return row < 0 || row >= deliveries.size() ? null : deliveries.get(row);
    }

    public static BigDecimal sum(ConsignmentDTO d, boolean delivered) {
        return d.lines().stream().map(l -> delivered ? l.delivered() : l.sold()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal sold(ConsignmentDTO d) {
        return sum(d, false);
    }

    private static String qty(BigDecimal value) {
        return value == null ? "—" : TableCellRenderers.format(value, 3, "");
    }

    private static DefaultTableModel readOnly(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }
}
