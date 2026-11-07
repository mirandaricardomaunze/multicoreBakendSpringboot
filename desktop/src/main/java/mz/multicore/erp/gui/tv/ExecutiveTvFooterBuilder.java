package mz.multicore.erp.gui.tv;

import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Font;

/** Rodapé curto do Painel Executivo: atalhos à esquerda, estado da sincronização à direita. */
final class ExecutiveTvFooterBuilder {

    private ExecutiveTvFooterBuilder() {}

    static JPanel build(boolean lightMode, String syncText, boolean syncOk) {
        JPanel footer = ExecutiveTvComponents.transparent(new BorderLayout());
        footer.setBorder(new EmptyBorder(4, 22, 10, 22));
        footer.add(ExecutiveTvComponents.label("F11  ecrã inteiro     Espaço  rodízio     Esc  fechar", Font.PLAIN, 12,
                ExecutiveTvTheme.getTextFaintColor(lightMode)), BorderLayout.WEST);
        JLabel sync = ExecutiveTvComponents.label(syncText, Font.PLAIN, 12, ExecutiveTvTheme.getTextMutedColor(lightMode));
        sync.setIcon(UIHelper.icon("fas-circle", 8, syncOk ? UIHelper.APPROVED_GREEN : UIHelper.PENDING_YELLOW));
        sync.setIconTextGap(8);
        footer.add(sync, BorderLayout.EAST);
        return footer;
    }
}
