package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.LicenseApiClient;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.licensing.dto.LicenseTermsDTO;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Gate modal da licença vigente; nenhuma chamada HTTP bloqueia o EDT. */
public class LicenseAcceptanceDialog extends JDialog {

    private final LicenseApiClient client;
    private final JTextArea contractArea = new JTextArea();
    private final JCheckBox authorityCheck = new JCheckBox(
            "Li o contrato e possuo poderes para o aceitar em nome da empresa.");
    private final ModernButton acceptButton = UIHelper.createPrimaryButton("Concordar e continuar");
    private final ModernButton declineButton = UIHelper.createDangerButton("Não concordo");
    private final JProgressBar progress = UIHelper.createBusyBar();
    private LicenseTermsDTO terms;
    private boolean accepted;

    public LicenseAcceptanceDialog(LicenseApiClient client) {
        super((Frame) null, "Contrato de Licença — Multicore", true);
        this.client = client;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(820, 680);
        setMinimumSize(new Dimension(700, 560));
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
        setIconImage(UIHelper.iconImage("fas-file-contract", 48, UIHelper.ACCENT));
        buildUi();
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent event) { loadTerms(); }
        });
    }

    public boolean isAccepted() {
        return accepted;
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        root.setBackground(UIHelper.BG_DARK);

        JLabel title = new JLabel("Contrato de Licença do Utilizador Final");
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 20));
        title.setForeground(UIHelper.TEXT_LIGHT);
        root.add(title, BorderLayout.NORTH);

        contractArea.setEditable(false);
        contractArea.setLineWrap(true);
        contractArea.setWrapStyleWord(true);
        contractArea.setText("A carregar o contrato vigente…");
        contractArea.setCaretPosition(0);
        contractArea.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JScrollPane scroll = new JScrollPane(contractArea);
        root.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(8, 8));
        footer.setOpaque(false);
        authorityCheck.setOpaque(false);
        authorityCheck.setForeground(UIHelper.TEXT_LIGHT);
        authorityCheck.setEnabled(false);
        authorityCheck.addActionListener(event -> acceptButton.setEnabled(authorityCheck.isSelected()));
        footer.add(authorityCheck, BorderLayout.NORTH);

        progress.setVisible(true);
        footer.add(progress, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        declineButton.addActionListener(event -> dispose());
        acceptButton.setEnabled(false);
        acceptButton.addActionListener(event -> acceptTerms());
        actions.add(declineButton);
        actions.add(acceptButton);
        footer.add(actions, BorderLayout.SOUTH);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void loadTerms() {
        new SwingWorker<LicenseTermsDTO, Void>() {
            @Override protected LicenseTermsDTO doInBackground() { return client.currentTerms(); }
            @Override protected void done() {
                try {
                    terms = get();
                    if (terms.accepted()) {
                        accepted = true;
                        dispose();
                        return;
                    }
                    contractArea.setText(terms.content());
                    contractArea.setCaretPosition(0);
                    authorityCheck.setEnabled(true);
                    progress.setVisible(false);
                } catch (Exception exception) {
                    showFailure(exception, "Não foi possível verificar o contrato vigente.");
                }
            }
        }.execute();
    }

    private void acceptTerms() {
        if (terms == null || !authorityCheck.isSelected()) return;
        setBusy(true);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() { client.accept(terms); return null; }
            @Override protected void done() {
                try {
                    get();
                    accepted = true;
                    dispose();
                } catch (Exception exception) {
                    setBusy(false);
                    showFailure(exception, "Não foi possível registar a aceitação.");
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        authorityCheck.setEnabled(!busy);
        acceptButton.setEnabled(!busy && authorityCheck.isSelected());
        declineButton.setEnabled(!busy);
        progress.setVisible(busy);
    }

    private void showFailure(Exception exception, String fallback) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        String message = cause.getMessage() == null || cause.getMessage().isBlank() ? fallback : cause.getMessage();
        JOptionPane.showMessageDialog(this, message, "Contrato de licença", JOptionPane.ERROR_MESSAGE);
        dispose();
    }
}
