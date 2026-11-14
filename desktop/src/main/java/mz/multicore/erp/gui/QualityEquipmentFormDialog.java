package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ButtonStyles;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.QualityApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.quality.dto.EquipmentCategory;
import mz.multicore.erp.modules.quality.dto.EquipmentCreateRequest;
import mz.multicore.erp.modules.quality.dto.EquipmentDTO;
import mz.multicore.erp.modules.quality.dto.PeriodUnit;

import javax.swing.*;
import java.awt.*;

public class QualityEquipmentFormDialog extends JDialog {

    private final QualityApiClient qualityApiClient;
    private final EquipmentDTO editingEquipment;
    private final Runnable onSuccess;

    private final JTextField codeField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JComboBox<EquipmentCategory> categoryCombo = new JComboBox<>(EquipmentCategory.values());
    private final JSpinner periodValueSpinner = new JSpinner(new SpinnerNumberModel(6, 1, 365, 1));
    private final JComboBox<PeriodUnit> periodUnitCombo = new JComboBox<>(PeriodUnit.values());
    private final JSpinner warningDaysSpinner = new JSpinner(new SpinnerNumberModel(15, 1, 90, 1));
    private final JCheckBox activeCheck = new JCheckBox("Equipamento ativo para novas atribuições", true);
    private final JTextArea descriptionArea = new JTextArea(3, 20);

    public QualityEquipmentFormDialog(Window owner, QualityApiClient qualityApiClient) {
        this(owner, qualityApiClient, null, null);
    }

    public QualityEquipmentFormDialog(Window owner, QualityApiClient qualityApiClient, EquipmentDTO editingEquipment, Runnable onSuccess) {
        super(owner, editingEquipment == null ? "Registar Novo Equipamento / EPI" : "Editar Equipamento", ModalityType.APPLICATION_MODAL);
        this.qualityApiClient = qualityApiClient;
        this.editingEquipment = editingEquipment;
        this.onSuccess = onSuccess;

        initComponents();
        if (editingEquipment != null) {
            populateFields(editingEquipment);
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIHelper.BG_DARK);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        content.setOpaque(false);

        // Estilos
        UIHelper.styleTextField(codeField);
        UIHelper.styleTextField(nameField);
        UIHelper.styleComboBox(categoryCombo);
        UIHelper.styleComboBox(periodUnitCombo);
        activeCheck.setOpaque(false);
        activeCheck.setForeground(UIHelper.TEXT_LIGHT);
        activeCheck.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));

        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER));

        categoryCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object val, int idx, boolean isSel, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, val, idx, isSel, cellHasFocus);
                if (val instanceof EquipmentCategory cat) {
                    setText(cat.getLabel());
                }
                return this;
            }
        });

        periodUnitCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object val, int idx, boolean isSel, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, val, idx, isSel, cellHasFocus);
                if (val instanceof PeriodUnit u) {
                    setText(u.getLabel());
                }
                return this;
            }
        });

        // Formulário em grid
        ModernPanel card = new ModernPanel(14);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addFormRow(card, g, row++, "Código / Referência:", codeField);
        addFormRow(card, g, row++, "Nome do Equipamento:", nameField);
        addFormRow(card, g, row++, "Categoria:", categoryCombo);

        // Linha de prazo
        JPanel periodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        periodPanel.setOpaque(false);
        periodValueSpinner.setPreferredSize(new Dimension(80, UIHelper.FORM_CONTROL_HEIGHT));
        periodPanel.add(periodValueSpinner);
        periodUnitCombo.setPreferredSize(new Dimension(110, UIHelper.FORM_CONTROL_HEIGHT));
        periodPanel.add(periodUnitCombo);
        addFormRow(card, g, row++, "Prazo de Validade:", periodPanel);

        // Linha de alerta
        JPanel warningPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        warningPanel.setOpaque(false);
        warningDaysSpinner.setPreferredSize(new Dimension(80, UIHelper.FORM_CONTROL_HEIGHT));
        warningPanel.add(warningDaysSpinner);
        JLabel daysLabel = new JLabel("dias antes da expiração");
        daysLabel.setForeground(UIHelper.TEXT_MUTED);
        warningPanel.add(daysLabel);
        addFormRow(card, g, row++, "Aviso Preventivo:", warningPanel);

        addFormRow(card, g, row++, "Estado:", activeCheck);
        addFormRow(card, g, row++, "Instruções / Descrição:", descScroll);

        content.add(card);
        add(content, BorderLayout.CENTER);

        // Rodapé de botões
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        footer.setBackground(UIHelper.BG_DARK);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIHelper.BORDER));

        ModernButton cancelBtn = ButtonStyles.cancelButton();
        cancelBtn.addActionListener(e -> dispose());
        ModernButton saveBtn = UIHelper.createPrimaryButton(editingEquipment == null ? "Gravar Equipamento" : "Actualizar");
        saveBtn.addActionListener(e -> save());

        footer.add(cancelBtn);
        footer.add(saveBtn);
        add(footer, BorderLayout.SOUTH);

        pack();
        UIHelper.applyStandardModalBounds(this, 560, 520);
    }

    private void addFormRow(JPanel p, GridBagConstraints g, int row, String labelText, Component field) {
        g.gridx = 0;
        g.gridy = row;
        g.weightx = 0.3;
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        lbl.setForeground(UIHelper.TEXT_LIGHT);
        p.add(lbl, g);

        g.gridx = 1;
        g.weightx = 0.7;
        p.add(field, g);
    }

    private void populateFields(EquipmentDTO eq) {
        codeField.setText(eq.code());
        nameField.setText(eq.name());
        if (eq.category() != null) categoryCombo.setSelectedItem(eq.category());
        periodValueSpinner.setValue(Math.max(1, eq.periodValue()));
        if (eq.periodUnit() != null) periodUnitCombo.setSelectedItem(eq.periodUnit());
        warningDaysSpinner.setValue(Math.max(1, eq.warningDaysBefore()));
        activeCheck.setSelected(eq.active());
        descriptionArea.setText(eq.description() != null ? eq.description() : "");
    }

    private void save() {
        String code = codeField.getText().trim();
        String name = nameField.getText().trim();

        if (code.isBlank()) {
            ToastManager.show(this, FeedbackType.WARNING, "Por favor indique o código do equipamento.");
            codeField.requestFocus();
            return;
        }
        if (name.isBlank()) {
            ToastManager.show(this, FeedbackType.WARNING, "Por favor indique o nome do equipamento.");
            nameField.requestFocus();
            return;
        }

        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) companyId = 1L;
        int periodVal = ((Number) periodValueSpinner.getValue()).intValue();
        PeriodUnit periodUnit = (PeriodUnit) periodUnitCombo.getSelectedItem();
        EquipmentCategory category = (EquipmentCategory) categoryCombo.getSelectedItem();
        int warningDays = ((Number) warningDaysSpinner.getValue()).intValue();
        boolean active = activeCheck.isSelected();
        String desc = descriptionArea.getText().trim();

        EquipmentCreateRequest req = new EquipmentCreateRequest(
                companyId, code, name, category, periodVal, periodUnit, warningDays, active, desc
        );

        if (qualityApiClient == null) {
            dispose();
            if (onSuccess != null) onSuccess.run();
            return;
        }

        UIHelper.runWithProgress(this, "A gravar equipamento...", () -> {
            if (editingEquipment == null) {
                return qualityApiClient.createEquipment(req);
            } else {
                return qualityApiClient.updateEquipment(editingEquipment.id(), req);
            }
        }, saved -> {
            dispose();
            if (onSuccess != null) onSuccess.run();
        }, err -> {
            ToastManager.show(this, FeedbackType.ERROR, "Erro ao gravar: " + err.getMessage());
        });
    }
}
