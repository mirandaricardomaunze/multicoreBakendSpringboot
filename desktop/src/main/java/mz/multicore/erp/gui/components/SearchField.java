package mz.multicore.erp.gui.components;

import javax.swing.Icon;
import javax.swing.JTextField;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;

/**
 * Campo de pesquisa com **ícone de lupa** e texto-dica desenhados directamente — funciona em qualquer
 * Look&Feel (o projecto usa o L&F por omissão, onde os client-properties do FlatLaf não renderizam).
 * É um {@link JTextField}, por isso serve tal e qual nos filtros ({@link TableFilter}).
 */
public class SearchField extends JTextField {

    private static final int ICON_SIZE = 14;
    private static final int PAD_LEFT = 26; // espaço reservado à esquerda para a lupa

    private final Icon icon = UIHelper.icon("fas-search", ICON_SIZE, UIHelper.TEXT_MUTED);
    private final String hint;

    public SearchField(String hint) {
        this.hint = hint == null ? "" : hint;
        UIHelper.styleTextField(this);
        // Empurra o texto para a direita da lupa, sem substituir a borda (foco continua a funcionar).
        Insets m = getMargin();
        setMargin(new Insets(m == null ? 0 : m.top, PAD_LEFT, m == null ? 0 : m.bottom, m == null ? 8 : m.right));
        setColumns(18);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int iconY = (getHeight() - icon.getIconHeight()) / 2;
        icon.paintIcon(this, g, 7, iconY);
        if (getText().isEmpty() && !hint.isEmpty()) {
            g.setColor(UIHelper.TEXT_MUTED);
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g.drawString(hint, PAD_LEFT, ty);
        }
    }
}
