package mz.multicore.erp.gui.intelligence;

import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.intelligence.dto.AiDocumentVersionDTO;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Diálogo modal para comparação lado a lado entre duas versões de documento de inteligência.
 */
public final class AiDocumentComparisonDialog {

    private AiDocumentComparisonDialog() {}

    public static void show(Component parent, AiDocumentVersionDTO current, AiDocumentVersionDTO previous) {
        Objects.requireNonNull(current, "A versão actual é obrigatória.");
        Objects.requireNonNull(previous, "A versão anterior é obrigatória.");

        JTextArea oldText = createComparisonTextArea(previous);
        JTextArea newText = createComparisonTextArea(current);

        JPanel comparison = new JPanel(new GridLayout(1, 2, 12, 0));
        comparison.setOpaque(false);
        comparison.add(createVersionColumn("Versão " + previous.versionNumber(), oldText));
        comparison.add(createVersionColumn("Versão " + current.versionNumber(), newText));

        Window owner = parent instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent);
        new ModernFormDialog(owner, "Comparar Versões", "fas-columns", comparison)
                .asReadOnly("Fechar")
                .setSize(920, 560)
                .showDialog();
    }

    private static JTextArea createComparisonTextArea(AiDocumentVersionDTO version) {
        JTextArea area = new JTextArea(version.generatedContent());
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);
        UIHelper.styleTextArea(area);
        return area;
    }

    private static JPanel createVersionColumn(String title, JTextArea text) {
        JPanel column = new JPanel(new BorderLayout(0, 6));
        column.setOpaque(false);
        column.add(UIHelper.createSubheading(title), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(text);
        UIHelper.styleScrollPane(scroll);
        column.add(scroll, BorderLayout.CENTER);
        return column;
    }
}
