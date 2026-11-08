package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ButcheryApiClient;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.butchery.dto.BreakdownTemplateDTO;
import mz.multicore.erp.modules.butchery.dto.CarcassBreakdownDTO;
import mz.multicore.erp.modules.butchery.dto.CarcassBreakdownLineDTO;
import mz.multicore.erp.modules.butchery.model.CarcassBreakdownStatus;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Butchery (F3, {@code docs/DESMANCHA_SPEC.md}). Tab "Desmanchas": every carcass cut, with input, cuts, loss and
 * cost; on the right, the cuts with cost per kg, yield against the model and the margin at the current price.
 * Tab "Modelos": the cuts each carcass should give. Both live in their own sidebar module, "Talho".
 */
public class ButcheryPanel extends JPanel {

    private static final String[] COLUMNS = {"Data", "Nº", "Carcaça", "Entrada (kg)", "Cortes (kg)", "Perda", "Custo", "Estado"};
    private static final String[] CUT_COLUMNS = {"Corte", "Peso (kg)", "Rend. real", "Rend. esperado", "Custo/kg", "Preço/kg", "Margem"};
    private static final String[] TEMPLATE_COLUMNS = {"Modelo", "Carcaça", "Cortes", "Rend. esperado", "Perda aceite", "Estado"};

    private final ButcheryApiClient api;
    private final ComercialApiClient comercialApi;
    private final InventoryApiClient inventoryApi;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final DefaultTableModel model = readOnly(COLUMNS);
    private final JTable table = new JTable(model);
    private final DefaultTableModel cutModel = readOnly(CUT_COLUMNS);
    private final JTable cutTable = new JTable(cutModel);
    private final DefaultTableModel templateModel = readOnly(TEMPLATE_COLUMNS);
    private final JTable templateTable = new JTable(templateModel);
    private final ModernButton cancelBtn = UIHelper.createSecondaryButton("Anular");
    private final ModernButton editTemplateBtn = UIHelper.createSecondaryButton("Editar");
    private final JLabel detailTitle = new JLabel(" ");
    private final JLabel detailMeta = new JLabel(" ");
    private final JLabel detailTotals = new JLabel(" ");
    private final CardLayout cards = new CardLayout();
    private final JPanel detail = new JPanel(cards);
    private final List<CarcassBreakdownDTO> breakdowns = new ArrayList<>();
    private final List<BreakdownTemplateDTO> templates = new ArrayList<>();
    private Consumer<List<CarcassBreakdownDTO>> onLoaded = list -> { };

    public ButcheryPanel(ButcheryApiClient api, ComercialApiClient comercialApi, InventoryApiClient inventoryApi) {
        this.api = api;
        this.comercialApi = comercialApi;
        this.inventoryApi = inventoryApi;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        add(feedback, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildList(), buildDetail());
        split.setOpaque(false);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setResizeWeight(0.55);
        split.setDividerSize(10);
        ResponsiveSupport.adaptiveSplit(split, 980);
        tabs.addTab("Desmanchas", UIHelper.icon("fas-drumstick-bite", 15, UIHelper.REJECTED_RED), split);
        tabs.addTab("Modelos de Desmancha", UIHelper.icon("fas-sitemap", 15, UIHelper.ACCENT_BLUE), buildTemplates());
        add(tabs, BorderLayout.CENTER);
        cancelBtn.setEnabled(false);
        editTemplateBtn.setEnabled(false);
    }

    /** Receives every loaded list of breakdowns, so the module page can update its indicators. */
    public void onLoaded(Consumer<List<CarcassBreakdownDTO>> listener) {
        this.onLoaded = listener == null ? list -> { } : listener;
    }

    private JComponent buildList() {
        UIHelper.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.status());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showDetail();
        });
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);
        JTextField search = TableFilter.searchField("Carcaça ou número…");
        search.setPreferredSize(new Dimension(190, UIHelper.FORM_CONTROL_HEIGHT));
        TableFilter.install(table, search);
        JPanel filters = TableFilter.bar(search);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));

        ModernButton newBtn = UIHelper.createSuccessButton("Nova Desmancha");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.addActionListener(e -> openNewBreakdown());
        cancelBtn.setIcon(UIHelper.icon("fas-ban", 14));
        cancelBtn.setToolTipText("Repõe a carcaça e retira os cortes (só gestão; recusa se já houve venda)");
        cancelBtn.addActionListener(e -> cancelSelected());
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::reload);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.add(ResponsiveSupport.cardTop("Desmanchas (últimos 90 dias)", filters, refreshBtn, cancelBtn, newBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(table), BorderLayout.SOUTH);
        return card;
    }

    private JComponent buildDetail() {
        ModernPanel empty = new ModernPanel(16);
        empty.setLayout(new GridBagLayout());
        JLabel hint = new JLabel("<html><div style='text-align:center;width:320px'><b>Quanto custa cada kg</b><br>"
                + "Escolha uma desmancha para ver os cortes, o custo por kg de cada um, o rendimento comparado com o "
                + "modelo e a margem ao preço de venda actual.</div></html>",
                UIHelper.icon("fas-drumstick-bite", 36, UIHelper.REJECTED_RED), SwingConstants.CENTER);
        hint.setHorizontalTextPosition(SwingConstants.CENTER);
        hint.setVerticalTextPosition(SwingConstants.BOTTOM);
        hint.setForeground(UIHelper.TEXT_MUTED);
        empty.add(hint);

        UIHelper.styleTable(cutTable);
        cutTable.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.money());
        cutTable.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.money());
        JScrollPane scroll = new JScrollPane(cutTable);
        UIHelper.styleScrollPane(scroll);
        detailTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        detailTitle.setForeground(UIHelper.TEXT_LIGHT);
        detailMeta.setForeground(UIHelper.TEXT_MUTED);
        detailTotals.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        JPanel head = new JPanel(new GridLayout(2, 1, 0, 2));
        head.setOpaque(false);
        head.add(detailTitle);
        head.add(detailMeta);
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.add(head, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(detailTotals, BorderLayout.SOUTH);
        detail.setOpaque(false);
        detail.add(empty, "empty");
        detail.add(card, "detail");
        cards.show(detail, "empty");
        return detail;
    }

    private JComponent buildTemplates() {
        UIHelper.styleTable(templateTable);
        templateTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        templateTable.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.status());
        templateTable.getSelectionModel().addListSelectionListener(e -> editTemplateBtn.setEnabled(selectedTemplate() != null));
        JScrollPane scroll = new JScrollPane(templateTable);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);
        ModernButton newBtn = UIHelper.createSuccessButton("Novo Modelo");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.addActionListener(e -> openTemplate(null));
        editTemplateBtn.setIcon(UIHelper.icon("fas-edit", 14));
        editTemplateBtn.addActionListener(e -> openTemplate(selectedTemplate()));
        JLabel note = new JLabel("Cada modelo diz que cortes uma carcaça deve dar e quanto se aceita perder.");
        note.setForeground(UIHelper.TEXT_MUTED);
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.add(ResponsiveSupport.cardTop("Modelos de Desmancha", note, UIHelper.createRefreshButton(this::reload), editTemplateBtn, newBtn),
                BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    public void reload() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null || api == null) return;
        Long keep = selected() == null ? null : selected().id();
        UIHelper.loadAsync(this, () -> new Object[]{api.breakdowns(companyId, 90), api.templates(companyId)}, loaded -> {
            @SuppressWarnings("unchecked") List<CarcassBreakdownDTO> b = (List<CarcassBreakdownDTO>) loaded[0];
            @SuppressWarnings("unchecked") List<BreakdownTemplateDTO> t = (List<BreakdownTemplateDTO>) loaded[1];
            apply(b, t, keep);
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar o talho", error.getMessage(), "Tentar novamente", this::reload));
    }

    private void apply(List<CarcassBreakdownDTO> loaded, List<BreakdownTemplateDTO> loadedTemplates, Long keep) {
        feedback.clear();
        breakdowns.clear();
        breakdowns.addAll(loaded);
        model.setRowCount(0);
        for (CarcassBreakdownDTO b : breakdowns) {
            model.addRow(new Object[]{b.createdAt() == null ? "—" : UIHelper.formatDate(b.createdAt().toLocalDate()), b.number(),
                    b.sourceProductName(), kg(b.inputKg()), kg(b.outputKg()), pct(b.lossPct()) + (b.lossAboveTolerance() ? "  acima" : ""),
                    b.inputCost(), b.status() == CarcassBreakdownStatus.CANCELLED ? "ANULADA" : "REGISTADA"});
        }
        templates.clear();
        templates.addAll(loadedTemplates);
        templateModel.setRowCount(0);
        for (BreakdownTemplateDTO t : templates) {
            templateModel.addRow(new Object[]{t.name(), t.sourceProductName(), t.lines().size(), pct(t.expectedYieldTotal()),
                    pct(t.lossTolerancePct()), t.active() ? "ACTIVO" : "INACTIVO"});
        }
        if (keep != null) {
            for (int i = 0; i < breakdowns.size(); i++) {
                if (!breakdowns.get(i).id().equals(keep)) continue;
                int view = table.convertRowIndexToView(i);
                if (view >= 0) table.setRowSelectionInterval(view, view);
            }
        }
        showDetail();
        onLoaded.accept(List.copyOf(breakdowns));
    }

    private void showDetail() {
        CarcassBreakdownDTO b = selected();
        cancelBtn.setEnabled(b != null && b.status() == CarcassBreakdownStatus.POSTED);
        if (b == null) {
            cards.show(detail, "empty");
            return;
        }
        detailTitle.setText(b.number() + "  ·  " + b.sourceProductName());
        detailMeta.setText((b.templateName() == null ? "Sem modelo" : "Modelo " + b.templateName()) + "  ·  " + b.warehouseName()
                + (b.reference() == null ? "" : "  ·  " + b.reference()) + "  ·  por " + b.createdBy()
                + (b.cancellationReason() == null ? "" : "  ·  Anulada: " + b.cancellationReason()));
        cutModel.setRowCount(0);
        for (CarcassBreakdownLineDTO l : b.lines()) {
            cutModel.addRow(new Object[]{l.productName(), kg(l.kg()), pct(l.realYieldPct()),
                    l.expectedYieldPct() == null ? "—" : pct(l.expectedYieldPct()), l.costPerKg(), l.unitSalePrice(), margin(l)});
        }
        BigDecimal avgCost = b.outputKg().signum() == 0 ? BigDecimal.ZERO : b.inputCost().divide(b.outputKg(), 2, RoundingMode.HALF_UP);
        detailTotals.setText("Entrada " + kg(b.inputKg()) + " kg  ·  Cortes " + kg(b.outputKg()) + " kg  ·  Perda " + kg(b.lossKg())
                + " kg (" + pct(b.lossPct()) + (b.lossTolerancePct() == null ? "" : ", aceite " + pct(b.lossTolerancePct())) + ")"
                + "  ·  Custo médio " + UIHelper.formatMzn(avgCost) + "/kg vendável");
        detailTotals.setForeground(b.lossAboveTolerance() ? UIHelper.REJECTED_RED : UIHelper.APPROVED_GREEN);
        cards.show(detail, "detail");
    }

    private void openNewBreakdown() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        UIHelper.loadAsync(this, () -> new Object[]{comercialApi.getAllProducts(), inventoryApi.getSalesWarehousesByCompany(companyId)}, data -> {
            @SuppressWarnings("unchecked") List<ProductDTO> products = (List<ProductDTO>) data[0];
            @SuppressWarnings("unchecked") List<WarehouseDTO> warehouses = (List<WarehouseDTO>) data[1];
            if (warehouses.isEmpty()) {
                feedback.show(FeedbackType.WARNING, "Sem armazém", "Crie um armazém de vendas antes de registar desmanchas.", null, null);
                return;
            }
            BreakdownFormDialog.show(this, api, templates, products, warehouses, posted -> {
                ToastManager.success(this, "Desmancha " + posted.number() + " registada: perda de " + pct(posted.lossPct()) + ".");
                if (posted.lossAboveTolerance()) {
                    feedback.show(FeedbackType.WARNING, "Perda acima do aceite", "A desmancha " + posted.number() + " perdeu "
                            + pct(posted.lossPct()) + " (aceite " + pct(posted.lossTolerancePct()) + "). Confirme pesagens e cortes.", null, null);
                }
                reloadKeeping(companyId, posted.id());
            });
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar os artigos", error.getMessage(), null, null));
    }

    private void openTemplate(BreakdownTemplateDTO existing) {
        UIHelper.loadAsync(this, comercialApi::getAllProducts, products -> BreakdownTemplateDialog.show(this, api, products, existing, saved -> {
            ToastManager.success(this, "Modelo '" + saved.name() + "' guardado.");
            reload();
        }), error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar os artigos", error.getMessage(), null, null));
    }

    private void cancelSelected() {
        CarcassBreakdownDTO b = selected();
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (b == null || companyId == null) return;
        JTextField reason = new JTextField();
        UIHelper.styleTextField(reason);
        FormField reasonForm = new FormField("Motivo da anulação", reason, true, "Fica na auditoria");
        JLabel info = new JLabel("<html><div style='width:340px'>Os cortes saem do stock e a carcaça volta a entrar. "
                + "Se parte dos cortes já foi vendida, a anulação é recusada.</div></html>");
        info.setForeground(UIHelper.TEXT_MUTED);
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Anular Desmancha " + b.number(), "fas-ban",
                "Repor a carcaça e retirar os cortes", UIHelper.createDialogForm(1, new Object[]{"", info, "", reasonForm}))
                .setConfirmButton("Anular", "fas-ban");
        CarcassBreakdownDTO[] result = new CarcassBreakdownDTO[1];
        dialog.setOnSaveAsync(() -> {
            if (!reasonForm.validateRequired()) throw new IllegalArgumentException("Indique o motivo.");
            String text = reason.getText().trim();
            return () -> { result[0] = api.cancel(companyId, b.id(), text); return null; };
        });
        if (!dialog.showDialog() || result[0] == null) return;
        ToastManager.success(this, "Desmancha " + result[0].number() + " anulada: carcaça reposta.");
        reloadKeeping(companyId, result[0].id());
    }

    private void reloadKeeping(Long companyId, Long id) {
        UIHelper.loadAsync(this, () -> new Object[]{api.breakdowns(companyId, 90), api.templates(companyId)}, loaded -> {
            @SuppressWarnings("unchecked") List<CarcassBreakdownDTO> b = (List<CarcassBreakdownDTO>) loaded[0];
            @SuppressWarnings("unchecked") List<BreakdownTemplateDTO> t = (List<BreakdownTemplateDTO>) loaded[1];
            apply(b, t, id);
        }, error -> reload());
    }

    private CarcassBreakdownDTO selected() {
        int row = TableFilter.selectedModelRow(table);
        return row < 0 || row >= breakdowns.size() ? null : breakdowns.get(row);
    }

    private BreakdownTemplateDTO selectedTemplate() {
        int row = TableFilter.selectedModelRow(templateTable);
        return row < 0 || row >= templates.size() ? null : templates.get(row);
    }

    /** Margin of the cut at its current sale price; "—" for cuts with no price (bones, fat). */
    static String margin(CarcassBreakdownLineDTO line) {
        BigDecimal price = line.unitSalePrice();
        if (price == null || price.signum() == 0) return "—";
        return pct(price.subtract(line.costPerKg()).multiply(BigDecimal.valueOf(100)).divide(price, 1, RoundingMode.HALF_UP));
    }

    private static String kg(BigDecimal value) {
        return TableCellRenderers.format(value, 3, "");
    }

    private static String pct(BigDecimal value) {
        return value == null ? "—" : value.setScale(1, RoundingMode.HALF_UP).toPlainString().replace('.', ',') + "%";
    }

    private static DefaultTableModel readOnly(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }
}
