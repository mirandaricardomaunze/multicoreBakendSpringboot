package mz.multicore.erp.gui.pos.scale;

import mz.multicore.erp.desktop.config.DesktopLocalSettings;
import mz.multicore.erp.desktop.config.DesktopLocalSettings.ScaleSettings;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.pos.scale.ScaleConnection;
import mz.multicore.erp.modules.pos.scale.SerialScaleReader.ScaleReading;
import mz.multicore.erp.modules.pos.scale.SerialScaleReader.ScaleStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * "Balança deste Posto" (Configurações): the serial scale of this computer. The settings live in the local file of
 * the workstation, not on the server, because each counter has its own scale and port. The live reading confirms
 * the link before anyone sells by weight; without a port the POS keeps the simulation.
 */
public class ScaleSettingsPanel extends JPanel {

    static final String[][] MODES = {{"CONTINUOUS", "Contínuo (a balança envia sempre o peso)"},
            {"POLL_W", "Por pedido, comando W"}, {"POLL_ENQ", "Por pedido, comando ENQ"}};
    private static final Integer[] BAUDS = {2400, 4800, 9600, 19200, 38400};

    private final JCheckBox enabled = new JCheckBox("Usar balança neste posto (POS e talho)");
    private final JComboBox<String> port = new JComboBox<>();
    private final JComboBox<Integer> baud = new JComboBox<>(BAUDS);
    private final JComboBox<String> mode = new JComboBox<>(java.util.Arrays.stream(MODES).map(m -> m[1]).toArray(String[]::new));
    private final JLabel live = new JLabel("—");
    private final JLabel liveStatus = new JLabel(" ");
    private final JLabel rawFrame = new JLabel(" ");
    private final Timer timer = new Timer(300, e -> refreshLive());

    public ScaleSettingsPanel() {
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(16, 16, 16, 16));
        enabled.setOpaque(false);
        enabled.setForeground(UIHelper.TEXT_LIGHT);
        port.setEditable(true);
        UIHelper.styleComboBox(port);
        UIHelper.styleComboBox(baud);
        UIHelper.styleComboBox(mode);
        ModernButton scan = UIHelper.createSecondaryButton("Procurar portas");
        scan.setIcon(UIHelper.icon("fas-search", 13));
        scan.addActionListener(e -> loadPorts());
        JPanel portRow = new JPanel(new BorderLayout(8, 0));
        portRow.setOpaque(false);
        portRow.add(port, BorderLayout.CENTER);
        portRow.add(scan, BorderLayout.EAST);

        JLabel help = new JLabel("<html><div style='width:520px'>Ligue a balança por USB ou RS-232, escolha a porta (COM3, COM4…) "
                + "e a velocidade indicada no manual. Use <b>Contínuo</b> quando a balança envia o peso sempre; se só "
                + "responde quando lhe pedem, use o comando indicado no manual. Grave e veja o peso mudar em baixo.</div></html>");
        help.setForeground(UIHelper.TEXT_MUTED);
        JPanel form = UIHelper.createDialogForm(1, new Object[]{"", help, "", enabled, "Porta série:", portRow,
                "Velocidade (baud):", baud, "Modo de leitura:", mode});

        live.setFont(new Font(UIHelper.FONT, Font.BOLD, 34));
        live.setForeground(UIHelper.TEXT_LIGHT);
        live.setIcon(UIHelper.icon("fas-balance-scale", 26, UIHelper.ACCENT_BLUE));
        live.setIconTextGap(12);
        liveStatus.setForeground(UIHelper.TEXT_MUTED);
        rawFrame.setForeground(UIHelper.TEXT_MUTED);
        rawFrame.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        rawFrame.setToolTipText("Para o teste de aceitação: o que a balança enviou, com os caracteres de controlo visíveis");
        JPanel liveBox = new JPanel(new GridLayout(3, 1, 0, 4));
        liveBox.setOpaque(false);
        liveBox.add(live);
        liveBox.add(liveStatus);
        liveBox.add(rawFrame);

        ModernButton save = UIHelper.createSuccessButton("Gravar e Ligar");
        save.setIcon(UIHelper.icon("fas-plug", 14));
        save.addActionListener(e -> save());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.setOpaque(false);
        actions.add(save);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(new EmptyBorder(18, 18, 18, 18));
        card.add(form, BorderLayout.NORTH);
        card.add(liveBox, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        add(card, BorderLayout.NORTH);
        loadSettings();
        timer.start();
    }

    private void loadSettings() {
        ScaleSettings settings = DesktopLocalSettings.scaleSettings();
        enabled.setSelected(settings.enabled());
        loadPorts();
        port.setSelectedItem(settings.port());
        baud.setSelectedItem(settings.baud());
        for (int i = 0; i < MODES.length; i++) if (MODES[i][0].equals(settings.mode())) mode.setSelectedIndex(i);
    }

    private void loadPorts() {
        Object current = port.getSelectedItem();
        List<ScaleConnection.PortInfo> ports = ScaleConnection.availablePorts();
        port.removeAllItems();
        ports.forEach(p -> port.addItem(p.name()));
        port.setSelectedItem(current);
        port.setToolTipText(ports.isEmpty() ? "Nenhuma porta série encontrada neste computador"
                : String.join("  |  ", ports.stream().map(ScaleConnection.PortInfo::label).toList()));
    }

    private void save() {
        String chosen = port.getSelectedItem() == null ? "" : port.getSelectedItem().toString().trim();
        ScaleSettings settings = new ScaleSettings(enabled.isSelected(), chosen,
                baud.getSelectedItem() == null ? 9600 : (Integer) baud.getSelectedItem(), MODES[Math.max(0, mode.getSelectedIndex())][0]);
        try {
            DesktopLocalSettings.saveScaleSettings(settings);
            ScaleConnection.shared().apply(settings);
            ToastManager.success(this, settings.enabled() && settings.hasPort()
                    ? "Balança ligada em " + settings.port() + "." : "Balança em simulação neste posto.");
        } catch (RuntimeException error) {
            ToastManager.show(this, FeedbackType.ERROR, error.getMessage());
        }
    }

    private void refreshLive() {
        if (!isShowing()) return;
        ScaleReading reading = ScaleConnection.shared().reader().getCurrentReading();
        live.setText(reading.weightKg().toPlainString().replace('.', ',') + " kg");
        boolean simulation = ScaleConnection.shared().reader().isSimulationMode();
        var reader = ScaleConnection.shared().reader();
        rawFrame.setText(simulation ? " " : "Tramas recebidas: " + reader.getFrameCount() + "   ·   última: "
                + mz.multicore.erp.modules.pos.scale.SerialScaleReader.visible(reader.getLastRawFrame()));
        liveStatus.setText(statusText(reading.status(), simulation));
        liveStatus.setForeground(reading.status() == ScaleStatus.STABLE && !simulation ? UIHelper.APPROVED_GREEN
                : reading.status() == ScaleStatus.DISCONNECTED || reading.status() == ScaleStatus.OVERLOAD
                ? UIHelper.REJECTED_RED : UIHelper.PENDING_YELLOW);
    }

    static String statusText(ScaleStatus status, boolean simulation) {
        if (simulation) return "Simulação: nenhuma balança ligada neste posto";
        return switch (status) {
            case STABLE -> "Peso estável: pronto a capturar";
            case UNSTABLE -> "A estabilizar…";
            case OVERLOAD -> "Sobrecarga: retire peso do prato";
            case DISCONNECTED -> "Sem resposta da balança: confirme cabo, porta e velocidade";
        };
    }
}
