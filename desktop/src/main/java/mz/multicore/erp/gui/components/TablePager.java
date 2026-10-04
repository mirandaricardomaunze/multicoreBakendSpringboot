package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.paging.PageResponse;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;

/**
 * Barra de paginação executiva para listagens servidas por página.
 * Botões de navegação simétricos com estilo moderno (ghost/outline), estados de hover/disabled refinados
 * e alinhamento harmónico de altura.
 *
 * <p>Complementa o {@link TableFooter} (que conta as linhas <i>visíveis</i>): aqui o total é o
 * que existe no servidor, e as setas pedem a página seguinte por HTTP. Componente único para
 * todas as listagens paginadas, em vez de cada painel desenhar os seus botões.
 */
public final class TablePager extends JPanel {

    private static final Integer[] PAGE_SIZES = {25, 50, 100, 200};
    static final int CONTROL_GAP = 8;
    static final int ACTION_ROW_GAP = 10;

    private final JLabel status = new JLabel(" ");
    private final JLabel pageState = new JLabel(" ");
    private final JLabel pageLabel = new JLabel("Por página:");
    private final PagerNavButton first = new PagerNavButton("fas-angle-double-left", "Primeira página");
    private final PagerNavButton previous = new PagerNavButton("fas-chevron-left", "Página anterior");
    private final PagerNavButton next = new PagerNavButton("fas-chevron-right", "Página seguinte");
    private final PagerNavButton last = new PagerNavButton("fas-angle-double-right", "Última página");
    private final JComboBox<Integer> pageSize = new JComboBox<>(PAGE_SIZES);
    private final BiConsumer<Integer, Integer> loader;

    private int page;
    private int totalPages = 1;

    /** @param loader recebe (página começada em 0, tamanho) e carrega essa página */
    public TablePager(BiConsumer<Integer, Integer> loader) {
        super(new BorderLayout(0, 0));
        this.loader = loader;
        setOpaque(false);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIHelper.GRID),
                new EmptyBorder(8, 4, ACTION_ROW_GAP, 4)));

        status.setForeground(UIHelper.TEXT_MUTED);
        status.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));

        pageState.setHorizontalAlignment(SwingConstants.CENTER);
        pageState.setForeground(UIHelper.TEXT_LIGHT);
        pageState.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));

        pageLabel.setForeground(UIHelper.TEXT_MUTED);
        pageLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));

        UIHelper.styleComboBox(pageSize);
        pageSize.setPreferredSize(new Dimension(86, UIHelper.FORM_CONTROL_HEIGHT - 4));
        pageSize.setSelectedItem(50);
        pageSize.getAccessibleContext().setAccessibleName("Registos por página");
        pageSize.addActionListener(e -> reload(0));

        first.addActionListener(e -> reload(0));
        previous.addActionListener(e -> reload(page - 1));
        next.addActionListener(e -> reload(page + 1));
        last.addActionListener(e -> reload(totalPages - 1));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(pageLabel);
        left.add(pageSize);
        left.add(status);
        add(left, BorderLayout.WEST);
        add(pageState, BorderLayout.CENTER);

        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        navigation.setOpaque(false);
        navigation.add(first);
        navigation.add(previous);
        navigation.add(next);
        navigation.add(last);
        add(navigation, BorderLayout.EAST);
        setEnabledState();
    }

    /** Carrega a primeira página — chamar quando o painel abre ou o utilizador actualiza. */
    public void reload() {
        reload(0);
    }

    /** Actualiza o estado da barra depois de a página chegar do servidor. */
    public void apply(PageResponse<?> response) {
        if (response == null) {
            status.setText(" ");
            pageState.setText(" ");
            return;
        }
        page = response.page();
        totalPages = Math.max(response.totalPages(), 1);
        status.setText(response.totalElements() == 0
                ? "Sem registos"
                : String.format("%d registo(s)", response.totalElements()));
        pageState.setText(response.totalElements() == 0
                ? ""
                : String.format("Página  %d  de  %d", page + 1, totalPages));
        setEnabledState();
    }

    private void reload(int target) {
        int size = (Integer) pageSize.getSelectedItem();
        int bounded = Math.max(0, Math.min(target, totalPages - 1));
        loader.accept(bounded, size);
    }

    private void setEnabledState() {
        boolean hasPrevious = page > 0;
        boolean hasNext = page + 1 < totalPages;
        first.setEnabled(hasPrevious);
        previous.setEnabled(hasPrevious);
        next.setEnabled(hasNext);
        last.setEnabled(hasNext);
    }

    /**
     * Botão quadrado simétrico moderno para controlos de paginação (30x30 px).
     * Renderização elegante com cantos arredondados, borda subtil e suporte a alto contraste.
     */
    private static final class PagerNavButton extends JButton {
        private final String iconCode;
        private boolean isHover = false;

        PagerNavButton(String iconCode, String tooltip) {
            this.iconCode = iconCode;
            setToolTipText(tooltip);
            getAccessibleContext().setAccessibleName(tooltip);
            Dimension size = new Dimension(30, 30);
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (isEnabled()) {
                        isHover = true;
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHover = false;
                    repaint();
                }
            });
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            setCursor(new Cursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int arc = UIHelper.RADIUS_SM;
            boolean enabled = isEnabled();

            if (UIHelper.isHighContrast()) {
                if (enabled && isHover) {
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);
                } else {
                    g2.setColor(Color.BLACK);
                    g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);
                    g2.setColor(enabled ? Color.WHITE : Color.GRAY);
                    g2.drawRoundRect(1, 1, w - 2, h - 2, arc, arc);
                }
            } else {
                if (!enabled) {
                    // Estado desabilitado refinado: fundo muito discreto e borda ténue (evita blocos opacos pesados)
                    g2.setColor(new Color(UIHelper.GRID.getRed(), UIHelper.GRID.getGreen(), UIHelper.GRID.getBlue(), 30));
                    g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);
                    g2.setColor(new Color(UIHelper.BORDER.getRed(), UIHelper.BORDER.getGreen(), UIHelper.BORDER.getBlue(), 70));
                    g2.drawRoundRect(1, 1, w - 2, h - 2, arc, arc);
                } else if (getModel().isPressed()) {
                    g2.setColor(UIHelper.ACCENT_BLUE_HOVER);
                    g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);
                } else if (isHover) {
                    g2.setColor(UIHelper.ACCENT_BLUE);
                    g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);
                } else {
                    g2.setColor(UIHelper.BG_CARD != null ? UIHelper.BG_CARD : new Color(30, 41, 59));
                    g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);
                    g2.setColor(UIHelper.BORDER);
                    g2.drawRoundRect(1, 1, w - 2, h - 2, arc, arc);
                }
            }

            // Cor do ícone
            Color iconColor;
            if (UIHelper.isHighContrast()) {
                iconColor = (enabled && isHover) ? Color.BLACK : (enabled ? Color.WHITE : Color.GRAY);
            } else if (!enabled) {
                iconColor = new Color(UIHelper.TEXT_MUTED.getRed(), UIHelper.TEXT_MUTED.getGreen(), UIHelper.TEXT_MUTED.getBlue(), 100);
            } else if (isHover || getModel().isPressed()) {
                iconColor = Color.WHITE;
            } else {
                iconColor = UIHelper.TEXT_LIGHT;
            }

            Icon icon = UIHelper.icon(iconCode, 12, iconColor);
            if (icon != null) {
                int ix = (w - icon.getIconWidth()) / 2;
                int iy = (h - icon.getIconHeight()) / 2;
                icon.paintIcon(this, g2, ix, iy);
            }

            g2.dispose();
        }
    }
}
