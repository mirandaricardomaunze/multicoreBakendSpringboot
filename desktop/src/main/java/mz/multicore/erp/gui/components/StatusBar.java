package mz.multicore.erp.gui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Barra de estado no rodapé da janela principal — padrão de ERP profissional (Multicore, Primavera, Sage).
 * Mostra só o que não está noutro sítio do ecrã: produto, aviso de versão e hora.
 * Actualiza a hora a cada minuto via {@link javax.swing.Timer} interno.
 */
public class StatusBar extends JPanel {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int BAR_HEIGHT = 24;

    private final JLabel clockLabel    = slimLabel(LocalTime.now().format(TIME_FMT));
    /** Aviso de versão nova. Fica invisível enquanto não houver nada a dizer. */
    private final JLabel updateLabel   = slimLabel("");
    private final JLabel updateIcon    = iconLabel("fas-arrow-circle-up", UIHelper.ACCENT);

    public StatusBar() {
        setLayout(new BorderLayout(0, 0));
        setPreferredSize(new Dimension(0, BAR_HEIGHT));
        setBackground(barBg());
        setBorder(new EmptyBorder(0, 8, 0, 8));

        // Lado direito — aviso de versão (quando houver), hora
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        updateIcon.setVisible(false);
        updateLabel.setVisible(false);
        updateLabel.setForeground(UIHelper.ACCENT);
        right.add(updateIcon);
        right.add(updateLabel);
        right.add(iconLabel("fas-clock", UIHelper.TEXT_MUTED));
        right.add(clockLabel);
        add(right, BorderLayout.EAST);

        // Centro — Identificação institucional do produto com atalho para Sobre o Sistema
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        center.setOpaque(false);
        JLabel aboutIcon = iconLabel("fas-info-circle", UIHelper.ACCENT_CYAN);
        aboutIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        aboutIcon.setToolTipText("Sobre o Multicore ERP (F12) • Clique para ver detalhes e licença do sistema");
        JLabel brandLabel = slimLabel("Multicore ERP • v1.0.0");
        brandLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        brandLabel.setToolTipText("Sobre o Multicore ERP (F12) • Clique para ver detalhes e licença do sistema");
        java.awt.event.MouseAdapter aboutClickAdapter = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                AboutDialog.show(StatusBar.this);
            }
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                brandLabel.setForeground(UIHelper.ACCENT_CYAN);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                brandLabel.setForeground(UIHelper.TEXT_MUTED);
            }
        };
        aboutIcon.addMouseListener(aboutClickAdapter);
        brandLabel.addMouseListener(aboutClickAdapter);
        center.add(aboutIcon);
        center.add(brandLabel);
        add(center, BorderLayout.CENTER);

        // Timer de hora — dispara a cada minuto no EDT
        new javax.swing.Timer(60_000, e -> {
            clockLabel.setText(LocalTime.now().format(TIME_FMT));
            setBackground(barBg());  // acompanha troca de tema em runtime
            right.setBackground(barBg());
            center.setBackground(barBg());
        }).start();
    }

    /**
     * Aviso discreto de que existe versão nova no servidor.
     *
     * <p>Fica no rodapé e <b>não interrompe ninguém</b>: é isto que faz as lojas actualizarem,
     * não o bloqueio. Um diálogo a meio de uma venda seria fechado sem ler; um aviso permanente
     * é visto no fecho do dia, que é quando dá jeito actualizar.
     *
     * @param newVersion versão disponível no servidor; {@code null} esconde o aviso
     */
    public void setUpdateAvailable(String newVersion) {
        boolean show = newVersion != null && !newVersion.isBlank();
        updateLabel.setText(show ? "Versão " + newVersion + " disponível" : "");
        updateLabel.setToolTipText(show
                ? "Há uma versão mais recente no servidor. Actualize quando for conveniente — "
                  + "o programa continua a funcionar."
                : null);
        updateIcon.setVisible(show);
        updateLabel.setVisible(show);
        revalidate();
        repaint();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────────────────

    private static Color barBg() {
        return UIHelper.isLight() ? new Color(241, 245, 249) : new Color(15, 23, 42);
    }

    private static JLabel slimLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        l.setForeground(UIHelper.TEXT_MUTED);
        return l;
    }

    private static JLabel iconLabel(String iconCode, Color color) {
        JLabel l = new JLabel(UIHelper.icon(iconCode, 12, color));
        l.setOpaque(false);
        return l;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            // Linha de topo — separador visual com o conteúdo acima
            g2.setColor(barBg());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(UIHelper.BORDER);
            g2.fillRect(0, 0, getWidth(), 1);
        } finally {
            g2.dispose();
        }
    }
}
