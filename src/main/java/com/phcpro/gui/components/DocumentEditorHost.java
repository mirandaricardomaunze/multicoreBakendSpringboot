package com.phcpro.gui.components;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.util.function.BooleanSupplier;

/**
 * Aloja o <b>conteúdo de um documento</b> (ex.: nova encomenda/fatura) a ecrã inteiro, com uma barra
 * de acções fixa no topo: <b>← Voltar à lista</b> (com guarda de alterações), título e <b>Guardar</b>.
 * O conteúdo do formulário está num <b>scroll vertical</b> — quando é mais alto que o painel, rola
 * (a barra de acções fica sempre visível).
 *
 * <p>Padrão do UX híbrido (2026-07-27): documentos com linhas editam-se em painel completo, não em
 * modal. Reutilizável por qualquer documento. Ver docs/DOCUMENTO_PAINEL_EDITOR_SPEC.md.</p>
 */
public class DocumentEditorHost extends JPanel {

    private final BooleanSupplier dirty;
    private final Runnable onBack;

    /**
     * @param title   título do documento (ex.: "Nova Encomenda")
     * @param content conteúdo do formulário do documento (já construído)
     * @param onSave  acção de guardar (valida e persiste; em erro deve manter o editor aberto)
     * @param onBack  acção de voltar à lista (só é chamada depois da guarda de alterações)
     * @param dirty   diz se há alterações por gravar; {@code null} = nunca pergunta
     */
    public DocumentEditorHost(String title, JComponent content, Runnable onSave, Runnable onBack,
                              BooleanSupplier dirty) {
        this.dirty = dirty;
        this.onBack = onBack;
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(4, 4, 4, 4));

        add(buildToolbar(title, onSave), BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        card.add(verticalScroll(content), BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
    }

    private JPanel buildToolbar(String title, Runnable onSave) {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        ModernButton back = UIHelper.createSecondaryButton("Voltar à lista");
        back.setIcon(UIHelper.icon("fas-arrow-left", 14));
        back.addActionListener(e -> requestBack());
        JLabel titleLabel = UIHelper.createHeading(title);
        left.add(back);
        left.add(titleLabel);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        ModernButton save = UIHelper.createPrimaryButton("Guardar");
        save.setIcon(UIHelper.icon("fas-save", 14));
        save.addActionListener(e -> { if (onSave != null) onSave.run(); });
        right.add(save);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    /** Envolve o conteúdo num scroll que ocupa toda a largura e rola na vertical quando é alto. */
    private static JScrollPane verticalScroll(JComponent content) {
        JScrollPane scroll = new JScrollPane(new WidthTrackingPanel(content),
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setUI(new SlimScrollBarUI());
        return scroll;
    }

    /** Voltar à lista, confirmando o descarte se houver alterações por gravar. */
    public void requestBack() {
        if (shouldConfirmDiscard(dirty != null && dirty.getAsBoolean())) {
            int opt = JOptionPane.showConfirmDialog(this,
                    "Há alterações por gravar. Sair e descartar?",
                    "Descartar alterações", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (opt != JOptionPane.YES_OPTION) return;
        }
        if (onBack != null) onBack.run();
    }

    /** Regra pura: só se confirma o descarte quando há alterações por gravar. */
    public static boolean shouldConfirmDiscard(boolean dirty) {
        return dirty;
    }

    /** Painel que ocupa a largura do viewport (sem scroll horizontal) mas cresce em altura (scroll vertical). */
    private static final class WidthTrackingPanel extends JPanel implements Scrollable {
        WidthTrackingPanel(JComponent content) {
            super(new BorderLayout());
            setOpaque(false);
            add(content, BorderLayout.NORTH);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) { return 120; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
