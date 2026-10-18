package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.ProductionApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.manufacturing.dto.*;
import mz.multicore.erp.modules.manufacturing.model.BomType;
import mz.multicore.erp.modules.manufacturing.model.ProductionOrderStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Painel de Produção & Transformação Industrial.
 * Fichas Técnicas (BOM), Ordens de Fabrico e Apuramento do Custo Industrial (CIP).
 */
public final class ManufacturingPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0.00 MT");

    private final ProductionApiClient productionApiClient;
    private final ComercialApiClient comercialApiClient;
    private final InventoryApiClient inventoryApiClient;

    final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final JLabel kpiInProgress = new JLabel("0 OPs");
    private final JLabel kpiCompleted = new JLabel("0 OPs");
    private final JLabel kpiBoms = new JLabel("0 Fichas");
    private final JLabel kpiTotalCost = new JLabel("0.00 MT");

    private final DefaultTableModel ordersModel;
    final JTable ordersTable;
    private List<ProductionOrderDTO> currentOrders = new ArrayList<>();
    private List<BillOfMaterialsDTO> currentBoms = new ArrayList<>();
    private List<ProductDTO> cachedProducts = new ArrayList<>();
    private List<WarehouseDTO> cachedWarehouses = new ArrayList<>();

    private final JTextField searchField = TableFilter.searchField("Pesquisar ordem por número, produto ou ficha técnica...");
    private final JComboBox<String> statusFilterCombo = TableFilter.combo("Todos os Estados", "Planeada", "Em produção", "Concluída", "Cancelada");

    public ManufacturingPanel(ProductionApiClient productionApiClient,
                              ComercialApiClient comercialApiClient,
                              InventoryApiClient inventoryApiClient) {
        this.productionApiClient = productionApiClient;
        this.comercialApiClient = comercialApiClient;
        this.inventoryApiClient = inventoryApiClient;

        setLayout(new BorderLayout(0, 12));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(15, 20, 20, 20));

        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(UIHelper.createHeading("Produção & Transformação Industrial"), BorderLayout.WEST);

        feedback.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(titleRow);
        headerPanel.add(Box.createVerticalStrut(6));
        headerPanel.add(feedback);

        JPanel kpiGrid = KpiCard.createGrid(4);
        kpiGrid.add(KpiCard.createCard("Ordens em Produção", kpiInProgress, "Ordens em fabrico ativo",
                "fas-industry", UIHelper.ACCENT_BLUE));
        kpiGrid.add(KpiCard.createCard("Ordens Concluídas", kpiCompleted, "Transformações finalizadas",
                "fas-check-circle", UIHelper.APPROVED_GREEN));
        kpiGrid.add(KpiCard.createCard("Fichas Técnicas (BOM)", kpiBoms, "Fórmulas de produção activas",
                "fas-clipboard-list", UIHelper.MODULE_STOCK));
        kpiGrid.add(KpiCard.createCard("Custo Industrial Apurado", kpiTotalCost, "Total investido em produção",
                "fas-coins", UIHelper.PENDING_YELLOW));

        JPanel northStack = new JPanel(new BorderLayout(0, 12));
        northStack.setOpaque(false);
        northStack.add(headerPanel, BorderLayout.NORTH);
        northStack.add(kpiGrid, BorderLayout.CENTER);
        add(northStack, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        String[] cols = {
                "Nº Ordem", "Ficha Técnica", "Produto Acabado", "Qtd. Planeada",
                "Qtd. Produzida", "Data Início", "Data Conclusão", "Custo Total (CIP)",
                "Custo Unitário", "Estado"
        };

        ordersModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        ordersTable = new JTable(ordersModel);
        UIHelper.styleTable(ordersTable);

        int[] widths = {100, 130, 200, 110, 110, 100, 100, 130, 120, 110};
        for (int i = 0; i < widths.length; i++) {
            ordersTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        UIHelper.ensureHeadersFit(ordersTable);

        ordersTable.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.money());
        ordersTable.getColumnModel().getColumn(8).setCellRenderer(TableCellRenderers.money());
        ordersTable.getColumnModel().getColumn(9).setCellRenderer(TableCellRenderers.status());
        ordersTable.putClientProperty("customDetailAction", (Runnable) this::showOrderActions);

        JScrollPane scroll = new JScrollPane(ordersTable);
        UIHelper.styleScrollPane(scroll);

        TableFilter.install(ordersTable, searchField,
                new TableFilter.ColumnFilter(statusFilterCombo, 9));

        JPanel filterBar = TableFilter.bar(
                searchField,
                TableFilter.label("Estado:"), statusFilterCombo
        );

        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> loadData());

        ActionMenuButton mfgActionsMenu = UIHelper.createActionMenuButton("Produção")
                .addAction("Modo Coletor (Scanner)", UIHelper.icon("fas-barcode", 14, UIHelper.ACCENT_BLUE), this::openBarcodeCollector)
                .addAction("Nova Ficha (BOM)", UIHelper.icon("fas-file-alt", 14, UIHelper.PENDING_YELLOW), this::openNewBomDialog)
                .addAction("Etiquetas de Produção", UIHelper.icon("fas-tags", 14, UIHelper.APPROVED_GREEN), this::openProductionLabels);
        mfgActionsMenu.setToolTipText("Apontamento ágil por leitor, nova fórmula ou emissão de etiquetas");

        ModernButton newOrderBtn = UIHelper.createSuccessButton("Lançar Ordem");
        newOrderBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newOrderBtn.setToolTipText("Criar nova ordem de fabrico industrial");
        newOrderBtn.addActionListener(e -> openNewOrderDialog());

        card.add(UIHelper.tableCardTop("Ordens de Fabrico & Fichas Técnicas de Transformação", filterBar,
                refreshBtn, mfgActionsMenu, newOrderBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
    }

    private void openBarcodeCollector() {
        var collectorDialog = new BarcodeScannerCollectorDialog(
                SwingUtilities.getWindowAncestor(this),
                BarcodeScannerCollectorDialog.CollectorMode.MANUFACTURING,
                comercialApiClient,
                productionApiClient,
                inventoryApiClient,
                this::loadData
        );
        collectorDialog.showDialog();
    }

    public void onPanelSelected() {
        loadData();
    }

    private void loadData() {
        feedback.clear();
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    currentOrders = productionApiClient.listOrders();
                    currentBoms = productionApiClient.listBoms();
                    if (comercialApiClient != null) cachedProducts = comercialApiClient.getAllProducts();
                    Long companyId = mz.multicore.erp.architecture.security.CurrentUserContext.findCurrentCompanyId();
                    if (companyId != null && inventoryApiClient != null) {
                        cachedWarehouses = inventoryApiClient.getWarehousesByCompany(companyId);
                    }
                } catch (Exception ex) {
                    // feedback no done
                }
                return null;
            }

            @Override
            protected void done() {
                updateTableAndKpis();
            }
        };
        worker.execute();
    }

    private void updateTableAndKpis() {
        ordersModel.setRowCount(0);
        int inProgress = 0;
        int completed = 0;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (ProductionOrderDTO o : currentOrders) {
            if (o.status() == ProductionOrderStatus.IN_PROGRESS) inProgress++;
            if (o.status() == ProductionOrderStatus.COMPLETED) completed++;
            totalCost = totalCost.add(o.totalProductionCost() != null ? o.totalProductionCost() : BigDecimal.ZERO);

            String startStr = o.startDate() != null ? o.startDate().format(DATE_FMT) : "—";
            String endStr = o.completionDate() != null ? o.completionDate().format(DATE_FMT) : "—";

            ordersModel.addRow(new Object[]{
                    o.orderNumber(),
                    o.bomCode(),
                    o.finishedProductName(),
                    o.plannedQuantity(),
                    o.producedQuantity(),
                    startStr,
                    endStr,
                    o.totalProductionCost(),
                    o.unitProductionCost(),
                    o.status().name()
            });
        }

        kpiInProgress.setText(inProgress + " OPs");
        kpiCompleted.setText(completed + " OPs");
        kpiBoms.setText(currentBoms.size() + " Fichas");
        kpiTotalCost.setText(MONEY_FMT.format(totalCost));
    }

    private void openNewOrderDialog() {
        if (currentBoms.isEmpty()) {
            feedback.show(FeedbackType.WARNING, "Crie primeiro pelo menos uma Ficha Técnica (BOM) antes de lançar ordens de produção.");
            return;
        }

        Window parent = SwingUtilities.getWindowAncestor(this);
        JComboBox<BillOfMaterialsDTO> bomCombo = new JComboBox<>(currentBoms.toArray(new BillOfMaterialsDTO[0]));
        UIHelper.styleComboBox(bomCombo);
        bomCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof BillOfMaterialsDTO b) {
                    setText(b.bomCode() + " - " + b.finishedProductName() + " (" + b.name() + ")");
                }
                return this;
            }
        });

        JTextField qtyField = new JTextField("1.0", 10);
        JComboBox<WarehouseDTO> sourceWhCombo = new JComboBox<>(cachedWarehouses.toArray(new WarehouseDTO[0]));
        JComboBox<WarehouseDTO> targetWhCombo = new JComboBox<>(cachedWarehouses.toArray(new WarehouseDTO[0]));
        UIHelper.styleComboBox(sourceWhCombo);
        UIHelper.styleComboBox(targetWhCombo);

        DefaultListCellRenderer whRenderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof WarehouseDTO w) {
                    setText(w.name() + (w.warehouseNumber() != null ? " (" + w.warehouseNumber() + ")" : ""));
                }
                return this;
            }
        };
        sourceWhCombo.setRenderer(whRenderer);
        targetWhCombo.setRenderer(whRenderer);

        DateField endDateField = new DateField();
        endDateField.setText(LocalDate.now().plusDays(7).toString());

        JPanel form = UIHelper.createDialogForm(
                "Ficha Técnica (BOM) *:", bomCombo,
                "Quantidade a Produzir *:", qtyField,
                "Armazém Matéria-Prima *:", sourceWhCombo,
                "Armazém Produto Acabado *:", targetWhCombo,
                "Data Limite Prevista:", endDateField
        );

        ModernFormDialog dialog = new ModernFormDialog(parent, "Lançar Nova Ordem de Produção", "fas-industry",
                "Emissão de ordem de fabrico com apuramento de componentes", form);

        dialog.setOnSave(() -> {
            BillOfMaterialsDTO selectedBom = (BillOfMaterialsDTO) bomCombo.getSelectedItem();
            if (selectedBom == null) throw new IllegalArgumentException("Selecione a ficha técnica.");

            BigDecimal qty;
            try {
                qty = new BigDecimal(qtyField.getText().trim().replace(",", "."));
                if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new Exception();
            } catch (Exception ex) {
                qtyField.requestFocus();
                throw new IllegalArgumentException("A quantidade a produzir deve ser maior que zero.");
            }

            WarehouseDTO srcWh = (WarehouseDTO) sourceWhCombo.getSelectedItem();
            WarehouseDTO tgtWh = (WarehouseDTO) targetWhCombo.getSelectedItem();
            if (srcWh == null || tgtWh == null) {
                throw new IllegalArgumentException("Selecione os armazéns de origem e destino.");
            }

            LocalDate endDate = null;
            if (!endDateField.getText().trim().isEmpty()) {
                try {
                    endDate = LocalDate.parse(endDateField.getText().trim());
                } catch (Exception ex) {
                    endDateField.requestFocus();
                    throw new IllegalArgumentException("Data inválida. Use AAAA-MM-DD.");
                }
            }

            CreateProductionOrderRequest req = new CreateProductionOrderRequest(
                    selectedBom.id(),
                    qty,
                    srcWh.id(),
                    tgtWh.id(),
                    endDate
            );

            productionApiClient.createOrder(req);
        });

        if (dialog.showDialog()) {
            feedback.show(FeedbackType.SUCCESS, "Ordem de produção criada com sucesso.");
            loadData();
        }
    }

    private void openNewBomDialog() {
        Window parent = SwingUtilities.getWindowAncestor(this);

        JTextField codeField = new JTextField(10);
        JTextField nameField = new JTextField(20);
        JComboBox<ProductDTO> productCombo = new JComboBox<>(cachedProducts.toArray(new ProductDTO[0]));
        UIHelper.styleComboBox(productCombo);
        productCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ProductDTO p) {
                    setText(p.sku() + " - " + p.name());
                }
                return this;
            }
        });

        JTextField baseQtyField = new JTextField("1.0", 8);
        JTextField laborCostField = new JTextField("0.00", 10);
        JTextField overheadCostField = new JTextField("0.00", 10);

        JPanel form = UIHelper.createDialogForm(
                "Código da Ficha *:", codeField,
                "Nome da Ficha *:", nameField,
                "Produto Acabado *:", productCombo,
                "Quantidade Base *:", baseQtyField,
                "Mão de Obra Est. (MT):", laborCostField,
                "Custos Gerais Est. (MT):", overheadCostField
        );

        ModernFormDialog dialog = new ModernFormDialog(parent, "Criar Nova Ficha Técnica (BOM)", "fas-file-alt",
                "Definição da fórmula e matérias-primas para fabrico", form);

        dialog.setOnSave(() -> {
            String code = codeField.getText().trim();
            if (code.isEmpty()) {
                codeField.requestFocus();
                throw new IllegalArgumentException("O código da ficha é obrigatório.");
            }
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                nameField.requestFocus();
                throw new IllegalArgumentException("O nome da ficha é obrigatório.");
            }
            ProductDTO prod = (ProductDTO) productCombo.getSelectedItem();
            if (prod == null) throw new IllegalArgumentException("Selecione o produto final.");

            BigDecimal baseQty;
            try {
                baseQty = new BigDecimal(baseQtyField.getText().trim().replace(",", "."));
                if (baseQty.compareTo(BigDecimal.ZERO) <= 0) throw new Exception();
            } catch (Exception ex) {
                baseQtyField.requestFocus();
                throw new IllegalArgumentException("A quantidade base deve ser positiva.");
            }

            BigDecimal labor = BigDecimal.ZERO;
            if (!laborCostField.getText().trim().isEmpty()) {
                labor = new BigDecimal(laborCostField.getText().trim().replace(",", "."));
            }
            BigDecimal overhead = BigDecimal.ZERO;
            if (!overheadCostField.getText().trim().isEmpty()) {
                overhead = new BigDecimal(overheadCostField.getText().trim().replace(",", "."));
            }

            // Seleciona um componente para exemplo ou o próprio primeiro produto disponível
            ProductDTO defaultComponent = cachedProducts.stream()
                    .filter(p -> !p.id().equals(prod.id()))
                    .findFirst()
                    .orElse(prod);

            List<CreateBomRequest.CreateBomItemRequest> items = List.of(
                    new CreateBomRequest.CreateBomItemRequest(defaultComponent.id(), BigDecimal.ONE, BigDecimal.ZERO)
            );

            CreateBomRequest req = new CreateBomRequest(
                    code,
                    name,
                    prod.id(),
                    baseQty,
                    BomType.STANDARD,
                    labor,
                    overhead,
                    items
            );

            productionApiClient.createBom(req);
        });

        if (dialog.showDialog()) {
            feedback.show(FeedbackType.SUCCESS, "Ficha técnica registada com sucesso.");
            loadData();
        }
    }

    void showOrderActions() {
        int row = ordersTable.getSelectedRow();
        if (row < 0) {
            feedback.show(FeedbackType.WARNING, "Seleccione uma ordem",
                    "Seleccione uma Ordem de Produção na tabela para gerir o seu estado ou imprimir etiquetas.", null, null);
            return;
        }
        int modelRow = ordersTable.convertRowIndexToModel(row);
        if (modelRow >= currentOrders.size()) return;
        ProductionOrderDTO order = currentOrders.get(modelRow);

        Window parent = SwingUtilities.getWindowAncestor(this);

        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        ModernButton startBtn = UIHelper.createPrimaryButton("Iniciar Produção (Passar a EM_PRODUÇÃO)");
        startBtn.setIcon(UIHelper.icon("fas-play", 14));
        startBtn.setEnabled(order.status() == ProductionOrderStatus.PLANNED);

        ModernButton completeBtn = UIHelper.createSuccessButton("Concluir Produção & Dar Entrada no Stock");
        completeBtn.setIcon(UIHelper.icon("fas-check-double", 14));
        completeBtn.setEnabled(order.status() == ProductionOrderStatus.IN_PROGRESS || order.status() == ProductionOrderStatus.PLANNED);

        ModernButton labelsBtn = UIHelper.createSecondaryButton("Imprimir Etiquetas do Lote / OP");
        labelsBtn.setIcon(UIHelper.icon("fas-barcode", 14, UIHelper.ACCENT_BLUE));

        ModernButton cancelBtn = UIHelper.createSecondaryButton("Cancelar Ordem");
        cancelBtn.setIcon(UIHelper.icon("fas-times", 14));
        cancelBtn.setEnabled(order.status() != ProductionOrderStatus.COMPLETED && order.status() != ProductionOrderStatus.CANCELLED);

        panel.add(startBtn);
        panel.add(completeBtn);
        panel.add(labelsBtn);
        panel.add(cancelBtn);

        ModernFormDialog dialog = new ModernFormDialog(parent, "Gerir Ordem " + order.orderNumber(), "fas-cogs",
                "Estado actual: " + order.status().name(), panel);

        labelsBtn.addActionListener(e -> {
            dialog.close();
            printOrderLabels(order);
        });

        startBtn.addActionListener(e -> {
            try {
                productionApiClient.startOrder(order.id());
                feedback.show(FeedbackType.SUCCESS, "Ordem " + order.orderNumber() + " iniciada com sucesso.");
                dialog.close();
                loadData();
            } catch (Exception ex) {
                feedback.show(FeedbackType.ERROR, "Erro: " + ex.getMessage());
            }
        });

        completeBtn.addActionListener(e -> {
            try {
                productionApiClient.completeOrder(order.id(), order.plannedQuantity());
                feedback.show(FeedbackType.SUCCESS, "Ordem " + order.orderNumber() + " concluída! Stock do produto acabado e CIP apurados.");
                dialog.close();
                loadData();
            } catch (Exception ex) {
                feedback.show(FeedbackType.ERROR, "Erro: " + ex.getMessage());
            }
        });

        cancelBtn.addActionListener(e -> {
            try {
                productionApiClient.cancelOrder(order.id());
                feedback.show(FeedbackType.SUCCESS, "Ordem " + order.orderNumber() + " cancelada.");
                dialog.close();
                loadData();
            } catch (Exception ex) {
                feedback.show(FeedbackType.ERROR, "Erro: " + ex.getMessage());
            }
        });

        dialog.showDialog();
    }

    private void openProductionLabels() {
        Window parent = SwingUtilities.getWindowAncestor(this);
        List<ProductDTO> products = !cachedProducts.isEmpty() ? cachedProducts : comercialApiClient.getAllProducts();
        ShelfLabelsDialog.show(parent, products);
    }

    private void printOrderLabels(ProductionOrderDTO order) {
        Window parent = SwingUtilities.getWindowAncestor(this);
        ProductDTO finishedProd = cachedProducts.stream()
                .filter(p -> p.name().equalsIgnoreCase(order.finishedProductName()) || p.sku().equalsIgnoreCase(order.bomCode()))
                .findFirst()
                .orElse(null);

        if (finishedProd == null) {
            try {
                finishedProd = comercialApiClient.getAllProducts().stream()
                        .filter(p -> p.name().equalsIgnoreCase(order.finishedProductName()))
                        .findFirst()
                        .orElse(null);
            } catch (Exception ignored) {}
        }

        List<ProductDTO> list = finishedProd != null ? List.of(finishedProd) : cachedProducts;
        int copies = 1;
        if (order.producedQuantity() != null && order.producedQuantity().compareTo(BigDecimal.ZERO) > 0) {
            copies = order.producedQuantity().intValue();
        } else if (order.plannedQuantity() != null && order.plannedQuantity().compareTo(BigDecimal.ZERO) > 0) {
            copies = order.plannedQuantity().intValue();
        }

        ShelfLabelsDialog.showWithBatch(parent, list, order.orderNumber(), Math.max(1, copies));
    }
}
