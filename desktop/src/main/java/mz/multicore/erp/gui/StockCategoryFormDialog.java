package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.CreateProductCategoryRequest;
import mz.multicore.erp.modules.comercial.dto.ProductCategoryDTO;

import javax.swing.*;
import java.awt.*;

/**
 * Diálogo modal para criação e edição de categorias de produtos de stock.
 */
public final class StockCategoryFormDialog {

    private StockCategoryFormDialog() {}

    public static void open(StockPanel owner, ProductCategoryDTO existing, Runnable onDone) {
        boolean editing = existing != null;
        JTextField codeField = new JTextField(editing ? existing.code() : "");
        JTextField nameField = new JTextField(editing ? existing.name() : "");

        final String[] colorHolder = { editing && existing.colorHex() != null ? existing.colorHex() : "" };
        JLabel swatch = new JLabel();
        swatch.setOpaque(true);
        swatch.setPreferredSize(new Dimension(40, UIHelper.FORM_CONTROL_HEIGHT));
        swatch.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER, 1, true));
        Runnable applySwatch = () -> {
            Color c = null;
            try { if (colorHolder[0].startsWith("#")) c = Color.decode(colorHolder[0]); } catch (NumberFormatException ignored) { }
            swatch.setBackground(c != null ? c : UIHelper.BG_CARD);
            swatch.setText(c == null ? "  sem cor" : "");
            swatch.setForeground(UIHelper.TEXT_MUTED);
        };
        applySwatch.run();

        ModernButton pickBtn = UIHelper.createSecondaryButton("Escolher Cor");
        pickBtn.setIcon(UIHelper.icon("fas-palette", 14));
        pickBtn.addActionListener(ev -> {
            Color initial = UIHelper.ACCENT_BLUE;
            try { if (colorHolder[0].startsWith("#")) initial = Color.decode(colorHolder[0]); } catch (NumberFormatException ignored) { }
            Color chosen = JColorChooser.showDialog(owner, "Cor da categoria", initial);
            if (chosen != null) {
                colorHolder[0] = String.format("#%02X%02X%02X", chosen.getRed(), chosen.getGreen(), chosen.getBlue());
                applySwatch.run();
            }
        });

        ModernButton clearBtn = UIHelper.createSecondaryButton("Limpar");
        clearBtn.setIcon(UIHelper.icon("fas-times", 14));
        clearBtn.addActionListener(ev -> { colorHolder[0] = ""; applySwatch.run(); });

        JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        colorPanel.setOpaque(false);
        colorPanel.add(swatch);
        colorPanel.add(pickBtn);
        colorPanel.add(clearBtn);

        JPanel form = UIHelper.createDialogForm(
                "Código:", codeField,
                "Nome:", nameField,
                "Cor (opcional):", colorPanel);

        Window parent = SwingUtilities.getWindowAncestor(owner);
        ModernFormDialog dlg = new ModernFormDialog(parent,
                editing ? "Editar Categoria" : "Nova Categoria", "fas-tags",
                "Organize os produtos em categorias da loja", form);
        dlg.setSize(480, 360);
        dlg.setOnSaveAsync(() -> {
            String code = codeField.getText().trim();
            String name = nameField.getText().trim();
            if (code.isEmpty() || name.isEmpty()) {
                throw new RuntimeException("Código e nome são obrigatórios.");
            }
            var req = new CreateProductCategoryRequest(
                    code, name, colorHolder[0].isBlank() ? null : colorHolder[0]);
            return () -> editing ? owner.productCategoryApiClient.update(existing.id(), req)
                    : owner.productCategoryApiClient.create(req);
        });
        if (dlg.showDialog()) {
            onDone.run();
        }
    }
}
