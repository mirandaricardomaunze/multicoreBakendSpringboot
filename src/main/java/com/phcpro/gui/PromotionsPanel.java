package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernFormDialog;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.TableFilter;
import com.phcpro.gui.components.TableCellRenderers;
import com.phcpro.gui.components.DateField;
import com.phcpro.gui.components.FormField;
import com.phcpro.gui.components.MoneyField;
import com.phcpro.gui.components.QuantityField;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.desktop.client.ComercialApiClient;
import com.phcpro.desktop.client.PromotionApiClient;
import com.phcpro.modules.comercial.dto.ProductCategoryDTO;
import com.phcpro.modules.comercial.dto.ProductDTO;
import com.phcpro.modules.promotions.dto.CreatePromotionRequest;
import com.phcpro.modules.promotions.dto.PromotionDTO;

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
    private List<PromotionDTO> promotions = new ArrayList<>();

    public PromotionsPanel(PromotionApiClient promotionApiClient, ComercialApiClient comercialApiClient) {
        this.promotionApiClient = promotionApiClient;
        this.comercialApiClient = comercialApiClient;

        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Promoções de Loja"), BorderLayout.WEST);

        ModernButton newBtn = UIHelper.createSuccessButton("Nova Promoção");
        newBtn.setIcon(UIHelper.icon("fas-tags", 14));
        newBtn.addActionListener(e -> createPromotionDialog());
        toggleBtn = UIHelper.createSecondaryButton("Activar / Desactivar");
        toggleBtn.setIcon(UIHelper.icon("fas-power-off", 14));
        toggleBtn.addActionListener(e -> toggleSelected());
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> reload());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(refreshBtn);
        actions.add(toggleBtn);
        actions.add(newBtn);
        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        String[] cols = {"Nome", "Tipo", "Alcance", "Benefício", "Início", "Fim", "Estado"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.status());

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        JTextField promoSearch = TableFilter.searchField("Nome, alcance ou benefício…");
        JComboBox<String> promoTipo = TableFilter.combo("Todos os tipos", "Percentagem", "Leve X, pague Y");
        JComboBox<String> promoEstado = TableFilter.combo("Todos os estados", "ACTIVA", "INACTIVA");
        TableFilter.install(table, promoSearch,
                new TableFilter.ColumnFilter(promoTipo, 1),
                new TableFilter.ColumnFilter(promoEstado, 6));
        JPanel promoBar = TableFilter.bar(promoSearch,
                TableFilter.label("Tipo:"), promoTipo,
                TableFilter.label("Estado:"), promoEstado);
        promoBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(promoBar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        reload();
    }

    private void reload() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> promotionApiClient.findByCompany(companyId), this::applyPromotions,
                error -> JOptionPane.showMessageDialog(this,
                        "Não foi possível carregar as promoções: " + error.getMessage(),
                        "Erro de ligação", JOptionPane.ERROR_MESSAGE));
    }

    private void applyPromotions(List<PromotionDTO> loaded) {
        promotions = loaded;
        model.setRowCount(0);
        for (PromotionDTO p : promotions) {
            model.addRow(new Object[]{
                    p.name(),
                    "PERCENT".equals(p.type()) ? "Percentagem" : "Leve X, pague Y",
                    p.productName() != null ? "Produto: " + p.productName()
                            : p.categoryName() != null ? "Categoria: " + p.categoryName() : "—",
                    benefit(p),
                    p.startDate() != null ? p.startDate().format(FMT) : "—",
                    p.endDate() != null ? p.endDate().format(FMT) : "—",
                    p.active() ? "ACTIVA" : "INACTIVA"
            });
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
            JOptionPane.showMessageDialog(this, "Selecione uma promoção primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        PromotionDTO selected = promotions.get(row);
        UIHelper.submitAsync(toggleBtn, () -> {
            promotionApiClient.setActive(selected.id(), !selected.active());
            return null;
        }, ignored -> reload(), error -> JOptionPane.showMessageDialog(this,
                error.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE));
    }

    private void createPromotionDialog() {
        UIHelper.loadAsync(this,
                () -> new PromotionOptions(comercialApiClient.getAllProducts(),
                        comercialApiClient.getActiveCategories()),
                this::openPromotionDialog,
                error -> JOptionPane.showMessageDialog(this,
                        "Não foi possível carregar produtos e categorias: " + error.getMessage(),
                        "Erro de ligação", JOptionPane.ERROR_MESSAGE));
    }

    private void openPromotionDialog(PromotionOptions options) {
        List<ProductDTO> products = options.products();
        List<ProductCategoryDTO> categories = options.categories();

        JTextField nameField = new JTextField();
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Percentagem", "Leve X, pague Y"});
        JComboBox<String> scopeCombo = new JComboBox<>(new String[]{"Produto", "Categoria"});
        JComboBox<String> productCombo = new JComboBox<>();
        JComboBox<String> categoryCombo = new JComboBox<>();
        for (ProductDTO p : products) productCombo.addItem(p.sku() + " — " + p.name());
        for (ProductCategoryDTO c : categories) categoryCombo.addItem(c.name());

        MoneyField percentField = new MoneyField();
        QuantityField buyField = new QuantityField("", true);
        QuantityField payField = new QuantityField("", true);
        DateField startField = new DateField(LocalDate.now());
        DateField endField = new DateField(LocalDate.now().plusMonths(1));

        UIHelper.styleTextField(nameField);
        UIHelper.styleComboBox(typeCombo);
        UIHelper.styleComboBox(scopeCombo);
        UIHelper.styleComboBox(productCombo);
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
            int productIndex = productCombo.getSelectedIndex();
            int categoryIndex = categoryCombo.getSelectedIndex();
            if (byProduct && (productIndex < 0 || productIndex >= products.size()))
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
                    byProduct ? products.get(productIndex).id() : null,
                    byProduct ? null : categories.get(categoryIndex).id(),
                    percentValue, buy, pay, start, end);
            return () -> { promotionApiClient.createPromotion(request); return null; };
        });
        if (dialog.showDialog()) {
            reload();
            JOptionPane.showMessageDialog(this, "Promoção criada.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private record PromotionOptions(List<ProductDTO> products, List<ProductCategoryDTO> categories) {}
}
