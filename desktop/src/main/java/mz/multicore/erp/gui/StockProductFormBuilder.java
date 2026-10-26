package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.quantity.PackagingComposition;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;

/**
 * Construtor de componentes visuais, seletores de imagem e ligação de campos
 * dos formulários de produtos de stock.
 */
final class StockProductFormBuilder {

    private StockProductFormBuilder() {}

    static JPanel createImageChooserPanel(StockPanel owner, byte[][] imageHolder) {
        JLabel imagePreview = new JLabel("Sem imagem", SwingConstants.CENTER);
        imagePreview.setPreferredSize(new Dimension(96, 96));
        imagePreview.setOpaque(true);
        imagePreview.setBackground(UIHelper.BG_CARD);
        imagePreview.setForeground(UIHelper.TEXT_MUTED);
        imagePreview.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER, 1, true));

        ModernButton chooseImageBtn = UIHelper.createSecondaryButton("Escolher Imagem");
        chooseImageBtn.setIcon(UIHelper.icon("fas-image", 14));
        chooseImageBtn.addActionListener(ev -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Imagens (png, jpg)", "png", "jpg", "jpeg"));
            if (fc.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION) {
                byte[] bytes = UIHelper.readScaledImage(fc.getSelectedFile(), 320);
                if (bytes == null) {
                    owner.showStockNotice(FeedbackType.ERROR, "Imagem inválida", "Não foi possível ler a imagem seleccionada.");
                    return;
                }
                imageHolder[0] = bytes;
                imagePreview.setText(null);
                imagePreview.setIcon(UIHelper.imageIconFromBytes(bytes, 96, 96));
            }
        });

        JPanel imagePanel = new JPanel(new BorderLayout(10, 0));
        imagePanel.setOpaque(false);
        imagePanel.add(imagePreview, BorderLayout.WEST);
        imagePanel.add(chooseImageBtn, BorderLayout.CENTER);
        return imagePanel;
    }

    static void bindPackagingFields(JTextField packagesPerBoxField, JTextField unitsPerPackageField, JTextField unitsPerBoxField) {
        Runnable recalculate = () -> {
            int pkgs = StockProductFormValidator.parseIntOrZero(packagesPerBoxField.getText());
            int un = StockProductFormValidator.parseIntOrZero(unitsPerPackageField.getText());
            if (pkgs <= 0 || un <= 0) {
                unitsPerBoxField.setText("—");
                return;
            }
            try {
                unitsPerBoxField.setText(String.valueOf(PackagingComposition.of(pkgs, un).unitsPerBox()));
            } catch (BusinessRuleException exception) {
                unitsPerBoxField.setText("—");
            }
        };
        UIHelper.onTextChange(packagesPerBoxField, recalculate);
        UIHelper.onTextChange(unitsPerPackageField, recalculate);
        recalculate.run();
    }
}
