package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.ProductionApiClient;
import mz.multicore.erp.desktop.client.ProductionPlanApiClient;
import mz.multicore.erp.gui.BomFormDialog;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.manufacturing.dto.BillOfMaterialsDTO;
import mz.multicore.erp.modules.manufacturing.model.BomType;
import mz.multicore.erp.modules.productionplan.dto.*;
import mz.multicore.erp.modules.productionplan.model.ProductionPlanStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Plano de Produção (F4, {@code docs/PLANO_PRODUCAO_SPEC.md}). The baker opens the day, asks for the suggestion
 * (what was sold on the same weekday), adjusts the quantities, sees what flour and yeast are missing, records each
 * batch taken out of the oven and closes the day with the leftovers. Recipes are the ordinary bills of materials.
 */
public class ProductionPlanPanel extends JPanel {

    private static final String[] PLAN_COLUMNS = {"Produto", "Receita", "Sugerido", "Planeado", "Produzido", "Falta", "Sobra", "Custo/un."};
    private static final String[] NEED_COLUMNS = {"Matéria-prima", "Unid.", "Precisa", "Em stock", "Falta comprar"};
    private static final String[] RUN_COLUMNS = {"Hora", "Produto", "Quantidade", "Lote", "Validade", "Custo total", "Custo/un.", "Por"};
    private static final String[] RECIPE_COLUMNS = {"Código", "Receita", "Produto", "Rende", "Ingredientes", "Custo estimado"};

    private final ProductionPlanApiClient api;
    private final ProductionApiClient productionApi;
    private final ComercialApiClient comercialApi;
    private final InventoryApiClient inventoryApi;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final DateField date = new DateField(LocalDate.now());
    private final JComboBox<String> warehouse = new JComboBox<>();
    private final JLabel status = new JLabel(" ");
    private final DefaultTableModel planModel = new DefaultTableModel(PLAN_COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return c == 3 && open(); }
    };
    private final JTable planTable = new JTable(planModel);
    private final DefaultTableModel needModel = readOnly(NEED_COLUMNS);
    private final DefaultTableModel runModel = readOnly(RUN_COLUMNS);
    private final DefaultTableModel recipeModel = readOnly(RECIPE_COLUMNS);
    private final ModernButton saveBtn = UIHelper.createSuccessButton("Guardar Plano");
    private final ModernButton runBtn = UIHelper.createPrimaryButton("Apontar Produção");
    private final ModernButton closeBtn = UIHelper.createSecondaryButton("Fechar o Dia");
    private final ModernButton suggestBtn = UIHelper.createSecondaryButton("Sugerir pelas Vendas");
    private final ModernButton addBtn = UIHelper.createSecondaryButton("Acrescentar Receita");
    private final List<WarehouseDTO> warehouses = new ArrayList<>();
    private final List<BillOfMaterialsDTO> recipes = new ArrayList<>();
    private final List<Long> rowBoms = new ArrayList<>();
    private final Map<Long, BigDecimal> suggested = new HashMap<>();
    private ProductionPlanDTO plan;
    private Consumer<ProductionPlanDTO> onLoaded = p -> { };

    public ProductionPlanPanel(ProductionPlanApiClient api, ProductionApiClient productionApi, ComercialApiClient comercialApi,
                               InventoryApiClient inventoryApi) {
        this.api = api;
        this.productionApi = productionApi;
        this.comercialApi = comercialApi;
        this.inventoryApi = inventoryApi;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.setOpaque(false);
        north.add(feedback, BorderLayout.NORTH);
        north.add(buildDayBar(), BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);
        tabs.addTab("Plano do Dia", UIHelper.icon("fas-clipboard-list", 15, UIHelper.ACCENT_ORANGE), buildPlan());
        tabs.addTab("Matéria-Prima Necessária", UIHelper.icon("fas-dolly", 15, UIHelper.ACCENT_BLUE), card("Para o que falta produzir hoje",
                table(needModel, new int[]{}, new int[]{2, 3, 4}, 4), null));
        tabs.addTab("Lotes Produzidos", UIHelper.icon("fas-bread-slice", 15, UIHelper.APPROVED_GREEN), card("Apontamentos do dia",
                table(runModel, new int[]{5, 6}, new int[]{2}, -1), null));
        tabs.addTab("Receitas", UIHelper.icon("fas-book-open", 15, UIHelper.ACCENT_PINK), buildRecipes());
        add(tabs, BorderLayout.CENTER);
        updateActions();
    }

    public void onLoaded(Consumer<ProductionPlanDTO> listener) {
        this.onLoaded = listener == null ? p -> { } : listener;
    }

    private JComponent buildDayBar() {
        UIHelper.styleComboBox(warehouse);
        warehouse.setPreferredSize(new Dimension(200, UIHelper.FORM_CONTROL_HEIGHT));
        date.setPreferredSize(new Dimension(130, UIHelper.FORM_CONTROL_HEIGHT));
        ModernButton prev = UIHelper.createSecondaryButton("");
        prev.setIcon(UIHelper.icon("fas-chevron-left", 13));
        prev.setToolTipText("Dia anterior");
        prev.addActionListener(e -> shiftDay(-1));
        ModernButton next = UIHelper.createSecondaryButton("");
        next.setIcon(UIHelper.icon("fas-chevron-right", 13));
        next.setToolTipText("Dia seguinte");
        next.addActionListener(e -> shiftDay(1));
        ModernButton today = UIHelper.createSecondaryButton("Hoje");
        today.addActionListener(e -> {
            date.setDate(LocalDate.now());
            loadDay();
        });
        ModernButton open = UIHelper.createRefreshButton(this::loadDay);
        warehouse.addActionListener(e -> {
            if (warehouse.isPopupVisible() || warehouse.hasFocus()) loadDay();
        });
        status.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.add(TableFilter.label("Dia:"));
        bar.add(prev);
        bar.add(date);
        bar.add(next);
        bar.add(today);
        bar.add(TableFilter.label("Armazém:"));
        bar.add(warehouse);
        bar.add(open);
        bar.add(status);
        return bar;
    }

    private JComponent buildPlan() {
        UIHelper.styleTable(planTable);
        planTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        planTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        planTable.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.money());
        planTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateActions();
        });
        JScrollPane scroll = new JScrollPane(planTable);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);
        suggestBtn.setIcon(UIHelper.icon("fas-magic", 14));
        suggestBtn.setToolTipText("Média vendida no mesmo dia da semana nas últimas 4 semanas");
        suggestBtn.addActionListener(e -> suggest());
        addBtn.setIcon(UIHelper.icon("fas-plus", 14));
        addBtn.addActionListener(e -> addRecipe());
        saveBtn.setIcon(UIHelper.icon("fas-save", 14));
        saveBtn.addActionListener(e -> savePlan(null));
        runBtn.setIcon(UIHelper.icon("fas-fire", 14));
        runBtn.setToolTipText("Regista um lote tirado do forno: consome ingredientes e dá entrada com validade");
        runBtn.addActionListener(e -> recordRun());
        closeBtn.setIcon(UIHelper.icon("fas-lock", 14));
        closeBtn.setToolTipText("Regista as sobras (abater ou vender amanhã) e fecha o dia");
        closeBtn.addActionListener(e -> closeDay());
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(suggestBtn);
        left.add(addBtn);
        return card("Plano do Dia", scroll, new JComponent[]{left, closeBtn, runBtn, saveBtn});
    }

    private JComponent buildRecipes() {
        ModernButton newBtn = UIHelper.createSuccessButton("Nova Receita");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.addActionListener(e -> UIHelper.loadAsync(this, comercialApi::getAllProducts, products ->
                BomFormDialog.show(SwingUtilities.getWindowAncestor(this), products, productionApi, feedback, this::loadDay),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar os artigos", error.getMessage(), null, null)));
        return card("Receitas (fichas técnicas)", table(recipeModel, new int[]{5}, new int[]{3}, -1),
                new JComponent[]{UIHelper.createRefreshButton(this::loadDay), newBtn});
    }

    public void reload() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null || api == null) return;
        if (!warehouses.isEmpty()) {
            loadDay();
            return;
        }
        UIHelper.loadAsync(this, () -> inventoryApi.getSalesWarehousesByCompany(companyId), loaded -> {
            warehouses.clear();
            warehouses.addAll(loaded);
            warehouse.removeAllItems();
            loaded.forEach(w -> warehouse.addItem(w.name()));
            if (loaded.isEmpty()) {
                feedback.show(FeedbackType.WARNING, "Sem armazém", "Crie um armazém para planear a produção.", null, null);
                return;
            }
            loadDay();
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar os armazéns", error.getMessage(), "Tentar novamente", this::reload));
    }

    private void shiftDay(int days) {
        LocalDate current = date.getDate() == null ? LocalDate.now() : date.getDate();
        date.setDate(current.plusDays(days));
        loadDay();
    }

    private void loadDay() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        WarehouseDTO wh = selectedWarehouse();
        if (companyId == null || wh == null) return;
        LocalDate day = date.getDate() == null ? LocalDate.now() : date.getDate();
        UIHelper.loadAsync(this, () -> {
            ProductionPlanDTO loaded = api.day(companyId, wh.id(), day);
            List<MaterialRequirementDTO> needs = loaded.id() == null ? List.of() : api.requirements(companyId, loaded.id());
            List<BillOfMaterialsDTO> boms = productionApi.listBoms();
            return new Object[]{loaded, needs, boms};
        }, data -> {
            @SuppressWarnings("unchecked") List<MaterialRequirementDTO> needs = (List<MaterialRequirementDTO>) data[1];
            @SuppressWarnings("unchecked") List<BillOfMaterialsDTO> boms = (List<BillOfMaterialsDTO>) data[2];
            apply((ProductionPlanDTO) data[0], needs, boms);
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar o plano", error.getMessage(), "Tentar novamente", this::loadDay));
    }

    private void apply(ProductionPlanDTO loaded, List<MaterialRequirementDTO> needs, List<BillOfMaterialsDTO> boms) {
        feedback.clear();
        plan = loaded;
        recipes.clear();
        boms.stream().filter(b -> b.active() && (b.bomType() == null || b.bomType() == BomType.STANDARD)).forEach(recipes::add);
        if (!loaded.planDate().equals(date.getDate())) suggested.clear();
        planModel.setRowCount(0);
        rowBoms.clear();
        for (ProductionPlanLineDTO l : loaded.lines()) {
            rowBoms.add(l.bomId());
            planModel.addRow(new Object[]{l.productName(), l.bomName(), qty(suggested.get(l.bomId())), plain(l.plannedQty()),
                    qty(l.producedQty()), qty(l.remainingQty()), qty(l.leftoverQty()), l.lastUnitCost()});
        }
        needModel.setRowCount(0);
        for (MaterialRequirementDTO n : needs) {
            needModel.addRow(new Object[]{n.productName(), n.unit() == null ? "" : n.unit(), qty(n.required()),
                    n.inStock() == null ? "não controla" : qty(n.inStock()), n.shortage().signum() > 0 ? qty(n.shortage()) : "—"});
        }
        runModel.setRowCount(0);
        for (ProductionRunDTO r : loaded.runs()) {
            runModel.addRow(new Object[]{r.createdAt() == null ? "" : r.createdAt().toLocalTime().withNano(0).toString(), r.productName(),
                    qty(r.quantity()), r.batchNumber(), r.expirationDate() == null ? "—" : UIHelper.formatDate(r.expirationDate()),
                    r.totalCost(), r.unitCost(), r.createdBy()});
        }
        recipeModel.setRowCount(0);
        for (BillOfMaterialsDTO b : recipes) {
            recipeModel.addRow(new Object[]{b.bomCode(), b.name(), b.finishedProductName(), qty(b.baseQuantity()), b.items().size(),
                    b.totalEstimatedCost()});
        }
        boolean closed = loaded.status() == ProductionPlanStatus.CLOSED;
        status.setText(loaded.id() == null ? "Sem plano para este dia" : closed ? "Dia fechado por " + loaded.closedBy() : "Dia em produção");
        status.setForeground(closed ? UIHelper.TEXT_MUTED : loaded.id() == null ? UIHelper.PENDING_YELLOW : UIHelper.APPROVED_GREEN);
        updateActions();
        onLoaded.accept(loaded);
    }

    private void updateActions() {
        boolean open = open();
        boolean saved = plan != null && plan.id() != null;
        int row = TableFilter.selectedModelRow(planTable);
        saveBtn.setEnabled(open);
        suggestBtn.setEnabled(open);
        addBtn.setEnabled(open);
        runBtn.setEnabled(open && saved && row >= 0 && row < plan.lines().size());
        closeBtn.setEnabled(open && saved);
    }

    private boolean open() {
        return plan == null || plan.status() != ProductionPlanStatus.CLOSED;
    }

    private void suggest() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        LocalDate day = date.getDate() == null ? LocalDate.now() : date.getDate();
        UIHelper.loadAsync(this, () -> api.suggestions(companyId, day), list -> {
            suggested.clear();
            list.forEach(s -> suggested.put(s.bomId(), s.suggestedQty()));
            Set<Long> present = new HashSet<>(rowBoms);
            for (ProductionSuggestionDTO s : list) {
                if (present.contains(s.bomId()) || s.suggestedQty().signum() == 0) continue;
                BillOfMaterialsDTO bom = recipes.stream().filter(b -> b.id().equals(s.bomId())).findFirst().orElse(null);
                if (bom == null) continue;
                rowBoms.add(bom.id());
                planModel.addRow(new Object[]{bom.finishedProductName(), bom.name(), qty(s.suggestedQty()), plain(s.suggestedQty()),
                        "0", plain(s.suggestedQty()), "0", null});
            }
            for (int r = 0; r < rowBoms.size(); r++) {
                BigDecimal value = suggested.get(rowBoms.get(r));
                planModel.setValueAt(qty(value), r, 2);
                if (value != null && BreakdownFormDialog.parse(planModel.getValueAt(r, 3)).signum() == 0) planModel.setValueAt(plain(value), r, 3);
            }
            ToastManager.success(this, "Sugestão pelas vendas do mesmo dia da semana. Ajuste e guarde o plano.");
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível calcular a sugestão", error.getMessage(), null, null));
    }

    private void addRecipe() {
        List<BillOfMaterialsDTO> available = recipes.stream().filter(b -> !rowBoms.contains(b.id())).toList();
        if (available.isEmpty()) {
            ToastManager.show(this, FeedbackType.INFO, "Todas as receitas activas já estão no plano. Crie mais no separador Receitas.");
            return;
        }
        JComboBox<String> recipe = new JComboBox<>(available.stream().map(b -> b.name() + "  ·  " + b.finishedProductName()).toArray(String[]::new));
        UIHelper.styleComboBox(recipe);
        DecimalField quantity = new DecimalField("", 3, true);
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Acrescentar ao Plano", "fas-plus", "Receita e quantidade a produzir",
                UIHelper.createDialogForm(1, new Object[]{"Receita:", recipe, "Quantidade a produzir:", quantity}));
        BigDecimal[] chosen = new BigDecimal[1];
        dialog.setOnSaveAsync(() -> {
            BigDecimal value = quantity.value();
            if (value == null || value.signum() <= 0) throw new IllegalArgumentException("Indique a quantidade a produzir.");
            chosen[0] = value;
            return () -> null;
        });
        if (!dialog.showDialog() || chosen[0] == null) return;
        BillOfMaterialsDTO bom = available.get(recipe.getSelectedIndex());
        rowBoms.add(bom.id());
        planModel.addRow(new Object[]{bom.finishedProductName(), bom.name(), qty(suggested.get(bom.id())), plain(chosen[0]),
                "0", plain(chosen[0]), "0", null});
        savePlan("Receita acrescentada ao plano.");
    }

    private void savePlan(String message) {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        WarehouseDTO wh = selectedWarehouse();
        if (companyId == null || wh == null) return;
        if (planTable.isEditing()) planTable.getCellEditor().stopCellEditing();
        List<PlanLineRequest> lines = new ArrayList<>();
        for (int r = 0; r < rowBoms.size(); r++) {
            lines.add(new PlanLineRequest(rowBoms.get(r), BreakdownFormDialog.parse(planModel.getValueAt(r, 3)).max(BigDecimal.ZERO)));
        }
        SaveProductionPlanRequest request = new SaveProductionPlanRequest(companyId, wh.id(),
                date.getDate() == null ? LocalDate.now() : date.getDate(), plan == null ? null : plan.notes(), lines);
        UIHelper.runWithProgress(this, "A guardar o plano…", () -> api.save(request), saved -> {
            ToastManager.success(this, message == null ? "Plano do dia guardado." : message);
            loadDay();
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível guardar o plano", error.getMessage(), null, null));
    }

    private void recordRun() {
        int row = TableFilter.selectedModelRow(planTable);
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (plan == null || companyId == null || row < 0 || row >= plan.lines().size()) return;
        ProductionPlanLineDTO line = plan.lines().get(row);
        DecimalField quantity = new DecimalField(plain(line.remainingQty().signum() > 0 ? line.remainingQty() : null), 3, true);
        DateField expiry = new DateField(plan.planDate().plusDays(1));
        JLabel info = new JLabel("<html><div style='width:340px'><b>" + line.productName() + "</b> · receita " + line.bomName()
                + "<br>Planeado " + qty(line.plannedQty()) + " · já produzido " + qty(line.producedQty()) + "<br><br>Os ingredientes saem "
                + "do stock pelo lote mais antigo e o produto entra com o lote do dia, a validade e o custo real.</div></html>");
        info.setForeground(UIHelper.TEXT_LIGHT);
        FormField qtyForm = new FormField("Quantidade tirada do forno", quantity, true, null);
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Apontar Produção", "fas-fire", "Lote produzido",
                UIHelper.createDialogForm(1, new Object[]{"", info, "", qtyForm, "Validade do lote:", expiry}))
                .setConfirmButton("Registar Lote", "fas-check");
        String key = "PD-" + UUID.randomUUID();
        ProductionRunDTO[] result = new ProductionRunDTO[1];
        dialog.setOnSaveAsync(() -> {
            BigDecimal value = quantity.value();
            if (value == null || value.signum() <= 0) {
                qtyForm.setError("Indique a quantidade produzida.");
                throw new IllegalArgumentException("Indique a quantidade produzida.");
            }
            RecordRunRequest request = new RecordRunRequest(companyId, line.id(), value, expiry.getDate(), key);
            return () -> { result[0] = api.recordRun(request); return null; };
        });
        if (!dialog.showDialog() || result[0] == null) return;
        ToastManager.success(this, "Lote " + result[0].batchNumber() + ": " + qty(result[0].quantity()) + " de " + result[0].productName()
                + " a " + UIHelper.formatMzn(result[0].unitCost()) + " cada.");
        loadDay();
    }

    private void closeDay() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (plan == null || plan.id() == null || companyId == null) return;
        DefaultTableModel leftovers = new DefaultTableModel(new String[]{"Produto", "Produzido", "Sobra", "Abater"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 2 || c == 3; }
            @Override public Class<?> getColumnClass(int c) { return c == 3 ? Boolean.class : Object.class; }
        };
        for (ProductionPlanLineDTO l : plan.lines()) leftovers.addRow(new Object[]{l.productName(), qty(l.producedQty()), "0", Boolean.FALSE});
        JTable table = new JTable(leftovers);
        UIHelper.styleTable(table);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(480, 200));
        JLabel info = new JLabel("<html><div style='width:460px'>Indique o que sobrou de cada produto. Marque <b>Abater</b> para o "
                + "que vai para o lixo (quebra por validade); o resto fica em stock para vender amanhã, com a remarcação das "
                + "Validades. Depois de fechado, o dia já não aceita lotes.</div></html>");
        info.setForeground(UIHelper.TEXT_MUTED);
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Fechar o Dia " + UIHelper.formatDate(plan.planDate()), "fas-lock",
                "Sobras e fecho da produção", UIHelper.createDialogForm(1, new Object[]{"", info, "Sobras:", scroll}))
                .setConfirmButton("Fechar o Dia", "fas-lock");
        ProductionPlanDTO[] result = new ProductionPlanDTO[1];
        dialog.setOnSaveAsync(() -> {
            if (table.isEditing()) table.getCellEditor().stopCellEditing();
            List<LeftoverRequest> list = new ArrayList<>();
            for (int r = 0; r < leftovers.getRowCount(); r++) {
                BigDecimal value = BreakdownFormDialog.parse(leftovers.getValueAt(r, 2));
                if (value.signum() > 0) list.add(new LeftoverRequest(plan.lines().get(r).id(), value, Boolean.TRUE.equals(leftovers.getValueAt(r, 3))));
            }
            ClosePlanRequest request = new ClosePlanRequest(companyId, list);
            return () -> { result[0] = api.close(plan.id(), request); return null; };
        });
        if (!dialog.showDialog() || result[0] == null) return;
        ToastManager.success(this, "Dia de produção fechado.");
        loadDay();
    }

    private WarehouseDTO selectedWarehouse() {
        int index = warehouse.getSelectedIndex();
        return index < 0 || index >= warehouses.size() ? null : warehouses.get(index);
    }

    private JComponent card(String title, JComponent body, JComponent[] actions) {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        JPanel filters = new JPanel();
        filters.setOpaque(false);
        card.add(UIHelper.tableCardTop(title, filters, actions == null ? new JComponent[0] : actions), BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private static JScrollPane table(DefaultTableModel model, int[] moneyColumns, int[] qtyColumns, int alertColumn) {
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        for (int c : moneyColumns) table.getColumnModel().getColumn(c).setCellRenderer(TableCellRenderers.money());
        if (alertColumn >= 0) {
            table.getColumnModel().getColumn(alertColumn).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                    Component cell = super.getTableCellRendererComponent(t, v, s, f, r, c);
                    if (!s) cell.setForeground(v != null && !"—".equals(v.toString()) ? UIHelper.REJECTED_RED : UIHelper.APPROVED_GREEN);
                    return cell;
                }
            });
        }
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);
        return scroll;
    }

    private static String qty(BigDecimal value) {
        return value == null ? "—" : TableCellRenderers.format(value, 3, "");
    }

    private static String plain(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private static DefaultTableModel readOnly(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }
}
