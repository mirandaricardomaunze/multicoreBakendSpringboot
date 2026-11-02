package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.ProductionApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.manufacturing.dto.BillOfMaterialsDTO;
import mz.multicore.erp.modules.manufacturing.dto.ProductionOrderDTO;
import mz.multicore.erp.modules.manufacturing.model.ProductionOrderStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
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
    private final ProductionOrderOperationsCoordinator coordinator;

    final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final JLabel kpiInProgress = new JLabel("0 OPs");
    private final JLabel kpiCompleted = new JLabel("0 OPs");
    private final JLabel kpiBoms = new JLabel("0 Fichas");
    private final JLabel kpiTotalCost = new JLabel("0,00 MT");

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
        this.coordinator = new ProductionOrderOperationsCoordinator(productionApiClient, comercialApiClient, inventoryApiClient);

        setLayout(new BorderLayout(0, 12));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(15, 20, 20, 20));

        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

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
                .addAction("Modo Coletor (Scanner)", UIHelper.icon("fas-barcode", 14, UIHelper.ACCENT_BLUE),
                        () -> coordinator.openBarcodeCollector(this, this::loadData))
                .addAction("Nova Ficha (BOM)", UIHelper.icon("fas-file-alt", 14, UIHelper.PENDING_YELLOW),
                        this::openNewBomDialog)
                .addAction("Etiquetas de Produção", UIHelper.icon("fas-tags", 14, UIHelper.APPROVED_GREEN),
                        () -> coordinator.openProductionLabels(SwingUtilities.getWindowAncestor(this), cachedProducts));
        mfgActionsMenu.setToolTipText("Apontamento ágil por leitor, nova fórmula ou emissão de etiquetas");

        ModernButton newOrderBtn = UIHelper.createSuccessButton("Lançar Ordem");
        newOrderBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newOrderBtn.setToolTipText("Criar nova ordem de fabrico industrial");
        newOrderBtn.addActionListener(e -> coordinator.openNewOrderDialog(
                SwingUtilities.getWindowAncestor(this), currentBoms, cachedWarehouses, feedback, this::loadData));

        card.add(UIHelper.tableCardTop("Ordens de Fabrico & Fichas Técnicas de Transformação", filterBar,
                refreshBtn, mfgActionsMenu, newOrderBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
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

    private void openNewBomDialog() {
        BomFormDialog.show(
                SwingUtilities.getWindowAncestor(this),
                cachedProducts,
                productionApiClient,
                feedback,
                this::loadData
        );
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

        coordinator.showOrderManagementDialog(
                SwingUtilities.getWindowAncestor(this),
                order,
                cachedProducts,
                feedback,
                this::loadData
        );
    }
}
