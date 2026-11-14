package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ButtonStyles;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.audit.dto.ForensicAnomalyDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;

/** Diálogo modal ergonómico de apresentação de dossiê de anomalia forense. */
public final class ForensicAnomalyDetailsDialog {

    private ForensicAnomalyDetailsDialog() {}

    public static void show(Component parent, ForensicAnomalyDTO a) {
        if (a == null) return;
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "Dossiê de Anomalia Forense", Dialog.ModalityType.APPLICATION_MODAL);

        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setBackground(UIHelper.BG_CARD);
        p.setBorder(new EmptyBorder(16, 20, 16, 20));
        p.add(UIHelper.createHeading("Ocorrência: " + (a.documentOrReference() != null ? a.documentOrReference() : "—")), BorderLayout.NORTH);

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        area.setBackground(UIHelper.BG_DARK);
        area.setForeground(UIHelper.TEXT_LIGHT);
        area.setBorder(new EmptyBorder(12, 12, 12, 12));

        String time = a.timestamp() != null ? a.timestamp().format(UIHelper.DATETIME_FMT) : "—";
        String impact = UIHelper.formatMzn(a.financialImpact() != null ? a.financialImpact() : BigDecimal.ZERO);
        area.setText("Data/Hora: " + time + "\n"
                + "Severidade: " + a.severity() + "\n"
                + "Categoria: " + a.categoryLabel() + "\n"
                + "Operador / Utilizador: " + a.operator() + "\n"
                + "Impacto Financeiro: " + impact + "\n"
                + "Motivo Original Declarado: " + (a.justification() != null ? a.justification() : "—") + "\n\n"
                + "DETALHES ANALÍTICOS:\n" + (a.details() != null ? a.details() : "—") + "\n\n"
                + "RECOMENDAÇÃO PREVENTIVA DA AUDITORIA:\n" + (a.recommendation() != null ? a.recommendation() : "—"));

        p.add(new JScrollPane(area), BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRow.setOpaque(false);
        ModernButton closeBtn = ButtonStyles.secondary(UIHelper.createSecondaryButton("Fechar"));
        closeBtn.addActionListener(e -> dialog.dispose());
        btnRow.add(closeBtn);
        p.add(btnRow, BorderLayout.SOUTH);

        dialog.setContentPane(p);
        dialog.setSize(580, 420);
        UIHelper.applyStandardModalBounds(dialog, 580, 420);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }
}
