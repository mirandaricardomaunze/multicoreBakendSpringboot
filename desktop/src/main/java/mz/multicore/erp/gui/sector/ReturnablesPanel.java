package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.ReturnablesApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.returnables.dto.*;
import mz.multicore.erp.modules.returnables.model.ReturnableMovementType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Vasilhame (SA-C12, {@code docs/VASILHAME_RETORNAVEIS_SPEC.md}). Tab "Saldos por Cliente": crates, bottles, gas
 * bottles and pallets each client still holds, the oldest first, with their value. "Entregar" and "Receber" move them
 * between the shop and the "Vasilhame em clientes" warehouse; "Cobrar Não Devolvidos" invoices what will not come back.
 */
public class ReturnablesPanel extends JPanel {

    private static final String[] BALANCE_COLUMNS = {"Cliente", "Vasilhame", "Em aberto", "Valor", "Fora há", "Último movimento"};
    private static final String[] MOVEMENT_COLUMNS = {"Data", "Cliente", "Vasilhame", "Movimento", "Quantidade", "Referência", "Fatura", "Por"};
    private static final String[] ITEM_COLUMNS = {"Artigo", "Código", "Valor a cobrar", "Nos clientes", "Estado"};

    private final ReturnablesApiClient api;
    private final ComercialApiClient comercialApi;
    private final InventoryApiClient inventoryApi;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final DefaultTableModel balanceModel = readOnly(BALANCE_COLUMNS);
    private final JTable balanceTable = new JTable(balanceModel);
    private final DefaultTableModel movementModel = readOnly(MOVEMENT_COLUMNS);
    private final DefaultTableModel itemModel = readOnly(ITEM_COLUMNS);
    private final JTable itemTable = new JTable(itemModel);
    private final ModernButton chargeBtn = UIHelper.createSecondaryButton("Cobrar Não Devolvidos");
    private final ModernButton toggleItemBtn = UIHelper.createSecondaryButton("Activar / Desactivar");
    private final List<ReturnableBalanceDTO> balances = new ArrayList<>();
    private final List<ReturnableItemDTO> items = new ArrayList<>();
    private Consumer<List<ReturnableBalanceDTO>> onLoaded = list -> { };

    public ReturnablesPanel(ReturnablesApiClient api, ComercialApiClient comercialApi, InventoryApiClient inventoryApi) {
        this.api = api;
        this.comercialApi = comercialApi;
        this.inventoryApi = inventoryApi;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        add(feedback, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);
        tabs.addTab("Saldos por Cliente", UIHelper.icon("fas-users", 15, UIHelper.ACCENT_BLUE), buildBalances());
        tabs.addTab("Movimentos", UIHelper.icon("fas-exchange-alt", 15, UIHelper.ACCENT_ORANGE), buildMovements());
        tabs.addTab("Artigos Retornáveis", UIHelper.icon("fas-box-open", 15, UIHelper.APPROVED_GREEN), buildItems());
        add(tabs, BorderLayout.CENTER);
        chargeBtn.setEnabled(false);
        toggleItemBtn.setEnabled(false);
    }

    public void onLoaded(Consumer<List<ReturnableBalanceDTO>> listener) {
        this.onLoaded = listener == null ? list -> { } : listener;
    }

    private JComponent buildBalances() {
        UIHelper.styleTable(balanceTable);
        balanceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        balanceTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        balanceTable.getSelectionModel().addListSelectionListener(e -> chargeBtn.setEnabled(selectedBalance() != null));
        JScrollPane scroll = new JScrollPane(balanceTable);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);
        JTextField search = TableFilter.searchField("Cliente ou vasilhame…");
        search.setPreferredSize(new Dimension(200, UIHelper.FORM_CONTROL_HEIGHT));
        TableFilter.install(balanceTable, search);
        JPanel filters = TableFilter.bar(search);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));
        ModernButton outBtn = UIHelper.createSuccessButton("Entregar ao Cliente");
        outBtn.setIcon(UIHelper.icon("fas-arrow-right", 14));
        outBtn.addActionListener(e -> openMovement(ReturnableMovementType.OUT, null));
        ModernButton inBtn = UIHelper.createPrimaryButton("Receber Devolução");
        inBtn.setIcon(UIHelper.icon("fas-undo", 14));
        inBtn.addActionListener(e -> openMovement(ReturnableMovementType.IN, selectedBalance()));
        chargeBtn.setIcon(UIHelper.icon("fas-file-invoice-dollar", 14));
        chargeBtn.setToolTipText("Fatura ao cliente o vasilhame que não vai voltar (só gestão)");
        chargeBtn.addActionListener(e -> openCharge());
        return card("Vasilhame nos Clientes", filters, scroll, UIHelper.createRefreshButton(this::reload), chargeBtn, inBtn, outBtn);
    }

    private JComponent buildMovements() {
        JTable table = new JTable(movementModel);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.status());
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);
        JTextField search = TableFilter.searchField("Cliente, vasilhame ou referência…");
        search.setPreferredSize(new Dimension(220, UIHelper.FORM_CONTROL_HEIGHT));
        TableFilter.install(table, search);
        JPanel filters = TableFilter.bar(search);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));
        return card("Movimentos (60 dias)", filters, scroll, UIHelper.createRefreshButton(this::reload));
    }

    private JComponent buildItems() {
        UIHelper.styleTable(itemTable);
        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemTable.getColumnModel().getColumn(2).setCellRenderer(TableCellRenderers.money());
        itemTable.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.status());
        itemTable.getSelectionModel().addListSelectionListener(e -> toggleItemBtn.setEnabled(selectedItem() != null));
        JScrollPane scroll = new JScrollPane(itemTable);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);
        ModernButton addBtn = UIHelper.createSuccessButton("Marcar Artigo como Vasilhame");
        addBtn.setIcon(UIHelper.icon("fas-plus", 14));
        addBtn.addActionListener(e -> openItem());
        toggleItemBtn.setIcon(UIHelper.icon("fas-power-off", 14));
        toggleItemBtn.addActionListener(e -> toggleItem());
        JTextArea note = ResponsiveSupport.wrappingText("O preço de venda do artigo é o valor cobrado quando o vasilhame não volta.",
                new Font(UIHelper.FONT, Font.PLAIN, 12), UIHelper.TEXT_MUTED);
        return card("Artigos Retornáveis", note, scroll, UIHelper.createRefreshButton(this::reload), toggleItemBtn, addBtn);
    }

    public void reload() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null || api == null) return;
        UIHelper.loadAsync(this, () -> new Object[]{api.balances(companyId), api.movements(companyId, 60), api.items(companyId)}, data -> {
            @SuppressWarnings("unchecked") List<ReturnableBalanceDTO> b = (List<ReturnableBalanceDTO>) data[0];
            @SuppressWarnings("unchecked") List<ReturnableMovementDTO> m = (List<ReturnableMovementDTO>) data[1];
            @SuppressWarnings("unchecked") List<ReturnableItemDTO> i = (List<ReturnableItemDTO>) data[2];
            apply(b, m, i);
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar o vasilhame", error.getMessage(), "Tentar novamente", this::reload));
    }

    private void apply(List<ReturnableBalanceDTO> loadedBalances, List<ReturnableMovementDTO> movements, List<ReturnableItemDTO> loadedItems) {
        feedback.clear();
        balances.clear();
        balances.addAll(loadedBalances);
        balanceModel.setRowCount(0);
        for (ReturnableBalanceDTO b : balances) {
            balanceModel.addRow(new Object[]{b.clientName(), b.itemName(), qty(b.balance()), b.value(),
                    b.daysOut() + (b.daysOut() == 1 ? " dia" : " dias"), UIHelper.formatDate(b.lastMovement())});
        }
        movementModel.setRowCount(0);
        for (ReturnableMovementDTO m : movements) {
            movementModel.addRow(new Object[]{m.createdAt() == null ? "" : UIHelper.formatDate(m.createdAt().toLocalDate()), m.clientName(),
                    m.itemName(), typeLabel(m.type()), qty(m.quantity()), m.reference() == null ? "—" : m.reference(),
                    m.invoiceNumber() == null ? "—" : m.invoiceNumber(), m.createdBy()});
        }
        items.clear();
        items.addAll(loadedItems);
        itemModel.setRowCount(0);
        for (ReturnableItemDTO i : items) {
            itemModel.addRow(new Object[]{i.productName(), i.sku(), i.chargeValue(), qty(i.withClients()), i.active() ? "ACTIVO" : "INACTIVO"});
        }
        chargeBtn.setEnabled(selectedBalance() != null);
        onLoaded.accept(List.copyOf(balances));
    }

    private void openMovement(ReturnableMovementType type, ReturnableBalanceDTO preset) {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        if (items.stream().noneMatch(ReturnableItemDTO::active)) {
            feedback.show(FeedbackType.INFO, "Sem vasilhame definido", "Marque primeiro os artigos retornáveis no separador Artigos Retornáveis.", null, null);
            return;
        }
        UIHelper.loadAsync(this, () -> new Object[]{comercialApi.getAllClients(), inventoryApi.getSalesWarehousesByCompany(companyId)}, data -> {
            @SuppressWarnings("unchecked") List<ClientDTO> clients = (List<ClientDTO>) data[0];
            @SuppressWarnings("unchecked") List<WarehouseDTO> warehouses = (List<WarehouseDTO>) data[1];
            showMovementDialog(companyId, type, preset, clients, warehouses);
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível preparar o movimento", error.getMessage(), null, null));
    }

    private void showMovementDialog(Long companyId, ReturnableMovementType type, ReturnableBalanceDTO preset,
                                    List<ClientDTO> clients, List<WarehouseDTO> warehouses) {
        if (warehouses.isEmpty()) {
            feedback.show(FeedbackType.WARNING, "Sem armazém", "Crie um armazém de vendas primeiro.", null, null);
            return;
        }
        SearchableComboBox<ClientDTO> client = new SearchableComboBox<>("Pesquisar cliente por nome ou NUIT",
                c -> c.name() + (c.taxId() == null ? "" : "  ·  NUIT " + c.taxId()), c -> c.name() + " " + c.taxId());
        client.setItems(clients.stream().filter(c -> !"000000000".equals(c.taxId())).toList());
        if (preset != null) client.selectFirst(c -> c.id().equals(preset.clientId()));
        List<ReturnableItemDTO> active = items.stream().filter(ReturnableItemDTO::active).toList();
        JComboBox<String> item = new JComboBox<>(active.stream().map(ReturnableItemDTO::productName).toArray(String[]::new));
        UIHelper.styleComboBox(item);
        if (preset != null) for (int i = 0; i < active.size(); i++) if (active.get(i).id().equals(preset.itemId())) item.setSelectedIndex(i);
        JComboBox<String> warehouse = new JComboBox<>(warehouses.stream().map(WarehouseDTO::name).toArray(String[]::new));
        UIHelper.styleComboBox(warehouse);
        DecimalField quantity = new DecimalField(preset != null && type == ReturnableMovementType.IN ? preset.balance().stripTrailingZeros().toPlainString() : "", 3, true);
        JTextField reference = new JTextField();
        UIHelper.styleTextField(reference);
        boolean out = type == ReturnableMovementType.OUT;
        FormField clientForm = new FormField("Cliente", client, true, null);
        FormField qtyForm = new FormField("Quantidade", quantity, true, null);
        JPanel form = UIHelper.createDialogForm(1, new Object[]{"", clientForm, "Vasilhame:", item,
                out ? "Sai do armazém:" : "Volta para o armazém:", warehouse, "", qtyForm, "Referência (fatura, guia):", reference});
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, out ? "Entregar Vasilhame" : "Receber Devolução",
                out ? "fas-arrow-right" : "fas-undo", out ? "Fica em nome do cliente" : "Volta ao armazém da loja", form)
                .setConfirmButton(out ? "Entregar" : "Receber", "fas-check");
        String key = "RT-" + UUID.randomUUID();
        ReturnableMovementDTO[] result = new ReturnableMovementDTO[1];
        dialog.setOnSaveAsync(() -> {
            ClientDTO c = client.selectedValue();
            if (c == null) {
                clientForm.setError("Escolha o cliente.");
                throw new IllegalArgumentException("Escolha o cliente.");
            }
            BigDecimal value = quantity.value();
            if (value == null || value.signum() <= 0) {
                qtyForm.setError("Indique a quantidade.");
                throw new IllegalArgumentException("Indique a quantidade.");
            }
            RecordReturnableRequest request = new RecordReturnableRequest(companyId, c.id(), active.get(item.getSelectedIndex()).id(),
                    warehouses.get(warehouse.getSelectedIndex()).id(), type, value, reference.getText().trim(), null, key);
            return () -> { result[0] = api.record(request); return null; };
        });
        if (!dialog.showDialog() || result[0] == null) return;
        ToastManager.success(this, qty(result[0].quantity()) + " " + result[0].itemName() + (out ? " entregue(s) a " : " recebido(s) de ")
                + result[0].clientName() + ".");
        reload();
    }

    private void openCharge() {
        ReturnableBalanceDTO b = selectedBalance();
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (b == null || companyId == null) return;
        DecimalField quantity = new DecimalField(b.balance().stripTrailingZeros().toPlainString(), 3, true);
        JTextField notes = new JTextField();
        UIHelper.styleTextField(notes);
        JTextArea info = ResponsiveSupport.wrappingText(b.clientName() + " tem " + qty(b.balance()) + " " + b.itemName()
                + " em aberto há " + b.daysOut() + " dias. É emitida uma fatura ao preço do artigo e essa quantidade deixa de contar no saldo.",
                new Font(UIHelper.FONT, Font.PLAIN, 12), UIHelper.TEXT_LIGHT);
        FormField qtyForm = new FormField("Quantidade a cobrar", quantity, true, "No máximo " + qty(b.balance()));
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Cobrar Vasilhame Não Devolvido", "fas-file-invoice-dollar",
                "Fatura do vasilhame perdido ou partido", UIHelper.createDialogForm(1, new Object[]{"", info, "", qtyForm, "Motivo:", notes}))
                .setConfirmButton("Emitir Fatura", "fas-file-invoice");
        String key = "RTC-" + UUID.randomUUID();
        ReturnableMovementDTO[] result = new ReturnableMovementDTO[1];
        dialog.setOnSaveAsync(() -> {
            BigDecimal value = quantity.value();
            if (value == null || value.signum() <= 0) {
                qtyForm.setError("Indique a quantidade.");
                throw new IllegalArgumentException("Indique a quantidade.");
            }
            ChargeReturnablesRequest request = new ChargeReturnablesRequest(companyId, b.clientId(), b.itemId(), value, notes.getText().trim(), key);
            return () -> { result[0] = api.charge(request); return null; };
        });
        if (!dialog.showDialog() || result[0] == null) return;
        ToastManager.success(this, "Fatura " + result[0].invoiceNumber() + " emitida a " + b.clientName() + ".");
        reload();
    }

    private void openItem() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        UIHelper.loadAsync(this, comercialApi::getAllProducts, products -> {
            List<ProductDTO> tracked = products.stream().filter(ProductDTO::stockTracked)
                    .filter(p -> items.stream().noneMatch(i -> i.productId().equals(p.id()))).toList();
            SearchableComboBox<ProductDTO> product = BreakdownFormDialog.productCombo(tracked, "Pesquisar grade, garrafa, botija ou palete");
            FormField productForm = new FormField("Artigo", product, true, "Tem de controlar stock");
            ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Marcar como Vasilhame", "fas-box-open",
                    "Artigo emprestado ao cliente e devolvido", UIHelper.createDialogForm(1, new Object[]{"", productForm}));
            ReturnableItemDTO[] result = new ReturnableItemDTO[1];
            dialog.setOnSaveAsync(() -> {
                ProductDTO p = product.selectedValue();
                if (p == null) {
                    productForm.setError("Escolha o artigo.");
                    throw new IllegalArgumentException("Escolha o artigo.");
                }
                return () -> { result[0] = api.saveItem(new SaveReturnableItemRequest(companyId, p.id(), true)); return null; };
            });
            if (dialog.showDialog() && result[0] != null) {
                ToastManager.success(this, result[0].productName() + " passa a ser tratado como vasilhame.");
                reload();
            }
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar os artigos", error.getMessage(), null, null));
    }

    private void toggleItem() {
        ReturnableItemDTO i = selectedItem();
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (i == null || companyId == null) return;
        UIHelper.runWithProgress(this, "A gravar…", () -> api.saveItem(new SaveReturnableItemRequest(companyId, i.productId(), !i.active())),
                saved -> reload(), error -> feedback.show(FeedbackType.ERROR, "Não foi possível gravar", error.getMessage(), null, null));
    }

    private JComponent card(String title, JComponent filters, JComponent body, JComponent... actions) {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.add(ResponsiveSupport.cardTop(title, filters, actions), BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private ReturnableBalanceDTO selectedBalance() {
        int row = TableFilter.selectedModelRow(balanceTable);
        return row < 0 || row >= balances.size() ? null : balances.get(row);
    }

    private ReturnableItemDTO selectedItem() {
        int row = TableFilter.selectedModelRow(itemTable);
        return row < 0 || row >= items.size() ? null : items.get(row);
    }

    static String typeLabel(ReturnableMovementType type) {
        return switch (type) {
            case OUT -> "ENTREGUE";
            case IN -> "DEVOLVIDO";
            case CHARGED -> "COBRADO";
        };
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
