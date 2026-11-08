package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.PricingApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.pricing.dto.ClientPriceListDTO;
import mz.multicore.erp.modules.pricing.dto.PriceListDTO;
import mz.multicore.erp.modules.pricing.dto.PriceListItemDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Price lists (SA-C03, {@code docs/TABELAS_PRECO_SPEC.md}) such as "Revendedor", "Empreiteiro" or
 * "Funcionário". Lists are on the left; prices (with quantity tiers) and assigned clients are on the right.
 * Invoices, orders and quotations apply these prices automatically.
 */
public class PriceListsPanel extends JPanel {

    private static final String[] LIST_COLUMNS = {"Tabela", "Código", "Validade", "Estado", "Preços", "Clientes"};
    private static final String[] ITEM_COLUMNS = {"Produto", "Código", "A partir de", "Preço da tabela", "Preço normal", "Diferença"};
    private static final String[] CLIENT_COLUMNS = {"Cliente"};

    private final PricingApiClient pricingApiClient;
    private final ComercialApiClient comercialApiClient;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    private final DefaultTableModel listModel = readOnlyModel(LIST_COLUMNS);
    private final JTable listTable = new JTable(listModel);
    private final DefaultTableModel itemModel = readOnlyModel(ITEM_COLUMNS);
    private final JTable itemTable = new JTable(itemModel);
    private final DefaultTableModel clientModel = readOnlyModel(CLIENT_COLUMNS);
    private final JTable clientTable = new JTable(clientModel);

    private final ModernButton newListBtn = UIHelper.createSuccessButton("Nova Tabela");
    private final ModernButton editListBtn = UIHelper.createSecondaryButton("Editar");
    private final ModernButton deleteListBtn = UIHelper.createDangerButton("Eliminar");
    private final ModernButton addItemBtn = UIHelper.createSuccessButton("Adicionar Preço");
    private final ModernButton removeItemBtn = UIHelper.createDangerButton("Remover");
    private final ModernButton assignBtn = UIHelper.createSuccessButton("Atribuir Cliente");
    private final ModernButton unassignBtn = UIHelper.createDangerButton("Retirar");

    private final JLabel detailTitle = new JLabel("Seleccione uma tabela");
    private final JLabel detailMeta = new JLabel(" ");
    private final CardLayout detailCards = new CardLayout();
    private final JPanel detailHolder = new JPanel(detailCards);

    private final List<PriceListDTO> lists = new ArrayList<>();
    private final List<PriceListItemDTO> items = new ArrayList<>();
    private final List<ClientPriceListDTO> clients = new ArrayList<>();
    private java.util.function.Consumer<List<PriceListDTO>> onLoaded = list -> { };

    public PriceListsPanel(PricingApiClient pricingApiClient, ComercialApiClient comercialApiClient) {
        this.pricingApiClient = pricingApiClient;
        this.comercialApiClient = comercialApiClient;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(15, 5, 5, 5));

        add(feedback, BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildListCard(), buildDetail());
        split.setOpaque(false);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setResizeWeight(0.42);
        split.setDividerSize(10);
        split.setContinuousLayout(true);
        add(split, BorderLayout.CENTER);
        syncButtons();
    }

    // ─── Layout ─────────────────────────────────────────────────────────────────

    private JComponent buildListCard() {
        UIHelper.styleTable(listTable);
        listTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.status());
        listTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelectedDetail();
        });
        listTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) editSelectedList();
            }
        });
        JScrollPane scroll = new JScrollPane(listTable);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);

        JTextField search = TableFilter.searchField("Nome ou código…");
        search.setPreferredSize(new Dimension(170, UIHelper.FORM_CONTROL_HEIGHT));
        JComboBox<String> state = TableFilter.combo("Todos os estados", "Activa", "Inactiva");
        UIHelper.styleComboBox(state);
        state.setPreferredSize(new Dimension(140, UIHelper.FORM_CONTROL_HEIGHT));
        TableFilter.install(listTable, search, new TableFilter.ColumnFilter(state, 3));
        JPanel filters = TableFilter.bar(search, TableFilter.label("Estado:"), state);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));

        newListBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newListBtn.setToolTipText("Criar uma tabela (ex.: Revendedor, Empreiteiro, Funcionário)");
        newListBtn.addActionListener(e -> openListDialog(null));
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::reload);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.add(UIHelper.tableCardTop("Tabelas de Preço", filters, refreshBtn, newListBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(listTable), BorderLayout.SOUTH);
        card.setMinimumSize(new Dimension(380, 200));
        return card;
    }

    private JComponent buildDetail() {
        detailHolder.setOpaque(false);
        detailHolder.add(buildEmptyDetail(), "empty");
        detailHolder.add(buildFilledDetail(), "detail");
        detailCards.show(detailHolder, "empty");
        return detailHolder;
    }

    private JComponent buildEmptyDetail() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new GridBagLayout());
        JPanel inner = new JPanel(new GridLayout(3, 1, 0, 8));
        inner.setOpaque(false);
        JLabel icon = new JLabel(UIHelper.icon("fas-tags", 40, UIHelper.ACCENT_PINK), SwingConstants.CENTER);
        JLabel title = new JLabel("Preços por tipo de cliente", SwingConstants.CENTER);
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        title.setForeground(UIHelper.TEXT_LIGHT);
        JLabel text = new JLabel("<html><div style='text-align:center;width:300px'>Escolha uma tabela à esquerda, "
                + "ou crie a primeira. Um cliente com tabela paga o preço da tabela na fatura, na encomenda e "
                + "na cotação; os restantes continuam com o preço normal.</div></html>", SwingConstants.CENTER);
        text.setForeground(UIHelper.TEXT_MUTED);
        inner.add(icon);
        inner.add(title);
        inner.add(text);
        card.add(inner);
        return card;
    }

    private JComponent buildFilledDetail() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        ModernPanel header = new ModernPanel(16);
        header.setLayout(new BorderLayout(12, 0));
        header.setBorder(new EmptyBorder(14, 16, 14, 16));
        JLabel icon = new JLabel(UIHelper.icon("fas-tags", 24, UIHelper.ACCENT_PINK));
        header.add(icon, BorderLayout.WEST);
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);
        detailTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        detailTitle.setForeground(UIHelper.TEXT_LIGHT);
        detailMeta.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        detailMeta.setForeground(UIHelper.TEXT_MUTED);
        text.add(detailTitle);
        text.add(detailMeta);
        header.add(text, BorderLayout.CENTER);
        editListBtn.setIcon(UIHelper.icon("fas-edit", 14));
        editListBtn.addActionListener(e -> editSelectedList());
        deleteListBtn.setIcon(UIHelper.icon("fas-trash-alt", 14));
        deleteListBtn.addActionListener(e -> deleteSelectedList());
        header.add(UIHelper.actionsBar(editListBtn, deleteListBtn), BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);
        tabs.addTab("Preços", UIHelper.icon("fas-money-bill-wave", 15, UIHelper.APPROVED_GREEN), buildItemsCard());
        tabs.addTab("Clientes", UIHelper.icon("fas-users", 15, UIHelper.MODULE_CLIENTES), buildClientsCard());
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildItemsCard() {
        UIHelper.styleTable(itemTable);
        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        itemTable.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.money());
        itemTable.getSelectionModel().addListSelectionListener(e -> syncButtons());
        itemTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) editSelectedItem();
            }
        });
        JScrollPane scroll = new JScrollPane(itemTable);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);

        JTextField search = TableFilter.searchField("Produto ou código…");
        search.setPreferredSize(new Dimension(190, UIHelper.FORM_CONTROL_HEIGHT));
        TableFilter.install(itemTable, search);
        JPanel filters = TableFilter.bar(search);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));

        addItemBtn.setIcon(UIHelper.icon("fas-plus", 14));
        addItemBtn.setToolTipText("Preço de um produto nesta tabela; repita com outra quantidade para criar escalões");
        addItemBtn.addActionListener(e -> openItemDialog(null));
        removeItemBtn.setIcon(UIHelper.icon("fas-trash-alt", 14));
        removeItemBtn.addActionListener(e -> removeSelectedItem());

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));
        card.add(UIHelper.tableCardTop("Preços da tabela", filters, removeItemBtn, addItemBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(itemTable), BorderLayout.SOUTH);
        return card;
    }

    private JComponent buildClientsCard() {
        UIHelper.styleTable(clientTable);
        clientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        clientTable.getSelectionModel().addListSelectionListener(e -> syncButtons());
        JScrollPane scroll = new JScrollPane(clientTable);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);

        JTextField search = TableFilter.searchField("Nome do cliente…");
        search.setPreferredSize(new Dimension(190, UIHelper.FORM_CONTROL_HEIGHT));
        TableFilter.install(clientTable, search);
        JPanel filters = TableFilter.bar(search);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));

        assignBtn.setIcon(UIHelper.icon("fas-user-tag", 14));
        assignBtn.addActionListener(e -> assignClient());
        unassignBtn.setIcon(UIHelper.icon("fas-user-times", 14));
        unassignBtn.setToolTipText("O cliente volta ao preço normal");
        unassignBtn.addActionListener(e -> unassignSelectedClient());

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));
        card.add(UIHelper.tableCardTop("Clientes com esta tabela", filters, unassignBtn, assignBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(clientTable), BorderLayout.SOUTH);
        return card;
    }

    // ─── Data ───────────────────────────────────────────────────────────────────

    /** Receives every loaded list, so the module page can update its indicators. */
    public void onLoaded(java.util.function.Consumer<List<PriceListDTO>> listener) {
        this.onLoaded = listener == null ? list -> { } : listener;
    }

    public void reload() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null || pricingApiClient == null) return;
        Long keep = selectedList() == null ? null : selectedList().id();
        UIHelper.loadAsync(this, () -> pricingApiClient.lists(companyId), loaded -> applyLists(loaded, keep),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar as tabelas de preço",
                        error.getMessage(), "Tentar novamente", this::reload));
    }

    private void applyLists(List<PriceListDTO> loaded, Long keepId) {
        lists.clear();
        lists.addAll(loaded);
        listModel.setRowCount(0);
        for (PriceListDTO list : lists) {
            listModel.addRow(new Object[]{list.name(), list.code(), validity(list),
                    list.active() ? "ACTIVA" : "INACTIVA", list.itemCount(), list.clientCount()});
        }
        onLoaded.accept(List.copyOf(lists));
        if (keepId != null) {
            for (int i = 0; i < lists.size(); i++) {
                if (lists.get(i).id().equals(keepId)) {
                    int view = listTable.convertRowIndexToView(i);
                    if (view >= 0) listTable.setRowSelectionInterval(view, view);
                    break;
                }
            }
        }
        if (selectedList() == null) detailCards.show(detailHolder, "empty");
        syncButtons();
    }

    private void loadSelectedDetail() {
        PriceListDTO list = selectedList();
        syncButtons();
        if (list == null) {
            detailCards.show(detailHolder, "empty");
            return;
        }
        detailTitle.setText(list.name() + "  ·  " + list.code());
        detailMeta.setText((list.active() ? "Activa" : "Inactiva") + "  ·  " + validity(list)
                + (list.description() == null ? "" : "  ·  " + list.description()));
        detailCards.show(detailHolder, "detail");
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        UIHelper.loadAsync(this, () -> new Detail(pricingApiClient.items(companyId, list.id()),
                        pricingApiClient.clients(companyId, list.id())),
                detail -> applyDetail(list.id(), detail),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar a tabela",
                        error.getMessage(), "Tentar novamente", this::loadSelectedDetail));
    }

    private void applyDetail(Long listId, Detail detail) {
        PriceListDTO list = selectedList();
        if (list == null || !list.id().equals(listId)) return;
        items.clear();
        items.addAll(detail.items());
        itemModel.setRowCount(0);
        for (PriceListItemDTO item : items) {
            itemModel.addRow(new Object[]{item.productName(), item.productSku(),
                    ProductUnitsPanel.plain(item.minQuantity()) + " " + item.baseUnitCode().toLowerCase(),
                    item.price(), item.retailPrice(), difference(item.price(), item.retailPrice())});
        }
        clients.clear();
        clients.addAll(detail.clients());
        clientModel.setRowCount(0);
        for (ClientPriceListDTO client : clients) clientModel.addRow(new Object[]{client.clientName()});
        syncButtons();
    }

    // ─── Actions ────────────────────────────────────────────────────────────────

    private void openListDialog(PriceListDTO existing) {
        PriceListFormDialog.show(this, pricingApiClient, existing, saved -> {
            ToastManager.success(this, existing == null ? "Tabela criada." : "Tabela actualizada.");
            Long companyId = CurrentUserContext.findCurrentCompanyId();
            UIHelper.loadAsync(this, () -> pricingApiClient.lists(companyId), loaded -> {
                applyLists(loaded, saved.id());
                loadSelectedDetail();
            }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível actualizar a lista",
                    error.getMessage(), "Tentar novamente", this::reload));
        });
    }

    private void editSelectedList() {
        PriceListDTO list = selectedList();
        if (list != null) openListDialog(list);
    }

    private void deleteSelectedList() {
        PriceListDTO list = selectedList();
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (list == null || companyId == null) return;
        if (list.clientCount() > 0) {
            feedback.show(FeedbackType.WARNING, "A tabela tem clientes",
                    "Retire-a dos " + list.clientCount() + " clientes ou desactive-a em Editar.", null, null);
            return;
        }
        if (!UIHelper.confirm(this, "Eliminar a tabela " + list.name() + " e os seus " + list.itemCount()
                + " preços?\nFaturas já emitidas não mudam.", "Confirmar")) return;
        UIHelper.submitAsync(deleteListBtn, () -> { pricingApiClient.delete(companyId, list.id()); return null; },
                ignored -> {
                    listTable.clearSelection();
                    reload();
                    ToastManager.success(this, "Tabela eliminada.");
                },
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível eliminar a tabela", error.getMessage(), null, null));
    }

    private void openItemDialog(PriceListItemDTO existing) {
        PriceListDTO list = selectedList();
        if (list == null) return;
        PriceListItemDialog.show(this, pricingApiClient, comercialApiClient, list, existing, saved -> {
            loadSelectedDetail();
            reload();
            ToastManager.success(this, "Preço guardado.");
        }, feedback);
    }

    private void editSelectedItem() {
        int row = TableFilter.selectedModelRow(itemTable);
        if (row >= 0 && row < items.size()) openItemDialog(items.get(row));
    }

    private void removeSelectedItem() {
        int row = TableFilter.selectedModelRow(itemTable);
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (row < 0 || row >= items.size() || companyId == null) return;
        PriceListItemDTO item = items.get(row);
        if (!UIHelper.confirm(this, "Remover o preço de " + item.productName() + "?\nO produto volta ao preço normal "
                + "para os clientes desta tabela.", "Confirmar")) return;
        UIHelper.submitAsync(removeItemBtn, () -> { pricingApiClient.deleteItem(companyId, item.id()); return null; },
                ignored -> { loadSelectedDetail(); reload(); },
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível remover o preço", error.getMessage(), null, null));
    }

    private void assignClient() {
        PriceListDTO list = selectedList();
        if (list == null) return;
        ClientPriceListAssignDialog.show(this, pricingApiClient, comercialApiClient, list, () -> {
            loadSelectedDetail();
            reload();
            ToastManager.success(this, "Cliente associado à tabela " + list.name() + ".");
        }, feedback);
    }

    private void unassignSelectedClient() {
        int row = TableFilter.selectedModelRow(clientTable);
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (row < 0 || row >= clients.size() || companyId == null) return;
        ClientPriceListDTO client = clients.get(row);
        if (!UIHelper.confirm(this, "Retirar " + client.clientName() + " desta tabela?\n"
                + "As próximas vendas a este cliente usam o preço normal.", "Confirmar")) return;
        UIHelper.submitAsync(unassignBtn, () -> pricingApiClient.assign(companyId, client.clientId(), null),
                ignored -> { loadSelectedDetail(); reload(); },
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível retirar o cliente", error.getMessage(), null, null));
    }

    // ─── Support ────────────────────────────────────────────────────────────────

    private void syncButtons() {
        boolean hasList = selectedList() != null;
        editListBtn.setEnabled(hasList);
        deleteListBtn.setEnabled(hasList);
        addItemBtn.setEnabled(hasList);
        assignBtn.setEnabled(hasList);
        removeItemBtn.setEnabled(hasList && TableFilter.selectedModelRow(itemTable) >= 0);
        unassignBtn.setEnabled(hasList && TableFilter.selectedModelRow(clientTable) >= 0);
    }

    private PriceListDTO selectedList() {
        int row = TableFilter.selectedModelRow(listTable);
        return row < 0 || row >= lists.size() ? null : lists.get(row);
    }

    private static String validity(PriceListDTO list) {
        if (list.validFrom() == null && list.validTo() == null) return "Sem prazo";
        String from = list.validFrom() == null ? "…" : UIHelper.formatDate(list.validFrom());
        String to = list.validTo() == null ? "…" : UIHelper.formatDate(list.validTo());
        return from + " a " + to;
    }

    static String difference(BigDecimal price, BigDecimal retail) {
        if (price == null || retail == null || retail.signum() <= 0) return "—";
        BigDecimal pct = price.subtract(retail).multiply(BigDecimal.valueOf(100)).divide(retail, 1, RoundingMode.HALF_UP);
        if (pct.signum() == 0) return "Igual";
        return (pct.signum() > 0 ? "+" : "") + TableCellRenderers.format(pct, 1, " %");
    }

    private static DefaultTableModel readOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private record Detail(List<PriceListItemDTO> items, List<ClientPriceListDTO> clients) {}
}
