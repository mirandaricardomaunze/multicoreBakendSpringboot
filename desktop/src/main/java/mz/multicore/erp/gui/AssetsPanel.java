package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.AssetApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.assets.dto.AssetDTO;
import mz.multicore.erp.modules.assets.dto.CreateAssetRequest;
import mz.multicore.erp.modules.assets.dto.DepreciationScheduleDTO;
import mz.multicore.erp.modules.assets.model.AssetCategory;
import mz.multicore.erp.modules.assets.model.AssetStatus;

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
 * Painel de Gestão de Activos Fixos & Património.
 * Implementa controlo de imobilizado, mapa de amortizações (Código do IRPC)
 * e apuramento do Valor Líquido Contabilístico (VLC).
 */
public final class AssetsPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0.00 MT");

    private final AssetApiClient assetApiClient;
    private final PrintApiClient printApiClient;

    // Componentes de Feedback e KPIs
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final JLabel kpiTotalCost = new JLabel("0.00 MT");
    private final JLabel kpiAccumulated = new JLabel("0.00 MT");
    private final JLabel kpiNetBookValue = new JLabel("0.00 MT");
    private final JLabel kpiActiveCount = new JLabel("0 Activos");

    // Tabela e Modelo
    private final DefaultTableModel tableModel;
    private final JTable table;
    private List<AssetDTO> currentAssets = new ArrayList<>();

    // Filtros
    private final JTextField searchField = TableFilter.searchField("Pesquisar activo por código, nome, local ou responsável...");
    private final JComboBox<String> categoryFilterCombo = TableFilter.combo("Todas as Categorias", "Edifícios & Construções", "Equipamento Básico & Máquinas", "Equipamento de Transporte", "Equipamento Administrativo & Informático", "Imobilizações Corpóreas Diversas", "Activos Intangíveis / Software");
    private final JComboBox<String> statusFilterCombo = TableFilter.combo("Todos os Estados", "Em Operação", "Em Depreciação", "Totalmente Depreciado", "Em Manutenção", "Abatido", "Alienado / Vendido");

    public AssetsPanel(AssetApiClient assetApiClient) {
        this(assetApiClient, null);
    }

    public AssetsPanel(AssetApiClient assetApiClient, PrintApiClient printApiClient) {
        this.assetApiClient = assetApiClient;
        this.printApiClient = printApiClient;
        setLayout(new BorderLayout(0, 12));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(15, 20, 20, 20));

        // Cabeçalho Principal
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(UIHelper.createHeading("Activos Fixos & Património"), BorderLayout.WEST);

        feedback.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(titleRow);
        headerPanel.add(Box.createVerticalStrut(6));
        headerPanel.add(feedback);

        // Grade canónica de KPIs (KpiCard.STANDARD_CARD_HEIGHT = 96px)
        JPanel kpiGrid = KpiCard.createGrid(4);
        kpiGrid.add(KpiCard.createCard("Total Imobilizado", kpiTotalCost, "Custo histórico de aquisição",
                "fas-building", UIHelper.ACCENT_BLUE));
        kpiGrid.add(KpiCard.createCard("Amortizações Acumuladas", kpiAccumulated, "Depreciações fiscais acumuladas",
                "fas-chart-line", UIHelper.PENDING_YELLOW));
        kpiGrid.add(KpiCard.createCard("Valor Líquido (VLC)", kpiNetBookValue, "Valor contabilístico actual",
                "fas-coins", UIHelper.APPROVED_GREEN));
        kpiGrid.add(KpiCard.createCard("Activos Registados", kpiActiveCount, "Património sob gestão",
                "fas-cubes", UIHelper.MODULE_ACCOUNTING));

        JPanel northStack = new JPanel(new BorderLayout(0, 12));
        northStack.setOpaque(false);
        northStack.add(headerPanel, BorderLayout.NORTH);
        northStack.add(kpiGrid, BorderLayout.CENTER);
        add(northStack, BorderLayout.NORTH);

        // Card Único ModernPanel(16) com Filtros e Tabela
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        String[] cols = {
                "Código", "Designação / Descrição", "Categoria", "Data Aquisição",
                "Custo Aquisição", "Taxa Anual", "Amort. Acumulada", "Valor Líquido (VLC)",
                "Localização", "Responsável", "Estado"
        };

        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(tableModel);
        UIHelper.styleTable(table);

        int[] widths = {90, 220, 140, 100, 120, 80, 120, 120, 130, 130, 100};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        UIHelper.ensureHeadersFit(table);

        table.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(10).setCellRenderer(TableCellRenderers.status());
        table.putClientProperty("customDetailAction", (Runnable) this::showAssetDetails);

        JScrollPane scrollPane = new JScrollPane(table);
        UIHelper.styleScrollPane(scrollPane);

        TableFilter.install(table, searchField,
                new TableFilter.ColumnFilter(categoryFilterCombo, 2),
                new TableFilter.ColumnFilter(statusFilterCombo, 10));

        JPanel filterBar = TableFilter.bar(
                searchField,
                TableFilter.label("Categoria:"), categoryFilterCombo,
                TableFilter.label("Estado:"), statusFilterCombo
        );

        // Botões de Acção do Topo
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> loadAssets());

        ActionMenuButton depreciationMenu = UIHelper.createActionMenuButton("Amortizações")
                .addAction("Calcular Quotas", UIHelper.icon("fas-calculator", 14, UIHelper.PENDING_YELLOW), this::openDepreciationDialog)
                .addAction("Mapa Fiscal (Mod. 22)", UIHelper.icon("fas-file-invoice", 14, UIHelper.APPROVED_GREEN), this::printAssetDepreciationMap);
        depreciationMenu.setToolTipText("Gestão e mapas de amortizações e reintegrações fiscais");

        ModernButton newAssetBtn = UIHelper.createSuccessButton("Novo Activo");
        newAssetBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newAssetBtn.setToolTipText("Registar novo bem do imobilizado");
        newAssetBtn.addActionListener(e -> openNewAssetDialog());

        card.add(UIHelper.tableCardTop("Inventário do Imobilizado Corpóreo & Incorpóreo", filterBar,
                refreshBtn, depreciationMenu, newAssetBtn), BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
    }

    private void printAssetDepreciationMap() {
        if (printApiClient == null) {
            feedback.show(FeedbackType.INFO, "Serviço de emissão de mapas fiscais indisponível.");
            return;
        }
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        int year = java.time.LocalDate.now().getYear();
        UIHelper.runWithProgress(this, "A gerar Mapa de Amortizações e Reintegrações (Modelo 22 Anexo D)…",
                () -> printApiClient.renderAssetDepreciationMap(companyId, year),
                pdf -> PrintPreviewDialog.show(this, pdf, "mapa-amortizacoes-mod22-" + year),
                ex -> feedback.show(FeedbackType.ERROR, "Falha ao emitir mapa fiscal: " + ex.getMessage()));
    }

    public void onPanelSelected() {
        loadAssets();
    }

    private void loadAssets() {
        feedback.clear();
        SwingWorker<List<AssetDTO>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<AssetDTO> doInBackground() {
                return assetApiClient.listAssets();
            }

            @Override
            protected void done() {
                try {
                    currentAssets = get();
                    updateTableAndKpis(currentAssets);
                } catch (Exception ex) {
                    feedback.show(FeedbackType.ERROR, "Falha ao carregar activos: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void updateTableAndKpis(List<AssetDTO> assets) {
        tableModel.setRowCount(0);
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalAccumulated = BigDecimal.ZERO;
        BigDecimal totalNetBook = BigDecimal.ZERO;
        int activeCount = 0;

        for (AssetDTO a : assets) {
            totalCost = totalCost.add(a.acquisitionCost() != null ? a.acquisitionCost() : BigDecimal.ZERO);
            totalAccumulated = totalAccumulated.add(a.accumulatedDepreciation() != null ? a.accumulatedDepreciation() : BigDecimal.ZERO);
            totalNetBook = totalNetBook.add(a.netBookValue() != null ? a.netBookValue() : BigDecimal.ZERO);
            if (a.status() == AssetStatus.ACTIVE || a.status() == AssetStatus.DEPRECIATING) {
                activeCount++;
            }

            String dateStr = a.acquisitionDate() != null ? a.acquisitionDate().format(DATE_FMT) : "—";
            String rateStr = a.annualDepreciationRate() != null ? a.annualDepreciationRate().toPlainString() + "%" : "—";
            String catName = a.category() != null ? a.category().label() : "—";
            String statusStr = a.status() != null ? a.status().label() : "—";

            tableModel.addRow(new Object[]{
                    a.assetCode(),
                    a.name(),
                    catName,
                    dateStr,
                    a.acquisitionCost(),
                    rateStr,
                    a.accumulatedDepreciation(),
                    a.netBookValue(),
                    a.location() != null ? a.location() : "—",
                    a.assignedEmployee() != null ? a.assignedEmployee() : "—",
                    statusStr
            });
        }

        kpiTotalCost.setText(MONEY_FMT.format(totalCost));
        kpiAccumulated.setText(MONEY_FMT.format(totalAccumulated));
        kpiNetBookValue.setText(MONEY_FMT.format(totalNetBook));
        kpiActiveCount.setText(activeCount + " Activos");
    }

    private void openNewAssetDialog() {
        Window parent = SwingUtilities.getWindowAncestor(this);

        JTextField codeField = new JTextField(12);
        JTextField nameField = new JTextField(25);
        JComboBox<AssetCategory> categoryCombo = new JComboBox<>(AssetCategory.values());
        UIHelper.styleComboBox(categoryCombo);
        categoryCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof AssetCategory cat) {
                    setText(cat.label() + " (" + (int)(cat.defaultAnnualRate() * 100) + "%)");
                }
                return this;
            }
        });

        DateField dateField = new DateField();
        dateField.setText(LocalDate.now().toString());

        JTextField costField = new JTextField(12);
        JTextField residualField = new JTextField("0.00", 12);
        JTextField rateField = new JTextField(8);
        JTextField serialField = new JTextField(15);
        JTextField locationField = new JTextField(15);
        JTextField employeeField = new JTextField(15);

        categoryCombo.addActionListener(e -> {
            AssetCategory selected = (AssetCategory) categoryCombo.getSelectedItem();
            if (selected != null) {
                rateField.setText(String.valueOf(BigDecimal.valueOf(selected.defaultAnnualRate() * 100).stripTrailingZeros()));
            }
        });
        if (categoryCombo.getSelectedItem() != null) {
            rateField.setText(String.valueOf(BigDecimal.valueOf(((AssetCategory) categoryCombo.getSelectedItem()).defaultAnnualRate() * 100).stripTrailingZeros()));
        }

        JPanel dialogForm = UIHelper.createDialogForm(
                "Código do Activo *:", codeField,
                "Designação / Nome *:", nameField,
                "Categoria Contabilística *:", categoryCombo,
                "Data de Aquisição *:", dateField,
                "Custo de Aquisição (MT) *:", costField,
                "Valor Residual (MT):", residualField,
                "Taxa Anual IRPC (%) *:", rateField,
                "Número de Série:", serialField,
                "Localização Física:", locationField,
                "Responsável:", employeeField
        );

        ModernFormDialog dialog = new ModernFormDialog(parent, "Registar Novo Activo Fixo", "fas-building",
                "Cadastramento de imobilizado e definição das quotas de amortização", dialogForm);

        dialog.setOnSave(() -> {
            String code = codeField.getText().trim();
            if (code.isEmpty()) {
                codeField.requestFocus();
                throw new IllegalArgumentException("O código do activo é obrigatório.");
            }
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                nameField.requestFocus();
                throw new IllegalArgumentException("A designação do activo é obrigatória.");
            }

            LocalDate acqDate;
            try {
                acqDate = LocalDate.parse(dateField.getText().trim());
            } catch (Exception ex) {
                dateField.requestFocus();
                throw new IllegalArgumentException("A data de aquisição é inválida. Use AAAA-MM-DD.");
            }

            BigDecimal cost;
            try {
                cost = new BigDecimal(costField.getText().trim().replace(",", "."));
                if (cost.compareTo(BigDecimal.ZERO) <= 0) throw new Exception();
            } catch (Exception ex) {
                costField.requestFocus();
                throw new IllegalArgumentException("O custo de aquisição deve ser um valor numérico positivo.");
            }

            BigDecimal residual = BigDecimal.ZERO;
            if (!residualField.getText().trim().isEmpty()) {
                try {
                    residual = new BigDecimal(residualField.getText().trim().replace(",", "."));
                } catch (Exception ex) {
                    residualField.requestFocus();
                    throw new IllegalArgumentException("O valor residual deve ser numérico.");
                }
            }

            BigDecimal rate;
            try {
                rate = new BigDecimal(rateField.getText().trim().replace(",", "."));
                if (rate.compareTo(BigDecimal.ZERO) <= 0) throw new Exception();
            } catch (Exception ex) {
                rateField.requestFocus();
                throw new IllegalArgumentException("A taxa de amortização deve ser maior que zero.");
            }

            AssetCategory cat = (AssetCategory) categoryCombo.getSelectedItem();
            CreateAssetRequest req = new CreateAssetRequest(
                    code,
                    name,
                    cat,
                    acqDate,
                    cost,
                    residual,
                    rate,
                    serialField.getText().trim().isEmpty() ? null : serialField.getText().trim(),
                    locationField.getText().trim().isEmpty() ? null : locationField.getText().trim(),
                    employeeField.getText().trim().isEmpty() ? null : employeeField.getText().trim()
            );

            assetApiClient.createAsset(req);
        });

        if (dialog.showDialog()) {
            feedback.show(FeedbackType.SUCCESS, "Activo registado com sucesso.");
            loadAssets();
        }
    }

    private void openDepreciationDialog() {
        Window parent = SwingUtilities.getWindowAncestor(this);
        LocalDate now = LocalDate.now();

        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(now.getYear(), 2000, 2099, 1));
        JSpinner monthSpinner = new JSpinner(new SpinnerNumberModel(now.getMonthValue(), 1, 12, 1));

        JPanel form = UIHelper.createDialogForm(
                "Ano de Competência:", yearSpinner,
                "Mês de Competência:", monthSpinner
        );

        ModernFormDialog dialog = new ModernFormDialog(parent, "Calcular Amortizações do Período", "fas-calculator",
                "Executa o apuramento mensal das quotas de depreciação e actualiza o VLC", form);

        dialog.setOnSave(() -> {
            int year = (Integer) yearSpinner.getValue();
            int month = (Integer) monthSpinner.getValue();
            List<DepreciationScheduleDTO> res = assetApiClient.runMonthlyDepreciation(year, month);
            if (res.isEmpty()) {
                throw new IllegalArgumentException("Nenhum activo elegível para amortização no período " + month + "/" + year + ".");
            }
        });

        if (dialog.showDialog()) {
            feedback.show(FeedbackType.SUCCESS, "Amortizações processadas com sucesso.");
            loadAssets();
        }
    }

    private void showAssetDetails() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int modelRow = table.convertRowIndexToModel(row);
        if (modelRow >= currentAssets.size()) return;
        AssetDTO asset = currentAssets.get(modelRow);

        Window parent = SwingUtilities.getWindowAncestor(this);
        JPanel detailsPanel = new JPanel(new BorderLayout(0, 10));
        detailsPanel.setOpaque(false);

        String[] cols = {"Período", "Data Cálculo", "VLC Inicial", "Quota Amortização", "Amort. Acumulada", "VLC Final"};
        DefaultTableModel histModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable histTable = new JTable(histModel);
        UIHelper.styleTable(histTable);
        histTable.getColumnModel().getColumn(2).setCellRenderer(TableCellRenderers.money());
        histTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        histTable.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.money());
        histTable.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.money());

        JScrollPane histScroll = new JScrollPane(histTable);
        histScroll.setPreferredSize(new Dimension(650, 240));
        detailsPanel.add(histScroll, BorderLayout.CENTER);

        try {
            List<DepreciationScheduleDTO> history = assetApiClient.getDepreciationHistory(asset.id());
            for (DepreciationScheduleDTO d : history) {
                String period = d.periodMonth() + "/" + d.periodYear();
                String calcDate = d.calculationDate() != null ? d.calculationDate().format(DATE_FMT) : "—";
                histModel.addRow(new Object[]{
                        period,
                        calcDate,
                        d.openingBookValue(),
                        d.depreciationAmount(),
                        d.accumulatedDepreciation(),
                        d.closingBookValue()
                });
            }
        } catch (Exception ex) {
            feedback.show(FeedbackType.ERROR, "Falha ao obter histórico de depreciações: " + ex.getMessage());
        }

        ModernFormDialog dialog = new ModernFormDialog(parent, "Histórico de Amortizações: " + asset.assetCode() + " - " + asset.name(),
                "fas-list-alt", "Registro cronológico das quotas de amortização processadas", detailsPanel);
        dialog.showDialog();
    }
}
