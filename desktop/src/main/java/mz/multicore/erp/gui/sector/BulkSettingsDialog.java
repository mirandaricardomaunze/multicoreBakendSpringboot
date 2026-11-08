package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.BulkStockApiClient;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.bulkstock.dto.BulkStockSettingDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Marks a product as bulk stock with its natural-loss tolerance, or removes it (empty tolerance). Shows the
 * products already measured in bulk so the manager sees the whole picture before changing one.
 */
final class BulkSettingsDialog {

    private BulkSettingsDialog() {
    }

    static void show(JComponent parent, BulkStockApiClient api, ComercialApiClient comercialApi,
                     InlineFeedbackPanel feedback, Runnable onSaved) {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        UIHelper.loadAsync(parent, () -> new Loaded(comercialApi.getAllProducts(), api.settings(companyId)),
                loaded -> open(parent, api, companyId, loaded, onSaved),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar os produtos", error.getMessage(),
                        "Tentar novamente", () -> show(parent, api, comercialApi, feedback, onSaved)));
    }

    private static void open(JComponent parent, BulkStockApiClient api, Long companyId, Loaded loaded, Runnable onSaved) {
        ProductSearchComboBox productCombo = new ProductSearchComboBox();
        productCombo.setProducts(loaded.products());
        DecimalField tolerance = new DecimalField("5", 2, false);
        productCombo.addActionListener(e -> {
            ProductDTO product = productCombo.selectedProduct();
            if (product == null) return;
            loaded.settings().stream().filter(s -> s.productId().equals(product.id())).findFirst()
                    .ifPresent(s -> tolerance.setText(ProductUnitsPanel.plain(s.tolerancePercent())));
        });

        DefaultTableModel model = new DefaultTableModel(new String[]{"Produto", "Unid.", "Tolerância"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (BulkStockSettingDTO s : loaded.settings()) {
            model.addRow(new Object[]{s.productName(), PickupsPanel.unit(s.baseUnitCode()), ProductUnitsPanel.plain(s.tolerancePercent()) + " %"});
        }
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);
        scroll.setPreferredSize(new Dimension(520, 160));
        ModernPanel card = new ModernPanel(14);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(10, 10, 10, 10));
        card.add(ResponsiveSupport.cardTop("Produtos já a granel", null), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        JLabel hint = new JLabel("<html>A tolerância é a perda natural aceite sem aprovação (vento, chuva, compactação). "
                + "Acima dela, o acerto espera por um gestor. Deixe vazio para o produto deixar de ser granel.</html>");
        hint.setForeground(UIHelper.TEXT_MUTED);
        FormField productForm = new FormField("Produto", productCombo, true, null);
        JPanel form = UIHelper.createDialogForm(1, new Object[]{"", productForm, "Tolerância de perda (%):", tolerance, "", hint});
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.add(form, BorderLayout.NORTH);
        content.add(card, BorderLayout.CENTER);

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Produtos a Granel", "fas-mountain",
                "Produtos medidos no monte e perda natural aceite", content);
        boolean[] saved = {false};
        dialog.setOnSaveAsync(() -> {
            ProductDTO product = productCombo.selectedProduct();
            if (product == null) {
                productForm.setError("Escolha o produto.");
                throw new IllegalArgumentException("Escolha o produto.");
            }
            BigDecimal value = tolerance.getText() == null || tolerance.getText().isBlank() ? null : tolerance.value();
            return () -> { api.saveSetting(companyId, product.id(), value); saved[0] = true; return null; };
        });
        if (dialog.showDialog() && saved[0]) {
            onSaved.run();
            ToastManager.success(parent, "Produtos a granel actualizados.");
        }
    }

    private record Loaded(List<ProductDTO> products, List<BulkStockSettingDTO> settings) {}
}
