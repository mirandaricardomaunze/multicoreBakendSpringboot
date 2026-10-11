package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.PromotionApiClient;
import mz.multicore.erp.modules.comercial.dto.ProductCategoryDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.promotions.dto.CreatePromotionRequest;
import mz.multicore.erp.modules.promotions.dto.PromotionDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestão de promoções de loja: lista, criação (percentagem ou "leve X, pague Y", por produto ou
 * categoria, com janela de validade) e activação/desactivação. UI fina — a lógica vive no backend,
 * acedida via {@link PromotionApiClient}.
 */
public class PromotionsPanel extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PromotionApiClient promotionApiClient;
    private final ComercialApiClient comercialApiClient;

    private final DefaultTableModel model;
    private final JTable table;
    private final ModernButton toggleBtn;
    private JComboBox<String> promoBusinessUnitCombo;
    private List<PromotionDTO> promotions = new ArrayList<>();
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    public PromotionsPanel(PromotionApiClient promotionApiClient, ComercialApiClient comercialApiClient) {
        this.promotionApiClient = promotionApiClient;
        this.comercialApiClient = comercialApiClient;

        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(15, 5, 5, 5));

        ModernButton newBtn = UIHelper.createSuccessButton("Nova Promoção");
        newBtn.setIcon(UIHelper.icon("fas-tags", 14));
        newBtn.addActionListener(e -> createPromotionDialog());
        toggleBtn = UIHelper.createSecondaryButton("Activar / Desactivar");
        toggleBtn.setIcon(UIHelper.icon("fas-power-off", 14));
        toggleBtn.addActionListener(e -> toggleSelected());
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> reload());

        add(feedback, BorderLayout.NORTH);

        String[] cols = {"Nome", "Tipo", "Alcance", "Benefício", "Início", "Fim", "Estado", "Negócio"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.status());
        table.getColumnModel().getColumn(7).setPreferredWidth(120);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        JTextField promoSearch = TableFilter.searchField("Nome, alcance ou benefício…");
        promoBusinessUnitCombo = TableFilter.combo("Todos os negócios");
        JComboBox<String> promoTipo = TableFilter.combo("Todos os tipos", "Percentagem", "Leve X, pague Y");
        UIHelper.styleComboBox(promoTipo);
        promoTipo.setPreferredSize(new Dimension(180, UIHelper.FORM_CONTROL_HEIGHT));

        JComboBox<String> promoEstado = TableFilter.combo("Todos os estados", "Activa", "Inactiva");
        UIHelper.styleComboBox(promoEstado);
        promoEstado.setPreferredSize(new Dimension(160, UIHelper.FORM_CONTROL_HEIGHT));

        TableFilter.install(table, promoSearch,
                new TableFilter.ColumnFilter(promoTipo, 1),
                new TableFilter.ColumnFilter(promoEstado, 6),
                new TableFilter.ColumnFilter(promoBusinessUnitCombo, 7));

        ModernButton peekToggleBtn = UIHelper.createSecondaryButton("");
        peekToggleBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekToggleBtn.setToolTipText("Espreitar detalhes da promoção (Espaço)");
        peekToggleBtn.getAccessibleContext().setAccessibleName("Espreitar promoção");
        peekToggleBtn.setPreferredSize(new Dimension(30, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel promoFilters = TableFilter.bar(
                promoSearch,
                TableFilter.label("Negócio:"), promoBusinessUnitCombo,
                TableFilter.label("Tipo:"), promoTipo,
                TableFilter.label("Estado:"), promoEstado,
                peekToggleBtn
        );
        promoFilters.setBorder(new EmptyBorder(0, 0, 10, 0));

        UIHelper.ensureHeadersFit(table);
        TableQuickPeekController peekController = TableQuickPeekController.install(table, card, (peek, mr) -> {
            if (mr < 0 || mr >= model.getRowCount()) return;
            String name = String.valueOf(model.getValueAt(mr, 0));
            String tipo = String.valueOf(model.getValueAt(mr, 1));
            String scope = String.valueOf(model.getValueAt(mr, 2));
            String benefit = String.valueOf(model.getValueAt(mr, 3));
            String start = String.valueOf(model.getValueAt(mr, 4));
            String end = String.valueOf(model.getValueAt(mr, 5));
            String status = String.valueOf(model.getValueAt(mr, 6));

            peek.setTitle(name);
            peek.setSubtitle(tipo + " • " + scope);
            String bu = mr < model.getRowCount() ? String.valueOf(model.getValueAt(mr, 7)) : "—";
            peek.setItems(java.util.List.of(
                    new QuickPeekPanel.PeekItem("Benefício", benefit, true),
                    new QuickPeekPanel.PeekItem("Data Início", start, false),
                    new QuickPeekPanel.PeekItem("Data Fim", end, false),
                    new QuickPeekPanel.PeekItem("Estado", status, false),
                    new QuickPeekPanel.PeekItem("Linha de Negócio", bu, false)
            ));
            peek.getOpenFullButton().setText("Activar / Desactivar");
            peek.setOnOpenFullAction(v -> toggleBtn.doClick());
        });
        peekToggleBtn.addActionListener(e -> peekController.toggle());

        card.add(UIHelper.tableCardTop("Promoções de Loja", promoFilters,
                refreshBtn, toggleBtn, newBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(table), BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    public void reload() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (companyId == null) {
            return;
        }
        UIHelper.loadAsync(this, () -> promotionApiClient.findByCompany(companyId), this::applyPromotions,
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar as promoções",
                        error.getMessage(), "Tentar novamente", this::reload));
    }

    private void applyPromotions(List<PromotionDTO> loaded) {
        promotions = loaded;
        model.setRowCount(0);
        java.util.Set<String> bus = new java.util.TreeSet<>();
        for (PromotionDTO p : promotions) {
            String buName = p.businessUnitName() != null && !p.businessUnitName().isBlank() ? p.businessUnitName() : "—";
            if (!"—".equals(buName)) bus.add(buName);
            model.addRow(new Object[]{
                    p.name(),
                    "PERCENT".equals(p.type()) ? "Percentagem" : "Leve X, pague Y",
                    p.productName() != null ? "Produto: " + p.productName()
                            : p.categoryName() != null ? "Categoria: " + p.categoryName() : "—",
                    benefit(p),
                    p.startDate() != null ? p.startDate().format(FMT) : "—",
                    p.endDate() != null ? p.endDate().format(FMT) : "—",
                    p.active() ? "ACTIVA" : "INACTIVA",
                    buName
            });
        }
        if (promoBusinessUnitCombo != null) {
            Object prev = promoBusinessUnitCombo.getSelectedItem();
            promoBusinessUnitCombo.removeAllItems();
            promoBusinessUnitCombo.addItem("Todos os negócios");
            for (String b : bus) promoBusinessUnitCombo.addItem(b);
            if (prev != null) promoBusinessUnitCombo.setSelectedItem(prev);
        }
    }

    private String benefit(PromotionDTO p) {
        if ("PERCENT".equals(p.type())) {
            return p.percentValue() == null ? "—" : "-" + p.percentValue().stripTrailingZeros().toPlainString() + "%";
        }
        return "Leve " + p.buyQuantity() + ", pague " + p.payQuantity();
    }

    private void toggleSelected() {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0 || row >= promotions.size()) {
            feedback.show(FeedbackType.WARNING, "Seleccione uma promoção",
                    "Escolha uma promoção na tabela para continuar.", null, null);
            return;
        }
        PromotionDTO selected = promotions.get(row);
        UIHelper.submitAsync(toggleBtn, () -> {
            promotionApiClient.setActive(selected.id(), !selected.active());
            return null;
        }, ignored -> reload(), error -> feedback.show(FeedbackType.ERROR,
                "Não foi possível actualizar a promoção", error.getMessage(), null, null));
    }

    private void createPromotionDialog() {
        UIHelper.loadAsync(this,
                () -> new PromotionOptions(comercialApiClient.getAllProducts(),
                        comercialApiClient.getActiveCategories()),
                this::openPromotionDialog,
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar produtos e categorias",
                        error.getMessage(), "Tentar novamente", this::createPromotionDialog));
    }

    private void openPromotionDialog(PromotionOptions options) {
        List<ProductDTO> products = options.products();
        List<ProductCategoryDTO> categories = options.categories();

        JTextField nameField = new JTextField();
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Percentagem", "Leve X, pague Y"});
        JComboBox<String> scopeCombo = new JComboBox<>(new String[]{"Produto", "Categoria"});
        ProductSearchComboBox productCombo = new ProductSearchComboBox();
        JComboBox<String> categoryCombo = new JComboBox<>();
        productCombo.setProducts(products);
        for (ProductCategoryDTO c : categories) categoryCombo.addItem(c.name());

        MoneyField percentField = new MoneyField();
        QuantityField buyField = new QuantityField("", true);
        QuantityField payField = new QuantityField("", true);
        DateField startField = new DateField(LocalDate.now());
        DateField endField = new DateField(LocalDate.now().plusMonths(1));

        UIHelper.styleTextField(nameField);
        UIHelper.styleComboBox(typeCombo);
        UIHelper.styleComboBox(scopeCombo);
        UIHelper.styleComboBox(categoryCombo);

        // "Leve X, pague Y" só faz sentido por produto → força e bloqueia o alcance.
        Runnable syncEnabled = () -> {
            boolean percent = typeCombo.getSelectedIndex() == 0;
            if (!percent) {
                scopeCombo.setSelectedIndex(0); // Produto
            }
            scopeCombo.setEnabled(percent);
            boolean byProduct = scopeCombo.getSelectedIndex() == 0;
            productCombo.setEnabled(byProduct);
            categoryCombo.setEnabled(!byProduct);
            percentField.setEnabled(percent);
            buyField.setEnabled(!percent);
            payField.setEnabled(!percent);
        };
        typeCombo.addActionListener(e -> syncEnabled.run());
        scopeCombo.addActionListener(e -> syncEnabled.run());
        syncEnabled.run();

        FormField nameForm = new FormField("Nome", nameField, true, null);
        JPanel form = UIHelper.createDialogForm(
                "", nameForm,
                "Tipo:", typeCombo,
                "Alcance:", scopeCombo,
                "Produto:", productCombo,
                "Categoria:", categoryCombo,
                "Desconto (%):", percentField,
                "Leve (X):", buyField,
                "Pague (Y):", payField,
                "Início (yyyy-MM-dd):", startField,
                "Fim (yyyy-MM-dd):", endField
        );

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Nova Promoção",
                "fas-percent", "Campanha de desconto na loja", form);
        dialog.setOnSaveAsync(() -> {
            if (!nameForm.validateRequired()) throw new IllegalArgumentException("Indique o nome da promoção.");
            boolean percent = typeCombo.getSelectedIndex() == 0;
            boolean byProduct = scopeCombo.getSelectedIndex() == 0;
            ProductDTO selectedProduct = productCombo.selectedProduct();
            int categoryIndex = categoryCombo.getSelectedIndex();
            if (byProduct && selectedProduct == null)
                throw new IllegalArgumentException("Selecione um produto.");
            if (!byProduct && (categoryIndex < 0 || categoryIndex >= categories.size()))
                throw new IllegalArgumentException("Selecione uma categoria.");
            BigDecimal percentValue = percent ? percentField.value() : null;
            Integer buy = percent ? null : buyField.value().intValueExact();
            Integer pay = percent ? null : payField.value().intValueExact();
            LocalDate start = startField.value();
            LocalDate end = endField.value();
            CreatePromotionRequest request = new CreatePromotionRequest(
                    CurrentUserContext.getCurrentCompanyId(), nameField.getText().trim(),
                    percent ? "PERCENT" : "BUY_X_GET_Y",
                    byProduct ? selectedProduct.id() : null,
                    byProduct ? null : categories.get(categoryIndex).id(),
                    percentValue, buy, pay, start, end);
            return () -> { promotionApiClient.createPromotion(request); return null; };
        });
        if (dialog.showDialog()) {
            reload();
            ToastManager.success(this, "Promoção criada.");
        }
    }

    private record PromotionOptions(List<ProductDTO> products, List<ProductCategoryDTO> categories) {}
    private JLabel filterLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(UIHelper.TEXT_MUTED);
        return label;
    }
}
