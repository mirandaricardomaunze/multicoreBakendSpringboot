package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.*;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Emissão de guia avulsa; a criação não movimenta stock. */
final class StandaloneDeliveryGuideDialog {

    private StandaloneDeliveryGuideDialog() {}

    static DeliveryGuideDTO show(Component owner, ComercialApiClient apiClient,
                                 List<ClientDTO> clients, List<WarehouseDTO> warehouses) {
        JComboBox<ClientDTO> clientCombo = UIHelper.createComboBox(
                clients.toArray(ClientDTO[]::new), ClientDTO::name);
        JComboBox<WarehouseDTO> warehouseCombo = UIHelper.createComboBox(
                warehouses.toArray(WarehouseDTO[]::new), WarehouseDTO::name);
        JTextField responsible = new JTextField();
        JTextField vehicle = new JTextField();
        JTextArea notes = new JTextArea(2, 30);
        UIHelper.styleTextField(responsible);
        UIHelper.styleTextField(vehicle);
        UIHelper.styleTextArea(notes);
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);

        DefaultTableModel lines = new DefaultTableModel(
                new String[]{"Produto", "Qtd", "Referência", "Cód. barras", "Valor unit.", "IVA", "Total"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return column == 1; }
            @Override public Class<?> getColumnClass(int column) {
                return column == 0 ? ProductDTO.class : Object.class;
            }
        };
        JTable table = new JTable(lines);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                                      boolean focus, int row, int column) {
                return super.getTableCellRendererComponent(t,
                        value instanceof ProductDTO product ? product.name() : "", selected, focus, row, column);
            }
        });
        table.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.money());
        UIHelper.ensureHeadersFit(table);

        ModernButton add = UIHelper.createAddLineButton();
        add.setText("Pesquisar e adicionar produto");
        ModernButton remove = UIHelper.createDangerButton("Remover item");
        add.addActionListener(event -> {
            WarehouseDTO warehouse = (WarehouseDTO) warehouseCombo.getSelectedItem();
            ProductDTO product = ProductPagePicker.pick(owner, apiClient,
                    warehouse == null ? null : warehouse.id());
            if (product == null) return;
            for (int row = 0; row < lines.getRowCount(); row++) {
                if (lines.getValueAt(row, 0) instanceof ProductDTO existing
                        && existing.id().equals(product.id())) {
                    table.setRowSelectionInterval(row, row);
                    return;
                }
            }
            BigDecimal price = product.unitPrice() == null ? BigDecimal.ZERO : product.unitPrice();
            BigDecimal tax = product.effectiveTaxRate();
            BigDecimal total = mz.multicore.erp.architecture.pricing.LineCalculator
                    .compute(price, BigDecimal.ONE, BigDecimal.ZERO, tax).total();
            lines.addRow(new Object[]{product, BigDecimal.ONE, first(product.reference(), product.sku()),
                    first(product.barcode(), "—"), price, tax.movePointRight(2) + "%", total});
        });
        remove.addActionListener(event -> {
            int row = table.getSelectedRow();
            if (row >= 0) lines.removeRow(table.convertRowIndexToModel(row));
        });
        lines.addTableModelListener(event -> {
            if (event.getColumn() != 1 || event.getFirstRow() < 0 || event.getFirstRow() >= lines.getRowCount()) return;
            int row = event.getFirstRow();
            if (!(lines.getValueAt(row, 0) instanceof ProductDTO product)) return;
            try {
                BigDecimal qty = new BigDecimal(String.valueOf(lines.getValueAt(row, 1)).replace(',', '.'));
                BigDecimal price = product.unitPrice() == null ? BigDecimal.ZERO : product.unitPrice();
                lines.setValueAt(mz.multicore.erp.architecture.pricing.LineCalculator
                        .compute(price, qty, BigDecimal.ZERO, product.effectiveTaxRate()).total(), row, 6);
            } catch (RuntimeException ignored) {
                lines.setValueAt(BigDecimal.ZERO, row, 6);
            }
        });

        ModernPanel lineCard = new ModernPanel(16);
        lineCard.setLayout(new BorderLayout(0, 8));
        lineCard.setBorder(new EmptyBorder(8, 12, 8, 12));
        lineCard.add(UIHelper.tableCardTop("Artigos da guia", new JPanel(), add, remove), BorderLayout.NORTH);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(820, 240));
        UIHelper.styleScrollPane(tableScroll);
        lineCard.add(tableScroll, BorderLayout.CENTER);

        JScrollPane notesScroll = new JScrollPane(notes);
        UIHelper.styleScrollPane(notesScroll);
        JPanel form = UIHelper.createDialogForm(
                "Cliente *:", clientCombo,
                "Armazém de saída *:", warehouseCombo,
                "Responsável *:", responsible,
                "Viatura / Matrícula *:", vehicle,
                "Observações:", notesScroll,
                "Artigos:", lineCard);

        DeliveryGuideDTO[] created = new DeliveryGuideDTO[1];
        ModernFormDialog dialog = new ModernFormDialog(SwingUtilities.getWindowAncestor(owner),
                "Nova guia avulsa", "fas-truck", "Seleccione os artigos; o stock sai apenas na aprovação.", form)
                .setConfirmButton("Criar guia", "fas-save")
                .setOnSaveAsync(() -> {
                    ClientDTO client = (ClientDTO) clientCombo.getSelectedItem();
                    WarehouseDTO warehouse = (WarehouseDTO) warehouseCombo.getSelectedItem();
                    if (client == null) throw new IllegalArgumentException("Seleccione o cliente.");
                    if (warehouse == null) throw new IllegalArgumentException("Seleccione o armazém de saída.");
                    if (responsible.getText().isBlank()) {
                        responsible.requestFocusInWindow();
                        throw new IllegalArgumentException("Indique o responsável pelo transporte.");
                    }
                    if (vehicle.getText().isBlank()) {
                        vehicle.requestFocusInWindow();
                        throw new IllegalArgumentException("Indique a viatura ou matrícula.");
                    }
                    if (table.isEditing() && !table.getCellEditor().stopCellEditing()) {
                        throw new IllegalArgumentException("Conclua a edição da quantidade.");
                    }
                    List<CreateStandaloneDeliveryGuideLineRequest> requestLines = new ArrayList<>();
                    for (int row = 0; row < lines.getRowCount(); row++) {
                        ProductDTO product = (ProductDTO) lines.getValueAt(row, 0);
                        BigDecimal qty;
                        try {
                            qty = new BigDecimal(String.valueOf(lines.getValueAt(row, 1)).replace(',', '.'));
                        } catch (RuntimeException ex) {
                            table.setRowSelectionInterval(row, row);
                            throw new IllegalArgumentException("Quantidade inválida na linha " + (row + 1) + ".");
                        }
                        if (qty.signum() <= 0) {
                            table.setRowSelectionInterval(row, row);
                            throw new IllegalArgumentException("A quantidade da linha " + (row + 1) + " deve ser positiva.");
                        }
                        requestLines.add(new CreateStandaloneDeliveryGuideLineRequest(product.id(), qty));
                    }
                    if (requestLines.isEmpty()) throw new IllegalArgumentException("Adicione pelo menos um produto à guia.");
                    CreateStandaloneDeliveryGuideRequest request = new CreateStandaloneDeliveryGuideRequest(
                            client.id(), warehouse.id(), responsible.getText(), vehicle.getText(), notes.getText(), requestLines);
                    return () -> created[0] = apiClient.createStandaloneDeliveryGuide(request);
                });
        return dialog.showDialog() ? created[0] : null;
    }

    private static String first(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static final class ProductPagePicker {
        private static ProductDTO pick(Component owner, ComercialApiClient apiClient, Long warehouseId) {
            JTextField search = TableFilter.searchField("Nome, SKU, referência ou código de barras…");
            DefaultTableModel model = new DefaultTableModel(
                    new String[]{"Produto", "SKU / Referência", "Código de barras", "Stock", "Preço"}, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            JTable table = new JTable(model);
            table.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
            UIHelper.styleTable(table);
            table.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.money());
            List<ProductDTO> pageProducts = new ArrayList<>();
            TablePager[] pager = new TablePager[1];
            java.util.function.BiConsumer<Integer, Integer> load = (page, size) -> UIHelper.loadAsync(table,
                    () -> apiClient.getPOSCatalogPage(search.getText(), true, warehouseId, page, size), response -> {
                        model.setRowCount(0);
                        pageProducts.clear();
                        for (var item : response.items()) {
                            ProductDTO product = item.product();
                            pageProducts.add(product);
                            model.addRow(new Object[]{product.name(), first(product.reference(), product.sku()),
                                    first(product.barcode(), "—"), item.stockQuantity(), product.unitPrice()});
                        }
                        pager[0].apply(response);
                    }, error -> JOptionPane.showMessageDialog(owner, error.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE));
            pager[0] = new TablePager(load);
            search.addActionListener(event -> pager[0].reload());

            ModernPanel card = new ModernPanel(16);
            card.setLayout(new BorderLayout(0, 8));
            card.add(TableFilter.bar(search), BorderLayout.NORTH);
            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(760, 330));
            UIHelper.styleScrollPane(scroll);
            card.add(scroll, BorderLayout.CENTER);
            card.add(pager[0], BorderLayout.SOUTH);

            ProductDTO[] selected = new ProductDTO[1];
            ModernFormDialog dialog = new ModernFormDialog(SwingUtilities.getWindowAncestor(owner),
                    "Pesquisar produto", "fas-search", "Resultados paginados do catálogo da empresa.", card)
                    .setConfirmButton("Adicionar produto", "fas-plus")
                    .setOnSave(() -> {
                        int row = table.getSelectedRow();
                        if (row < 0) throw new IllegalArgumentException("Seleccione um produto na tabela.");
                        selected[0] = pageProducts.get(table.convertRowIndexToModel(row));
                    });
            pager[0].reload();
            return dialog.showDialog() ? selected[0] : null;
        }
    }
}
