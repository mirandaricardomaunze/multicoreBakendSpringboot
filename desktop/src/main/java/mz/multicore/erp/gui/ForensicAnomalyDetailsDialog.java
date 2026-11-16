package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.DialogChrome;
import mz.multicore.erp.gui.components.Spacing;
import mz.multicore.erp.gui.components.Typography;
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


        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Typography.BODY);
        area.setBackground(UIHelper.BG_DARK);
        area.setForeground(UIHelper.TEXT_LIGHT);
        area.setBorder(new EmptyBorder(Spacing.MD, Spacing.MD, Spacing.MD, Spacing.MD));

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

        ModernButton closeBtn = ButtonStyles.secondary(UIHelper.createSecondaryButton("Fechar"));
        closeBtn.addActionListener(e -> dialog.dispose());
        dialog.setContentPane(DialogChrome.frame("fas-user-secret",
                "Ocorrência: " + (a.documentOrReference() != null ? a.documentOrReference() : "—"),
                "Dossiê de anomalia forense", new JScrollPane(area), DialogChrome.footer(closeBtn)));
        dialog.setSize(580, 420);
        UIHelper.applyStandardModalBounds(dialog, 580, 420);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }
}
