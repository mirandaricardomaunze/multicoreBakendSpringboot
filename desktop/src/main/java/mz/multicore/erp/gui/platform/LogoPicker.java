package mz.multicore.erp.gui.platform;

import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;

/** "Choose image" button with a thumbnail; the reduced image bytes go into {@code holder[0]}. */
final class LogoPicker {

    private static final int MAX_DIMENSION = 320;

    private LogoPicker() {}

    static JComponent create(Component parent, byte[][] holder, boolean hasExisting) {
        JLabel preview = new JLabel(hasExisting ? "Logótipo actual definido" : "Sem logótipo");
        preview.setForeground(UIHelper.TEXT_MUTED);
        JButton pick = new JButton("Escolher imagem", UIHelper.icon("fas-image", 16, UIHelper.ACCENT_BLUE));
        pick.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Imagens (PNG, JPG)", "png", "jpg", "jpeg"));
            if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return;
            byte[] bytes = UIHelper.readScaledImage(chooser.getSelectedFile(), MAX_DIMENSION);
            if (bytes == null) {
                ModernMessageDialog.show(SwingUtilities.getWindowAncestor(pick), FeedbackType.WARNING,
                        "Imagem inválida", "Seleccione uma imagem PNG ou JPG válida para o logótipo.");
                return;
            }
            holder[0] = bytes;
            preview.setText(null);
            preview.setIcon(UIHelper.imageIconFromBytes(bytes, 64, 40));
        });
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.setOpaque(false);
        panel.add(pick);
        panel.add(preview);
        return panel;
    }
}
