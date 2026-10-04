package mz.multicore.erp.gui.pos.scale;

import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.modules.pos.scale.SerialScaleReader;
import mz.multicore.erp.modules.pos.scale.SerialScaleReader.ScaleReading;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * Componente visual compacto de estado e peso em direto da balança USB/Serial no POS.
 */
public class PosScaleLiveWidget extends ModernPanel {

    private final SerialScaleReader scaleReader;
    private final JLabel weightDisplayLabel;
    private final JLabel statusDotLabel;
    private final ModernButton captureButton;
    private final ModernButton tareButton;
    private final ModernButton simulateButton;

    private Consumer<BigDecimal> onWeightCapturedCallback;

    public PosScaleLiveWidget(SerialScaleReader scaleReader) {
        super(12);
        setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));
        this.scaleReader = scaleReader != null ? scaleReader : new SerialScaleReader(true);
        setBackground(UIHelper.ROW_ALT);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER, 1),
                new EmptyBorder(2, 6, 2, 6)
        ));

        statusDotLabel = new JLabel(UIHelper.icon("fas-circle", 9, UIHelper.APPROVED_GREEN));
        statusDotLabel.setToolTipText("Balança Serial COM Ligada");

        weightDisplayLabel = new JLabel("0.000 kg");
        weightDisplayLabel.setIcon(UIHelper.icon("fas-balance-scale", 13, UIHelper.ACCENT_BLUE));
        weightDisplayLabel.setIconTextGap(5);
        weightDisplayLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        weightDisplayLabel.setForeground(UIHelper.TEXT_LIGHT);

        captureButton = UIHelper.createButton("Capturar", UIHelper.icon("fas-balance-scale", 11, Color.WHITE), UIHelper.ACCENT_BLUE, e -> captureWeight());
        captureButton.setForeground(Color.WHITE);
        captureButton.setToolTipText("Capturar peso activo da balança para o artigo seleccionado");
        captureButton.setFont(new Font("Segoe UI", Font.BOLD, 11));
        captureButton.setMargin(new Insets(2, 6, 2, 6));
        captureButton.setPreferredSize(new Dimension(88, 30));

        tareButton = new ModernButton("Tarar", UIHelper.ACCENT_SKY, UIHelper.ACCENT_SKY.darker());
        tareButton.setIcon(UIHelper.icon("fas-sync", 11, Color.WHITE));
        tareButton.setForeground(Color.WHITE);
        tareButton.setFont(new Font("Segoe UI", Font.BOLD, 11));
        tareButton.setMargin(new Insets(2, 6, 2, 6));
        tareButton.addActionListener(e -> handleTare());
        tareButton.setToolTipText("Zerar / Aplicar Tara no prato da balança");
        tareButton.setPreferredSize(new Dimension(72, 30));

        simulateButton = new ModernButton("Simular", UIHelper.ACCENT_CYAN, UIHelper.ACCENT_CYAN.darker());
        simulateButton.setIcon(UIHelper.icon("fas-edit", 11, Color.WHITE));
        simulateButton.setForeground(Color.WHITE);
        simulateButton.setFont(new Font("Segoe UI", Font.BOLD, 11));
        simulateButton.setMargin(new Insets(2, 6, 2, 6));
        simulateButton.addActionListener(e -> openSimulationDialog());
        simulateButton.setToolTipText("Inserir peso simulado para testes sem balança física");
        simulateButton.setPreferredSize(new Dimension(82, 30));

        add(statusDotLabel);
        add(weightDisplayLabel);
        add(captureButton);
        add(tareButton);
        add(simulateButton);

        this.scaleReader.addListener(this::updateDisplay);
        updateDisplay(this.scaleReader.getCurrentReading());
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, UIHelper.FORM_CONTROL_HEIGHT);
    }

    public void setOnWeightCapturedCallback(Consumer<BigDecimal> callback) {
        this.onWeightCapturedCallback = callback;
    }

    public BigDecimal getCurrentWeightKg() {
        return scaleReader.getCurrentReading().weightKg();
    }

    private void captureWeight() {
        ScaleReading reading = scaleReader.getCurrentReading();
        if (reading.status() == SerialScaleReader.ScaleStatus.UNSTABLE) {
            ToastManager.show(this, FeedbackType.WARNING, "A balança está instável. Aguarde a estabilização do peso.");
            return;
        }
        if (onWeightCapturedCallback != null) {
            onWeightCapturedCallback.accept(reading.weightKg());
        }
    }

    private void handleTare() {
        if (scaleReader.getTareWeightKg().signum() > 0) {
            scaleReader.clearTare();
        } else {
            scaleReader.applyTare();
        }
        updateDisplay(scaleReader.getCurrentReading());
    }

    private void openSimulationDialog() {
        String input = ModernMessageDialog.prompt(this, FeedbackType.INFO, "Simulador de Balança",
                "Peso simulado em quilogramas (ex.: 1,450):", "", false);
        if (input != null && !input.isBlank()) {
            try {
                BigDecimal weight = new BigDecimal(input.trim().replace(',', '.'));
                scaleReader.setSimulatedWeight(weight, true);
            } catch (Exception ex) {
                ToastManager.show(this, FeedbackType.ERROR, "Valor de peso inválido.");
            }
        }
    }

    private void updateDisplay(ScaleReading reading) {
        SwingUtilities.invokeLater(() -> {
            String text = String.format("%.3f kg", reading.weightKg());
            if (scaleReader.getTareWeightKg().signum() > 0) {
                text += " (Líq.)";
            }
            weightDisplayLabel.setText(text);

            switch (reading.status()) {
                case STABLE -> {
                    statusDotLabel.setIcon(UIHelper.icon("fas-circle", 9, UIHelper.APPROVED_GREEN));
                    statusDotLabel.setToolTipText("Balança Estável");
                    weightDisplayLabel.setForeground(UIHelper.TEXT_LIGHT);
                }
                case UNSTABLE -> {
                    statusDotLabel.setIcon(UIHelper.icon("fas-circle", 9, UIHelper.PENDING_YELLOW));
                    statusDotLabel.setToolTipText("Balança Instável");
                    weightDisplayLabel.setForeground(UIHelper.PENDING_YELLOW);
                }
                case DISCONNECTED -> {
                    statusDotLabel.setIcon(UIHelper.icon("fas-circle", 9, UIHelper.REJECTED_RED));
                    statusDotLabel.setToolTipText("Balança Desconectada");
                    weightDisplayLabel.setForeground(UIHelper.REJECTED_RED);
                }
                default -> {}
            }
        });
    }
}
