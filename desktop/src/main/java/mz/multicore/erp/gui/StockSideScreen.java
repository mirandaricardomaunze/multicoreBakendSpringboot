package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Ecrã próprio no menu lateral para uma área de cadastro do stock (Armazéns, Categorias).
 * Antes eram abas escondidas dentro de Stock; os dados e as acções continuam no {@link StockPanel}.
 */
public final class StockSideScreen extends JPanel {

    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final Runnable onRefresh;

    StockSideScreen(JComponent content, Runnable onRefresh) {
        super(new BorderLayout(0, 8));
        this.onRefresh = onRefresh;
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(8, 12, 8, 12));
        add(feedback, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
    }

    /** Os avisos aparecem no ecrã que está aberto: o de um cadastro, ou o do Stock por omissão. */
    static InlineFeedbackPanel visibleFeedback(InlineFeedbackPanel stockFeedback, StockSideScreen... screens) {
        for (StockSideScreen screen : screens) {
            if (screen != null && screen.isShowing()) return screen.feedback;
        }
        return stockFeedback;
    }

    public void onPanelSelected() { onRefresh.run(); }
}
