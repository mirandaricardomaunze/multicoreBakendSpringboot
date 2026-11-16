package mz.multicore.erp.gui.tv;

import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * Moldura única dos modais do Painel Executivo (Metas, Vendedores).
 *
 * <p>O modal abre <b>no tema em que o painel está nesse momento</b>, claro ou escuro, e não no tema
 * da aplicação. Antes, as Metas usavam o modal de formulário global do ERP e abriam brancas por cima
 * do painel escuro; os Vendedores tinham uma paleta própria, um tom ao lado da do painel. Aqui todas
 * as cores vêm de {@link ExecutiveTvTheme}, e o comportamento é o dos modais do ERP: Ctrl+S grava,
 * Esc fecha, o erro aparece dentro do modal e o painel por trás fica escurecido.
 */
final class ExecutiveTvDialog {

    private final Window owner;
    private final JDialog dialog;
    private final boolean lightMode;
    private final JPanel body;
    private final JPanel buttons;
    private final JLabel error;
    private Runnable onSave;
    private boolean saved;

    ExecutiveTvDialog(Window owner, String title, String iconCode, String subtitle, boolean lightMode) {
        this.owner = owner;
        this.lightMode = lightMode;
        this.dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().setBackground(ExecutiveTvTheme.getBgColor(lightMode));

        JPanel root = ExecutiveTvComponents.transparent(new BorderLayout(0, 14));
        root.setBorder(new EmptyBorder(20, 22, 18, 22));
        root.add(header(iconCode, title, subtitle), BorderLayout.NORTH);

        body = ExecutiveTvComponents.transparent(new BorderLayout(0, 10));
        error = ExecutiveTvComponents.label(" ", Font.BOLD, 12, UIHelper.REJECTED_RED);
        error.setVisible(false);
        body.add(error, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);

        buttons = ExecutiveTvComponents.transparent(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, ExecutiveTvTheme.getCardBorderColor(lightMode)),
                new EmptyBorder(14, 0, 0, 0)));
        root.add(buttons, BorderLayout.SOUTH);
        dialog.add(root);

        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.getRootPane().registerKeyboardAction(e -> { if (onSave != null) attemptSave(); },
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private JPanel header(String iconCode, String title, String subtitle) {
        JPanel header = ExecutiveTvComponents.transparent(new BorderLayout(12, 0));
        header.add(ExecutiveTvComponents.iconChip(iconCode, UIHelper.ACCENT_BLUE, lightMode), BorderLayout.WEST);
        JPanel texts = ExecutiveTvComponents.transparent(new BorderLayout(0, 2));
        texts.add(ExecutiveTvComponents.label(title, Font.BOLD, 16, ExecutiveTvTheme.getTextLightColor(lightMode)), BorderLayout.NORTH);
        if (subtitle != null && !subtitle.isBlank()) {
            texts.add(ExecutiveTvComponents.label(subtitle, Font.PLAIN, 12, ExecutiveTvTheme.getTextMutedColor(lightMode)), BorderLayout.SOUTH);
        }
        header.add(texts, BorderLayout.CENTER);
        return header;
    }

    boolean lightMode() {
        return lightMode;
    }

    JDialog window() {
        return dialog;
    }

    ExecutiveTvDialog content(JComponent content) {
        body.add(ExecutiveTvComponents.verticalScroll(content), BorderLayout.CENTER);
        return this;
    }

    /** Cancelar + Gravar. Uma {@link RuntimeException} da acção fica escrita no modal, que não fecha. */
    ExecutiveTvDialog onSave(String saveLabel, Runnable action) {
        this.onSave = action;
        buttons.add(ExecutiveTvComponents.ghostButton("Cancelar", "fas-times",
                ExecutiveTvTheme.getTextMutedColor(lightMode), dialog::dispose, lightMode));
        buttons.add(ExecutiveTvFormControls.primaryButton(saveLabel, "fas-save", this::attemptSave));
        return this;
    }

    /** Só leitura: um único botão para fechar. */
    ExecutiveTvDialog closeOnly(String label) {
        buttons.add(ExecutiveTvComponents.ghostButton(label, "fas-check",
                ExecutiveTvTheme.getTextLightColor(lightMode), dialog::dispose, lightMode));
        return this;
    }

    /** Mostra e bloqueia. Devolve true se gravou. */
    boolean show(int preferredWidth, int preferredHeight) {
        Rectangle area = owner != null ? owner.getBounds() : new Rectangle(0, 0, 1366, 800);
        dialog.setSize(Math.min(preferredWidth, (int) (area.width * 0.94)), Math.min(preferredHeight, (int) (area.height * 0.94)));
        dialog.setMinimumSize(new Dimension(Math.min(560, dialog.getWidth()), Math.min(360, dialog.getHeight())));
        dialog.setLocationRelativeTo(owner);
        Component previousGlass = owner instanceof RootPaneContainer rpc ? rpc.getGlassPane() : null;
        if (owner instanceof RootPaneContainer rpc) {
            JPanel dim = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    g.setColor(new Color(0, 0, 0, lightMode ? 70 : 120));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            };
            dim.setOpaque(false);
            rpc.setGlassPane(dim);
            dim.setVisible(true);
        }
        try {
            dialog.setVisible(true);
        } finally {
            if (owner instanceof RootPaneContainer rpc && previousGlass != null) {
                rpc.setGlassPane(previousGlass);
                previousGlass.setVisible(false);
            }
        }
        return saved;
    }

    private void attemptSave() {
        try {
            onSave.run();
            saved = true;
            dialog.dispose();
        } catch (RuntimeException ex) {
            String message = ex.getMessage() == null || ex.getMessage().isBlank()
                    ? "Não foi possível gravar. Verifique os valores e tente novamente." : ex.getMessage();
            error.setText(message);
            error.setIcon(UIHelper.icon("fas-exclamation-circle", 13, UIHelper.REJECTED_RED));
            error.setVisible(true);
            dialog.revalidate();
            dialog.repaint();
        }
    }
}
