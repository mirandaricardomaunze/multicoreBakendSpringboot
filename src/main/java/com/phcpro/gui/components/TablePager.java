package com.phcpro.gui.components;

import com.phcpro.architecture.paging.PageResponse;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.awt.FlowLayout;
import java.awt.Font;
import java.util.function.BiConsumer;

/**
 * Barra de paginação para listagens servidas por página.
 *
 * <p>Complementa o {@link TableFooter} (que conta as linhas <i>visíveis</i>): aqui o total é o
 * que existe no servidor, e as setas pedem a página seguinte por HTTP. Componente único para
 * todas as listagens paginadas, em vez de cada painel desenhar os seus botões.
 *
 * <p>Não sabe carregar nada: recebe um {@code loader} (página, tamanho) e chama-o. Quem o usa é
 * que decide como falar com o servidor — o painel continua a ser o único a fazer HTTP.
 */
public final class TablePager extends JPanel {

    private static final Integer[] PAGE_SIZES = {25, 50, 100, 200};

    private final JLabel status = new JLabel(" ");
    private final ModernButton first = navButton("fas-angle-double-left", "Primeira página");
    private final ModernButton previous = navButton("fas-angle-left", "Página anterior");
    private final ModernButton next = navButton("fas-angle-right", "Página seguinte");
    private final ModernButton last = navButton("fas-angle-double-right", "Última página");
    private final JComboBox<Integer> pageSize = new JComboBox<>(PAGE_SIZES);
    private final BiConsumer<Integer, Integer> loader;

    private int page;
    private int totalPages = 1;

    /** @param loader recebe (página começada em 0, tamanho) e carrega essa página */
    public TablePager(BiConsumer<Integer, Integer> loader) {
        super(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        this.loader = loader;
        setOpaque(false);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIHelper.GRID),
                new EmptyBorder(6, 4, 0, 4)));

        status.setForeground(UIHelper.TEXT_MUTED);
        status.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        UIHelper.styleComboBox(pageSize);
        pageSize.setSelectedItem(50);
        pageSize.getAccessibleContext().setAccessibleName("Registos por página");
        pageSize.addActionListener(e -> reload(0));

        first.addActionListener(e -> reload(0));
        previous.addActionListener(e -> reload(page - 1));
        next.addActionListener(e -> reload(page + 1));
        last.addActionListener(e -> reload(totalPages - 1));

        add(status);
        add(new JLabel("Por página:"));
        add(pageSize);
        add(first);
        add(previous);
        add(next);
        add(last);
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
            return;
        }
        page = response.page();
        totalPages = Math.max(response.totalPages(), 1);
        status.setText(response.totalElements() == 0
                ? "Sem registos"
                : String.format("Página %d de %d · %d registo(s)",
                        page + 1, totalPages, response.totalElements()));
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

    private static ModernButton navButton(String icon, String tooltip) {
        ModernButton button = UIHelper.createSecondaryButton("");
        button.setIcon(UIHelper.icon(icon, 12));
        button.setToolTipText(tooltip);
        // Botão só com ícone precisa de nome acessível (regra da consistência de UI).
        button.getAccessibleContext().setAccessibleName(tooltip);
        return button;
    }
}
