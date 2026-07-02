package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernFormDialog;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.comercial.dto.ProductCategoryDTO;
import com.phcpro.modules.comercial.dto.ProductDTO;
import com.phcpro.modules.comercial.service.ComercialService;
import com.phcpro.modules.promotions.dto.CreatePromotionRequest;
import com.phcpro.modules.promotions.dto.PromotionDTO;
import com.phcpro.modules.promotions.service.PromotionService;

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
 * categoria, com janela de validade) e activação/desactivação. UI fina — a lógica vive no
 * {@link PromotionService}.
 */
public class PromotionsPanel extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PromotionService promotionService;
    private final ComercialService comercialService;

    private final DefaultTableModel model;
    private final JTable table;
    private List<PromotionDTO> promotions = new ArrayList<>();

    public PromotionsPanel(PromotionService promotionService, ComercialService comercialService) {
        this.promotionService = promotionService;
        this.comercialService = comercialService;

        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Promoções de Loja"), BorderLayout.WEST);

        ModernButton newBtn = UIHelper.createSuccessButton("Nova Promoção");
        newBtn.setIcon(UIHelper.icon("fas-tags", 14));
        newBtn.addActionListener(e -> createPromotionDialog());
        ModernButton toggleBtn = UIHelper.createSecondaryButton("Activar / Desactivar");
        toggleBtn.setIcon(UIHelper.icon("fas-power-off", 14));
        toggleBtn.addActionListener(e -> toggleSelected());
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Atualizar");
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

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        reload();
    }

    private void reload() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        try {
            promotions = promotionService.findByCompany(companyId);
        } catch (Exception ex) {
            promotions = new ArrayList<>();
        }
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
        int row = table.getSelectedRow();
        if (row < 0 || row >= promotions.size()) {
            JOptionPane.showMessageDialog(this, "Selecione uma promoção primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        PromotionDTO selected = promotions.get(row);
        try {
            promotionService.setActive(selected.id(), !selected.active());
            reload();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createPromotionDialog() {
        List<ProductDTO> products = comercialService.getAllProducts();
        List<ProductCategoryDTO> categories = comercialService.getActiveCategories();

        JTextField nameField = new JTextField();
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Percentagem", "Leve X, pague Y"});
        JComboBox<String> scopeCombo = new JComboBox<>(new String[]{"Produto", "Categoria"});
        JComboBox<String> productCombo = new JComboBox<>();
        JComboBox<String> categoryCombo = new JComboBox<>();
        for (ProductDTO p : products) productCombo.addItem(p.sku() + " — " + p.name());
        for (ProductCategoryDTO c : categories) categoryCombo.addItem(c.name());

        JTextField percentField = new JTextField();
        JTextField buyField = new JTextField();
        JTextField payField = new JTextField();
        JTextField startField = new JTextField(LocalDate.now().toString());
        JTextField endField = new JTextField(LocalDate.now().plusMonths(1).toString());

        UIHelper.styleTextField(nameField);
        UIHelper.styleComboBox(typeCombo);
        UIHelper.styleComboBox(scopeCombo);
        UIHelper.styleComboBox(productCombo);
        UIHelper.styleComboBox(categoryCombo);
        UIHelper.styleTextField(percentField);
        UIHelper.styleTextField(buyField);
        UIHelper.styleTextField(payField);
        UIHelper.styleTextField(startField);
        UIHelper.styleTextField(endField);
        startField.putClientProperty("JTextField.placeholderText", "yyyy-MM-dd");
        endField.putClientProperty("JTextField.placeholderText", "yyyy-MM-dd");

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

        JPanel form = UIHelper.createDialogForm(
                "Nome:", nameField,
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

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Nova Promoção", "fas-percent", "Campanha de desconto na loja", form).showDialog();
        if (!confirmed) return;

        boolean percent = typeCombo.getSelectedIndex() == 0;
        boolean byProduct = scopeCombo.getSelectedIndex() == 0;

        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "O nome é obrigatório.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Long productId = null;
        Long categoryId = null;
        if (byProduct) {
            int idx = productCombo.getSelectedIndex();
            if (idx < 0 || idx >= products.size()) {
                JOptionPane.showMessageDialog(this, "Selecione um produto.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            productId = products.get(idx).id();
        } else {
            int idx = categoryCombo.getSelectedIndex();
            if (idx < 0 || idx >= categories.size()) {
                JOptionPane.showMessageDialog(this, "Selecione uma categoria.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            categoryId = categories.get(idx).id();
        }

        BigDecimal percentValue = null;
        Integer buy = null;
        Integer pay = null;
        try {
            if (percent) {
                percentValue = new BigDecimal(percentField.getText().trim().replace(",", "."));
            } else {
                buy = Integer.parseInt(buyField.getText().trim());
                pay = Integer.parseInt(payField.getText().trim());
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Valores numéricos inválidos.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LocalDate start;
        LocalDate end;
        try {
            start = LocalDate.parse(startField.getText().trim());
            end = LocalDate.parse(endField.getText().trim());
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Datas inválidas. Use o formato yyyy-MM-dd.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            promotionService.createPromotion(new CreatePromotionRequest(
                    CurrentUserContext.getCurrentCompanyId(),
                    name,
                    percent ? "PERCENT" : "BUY_X_GET_Y",
                    productId,
                    categoryId,
                    percentValue,
                    buy,
                    pay,
                    start,
                    end
            ));
            reload();
            JOptionPane.showMessageDialog(this, "Promoção '" + name + "' criada.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
