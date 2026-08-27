package mz.multicore.erp.gui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Barra de estado no rodapé da janela principal — padrão de ERP profissional (Multicore, Primavera, Sage).
 * Mostra: módulo activo · nº registos · empresa · utilizador · hora.
 * Actualiza a hora a cada minuto via {@link javax.swing.Timer} interno.
 *
 * <p>Uso:</p>
 * <pre>
 *   StatusBar bar = new StatusBar();
 *   frame.add(bar, BorderLayout.SOUTH);
 *   bar.setContext("Faturação", 42, "Loja Central", "maria");
 * </pre>
 */
public class StatusBar extends JPanel {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int BAR_HEIGHT = 24;

    private final JLabel moduleLabel   = slimLabel("");
    private final JLabel recordsLabel  = slimLabel("");
    private final JLabel companyLabel  = slimLabel("");
    private final JLabel userLabel     = slimLabel("");
    private final JLabel clockLabel    = slimLabel(LocalTime.now().format(TIME_FMT));
    /** Aviso de versão nova. Fica invisível enquanto não houver nada a dizer. */
    private final JLabel updateLabel   = slimLabel("");
    private final JLabel updateIcon    = iconLabel("fas-arrow-circle-up", UIHelper.ACCENT);

    public StatusBar() {
        setLayout(new BorderLayout(0, 0));
        setPreferredSize(new Dimension(0, BAR_HEIGHT));
        setBackground(barBg());
        setBorder(new EmptyBorder(0, 8, 0, 8));

        // Lado esquerdo — módulo e nº registos
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(iconLabel("fas-layer-group", UIHelper.ACCENT));
        left.add(moduleLabel);
        left.add(separator());
        left.add(iconLabel("fas-list", UIHelper.TEXT_MUTED));
        left.add(recordsLabel);
        add(left, BorderLayout.WEST);

        // Lado direito — aviso de versão (quando houver), empresa, utilizador, hora
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        updateIcon.setVisible(false);
        updateLabel.setVisible(false);
        updateLabel.setForeground(UIHelper.ACCENT);
        right.add(updateIcon);
        right.add(updateLabel);
        right.add(iconLabel("fas-building", UIHelper.TEXT_MUTED));
        right.add(companyLabel);
        right.add(separator());
        right.add(iconLabel("fas-user-circle", UIHelper.TEXT_MUTED));
        right.add(userLabel);
        right.add(separator());
        right.add(iconLabel("fas-clock", UIHelper.TEXT_MUTED));
        right.add(clockLabel);
        add(right, BorderLayout.EAST);

        // Timer de hora — dispara a cada minuto no EDT
        new javax.swing.Timer(60_000, e -> {
            clockLabel.setText(LocalTime.now().format(TIME_FMT));
            setBackground(barBg());  // acompanha troca de tema em runtime
            left.setBackground(barBg());
            right.setBackground(barBg());
        }).start();
    }

    /**
     * Actualiza o contexto visível na barra.
     * @param module   nome do módulo activo (ex.: "Faturação")
     * @param records  número de registos na tabela activa (-1 = oculta)
     * @param company  nome da empresa activa
     * @param user     nome de utilizador
     */
    public void setContext(String module, int records, String company, String user) {
        moduleLabel.setText(module == null ? "" : module);
        recordsLabel.setText(records < 0 ? "" : records + " registo" + (records == 1 ? "" : "s"));
        companyLabel.setText(company == null ? "" : company);
        userLabel.setText(user == null ? "" : user);
    }

    /** Actualiza apenas o nº de registos (ex.: após filtro ser aplicado). */
    public void setRecords(int records) {
        recordsLabel.setText(records < 0 ? "" : records + " registo" + (records == 1 ? "" : "s"));
    }

    /** Actualiza apenas o módulo activo. */
    public void setModule(String module) {
        moduleLabel.setText(module == null ? "" : module);
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

    private static JLabel separator() {
        JLabel sep = new JLabel("·");
        sep.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        sep.setForeground(UIHelper.TEXT_MUTED);
        return sep;
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
