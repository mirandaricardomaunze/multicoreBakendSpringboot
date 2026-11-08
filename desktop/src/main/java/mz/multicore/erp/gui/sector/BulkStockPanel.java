package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.BulkStockApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.bulkstock.dto.BulkMeasurementDTO;
import mz.multicore.erp.modules.bulkstock.dto.BulkMeasurementRequest;
import mz.multicore.erp.modules.bulkstock.dto.BulkStockSettingDTO;
import mz.multicore.erp.modules.bulkstock.model.BulkMeasurementMethod;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Bulk stock measured on the pile (SA-C17, {@code docs/STOCK_GRANEL_SPEC.md}). The yard measures sand, gravel
 * or stone, previews the comparison with the system stock (net of sold material awaiting pickup) and records
 * it. Differences within tolerance are posted at once; larger ones wait for a manager in the history below.
 */
public class BulkStockPanel extends JPanel {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] COLUMNS = {"Data", "Produto", "Armazém", "Medido", "Por levantar", "Sistema", "Diferença", "%", "Estado"};

    private final BulkStockApiClient api;
    private final InventoryApiClient inventoryApi;
    private final mz.multicore.erp.desktop.client.ComercialApiClient comercialApi;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final SearchableComboBox<BulkStockSettingDTO> productCombo = new SearchableComboBox<>("Pesquisar produto a granel",
            s -> s.productName() + " (" + PickupsPanel.unit(s.baseUnitCode()) + ") · tolerância "
                    + ProductUnitsPanel.plain(s.tolerancePercent()) + "%", s -> s.productSku() + " " + s.productName());
    private final SearchableComboBox<WarehouseDTO> warehouseCombo = new SearchableComboBox<>("Armazém",
            WarehouseDTO::name, WarehouseDTO::name);
    private final JComboBox<BulkMeasurementMethod> methodCombo = new JComboBox<>(BulkMeasurementMethod.values());
    private final DecimalField d1 = new DecimalField("", 3, false);
    private final DecimalField d2 = new DecimalField("", 3, false);
    private final DecimalField d3 = new DecimalField("", 3, false);
    private final DecimalField quantity = new DecimalField("", 3, false);
    private final JTextField notes = new JTextField();
    private final JLabel l1 = new JLabel(), l2 = new JLabel(), l3 = new JLabel(), lq = new JLabel("Quantidade medida:");
    private final JLabel result = new JLabel(" ");
    private final CardLayout firstMeasureCards = new CardLayout();
    private final JPanel firstMeasure = new JPanel(firstMeasureCards);
    private final JPanel emptyBanner = new JPanel();
    private JPanel secondMeasure;
    private JPanel thirdMeasure;
    private final ModernButton previewBtn = UIHelper.createSecondaryButton("Pré-visualizar");
    private final ModernButton recordBtn = UIHelper.createSuccessButton("Nova Medição");
    private final ModernButton settingsBtn = UIHelper.createSecondaryButton("Produtos a Granel");
    private final JComponent measureForm;
    private final ModernButton approveBtn = UIHelper.createSuccessButton("Aprovar");
    private final ModernButton rejectBtn = UIHelper.createDangerButton("Rejeitar");
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final List<BulkMeasurementDTO> rows = new ArrayList<>();

    public BulkStockPanel(BulkStockApiClient api, InventoryApiClient inventoryApi,
                          mz.multicore.erp.desktop.client.ComercialApiClient comercialApi) {
        this.api = api;
        this.inventoryApi = inventoryApi;
        this.comercialApi = comercialApi;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(12, 5, 5, 5));
        measureForm = buildMeasureForm();
        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.setOpaque(false);
        north.add(feedback, BorderLayout.NORTH);
        north.add(buildEmptyBanner(settingsBtn), BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);
        add(buildHistoryCard(), BorderLayout.CENTER);
        syncMethod();
        syncButtons();
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) reload();
        });
    }

    // ─── Layout ─────────────────────────────────────────────────────────────────

    /** Measurement form shown inside the "Nova Medição" dialog, with a live preview of the comparison. */
    private JComponent buildMeasureForm() {
        UIHelper.styleComboBox(methodCombo);
        methodCombo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, index, sel, focus);
                if (value instanceof BulkMeasurementMethod m) setText(m.label());
                return this;
            }
        });
        methodCombo.addActionListener(e -> syncMethod());
        productCombo.addActionListener(e -> syncMethod());
        UIHelper.styleTextField(notes);
        notes.putClientProperty("JTextField.placeholderText", "Opcional (ex.: monte junto ao portão)");

        JPanel first = new JPanel(new GridLayout(1, 3, 12, 0));
        first.setOpaque(false);
        first.add(block(caption("Produto a granel"), productCombo));
        first.add(block(caption("Armazém"), warehouseCombo));
        first.add(block(caption("Como foi medido"), methodCombo));

        firstMeasure.setOpaque(false);
        firstMeasure.add(block(l1, d1), "dimension");
        firstMeasure.add(block(lq, quantity), "quantity");
        secondMeasure = block(l2, d2);
        thirdMeasure = block(l3, d3);
        JPanel second = new JPanel(new GridLayout(1, 3, 12, 0));
        second.setOpaque(false);
        second.add(firstMeasure);
        second.add(secondMeasure);
        second.add(thirdMeasure);

        result.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        result.setForeground(UIHelper.TEXT_MUTED);
        previewBtn.setIcon(UIHelper.icon("fas-calculator", 14));
        previewBtn.setToolTipText("Calcular a diferença sem gravar");
        previewBtn.addActionListener(e -> preview());
        JPanel previewRow = new JPanel(new BorderLayout(10, 0));
        previewRow.setOpaque(false);
        previewRow.add(result, BorderLayout.CENTER);
        previewRow.add(previewBtn, BorderLayout.EAST);

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 12));
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(6, 4, 6, 4));
        form.add(first);
        form.add(second);
        form.add(block(caption("Observações"), notes));
        form.add(previewRow);
        form.setPreferredSize(new Dimension(760, form.getPreferredSize().height));
        return form;
    }

    private void openMeasureDialog() {
        if (productCombo.allItems().isEmpty()) {
            feedback.show(FeedbackType.INFO, "Ainda não há produtos a granel",
                    "Defina-os primeiro em \"Produtos a Granel\".", null, null);
            return;
        }
        for (DecimalField f : new DecimalField[]{d1, d2, d3, quantity}) f.setText("");
        notes.setText("");
        result.setForeground(UIHelper.TEXT_MUTED);
        result.setText("Meça o monte e carregue em Pré-visualizar.");
        syncMethod();
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Nova Medição de Granel", "fas-mountain",
                "Dentro da tolerância o stock é acertado já; acima, espera por um gestor", measureForm);
        dialog.setConfirmButton("Registar Medição", "fas-save");
        BulkMeasurementDTO[] saved = new BulkMeasurementDTO[1];
        dialog.setOnSaveAsync(() -> {
            BulkMeasurementRequest request = request();
            return () -> { saved[0] = api.record(request); return null; };
        });
        if (dialog.showDialog() && saved[0] != null) {
            BulkMeasurementDTO dto = saved[0];
            reload();
            ToastManager.success(this, "APPLIED".equals(dto.status()) ? "Medição registada e stock acertado."
                    : "PENDING_APPROVAL".equals(dto.status()) ? "Medição registada: aguarda aprovação de um gestor."
                    : "Medição registada: o monte bate certo com o sistema.");
        }
    }

    /** Explains the first step when no bulk product exists yet, instead of showing an empty product list. */
    private JComponent buildEmptyBanner(ModernButton settingsBtn) {
        emptyBanner.setOpaque(false);
        emptyBanner.setLayout(new BorderLayout(10, 0));
        emptyBanner.setBorder(new EmptyBorder(4, 6, 4, 6));
        JLabel icon = new JLabel(UIHelper.icon("fas-info-circle", 18, UIHelper.ACCENT_BLUE));
        icon.setVerticalAlignment(SwingConstants.TOP);
        JLabel text = new JLabel("<html><b>Primeiro passo:</b> um gestor marca os produtos medidos no monte (areia, brita, "
                + "pedra) e a perda natural aceite, no botão <b>Produtos a Granel</b>. Depois escolhe-os aqui.</html>");
        text.setForeground(UIHelper.TEXT_LIGHT);
        ModernButton defineBtn = UIHelper.createSuccessButton("Definir produtos a granel");
        defineBtn.setIcon(UIHelper.icon("fas-plus", 13));
        defineBtn.addActionListener(e -> settingsBtn.doClick());
        emptyBanner.add(icon, BorderLayout.WEST);
        emptyBanner.add(text, BorderLayout.CENTER);
        emptyBanner.add(defineBtn, BorderLayout.EAST);
        emptyBanner.setVisible(false);
        return emptyBanner;
    }

    private static JPanel block(JLabel label, JComponent input) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        label.setForeground(UIHelper.TEXT_MUTED);
        panel.add(label, BorderLayout.NORTH);
        panel.add(input, BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildHistoryCard() {
        UIHelper.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        for (int c = 3; c <= 6; c++) table.getColumnModel().getColumn(c).setCellRenderer(TableCellRenderers.quantity());
        table.getColumnModel().getColumn(8).setCellRenderer(TableCellRenderers.status());
        table.getSelectionModel().addListSelectionListener(e -> syncButtons());
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);
        approveBtn.setIcon(UIHelper.icon("fas-check", 14));
        approveBtn.setToolTipText("Acertar o stock com a diferença medida (gestão)");
        approveBtn.addActionListener(e -> review(true));
        rejectBtn.setIcon(UIHelper.icon("fas-times", 14));
        rejectBtn.addActionListener(e -> review(false));
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::reload);
        settingsBtn.setIcon(UIHelper.icon("fas-mountain", 14, UIHelper.ACCENT_ORANGE));
        settingsBtn.setToolTipText("Escolher os produtos medidos no monte e a tolerância de perda natural (gestão)");
        settingsBtn.addActionListener(e -> BulkSettingsDialog.show(this, api, comercialApi, feedback, this::reload));
        recordBtn.setIcon(UIHelper.icon("fas-plus", 14));
        recordBtn.setToolTipText("Registar a medição de um monte (areia, brita, pedra)");
        recordBtn.addActionListener(e -> openMeasureDialog());

        JTextField search = TableFilter.searchField("Produto ou armazém…");
        search.setPreferredSize(new Dimension(190, UIHelper.FORM_CONTROL_HEIGHT));
        JComboBox<String> state = TableFilter.combo("Todos os estados", "Pendente de aprovação", "Concluído", "Rejeitado", "Resolvido");
        UIHelper.styleComboBox(state);
        state.setPreferredSize(new Dimension(190, UIHelper.FORM_CONTROL_HEIGHT));
        TableFilter.install(table, search, new TableFilter.ColumnFilter(state, 8));
        JPanel filters = TableFilter.bar(search, TableFilter.label("Estado:"), state);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));
        card.add(ResponsiveSupport.cardTop("Medições de Granel (areia, brita, pedra)", filters, refreshBtn, settingsBtn,
                rejectBtn, approveBtn, recordBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(table), BorderLayout.SOUTH);
        return card;
    }

    // ─── Data ───────────────────────────────────────────────────────────────────

    public void reload() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null || api == null) return;
        UIHelper.loadAsync(this, () -> new Loaded(api.settings(companyId), inventoryApi == null ? List.of()
                        : inventoryApi.getWarehousesByCompany(companyId), api.measurements(companyId)),
                this::apply,
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar o granel", error.getMessage(),
                        "Tentar novamente", this::reload));
    }

    private void apply(Loaded loaded) {
        productCombo.setItems(loaded.settings());
        warehouseCombo.setItems(loaded.warehouses());
        boolean none = loaded.settings().isEmpty();
        emptyBanner.setVisible(none);
        previewBtn.setEnabled(!none);
        rows.clear();
        rows.addAll(loaded.measurements());
        model.setRowCount(0);
        for (BulkMeasurementDTO m : rows) {
            model.addRow(new Object[]{m.measuredAt() == null ? "—" : DATE_TIME.format(m.measuredAt()), m.productName(),
                    m.warehouseName(), m.measuredQuantity(), m.pendingPickup(), m.systemQuantity(), m.difference(),
                    ProductUnitsPanel.plain(m.differencePercent()) + " %", statusCode(m.status())});
        }
        syncButtons();
    }

    // ─── Actions ────────────────────────────────────────────────────────────────

    /** Computes the comparison without saving; problems are shown on the dialog's result line, where the user is looking. */
    private void preview() {
        BulkMeasurementRequest request;
        try {
            request = request();
        } catch (IllegalArgumentException ex) {
            result.setForeground(UIHelper.PENDING_YELLOW);
            result.setText(ex.getMessage());
            return;
        }
        UIHelper.submitAsync(previewBtn, () -> api.preview(request), this::showResult, error -> {
            result.setForeground(UIHelper.REJECTED_RED);
            result.setText("<html>" + error.getMessage() + "</html>");
        });
    }

    private void review(boolean approve) {
        int row = TableFilter.selectedModelRow(table);
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (row < 0 || row >= rows.size() || companyId == null) return;
        BulkMeasurementDTO m = rows.get(row);
        String reason = null;
        if (approve) {
            if (!UIHelper.confirm(this, "Acertar o stock de " + m.productName() + " em " + ProductUnitsPanel.plain(m.difference())
                    + " " + PickupsPanel.unit(m.baseUnitCode()) + " (" + ProductUnitsPanel.plain(m.differencePercent()) + " %)?",
                    "Confirmar")) return;
        } else {
            reason = UIHelper.promptRequiredText("Rejeitar Medição", "fas-times", m.productName(), "Motivo (ex.: medir de novo):");
            if (reason == null || reason.isBlank()) return;
        }
        String finalReason = reason;
        ModernButton button = approve ? approveBtn : rejectBtn;
        UIHelper.submitAsync(button, () -> approve ? api.approve(companyId, m.id()) : api.reject(companyId, m.id(), finalReason.trim()),
                done -> {
                    reload();
                    ToastManager.success(this, approve ? "Stock acertado." : "Medição rejeitada.");
                },
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível decidir a medição", error.getMessage(), null, null));
    }

    // ─── Support ────────────────────────────────────────────────────────────────

    private BulkMeasurementRequest request() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        BulkStockSettingDTO product = productCombo.selectedValue();
        WarehouseDTO warehouse = warehouseCombo.selectedValue();
        if (product == null) throw new IllegalArgumentException("Escolha o produto a granel.");
        if (warehouse == null) throw new IllegalArgumentException("Escolha o armazém onde está o monte.");
        BulkMeasurementMethod method = (BulkMeasurementMethod) methodCombo.getSelectedItem();
        boolean direct = method == BulkMeasurementMethod.DIRECT;
        return new BulkMeasurementRequest(companyId, product.productId(), warehouse.id(), method.name(),
                direct ? quantity.value() : null,
                direct ? null : d1.value(), direct ? null : d2.value(),
                direct || method.dimensions() < 3 ? null : d3.value(),
                notes.getText().trim());
    }

    private void showResult(BulkMeasurementDTO m) {
        String unit = PickupsPanel.unit(m.baseUnitCode());
        String volume = m.volumeM3() == null ? "" : "Volume " + ProductUnitsPanel.plain(m.volumeM3()) + " m³ · ";
        boolean even = m.difference().signum() == 0;
        result.setForeground(even ? UIHelper.APPROVED_GREEN : m.withinTolerance() ? UIHelper.PENDING_YELLOW : UIHelper.REJECTED_RED);
        result.setText("<html>" + volume + "Medido <b>" + ProductUnitsPanel.plain(m.measuredQuantity()) + " " + unit + "</b>"
                + " − por levantar " + ProductUnitsPanel.plain(m.pendingPickup()) + " − sistema "
                + ProductUnitsPanel.plain(m.systemQuantity()) + " = <b>" + ProductUnitsPanel.plain(m.difference()) + " " + unit
                + "</b> (" + ProductUnitsPanel.plain(m.differencePercent()) + " %, tolerância "
                + ProductUnitsPanel.plain(m.tolerancePercent()) + " %)</html>");
    }

    private void syncMethod() {
        BulkMeasurementMethod method = (BulkMeasurementMethod) methodCombo.getSelectedItem();
        boolean direct = method == BulkMeasurementMethod.DIRECT;
        boolean cone = method == BulkMeasurementMethod.CONE;
        BulkStockSettingDTO product = productCombo.selectedValue();
        String unit = product == null ? "unidade do produto" : PickupsPanel.unit(product.baseUnitCode());
        firstMeasureCards.show(firstMeasure, direct ? "quantity" : "dimension");
        lq.setText("Quantidade medida (" + unit + ")");
        l1.setText(cone ? "Diâmetro da base (m)" : "Comprimento (m)");
        l2.setText(cone ? "Altura (m)" : "Largura (m)");
        l3.setText("Altura (m)");
        if (secondMeasure != null) secondMeasure.setVisible(!direct);
        if (thirdMeasure != null) thirdMeasure.setVisible(!direct && !cone);
        revalidate();
        repaint();
    }

    private void syncButtons() {
        int row = TableFilter.selectedModelRow(table);
        boolean pending = row >= 0 && row < rows.size() && "PENDING_APPROVAL".equals(rows.get(row).status());
        approveBtn.setEnabled(pending);
        rejectBtn.setEnabled(pending);
    }

    /** Maps measurement states to the shared status renderer (PT-MZ labels and colours). */
    static String statusCode(String status) {
        return switch (status) {
            case "APPLIED" -> "COMPLETED";
            case "PENDING_APPROVAL" -> "PENDING_APPROVAL";
            case "REJECTED" -> "REJECTED";
            default -> "RESOLVED";
        };
    }

    private static JLabel caption(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UIHelper.TEXT_MUTED);
        return label;
    }

    private record Loaded(List<BulkStockSettingDTO> settings, List<WarehouseDTO> warehouses, List<BulkMeasurementDTO> measurements) {}
}
