package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.StockWasteApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.*;
import mz.multicore.erp.modules.inventory.model.WasteReason;
import mz.multicore.erp.modules.inventory.model.WasteStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Centro de Gestão de Quebras, Perdas e Prevenção de Desperdício (Waste & Shrinkage Management).
 * Alinhado com o Design System Multicore, controlos normalizados a 38px, e fluxo de aprovação por alçada.
 */
public class StockWastePanel extends JPanel {


    private final StockWasteApiClient wasteApiClient;
    private final InventoryApiClient inventoryApiClient;
    private final ComercialApiClient comercialApiClient;

    final JLabel kpiTotalCost = new JLabel("0,00 MT");
    final JLabel kpiTotalQty = new JLabel("0 un");
    final JLabel kpiWasteRate = new JLabel("0,00%");
    final JLabel kpiRiskBatches = new JLabel("0 lotes");

    // Aba 1: Histórico
    DefaultTableModel wasteTableModel;
    JTable wasteTable;
    JComboBox<String> statusFilterCombo;
    JComboBox<String> reasonFilterCombo;
    JComboBox<String> periodFilterCombo;
    JTextField wasteSearchField;
    ModernButton approveBtn;
    ModernButton rejectBtn;
    List<StockWasteDTO> currentWasteList = new ArrayList<>();

    // Aba 2: Radar
    DefaultTableModel radarTableModel;
    JTable radarTable;
    JComboBox<String> urgencyFilterCombo;
    JTextField radarSearchField;
    List<ExpiringBatchAlertDTO> currentRadarList = new ArrayList<>();

    // Aba 3: Métricas & PDF
    private JTextField dateStartField;
    private JTextField dateEndField;
    private DefaultTableModel reasonMetricsModel;
    private DefaultTableModel categoryMetricsModel;

    // Cache local de apoio a formulários
    private List<WarehouseDTO> cachedWarehouses = new ArrayList<>();
    private List<ProductDTO> cachedProducts = new ArrayList<>();

    public StockWastePanel(
            StockWasteApiClient wasteApiClient,
            InventoryApiClient inventoryApiClient,
            ComercialApiClient comercialApiClient
    ) {
        this.wasteApiClient = wasteApiClient;
        this.inventoryApiClient = inventoryApiClient;
        this.comercialApiClient = comercialApiClient;

        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel mainContent = new JPanel(new BorderLayout(0, 4));
        mainContent.setOpaque(false);
        mainContent.setBorder(new EmptyBorder(2, 2, 2, 2));

        JPanel centerPanel = new JPanel(new BorderLayout(0, 4));
        centerPanel.setOpaque(false);
        centerPanel.add(buildKpiBar(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);
        tabs.addTab("Registo & Validação de Quebras", UIHelper.icon("fas-clipboard-check", 16, UIHelper.REJECTED_RED), buildHistoryTab());
        tabs.addTab("Radar de Validades & Prevenção", UIHelper.icon("fas-shield-alt", 16, UIHelper.PENDING_YELLOW), buildRadarTab());
        tabs.addTab("Métricas & Relatório Executivo", UIHelper.icon("fas-chart-pie", 16, UIHelper.ACCENT_BLUE), buildMetricsTab());
        centerPanel.add(tabs, BorderLayout.CENTER);

        mainContent.add(centerPanel, BorderLayout.CENTER);

        add(mainContent, BorderLayout.CENTER);
    }

    private JPanel buildKpiBar() {
        JPanel kpiRow = KpiCard.createGrid(4);

        kpiRow.add(KpiCard.createCard("PERDA FINANCEIRA TOTAL", kpiTotalCost, "Custo acumulado no período", "fas-dollar-sign", UIHelper.REJECTED_RED));
        kpiRow.add(KpiCard.createCard("QTD TOTAL DESPERDIÇADA", kpiTotalQty, "Unidades abatidas de stock", "fas-boxes", UIHelper.PENDING_YELLOW));
        kpiRow.add(KpiCard.createCard("RÁCIO DE QUEBRA S/ VENDAS", kpiWasteRate, "Meta recomendada: < 1.5%", "fas-percent", UIHelper.ACCENT_BLUE));
        kpiRow.add(KpiCard.createCard("LOTES EM RISCO IMINENTE", kpiRiskBatches, "A vencer nos próximos 7 dias", "fas-exclamation-triangle", UIHelper.ACCENT_ORANGE));

        return kpiRow;
    }

    private JPanel buildHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(4, 0, 0, 0));

        statusFilterCombo = new JComboBox<>(new String[]{"Todos os Estados", "Aprovado", "Pendente Aprovação", "Rejeitado"});
        UIHelper.styleComboBox(statusFilterCombo);
        statusFilterCombo.setPreferredSize(new Dimension(160, UIHelper.FORM_CONTROL_HEIGHT));
        statusFilterCombo.addActionListener(e -> applyWasteFilters());

        reasonFilterCombo = new JComboBox<>(new String[]{
                "Todos os Motivos",
                "Validade Expirada",
                "Avaria / Quebra Física",
                "Falha de Refrigeração",
                "Consumo Interno / Amostra",
                "Furto / Quebra Desconhecida",
                "Outro Motivo"
        });
        UIHelper.styleComboBox(reasonFilterCombo);
        reasonFilterCombo.setPreferredSize(new Dimension(200, UIHelper.FORM_CONTROL_HEIGHT));
        reasonFilterCombo.addActionListener(e -> applyWasteFilters());

        periodFilterCombo = TableFilter.periodCombo();
        UIHelper.styleComboBox(periodFilterCombo);
        periodFilterCombo.setPreferredSize(new Dimension(140, UIHelper.FORM_CONTROL_HEIGHT));
        periodFilterCombo.addActionListener(e -> applyWasteFilters());

        wasteSearchField = new SearchField("Pesquisar por produto, armazém, lote ou registador...");
        wasteSearchField.setPreferredSize(new Dimension(260, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.onTextChange(wasteSearchField, this::applyWasteFilters);

        ModernButton reloadBtn = UIHelper.createSecondaryButton("Actualizar");
        reloadBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        reloadBtn.addActionListener(e -> reloadData());

        ModernButton registerBtn = UIHelper.createDangerButton("Registar Quebra");
        registerBtn.setIcon(UIHelper.icon("fas-plus", 14));
        registerBtn.addActionListener(e -> openRegisterWasteDialog(null));

        approveBtn = UIHelper.createSuccessButton("Aprovar");
        approveBtn.setIcon(UIHelper.icon("fas-check", 14));
        approveBtn.setEnabled(false);
        approveBtn.addActionListener(e -> handleApproveSelected(true));

        rejectBtn = UIHelper.createDangerButton("Rejeitar");
        rejectBtn.setIcon(UIHelper.icon("fas-times", 14));
        rejectBtn.setEnabled(false);
        rejectBtn.addActionListener(e -> handleApproveSelected(false));

        JPanel filters = new JPanel(new GridBagLayout());
        filters.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 0, 8);

        g.gridx = 0; g.weightx = 0; filters.add(statusFilterCombo, g);
        g.gridx = 1; g.weightx = 0; filters.add(reasonFilterCombo, g);
        g.gridx = 2; g.weightx = 0; filters.add(periodFilterCombo, g);
        g.gridx = 3; g.weightx = 1.0; g.insets = new Insets(0, 0, 0, 0);
        filters.add(wasteSearchField, g);
        filters.setBorder(new EmptyBorder(0, 0, 4, 0));

        String[] cols = {"ID", "Data", "Produto", "Armazém", "Lote", "Motivo", "Quantidade", "Custo Unit.", "Total Perda", "Estado", "Registado Por", "Aprovado Por"};
        wasteTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        wasteTable = new JTable(wasteTableModel);
        UIHelper.styleTable(wasteTable);
        wasteTable.setRowHeight(42);
        wasteTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        wasteTable.getSelectionModel().addListSelectionListener(e -> updateApprovalButtonsState());

        // Alinhamentos e Renderers
        wasteTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        wasteTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        wasteTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        wasteTable.getColumnModel().getColumn(3).setPreferredWidth(130);
        wasteTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        wasteTable.getColumnModel().getColumn(5).setPreferredWidth(160);
        wasteTable.getColumnModel().getColumn(6).setPreferredWidth(90);
        wasteTable.getColumnModel().getColumn(7).setPreferredWidth(100);
        wasteTable.getColumnModel().getColumn(8).setPreferredWidth(110);
        wasteTable.getColumnModel().getColumn(9).setPreferredWidth(140);
        wasteTable.getColumnModel().getColumn(10).setPreferredWidth(110);
        wasteTable.getColumnModel().getColumn(11).setPreferredWidth(110);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        wasteTable.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);
        wasteTable.getColumnModel().getColumn(7).setCellRenderer(rightRenderer);
        wasteTable.getColumnModel().getColumn(8).setCellRenderer(rightRenderer);

        wasteTable.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                String val = String.valueOf(value);
                if (val.contains("Aprovado")) {
                    setForeground(UIHelper.APPROVED_GREEN);
                } else if (val.contains("Pendente")) {
                    setForeground(UIHelper.PENDING_YELLOW);
                } else if (val.contains("Rejeitado")) {
                    setForeground(UIHelper.REJECTED_RED);
                }
                setFont(getFont().deriveFont(Font.BOLD));
                return c;
            }
        });

        wasteTable.setPreferredScrollableViewportSize(new Dimension(800, 480));

        JScrollPane scroll = new JScrollPane(wasteTable);
        UIHelper.styleScrollPane(scroll);
        scroll.setMinimumSize(new Dimension(200, 180));

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(8, 12, 8, 12));
        card.add(UIHelper.tableCardTop("Registo de Quebras & Perdas", filters, reloadBtn, registerBtn, approveBtn, rejectBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(wasteTable), BorderLayout.SOUTH);
        panel.add(card, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildRadarTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(4, 0, 0, 0));

        urgencyFilterCombo = new JComboBox<>(new String[]{
                "Todas as Urgências",
                "Vencidos",
                "Crítico (≤ 3 dias)",
                "Alto (≤ 7 dias)",
                "Médio (≤ 15 dias)",
                "Atenção (≤ 30 dias)"
        });
        UIHelper.styleComboBox(urgencyFilterCombo);
        urgencyFilterCombo.setPreferredSize(new Dimension(180, UIHelper.FORM_CONTROL_HEIGHT));
        urgencyFilterCombo.addActionListener(e -> applyRadarFilters());

        radarSearchField = TableFilter.searchField("Pesquisar por lote, produto, armazém…");
        radarSearchField.setPreferredSize(new Dimension(280, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.onTextChange(radarSearchField, this::applyRadarFilters);

        ModernButton reloadRadarBtn = UIHelper.createSecondaryButton("Actualizar");
        reloadRadarBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        reloadRadarBtn.addActionListener(e -> reloadData());

        ModernButton wasteRadarBtn = UIHelper.createDangerButton("Registar Quebra deste Lote");
        wasteRadarBtn.setIcon(UIHelper.icon("fas-trash-alt", 14));
        wasteRadarBtn.addActionListener(e -> handleWasteFromSelectedBatch());

        JPanel radarFilters = new JPanel(new GridBagLayout());
        radarFilters.setOpaque(false);
        GridBagConstraints rg = new GridBagConstraints();
        rg.gridy = 0;
        rg.fill = GridBagConstraints.HORIZONTAL;
        rg.insets = new Insets(0, 0, 0, 8);
        rg.gridx = 0; rg.weightx = 0; radarFilters.add(urgencyFilterCombo, rg);
        rg.gridx = 1; rg.weightx = 1.0; rg.insets = new Insets(0, 0, 0, 0); radarFilters.add(radarSearchField, rg);
        radarFilters.setBorder(new EmptyBorder(0, 0, 4, 0));

        String[] cols = {"Lote", "Produto", "Categoria", "Armazém", "Qtd Disponível", "Custo Unit.", "Perda Estimada", "Data Validade", "Dias Restantes", "Urgência"};
        radarTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        radarTable = new JTable(radarTableModel);
        UIHelper.styleTable(radarTable);
        radarTable.setRowHeight(42);
        radarTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        radarTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        radarTable.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);
        radarTable.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);

        radarTable.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                String val = String.valueOf(value);
                if (val.contains("VENCIDO")) {
                    setForeground(UIHelper.REJECTED_RED);
                } else if (val.contains("CRÍTICO")) {
                    setForeground(UIHelper.REJECTED_RED);
                } else if (val.contains("ALTO")) {
                    setForeground(UIHelper.KPI_ORANGE_DARK);
                } else if (val.contains("MÉDIO")) {
                    setForeground(UIHelper.PENDING_YELLOW);
                } else {
                    setForeground(UIHelper.ACCENT_BLUE);
                }
                setFont(getFont().deriveFont(Font.BOLD));
                return c;
            }
        });

        radarTable.setPreferredScrollableViewportSize(new Dimension(800, 480));

        JScrollPane scroll = new JScrollPane(radarTable);
        UIHelper.styleScrollPane(scroll);
        scroll.setMinimumSize(new Dimension(200, 180));

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(8, 12, 8, 12));
        card.add(UIHelper.tableCardTop("Radar de Validades & Prevenção", radarFilters, reloadRadarBtn, wasteRadarBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(radarTable), BorderLayout.SOUTH);
        panel.add(card, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildMetricsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 0, 0, 0));

        LocalDate now = LocalDate.now();
        dateStartField = new JTextField(now.minusDays(30).format(UIHelper.DATE_FMT));
        UIHelper.styleTextField(dateStartField);
        dateStartField.setPreferredSize(new Dimension(120, UIHelper.FORM_CONTROL_HEIGHT));

        dateEndField = new JTextField(now.format(UIHelper.DATE_FMT));
        UIHelper.styleTextField(dateEndField);
        dateEndField.setPreferredSize(new Dimension(120, UIHelper.FORM_CONTROL_HEIGHT));

        ModernButton filterBtn = UIHelper.createPrimaryButton("Filtrar Período");
        filterBtn.setIcon(UIHelper.icon("fas-filter", 14));
        filterBtn.addActionListener(e -> reloadMetrics());

        ModernButton printPdfBtn = UIHelper.createSecondaryButton("Imprimir Relatório Oficial PDF");
        printPdfBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        printPdfBtn.addActionListener(e -> exportPdfReport());

        ModernPanel filterCard = new ModernPanel(14);
        filterCard.setLayout(new BorderLayout());
        filterCard.setBorder(new EmptyBorder(10, 14, 10, 14));

        JPanel filterToolbar = TableFilter.toolbar(
            new JComponent[]{
                TableFilter.label("Data Início:", "fas-calendar-alt"),
                dateStartField,
                TableFilter.label("Data Fim:", "fas-calendar-alt"),
                dateEndField,
                filterBtn
            },
            new JComponent[]{
                printPdfBtn
            }
        );
        filterCard.add(filterToolbar, BorderLayout.CENTER);
        panel.add(filterCard, BorderLayout.NORTH);

        JPanel tablesGrid = new JPanel(new GridLayout(1, 2, 14, 0));
        tablesGrid.setOpaque(false);

        // Motivos
        ModernPanel reasonCard = new ModernPanel(16);
        reasonCard.setLayout(new BorderLayout(0, 8));
        reasonCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        reasonCard.add(UIHelper.createSubheading("Perdas por Motivo"), BorderLayout.NORTH);
        reasonMetricsModel = new DefaultTableModel(new String[]{"Motivo de Quebra", "Custo Total (MT)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable reasonTable = new JTable(reasonMetricsModel);
        UIHelper.styleTable(reasonTable);
        reasonTable.setRowHeight(36);
        reasonTable.setPreferredScrollableViewportSize(new Dimension(350, 320));
        JScrollPane reasonScroll = new JScrollPane(reasonTable);
        UIHelper.styleScrollPane(reasonScroll);
        reasonScroll.setMinimumSize(new Dimension(200, 220));
        reasonCard.add(reasonScroll, BorderLayout.CENTER);
        tablesGrid.add(reasonCard);

        // Categorias
        ModernPanel catCard = new ModernPanel(16);
        catCard.setLayout(new BorderLayout(0, 8));
        catCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        catCard.add(UIHelper.createSubheading("Perdas por Categoria de Produto"), BorderLayout.NORTH);
        categoryMetricsModel = new DefaultTableModel(new String[]{"Categoria", "Custo Total (MT)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable catTable = new JTable(categoryMetricsModel);
        UIHelper.styleTable(catTable);
        catTable.setRowHeight(36);
        catTable.setPreferredScrollableViewportSize(new Dimension(350, 320));
        JScrollPane catScroll = new JScrollPane(catTable);
        UIHelper.styleScrollPane(catScroll);
        catScroll.setMinimumSize(new Dimension(200, 220));
        catCard.add(catScroll, BorderLayout.CENTER);
        tablesGrid.add(catCard);

        panel.add(tablesGrid, BorderLayout.CENTER);

        return panel;
    }

    public void reload() {
        reloadData();
    }

    private void reloadData() {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();

        // 1. Carregar armazéns e produtos em background para cache de formulários
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    cachedWarehouses = inventoryApiClient.getWarehousesByCompany(companyId);
                    cachedProducts = comercialApiClient.getAllProducts();
                } catch (Exception ignored) {}
                return null;
            }
        }.execute();

        // 2. Carregar Quebras
        new SwingWorker<List<StockWasteDTO>, Void>() {
            @Override
            protected List<StockWasteDTO> doInBackground() {
                return wasteApiClient.findByCompany(companyId, null);
            }

            @Override
            protected void done() {
                try {
                    currentWasteList = get();
                    applyWasteFilters();
                } catch (Exception ex) {
                    ToastManager.show(StockWastePanel.this, FeedbackType.ERROR, "Erro ao carregar quebras: " + ex.getMessage());
                }
            }
        }.execute();

        // 3. Carregar Radar
        new SwingWorker<List<ExpiringBatchAlertDTO>, Void>() {
            @Override
            protected List<ExpiringBatchAlertDTO> doInBackground() {
                return wasteApiClient.getExpiringRadar(companyId, 30);
            }

            @Override
            protected void done() {
                try {
                    currentRadarList = get();
                    applyRadarFilters();

                    long riskCount = currentRadarList.stream()
                            .filter(b -> b.daysUntilExpiration() <= 7)
                            .count();
                    kpiRiskBatches.setText(riskCount + " lotes");
                } catch (Exception ex) {
                    ToastManager.show(StockWastePanel.this, FeedbackType.ERROR, "Erro ao carregar radar: " + ex.getMessage());
                }
            }
        }.execute();

        // 4. Carregar Resumo KPI & Métricas
        reloadMetrics();
    }

    private void reloadMetrics() {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        LocalDate start = parseDate(dateStartField.getText().trim(), LocalDate.now().minusDays(30));
        LocalDate end = parseDate(dateEndField.getText().trim(), LocalDate.now());

        new SwingWorker<WasteSummaryDTO, Void>() {
            @Override
            protected WasteSummaryDTO doInBackground() {
                return wasteApiClient.getSummary(companyId, start, end);
            }

            @Override
            protected void done() {
                try {
                    WasteSummaryDTO s = get();
                    kpiTotalCost.setText(UIHelper.formatMzn(s.totalWasteCost()));
                    kpiTotalQty.setText(s.totalWasteQuantity().stripTrailingZeros().toPlainString() + " un");
                    kpiWasteRate.setText(s.wasteRatePercentage().toPlainString() + "%");

                    reasonMetricsModel.setRowCount(0);
                    if (s.costByReason() != null) {
                        for (Map.Entry<WasteReason, BigDecimal> entry : s.costByReason().entrySet()) {
                            reasonMetricsModel.addRow(new Object[]{
                                    entry.getKey().getDescription(),
                                    UIHelper.formatMzn(entry.getValue())
                            });
                        }
                    }

                    categoryMetricsModel.setRowCount(0);
                    if (s.costByCategory() != null) {
                        for (Map.Entry<String, BigDecimal> entry : s.costByCategory().entrySet()) {
                            categoryMetricsModel.addRow(new Object[]{
                                    entry.getKey(),
                                    UIHelper.formatMzn(entry.getValue())
                            });
                        }
                    }
                } catch (Exception ex) {
                    ToastManager.show(StockWastePanel.this, FeedbackType.ERROR, "Erro ao carregar métricas: " + ex.getMessage());
                }
            }
        }.execute();
    }

    void applyWasteFilters() {
        String statusSel = (String) statusFilterCombo.getSelectedItem();
        String reasonSel = (String) reasonFilterCombo.getSelectedItem();
        String periodSel = (String) (periodFilterCombo != null ? periodFilterCombo.getSelectedItem() : "Todo o período");
        String query = wasteSearchField.getText().trim().toLowerCase();

        wasteTableModel.setRowCount(0);
        LocalDate today = LocalDate.now();
        for (StockWasteDTO w : currentWasteList) {
            if (!"Todos os Estados".equals(statusSel) && !w.status().getDescription().equals(statusSel)) {
                continue;
            }
            if (!"Todos os Motivos".equals(reasonSel) && !w.reason().getDescription().equals(reasonSel)) {
                continue;
            }
            if (w.createdAt() != null && !TableFilter.matchesPeriod(w.createdAt().toLocalDate(), periodSel, today)) {
                continue;
            }
            if (!query.isEmpty()) {
                boolean match = (w.productName() != null && w.productName().toLowerCase().contains(query))
                        || (w.productSku() != null && w.productSku().toLowerCase().contains(query))
                        || (w.warehouseName() != null && w.warehouseName().toLowerCase().contains(query))
                        || (w.batchNumber() != null && w.batchNumber().toLowerCase().contains(query))
                        || (w.registeredBy() != null && w.registeredBy().toLowerCase().contains(query));
                if (!match) continue;
            }

            wasteTableModel.addRow(new Object[]{
                    w.id(),
                    w.createdAt() != null ? w.createdAt().format(UIHelper.DATETIME_FMT) : "-",
                    w.productName(),
                    w.warehouseName(),
                    w.batchNumber() != null ? w.batchNumber() : "-",
                    w.reason().getDescription(),
                    w.quantity().stripTrailingZeros().toPlainString() + " un",
                    UIHelper.formatMzn(w.unitCost()),
                    UIHelper.formatMzn(w.totalCost()),
                    w.status().getDescription(),
                    w.registeredBy(),
                    w.approvedBy() != null ? w.approvedBy() : "-"
            });
        }
        updateApprovalButtonsState();
    }

    private void applyRadarFilters() {
        String urgencySel = (String) urgencyFilterCombo.getSelectedItem();
        String query = radarSearchField != null ? radarSearchField.getText().trim().toLowerCase() : "";
        radarTableModel.setRowCount(0);

        for (ExpiringBatchAlertDTO b : currentRadarList) {
            if ("Vencidos".equals(urgencySel) && b.daysUntilExpiration() >= 0) continue;
            if ("Crítico (≤ 3 dias)".equals(urgencySel) && (b.daysUntilExpiration() < 0 || b.daysUntilExpiration() > 3)) continue;
            if ("Alto (≤ 7 dias)".equals(urgencySel) && (b.daysUntilExpiration() < 0 || b.daysUntilExpiration() > 7)) continue;
            if ("Médio (≤ 15 dias)".equals(urgencySel) && (b.daysUntilExpiration() < 0 || b.daysUntilExpiration() > 15)) continue;
            if ("Atenção (≤ 30 dias)".equals(urgencySel) && (b.daysUntilExpiration() < 0 || b.daysUntilExpiration() > 30)) continue;

            if (!query.isEmpty()) {
                boolean match = (b.batchNumber() != null && b.batchNumber().toLowerCase().contains(query))
                        || (b.productName() != null && b.productName().toLowerCase().contains(query))
                        || (b.categoryName() != null && b.categoryName().toLowerCase().contains(query))
                        || (b.warehouseName() != null && b.warehouseName().toLowerCase().contains(query));
                if (!match) continue;
            }

            radarTableModel.addRow(new Object[]{
                    b.batchNumber(),
                    b.productName(),
                    b.categoryName(),
                    b.warehouseName(),
                    b.quantity().stripTrailingZeros().toPlainString() + " un",
                    UIHelper.formatMzn(b.unitCost()),
                    UIHelper.formatMzn(b.potentialLossValue()),
                    b.expirationDate() != null ? b.expirationDate().format(UIHelper.DATE_FMT) : "-",
                    b.daysUntilExpiration() < 0 ? "Venceu há " + (-b.daysUntilExpiration()) + " dias" : b.daysUntilExpiration() + " dias",
                    b.alertLevel()
            });
        }
    }

    void updateApprovalButtonsState() {
        int row = TableFilter.selectedModelRow(wasteTable);
        if (row < 0 || row >= wasteTableModel.getRowCount()) {
            approveBtn.setEnabled(false);
            rejectBtn.setEnabled(false);
            return;
        }

        String statusStr = (String) wasteTableModel.getValueAt(row, 9);
        boolean isPending = statusStr != null && statusStr.contains("Pendente");
        approveBtn.setEnabled(isPending);
        rejectBtn.setEnabled(isPending);
    }

    private void handleApproveSelected(boolean approve) {
        int row = TableFilter.selectedModelRow(wasteTable);
        if (row < 0) {
            ToastManager.show(this, FeedbackType.WARNING, "Seleccione uma quebra para continuar.");
            return;
        }
        Long id = (Long) wasteTableModel.getValueAt(row, 0);
        String actionTitle = approve ? "Aprovar Registo de Quebra" : "Rejeitar Registo de Quebra";
        String prompt = approve
                ? "Deseja aprovar esta quebra de stock? O stock será abatido no sistema."
                : "Indique o motivo da rejeição da quebra:";

        JPanel form = new JPanel(new BorderLayout(0, 10));
        form.setOpaque(false);
        JLabel lbl = new JLabel("<html><body style='width:360px'>" + prompt + "</body></html>");
        lbl.setForeground(UIHelper.TEXT_LIGHT);
        lbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        form.add(lbl, BorderLayout.NORTH);

        JTextField notesField = new JTextField();
        UIHelper.styleTextField(notesField);
        if (!approve) {
            form.add(notesField, BorderLayout.CENTER);
        }

        ModernFormDialog dlg = new ModernFormDialog(SwingUtilities.getWindowAncestor(this), actionTitle, form);
        dlg.setConfirmButton(approve ? "Aprovar" : "Rejeitar", approve ? "fas-check" : "fas-times");
        final String[] resultNotes = new String[1];
        dlg.setOnSave(() -> {
            String text = notesField.getText().trim();
            if (!approve && text.isEmpty()) {
                throw new mz.multicore.erp.architecture.exception.BusinessRuleException("Indique o motivo da rejeição.");
            }
            resultNotes[0] = text;
        });
        if (!dlg.showDialog()) return;
        String notes = resultNotes[0];

        UIHelper.runWithProgress(this, (approve ? "A aprovar" : "A rejeitar") + " quebra...", () -> {
            wasteApiClient.approve(id, approve, notes);
            return null;
        }, res -> {
            ToastManager.success(this, "Registo " + (approve ? "aprovado" : "rejeitado") + " com sucesso!");
            reloadData();
        }, err -> ToastManager.show(this, FeedbackType.ERROR, "Falha ao processar: " + err.getMessage()));
    }

    private void handleWasteFromSelectedBatch() {
        int row = radarTable.getSelectedRow();
        if (row < 0) {
            ToastManager.show(this, FeedbackType.INFO, "Selecione primeiro um lote na tabela do radar.");
            return;
        }

        String batchNum = (String) radarTableModel.getValueAt(row, 0);
        ExpiringBatchAlertDTO selectedAlert = currentRadarList.stream()
                .filter(b -> b.batchNumber().equals(batchNum))
                .findFirst()
                .orElse(null);

        if (selectedAlert != null) {
            openRegisterWasteDialog(selectedAlert);
        }
    }

    private void openRegisterWasteDialog(ExpiringBatchAlertDTO prefillBatch) {
        if (cachedWarehouses.isEmpty() || cachedProducts.isEmpty()) {
            ToastManager.show(this, FeedbackType.INFO, "A carregar dados de apoio, aguarde um momento...");
            return;
        }

        JComboBox<WarehouseItem> whCombo = new JComboBox<>();
        UIHelper.styleComboBox(whCombo);
        whCombo.setPreferredSize(new Dimension(320, UIHelper.FORM_CONTROL_HEIGHT));
        for (WarehouseDTO w : cachedWarehouses) {
            whCombo.addItem(new WarehouseItem(w.id(), w.name()));
        }

        SearchableComboBox<ProductItem> prdCombo = new SearchableComboBox<>(
                "Pesquisar produto por código ou nome", ProductItem::toString,
                product -> product.sku() + " " + product.name());
        prdCombo.setPreferredSize(new Dimension(320, UIHelper.FORM_CONTROL_HEIGHT));
        List<ProductItem> productItems = new ArrayList<>();
        for (ProductDTO p : cachedProducts) {
            productItems.add(new ProductItem(p.id(), p.sku(), p.name(),
                    p.purchasePrice() != null ? p.purchasePrice() : p.unitPrice()));
        }
        prdCombo.setItems(productItems);

        JTextField qtyField = new JTextField("1");
        UIHelper.styleTextField(qtyField);
        qtyField.setPreferredSize(new Dimension(320, UIHelper.FORM_CONTROL_HEIGHT));

        JComboBox<WasteReason> reasonCombo = new JComboBox<>(WasteReason.values());
        UIHelper.styleComboBox(reasonCombo);
        reasonCombo.setPreferredSize(new Dimension(320, UIHelper.FORM_CONTROL_HEIGHT));
        reasonCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof WasteReason r) setText(r.getDescription());
                return this;
            }
        });

        JTextField notesField = new JTextField();
        UIHelper.styleTextField(notesField);
        notesField.setPreferredSize(new Dimension(320, UIHelper.FORM_CONTROL_HEIGHT));

        JLabel costPreviewLabel = new JLabel("Custo Estimado: 0,00 MT");
        costPreviewLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        costPreviewLabel.setForeground(UIHelper.ACCENT_BLUE);

        JLabel thresholdWarning = new JLabel("Perdas > 2.500,00 MT requerem aprovação formal da gerência.");
        thresholdWarning.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        thresholdWarning.setForeground(UIHelper.PENDING_YELLOW);

        Runnable updateCostPreview = () -> {
            ProductItem selProd = (ProductItem) prdCombo.getSelectedItem();
            if (selProd == null) return;
            try {
                BigDecimal qty = new BigDecimal(qtyField.getText().trim().replace(",", "."));
                BigDecimal cost = selProd.cost().multiply(qty).setScale(2, RoundingMode.HALF_UP);
                costPreviewLabel.setText("Custo Estimado: " + UIHelper.formatMzn(cost));
                if (cost.compareTo(new BigDecimal("2500.00")) > 0) {
                    thresholdWarning.setText("Atenção: Custo excede 2.500 MT. Ficará pendente de aprovação.");
                    thresholdWarning.setForeground(UIHelper.REJECTED_RED);
                } else {
                    thresholdWarning.setText("Custo dentro da alçada ordinária (auto-aprovação).");
                    thresholdWarning.setForeground(UIHelper.APPROVED_GREEN);
                }
            } catch (Exception ex) {
                costPreviewLabel.setText("Custo Estimado: 0,00 MT");
            }
        };

        prdCombo.addActionListener(e -> updateCostPreview.run());
        UIHelper.onTextChange(qtyField, updateCostPreview);

        Long batchId = null;
        if (prefillBatch != null) {
            batchId = prefillBatch.batchId();
            for (int i = 0; i < whCombo.getItemCount(); i++) {
                if (whCombo.getItemAt(i).id().equals(prefillBatch.warehouseId())) {
                    whCombo.setSelectedIndex(i);
                    break;
                }
            }
            for (int i = 0; i < prdCombo.getItemCount(); i++) {
                if (prdCombo.getItemAt(i).id().equals(prefillBatch.productId())) {
                    prdCombo.setSelectedIndex(i);
                    break;
                }
            }
            qtyField.setText(prefillBatch.quantity().stripTrailingZeros().toPlainString());
            reasonCombo.setSelectedItem(WasteReason.EXPIRED);
            notesField.setText("Quebra por validade expirada / crítica (Lote: " + prefillBatch.batchNumber() + ")");
        }
        updateCostPreview.run();

        JPanel formPanel = UIHelper.createDialogForm(
                "Armazém:", whCombo,
                "Produto:", prdCombo,
                "Quantidade:", qtyField,
                "Motivo:", reasonCombo,
                "Observações:", notesField
        );

        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setOpaque(false);
        wrapper.add(formPanel, BorderLayout.NORTH);

        JPanel infoBottom = new JPanel(new GridLayout(2, 1, 0, 4));
        infoBottom.setOpaque(false);
        infoBottom.add(costPreviewLabel);
        infoBottom.add(thresholdWarning);
        wrapper.add(infoBottom, BorderLayout.SOUTH);

        boolean confirmed = new ModernFormDialog(
                UIHelper.mainWindow,
                "Registar Quebra de Stock",
                "fas-trash-alt",
                "Abate de mercadoria danificada, vencida ou avariada",
                wrapper
        ).showDialog();

        if (!confirmed) return;

        WarehouseItem selWh = (WarehouseItem) whCombo.getSelectedItem();
        ProductItem selPrd = (ProductItem) prdCombo.getSelectedItem();
        WasteReason selReason = (WasteReason) reasonCombo.getSelectedItem();
        BigDecimal qty;
        try {
            qty = new BigDecimal(qtyField.getText().trim().replace(",", "."));
            if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException();
        } catch (Exception ex) {
            ToastManager.show(this, FeedbackType.ERROR, "Quantidade inválida. Digite um valor numérico positivo.");
            return;
        }

        final Long finalBatchId = batchId;
        CreateStockWasteRequest req = new CreateStockWasteRequest(
                CurrentUserContext.requireCurrentCompanyId(),
                selWh.id(),
                selPrd.id(),
                finalBatchId,
                qty,
                selReason,
                notesField.getText().trim()
        );

        UIHelper.runWithProgress(this, "A registar quebra de stock...", () -> {
            return wasteApiClient.register(req);
        }, res -> {
            if (res.status() == WasteStatus.APPROVED) {
                ToastManager.success(this, "Quebra registada e stock abatido com sucesso!");
            } else {
                ToastManager.show(this, FeedbackType.INFO, "Registo guardado com estado 'Pendente Aprovação' por exceder a alçada.");
            }
            reloadData();
        }, err -> ToastManager.show(this, FeedbackType.ERROR, "Erro ao registar: " + err.getMessage()));
    }

    private void exportPdfReport() {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        LocalDate start = parseDate(dateStartField.getText().trim(), LocalDate.now().minusDays(30));
        LocalDate end = parseDate(dateEndField.getText().trim(), LocalDate.now());

        UIHelper.runWithProgress(this, "A gerar relatório oficial de quebras em PDF...", () -> {
            return wasteApiClient.renderReportPdf(companyId, start, end);
        }, pdf -> {
            PrintPreviewDialog.show(this, pdf, "relatorio-quebras-perdas");
        }, err -> ToastManager.show(this, FeedbackType.ERROR, "Erro ao gerar PDF: " + err.getMessage()));
    }

    private LocalDate parseDate(String text, LocalDate fallback) {
        try {
            return LocalDate.parse(text, UIHelper.DATE_FMT);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private record WarehouseItem(Long id, String name) {
        @Override public String toString() { return name; }
    }

    private record ProductItem(Long id, String sku, String name, BigDecimal cost) {
        @Override public String toString() { return sku + " - " + name; }
    }
    private JLabel filterLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(UIHelper.TEXT_MUTED);
        return label;
    }
}
