package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.components.CollapsibleSidebar;
import mz.multicore.erp.gui.components.SidebarNavItem;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.sector.*;
import mz.multicore.erp.modules.activity.dto.CompanyActivityDTO;

import javax.swing.*;
import java.awt.Color;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Business-sector modules as first-class screens (docs/SECTORES_ACTIVIDADE_SPEC.md): Preços, Levantamentos and
 * Obras, each with its own sidebar entry, page header and indicators. The company activity profile decides which
 * of them the menu shows (SA-C01); without a profile, or if it cannot be read, every module stays visible.
 * {@link MainFrame} only creates this registry and delegates cards, titles, selection and sidebar items to it.
 */
final class SectorModules {

    static final String PRICES = "precos";
    static final String PICKUPS = "levantamentos";
    static final String PROJECTS = "obras";
    static final String ADVANCES = "adiantamentos";
    static final String BUTCHERY = "talho";
    static final String EXPIRY = "validades";
    static final String PRODUCTION_PLAN = "plano-producao";
    static final String CUSTOM_ORDERS = "agenda-encomendas";

    private static final Map<String, String> TITLES = Map.of(PRICES, "Preços", PICKUPS, "Levantamentos", PROJECTS, "Obras",
            ADVANCES, "Adiantamentos", BUTCHERY, "Talho", EXPIRY, "Validades", PRODUCTION_PLAN, "Plano de Produção",
            CUSTOM_ORDERS, "Agenda de Encomendas");
    private static final Map<String, String> CAPABILITIES = Map.of(PRICES, "PRICE_LISTS", PICKUPS, "PENDING_PICKUP",
            PROJECTS, "CUSTOMER_PROJECTS", ADVANCES, "ADVANCE_PAYMENTS", BUTCHERY, "CARCASS_BREAKDOWN", EXPIRY, "PERISHABLES",
            PRODUCTION_PLAN, "PRODUCTION", CUSTOM_ORDERS, "CUSTOM_ORDERS");

    private record Module(String card, String icon, Color color, boolean sellerVisible, JComponent page, Runnable onSelected) {}

    private final Map<String, Module> modules = new LinkedHashMap<>();
    private final Map<String, List<SidebarNavItem>> sidebarItems = new HashMap<>();
    private final ActivityApiClient activityApi;

    private SectorModules(ActivityApiClient activityApi) {
        this.activityApi = activityApi;
    }

    /** Builds the modules whose clients exist and wires the add-ons that live inside Commercial and Stock. */
    static SectorModules create(ComercialPanel comercial, StockPanel stock, ConfigPanel config, ComercialApiClient comercialApi,
                                InventoryApiClient inventoryApi, PromotionApiClient promotionApi, ActivityApiClient activityApi,
                                UnitsApiClient unitsApi, PricingApiClient pricingApi, PickupApiClient pickupApi,
                                BulkStockApiClient bulkApi, ProjectsApiClient projectsApi, FinanceApiClient financeApi,
                                ProductionApiClient productionApi, SectorApiClients sector) {
        AdvancesApiClient advancesApi = sector == null ? null : sector.advances();
        ButcheryApiClient butcheryApi = sector == null ? null : sector.butchery();
        ExpiryApiClient expiryApi = sector == null ? null : sector.expiry();
        ProductionPlanApiClient planApi = sector == null ? null : sector.productionPlan();
        CustomOrdersApiClient customOrdersApi = sector == null ? null : sector.customOrders();
        SectorModules registry = new SectorModules(activityApi);
        if (pricingApi != null) registry.addPrices(pricingApi, comercialApi, promotionApi);
        if (pickupApi != null) registry.addPickups(pickupApi);
        if (projectsApi != null) registry.addProjects(projectsApi, comercialApi);
        if (advancesApi != null) registry.addAdvances(advancesApi, comercialApi, financeApi, inventoryApi);
        if (butcheryApi != null) registry.addButchery(butcheryApi, comercialApi, inventoryApi);
        if (expiryApi != null) registry.addExpiry(expiryApi, comercialApi);
        if (planApi != null) registry.addProductionPlan(planApi, productionApi, comercialApi, inventoryApi);
        if (customOrdersApi != null) registry.addCustomOrders(customOrdersApi, comercialApi);
        if (comercial != null) {
            if (pricingApi != null) comercial.putClientProperty(OrderEditorExtras.PRICING_KEY, pricingApi);
            SectorDesktopWiring.attachProjects(comercial, projectsApi);
            SectorDesktopWiring.installUnitQuantityEntry(comercial, unitsApi);
            SectorDesktopWiring.installFreightEntry(comercial, comercialApi);
            SectorDesktopWiring.installDraftToolbarExtras(comercial);
        }
        if (stock != null) {
            SectorDesktopWiring.attachUnits(stock, unitsApi);
            SectorDesktopWiring.installBulkStock(stock, bulkApi, inventoryApi, comercialApi);
        }
        ActivityProfilePanel profilePanel = find(config, ActivityProfilePanel.class);
        if (profilePanel != null) profilePanel.onSaved(registry::applyCapabilities);
        return registry;
    }

    // ─── Modules ────────────────────────────────────────────────────────────────

    private void addPrices(PricingApiClient pricingApi, ComercialApiClient comercialApi, PromotionApiClient promotionApi) {
        PriceListsPanel lists = new PriceListsPanel(pricingApi, comercialApi);
        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);
        tabs.addTab("Tabelas de Preço", UIHelper.icon("fas-tags", 15, UIHelper.ACCENT_BLUE), lists);
        PromotionsPanel promotions = promotionApi == null ? null : new PromotionsPanel(promotionApi, comercialApi);
        if (promotions != null) tabs.addTab("Promoções de Loja", UIHelper.icon("fas-percentage", 15, UIHelper.ACCENT_PINK), promotions);
        ModulePage page = new ModulePage("fas-tags", UIHelper.ACCENT_PINK, "Preços",
                "Tabelas de preço por tipo de cliente (empreiteiro, revendedor, funcionário) e promoções de loja.",
                new String[][]{{"Tabelas activas", "Em vigor hoje", "fas-check-circle", "green"},
                        {"Clientes com tabela", "Pagam o preço da tabela", "fas-users", "blue"},
                        {"Preços definidos", "Linhas em todas as tabelas", "fas-list-ol", null}}, tabs);
        lists.onLoaded(all -> {
            page.setIndicator("Tabelas activas", String.valueOf(all.stream().filter(l -> l.active()).count()));
            page.setIndicator("Clientes com tabela", String.valueOf(all.stream().mapToLong(l -> l.clientCount()).sum()));
            page.setIndicator("Preços definidos", String.valueOf(all.stream().mapToLong(l -> l.itemCount()).sum()));
        });
        modules.put(PRICES, new Module(PRICES, "fas-tags", UIHelper.ACCENT_PINK, false, page, () -> {
            lists.reload();
            if (promotions != null) promotions.reload();
        }));
    }

    private void addPickups(PickupApiClient pickupApi) {
        PickupsPanel panel = new PickupsPanel(pickupApi);
        ModulePage page = new ModulePage("fas-people-carry", UIHelper.ACCENT_ORANGE, "Levantamentos",
                "Material pago que o cliente levanta aos poucos. Cada entrega gera um talão LV para o portão.",
                new String[][]{{"Faturas por levantar", "Clientes com saldo", "fas-file-invoice", "blue"},
                        {"Prazo vencido", "Precisam de decisão", "fas-exclamation-triangle", "red"},
                        {"Artigos por entregar", "Linhas com saldo", "fas-boxes", "amber"},
                        {"Concluídas", "Tudo levantado", "fas-check-double", "green"}}, panel);
        panel.onLoaded(all -> {
            page.setIndicator("Faturas por levantar", String.valueOf(all.stream().filter(a -> "OPEN".equals(a.status())).count()));
            page.setIndicator("Prazo vencido", String.valueOf(all.stream().filter(a -> a.overdue()).count()));
            page.setIndicator("Artigos por entregar", String.valueOf(all.stream().filter(a -> "OPEN".equals(a.status()))
                    .mapToInt(a -> a.pendingLineCount()).sum()));
            page.setIndicator("Concluídas", String.valueOf(all.stream().filter(a -> "COMPLETED".equals(a.status())).count()));
        });
        modules.put(PICKUPS, new Module(PICKUPS, "fas-people-carry", UIHelper.ACCENT_ORANGE, true, page, panel::reload));
    }

    private void addProjects(ProjectsApiClient projectsApi, ComercialApiClient comercialApi) {
        ProjectsPanel panel = new ProjectsPanel(projectsApi, comercialApi);
        ModulePage page = new ModulePage("fas-hard-hat", UIHelper.ACCENT_ORANGE, "Obras",
                "Obras e projectos dos clientes: encomendas, faturas e dívida de cada obra num só extracto.",
                new String[][]{{"Obras activas", "Aceitam encomendas", "fas-hard-hat", "blue"},
                        {"Encomendado", "Em todas as obras", "fas-clipboard-list", null},
                        {"Em dívida", "Faturado e não pago", "fas-hand-holding-usd", "red"}}, panel);
        panel.onLoaded(all -> {
            page.setIndicator("Obras activas", String.valueOf(all.stream().filter(p -> p.active()).count()));
            page.setIndicator("Encomendado", UIHelper.formatMzn(all.stream().map(p -> p.orderedTotal()).reduce(BigDecimal.ZERO, BigDecimal::add)));
            page.setIndicator("Em dívida", UIHelper.formatMzn(all.stream().map(p -> p.outstanding()).reduce(BigDecimal.ZERO, BigDecimal::add)));
        });
        modules.put(PROJECTS, new Module(PROJECTS, "fas-hard-hat", UIHelper.ACCENT_ORANGE, true, page, panel::reload));
    }

    private void addAdvances(AdvancesApiClient advancesApi, ComercialApiClient comercialApi, FinanceApiClient financeApi,
                             InventoryApiClient inventoryApi) {
        AdvancesPanel panel = new AdvancesPanel(advancesApi, comercialApi, financeApi, inventoryApi);
        ModulePage page = new ModulePage("fas-hand-holding-usd", UIHelper.APPROVED_GREEN, "Adiantamentos",
                "Sinais e adiantamentos de clientes: fatura com IVA no recebimento e desconto na fatura final, sem cobrar duas vezes.",
                new String[][]{{"Em aberto", "Sinais por aplicar", "fas-hourglass-half", "amber"},
                        {"Valor em aberto", "Recebido e por descontar", "fas-coins", "blue"},
                        {"Aplicados no mês", "Descontados em faturas", "fas-check-double", "green"}}, panel);
        panel.onLoaded(all -> {
            var open = all.stream().filter(a -> a.status() == mz.multicore.erp.modules.advances.model.CustomerAdvanceStatus.OPEN).toList();
            java.time.LocalDate monthStart = java.time.LocalDate.now().withDayOfMonth(1);
            page.setIndicator("Em aberto", String.valueOf(open.size()));
            page.setIndicator("Valor em aberto", UIHelper.formatMzn(open.stream().map(a -> a.amount()).reduce(BigDecimal.ZERO, BigDecimal::add)));
            page.setIndicator("Aplicados no mês", String.valueOf(all.stream().filter(a -> a.appliedAt() != null
                    && !a.appliedAt().toLocalDate().isBefore(monthStart)).count()));
        });
        modules.put(ADVANCES, new Module(ADVANCES, "fas-hand-holding-usd", UIHelper.APPROVED_GREEN, false, page, panel::reload));
    }

    private void addButchery(ButcheryApiClient butcheryApi, ComercialApiClient comercialApi, InventoryApiClient inventoryApi) {
        ButcheryPanel panel = new ButcheryPanel(butcheryApi, comercialApi, inventoryApi);
        ModulePage page = new ModulePage("fas-drumstick-bite", UIHelper.REJECTED_RED, "Talho",
                "Desmancha de carcaças: cortes por peso, custo real de cada kg, rendimento contra o modelo e perda.",
                new String[][]{{"Desmanchas no mês", "Carcaças cortadas", "fas-drumstick-bite", "blue"},
                        {"Kg processados", "Entrada no mês", "fas-weight-hanging", null},
                        {"Perda média", "Peso não aproveitado", "fas-percentage", "amber"},
                        {"Acima do aceite", "Rever pesagens", "fas-exclamation-triangle", "red"}}, panel);
        panel.onLoaded(all -> {
            java.time.LocalDate monthStart = java.time.LocalDate.now().withDayOfMonth(1);
            var month = all.stream().filter(b -> b.status() == mz.multicore.erp.modules.butchery.model.CarcassBreakdownStatus.POSTED
                    && b.createdAt() != null && !b.createdAt().toLocalDate().isBefore(monthStart)).toList();
            BigDecimal input = month.stream().map(b -> b.inputKg()).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal loss = month.stream().map(b -> b.lossKg()).reduce(BigDecimal.ZERO, BigDecimal::add);
            page.setIndicator("Desmanchas no mês", String.valueOf(month.size()));
            page.setIndicator("Kg processados", mz.multicore.erp.architecture.format.DisplayNumbers.qty(input) + " kg");
            page.setIndicator("Perda média", input.signum() == 0 ? "—" : loss.multiply(BigDecimal.valueOf(100))
                    .divide(input, 1, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',') + "%");
            page.setIndicator("Acima do aceite", String.valueOf(month.stream().filter(b -> b.lossAboveTolerance()).count()));
        });
        modules.put(BUTCHERY, new Module(BUTCHERY, "fas-drumstick-bite", UIHelper.REJECTED_RED, true, page, panel::reload));
    }

    private void addExpiry(ExpiryApiClient expiryApi, ComercialApiClient comercialApi) {
        ExpiryPanel panel = new ExpiryPanel(expiryApi, comercialApi);
        ModulePage page = new ModulePage("fas-hourglass-half", UIHelper.PENDING_YELLOW, "Validades",
                "Lotes vencidos e por vencer, remarcação automática no POS e abate do que venceu, sem perder de vista o valor.",
                new String[][]{{"Vencido em stock", "Valor a custo a abater", "fas-skull-crossbones", "red"},
                        {"Em risco (7 dias)", "Valor a custo a vender já", "fas-hourglass-half", "amber"},
                        {"Lotes remarcados", "Com desconto hoje", "fas-tag", "blue"},
                        {"Perdas no mês", "Abatido por validade", "fas-chart-line", null}}, panel);
        panel.onLoaded(board -> {
            page.setIndicator("Vencido em stock", UIHelper.formatMzn(board.expiredCostValue()));
            page.setIndicator("Em risco (7 dias)", UIHelper.formatMzn(board.atRiskCostValue()));
            page.setIndicator("Lotes remarcados", String.valueOf(board.markedDownLots()));
            page.setIndicator("Perdas no mês", UIHelper.formatMzn(board.expiryLossesThisMonth()));
        });
        modules.put(EXPIRY, new Module(EXPIRY, "fas-hourglass-half", UIHelper.PENDING_YELLOW, true, page, panel::reload));
    }

    private void addProductionPlan(ProductionPlanApiClient planApi, ProductionApiClient productionApi, ComercialApiClient comercialApi,
                                   InventoryApiClient inventoryApi) {
        ProductionPlanPanel panel = new ProductionPlanPanel(planApi, productionApi, comercialApi, inventoryApi);
        ModulePage page = new ModulePage("fas-bread-slice", UIHelper.ACCENT_ORANGE, "Plano de Produção",
                "Plano do dia pelas vendas, matéria-prima em falta, lotes tirados do forno com custo real e fecho com sobras.",
                new String[][]{{"Receitas no plano", "Produtos a fazer hoje", "fas-clipboard-list", "blue"},
                        {"Produzido", "Unidades tiradas do forno", "fas-bread-slice", "green"},
                        {"Por produzir", "Ainda falta fazer", "fas-hourglass-half", "amber"},
                        {"Custo produzido", "Ingredientes e mão-de-obra", "fas-coins", null}}, panel);
        panel.onLoaded(plan -> {
            BigDecimal produced = plan.lines().stream().map(l -> l.producedQty()).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remaining = plan.lines().stream().map(l -> l.remainingQty()).reduce(BigDecimal.ZERO, BigDecimal::add);
            page.setIndicator("Receitas no plano", String.valueOf(plan.lines().size()));
            page.setIndicator("Produzido", mz.multicore.erp.architecture.format.DisplayNumbers.qty(produced));
            page.setIndicator("Por produzir", mz.multicore.erp.architecture.format.DisplayNumbers.qty(remaining));
            page.setIndicator("Custo produzido", UIHelper.formatMzn(plan.producedCost()));
        });
        modules.put(PRODUCTION_PLAN, new Module(PRODUCTION_PLAN, "fas-bread-slice", UIHelper.ACCENT_ORANGE, true, page, panel::reload));
    }

    private void addCustomOrders(CustomOrdersApiClient customOrdersApi, ComercialApiClient comercialApi) {
        CustomOrdersPanel panel = new CustomOrdersPanel(customOrdersApi, comercialApi);
        ModulePage page = new ModulePage("fas-birthday-cake", UIHelper.ACCENT_PINK, "Agenda de Encomendas",
                "Bolos, salgados e carne por encomenda: dia e hora de entrega, personalização com fotografia e produção do dia.",
                new String[][]{{"Encomendas", "No período escolhido", "fas-calendar-alt", "blue"},
                        {"Para hoje", "Entregas de hoje", "fas-clock", "amber"},
                        {"Prontas", "À espera do cliente", "fas-check-circle", "green"},
                        {"Por receber", "Total menos sinais", "fas-hand-holding-usd", null}}, panel);
        panel.onLoaded(all -> {
            java.time.LocalDate today = java.time.LocalDate.now();
            page.setIndicator("Encomendas", String.valueOf(all.size()));
            page.setIndicator("Para hoje", String.valueOf(all.stream().filter(o -> today.equals(o.deliveryDate())).count()));
            page.setIndicator("Prontas", String.valueOf(all.stream().filter(o -> o.stage() == mz.multicore.erp.modules.customorders.model.CustomOrderStage.READY).count()));
            page.setIndicator("Por receber", UIHelper.formatMzn(all.stream().map(o -> (o.total() == null ? BigDecimal.ZERO : o.total())
                    .subtract(o.advancePaid()).max(BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add)));
        });
        modules.put(CUSTOM_ORDERS, new Module(CUSTOM_ORDERS, "fas-birthday-cake", UIHelper.ACCENT_PINK, true, page, panel::reload));
    }

    // ─── MainFrame delegation ───────────────────────────────────────────────────

    void registerCards(BiConsumer<JComponent, String> addCard) {
        modules.values().forEach(m -> addCard.accept(m.page(), m.card()));
    }

    /** Runs the module refresh; returns {@code false} for cards that are not sector modules. */
    boolean onSelected(String card) {
        Module module = modules.get(card);
        if (module == null) return false;
        module.onSelected().run();
        return true;
    }

    static String title(String card) {
        return TITLES.getOrDefault(card, card);
    }

    /** Adds the sidebar entry of one module, if it exists and the user may see it. */
    void addSidebarItem(CollapsibleSidebar sidebar, String card, boolean seller, Consumer<String> navigate) {
        Module module = modules.get(card);
        if (module == null || (seller && !module.sellerVisible())) return;
        SidebarNavItem item = sidebar.addItem(UIHelper.icon(module.icon(), 16, module.color()), title(card), module.color(),
                () -> navigate.accept(card));
        sidebarItems.computeIfAbsent(card, k -> new ArrayList<>()).add(item);
    }

    /** Reads the company profile once the sidebar exists and hides the modules it does not use. */
    void applyProfileAsync(JComponent scope) {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (activityApi == null || companyId == null) return;
        UIHelper.loadAsync(scope, () -> activityApi.company(companyId),
                (CompanyActivityDTO activity) -> applyCapabilities(activity.capabilities()), error -> { });
    }

    void applyCapabilities(List<String> capabilities) {
        Set<String> active = capabilities == null ? Set.of() : new HashSet<>(capabilities);
        sidebarItems.forEach((card, items) -> {
            boolean visible = active.isEmpty() || active.contains(CAPABILITIES.get(card));
            items.forEach(item -> item.setVisible(visible));
        });
        sidebarItems.values().stream().flatMap(List::stream).findFirst()
                .map(SwingUtilities::getWindowAncestor).ifPresent(w -> { w.revalidate(); w.repaint(); });
    }

    Set<String> cards() {
        return Collections.unmodifiableSet(modules.keySet());
    }

    private static <T> T find(java.awt.Container root, Class<T> type) {
        if (root == null) return null;
        for (java.awt.Component child : root.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof java.awt.Container c) {
                T found = find(c, type);
                if (found != null) return found;
            }
        }
        return null;
    }
}
