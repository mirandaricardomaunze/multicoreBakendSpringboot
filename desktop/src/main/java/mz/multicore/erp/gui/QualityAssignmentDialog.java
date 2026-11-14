package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ButtonStyles;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.HRApiClient;
import mz.multicore.erp.desktop.client.QualityApiClient;
import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.PrintPreviewDialog;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import mz.multicore.erp.modules.quality.dto.EquipmentAssignmentRequest;
import mz.multicore.erp.modules.quality.dto.EquipmentDTO;
import mz.multicore.erp.modules.quality.dto.PeriodUnit;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class QualityAssignmentDialog extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final QualityApiClient qualityApiClient;
    private final HRApiClient hrApiClient;
    private final Runnable onSuccess;

    private final JComboBox<EmployeeDTO> employeeCombo = new JComboBox<>();
    private final JComboBox<EquipmentDTO> equipmentCombo = new JComboBox<>();
    private final DateField assignedDateField = new DateField(LocalDate.now());
    private final JLabel calculatedExpiryLabel = new JLabel("-");
    private final JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
    private final JTextField sizeOrSpecField = new JTextField();
    private final JTextField serialNumberField = new JTextField();
    private final JTextArea notesArea = new JTextArea(2, 20);
    private final JCheckBox printSheetCheck = new JCheckBox("Imprimir Termo de Entrega (EPI) após gravar", true);

    private List<EmployeeDTO> employees = new ArrayList<>();
    private List<EquipmentDTO> equipments = new ArrayList<>();

    public QualityAssignmentDialog(Window owner, QualityApiClient qualityApiClient, HRApiClient hrApiClient, Runnable onSuccess) {
        this(owner, qualityApiClient, hrApiClient, onSuccess, null, null);
    }

    public QualityAssignmentDialog(Window owner, QualityApiClient qualityApiClient, HRApiClient hrApiClient, Runnable onSuccess,
                                   List<EquipmentDTO> presetEquipments, List<EmployeeDTO> presetEmployees) {
        super(owner, "Atribuir Equipamento / EPI a Trabalhador", ModalityType.APPLICATION_MODAL);
        this.qualityApiClient = qualityApiClient;
        this.hrApiClient = hrApiClient;
        this.onSuccess = onSuccess;

        initComponents();

        if (presetEquipments != null) {
            this.equipments = presetEquipments;
            for (EquipmentDTO eq : presetEquipments) equipmentCombo.addItem(eq);
        }
        if (presetEmployees != null) {
            this.employees = presetEmployees;
            for (EmployeeDTO emp : presetEmployees) employeeCombo.addItem(emp);
        }

        if ((presetEquipments == null || presetEquipments.isEmpty()) && (hrApiClient != null || qualityApiClient != null)) {
            loadData();
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
        UIHelper.styleComboBox(employeeCombo);
        UIHelper.styleComboBox(equipmentCombo);
        UIHelper.styleTextField(assignedDateField);
        UIHelper.styleTextField(sizeOrSpecField);
        UIHelper.styleTextField(serialNumberField);

        sizeOrSpecField.putClientProperty("JTextField.placeholderText", "Ex: Tam. 42, XL, L, 50mm...");
        serialNumberField.putClientProperty("JTextField.placeholderText", "Ex: Lote ou Nº de série do fabricante (opcional)");

        calculatedExpiryLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        calculatedExpiryLabel.setForeground(UIHelper.TEXT_MUTED);
        calculatedExpiryLabel.setText("Aguardando selecção de equipamento...");

        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER));

        employeeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object val, int idx, boolean isSel, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, val, idx, isSel, cellHasFocus);
                if (val instanceof EmployeeDTO emp) {
                    setText(emp.name() + (emp.department() != null ? " (" + emp.department() + ")" : ""));
                } else if (val == null) {
                    setText("- Seleccione o Colaborador -");
                }
                return this;
            }
        });

        equipmentCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object val, int idx, boolean isSel, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, val, idx, isSel, cellHasFocus);
                if (val instanceof EquipmentDTO eq) {
                    setText(eq.code() + " - " + eq.name() + " [Validade: " + eq.periodValue() + " " + eq.periodUnitLabel() + "]");
                } else if (val == null) {
                    setText(equipments.isEmpty() ? "- Nenhum equipamento cadastrado (clique [+] para criar) -" : "- Seleccione o Equipamento -");
                }
                return this;
            }
        });

        JPanel eqComboPanel = new JPanel(new BorderLayout(6, 0));
        eqComboPanel.setOpaque(false);
        eqComboPanel.add(equipmentCombo, BorderLayout.CENTER);

        JButton addEqBtn = new JButton(UIHelper.icon("fas-plus", 12, UIHelper.ACCENT_BLUE));
        addEqBtn.setToolTipText("Registar novo equipamento no catálogo");
        addEqBtn.setPreferredSize(new Dimension(34, UIHelper.FORM_CONTROL_HEIGHT));
        addEqBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addEqBtn.addActionListener(e -> {
            QualityEquipmentFormDialog eqDialog = new QualityEquipmentFormDialog(this, qualityApiClient, null, this::loadData);
            eqDialog.setVisible(true);
        });
        eqComboPanel.add(addEqBtn, BorderLayout.EAST);

        equipmentCombo.addActionListener(e -> updateCalculatedExpiry());
        assignedDateField.addActionListener(e -> updateCalculatedExpiry());
        assignedDateField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { updateCalculatedExpiry(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { updateCalculatedExpiry(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { updateCalculatedExpiry(); }
        });

        ModernPanel card = new ModernPanel(14);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        JPanel qtyAndSpecPanel = new JPanel(new GridBagLayout());
        qtyAndSpecPanel.setOpaque(false);
        GridBagConstraints qg = new GridBagConstraints();
        qg.fill = GridBagConstraints.HORIZONTAL;
        qg.insets = new Insets(0, 0, 0, 8);

        qg.gridx = 0; qg.weightx = 0.22;
        quantitySpinner.setPreferredSize(new Dimension(75, UIHelper.FORM_CONTROL_HEIGHT));
        qtyAndSpecPanel.add(quantitySpinner, qg);

        qg.gridx = 1; qg.weightx = 0.28;
        JLabel specLbl = new JLabel("Tamanho / Calibre:");
        specLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        specLbl.setForeground(UIHelper.TEXT_LIGHT);
        qtyAndSpecPanel.add(specLbl, qg);

        qg.gridx = 2; qg.weightx = 0.50;
        qg.insets = new Insets(0, 0, 0, 0);
        qtyAndSpecPanel.add(sizeOrSpecField, qg);

        int row = 0;
        addFormRow(card, g, row++, "Colaborador (RH):", employeeCombo);
        addFormRow(card, g, row++, "Equipamento a Atribuir:", eqComboPanel);
        addFormRow(card, g, row++, "Data de Entrega:", assignedDateField);
        addFormRow(card, g, row++, "Data Prevista de Expiração:", calculatedExpiryLabel);
        addFormRow(card, g, row++, "Quantidade & Tamanho:", qtyAndSpecPanel);
        addFormRow(card, g, row++, "Nº Série (se aplicável):", serialNumberField);
        addFormRow(card, g, row++, "Observações da Entrega:", notesScroll);

        printSheetCheck.setOpaque(false);
        addFormRow(card, g, row++, "", printSheetCheck);

        content.add(card);
        add(content, BorderLayout.CENTER);

        // Rodapé
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        footer.setBackground(UIHelper.BG_DARK);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIHelper.BORDER));

        ModernButton cancelBtn = ButtonStyles.cancelButton();
        cancelBtn.addActionListener(e -> dispose());
        ModernButton saveBtn = ButtonStyles.main(UIHelper.createPrimaryButton("Gravar Atribuição"));
        saveBtn.addActionListener(e -> save());

        footer.add(cancelBtn);
        footer.add(saveBtn);
        add(footer, BorderLayout.SOUTH);

        pack();
        UIHelper.applyStandardModalBounds(this, 580, 560);
    }

    private void addFormRow(JPanel p, GridBagConstraints g, int row, String labelText, Component field) {
        g.gridx = 0;
        g.gridy = row;
        g.weightx = 0.35;
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        lbl.setForeground(UIHelper.TEXT_LIGHT);
        p.add(lbl, g);

        g.gridx = 1;
        g.weightx = 0.65;
        p.add(field, g);
    }

    private void loadData() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) companyId = 1L;
        final Long cid = companyId;

        UIHelper.runWithProgress(this, "A carregar colaboradores e equipamentos...", () -> {
            List<EmployeeDTO> emps = hrApiClient != null ? hrApiClient.getAllEmployees() : List.of();
            List<EquipmentDTO> eqs = qualityApiClient != null ? qualityApiClient.listActiveEquipments(cid) : List.of();
            return new Object[]{emps, eqs};
        }, res -> {
            Object[] data = (Object[]) res;
            employees = (List<EmployeeDTO>) data[0];
            equipments = (List<EquipmentDTO>) data[1];

            employeeCombo.removeAllItems();
            for (EmployeeDTO emp : employees) {
                employeeCombo.addItem(emp);
            }

            equipmentCombo.removeAllItems();
            for (EquipmentDTO eq : equipments) {
                equipmentCombo.addItem(eq);
            }
            updateCalculatedExpiry();
        }, err -> {
            ToastManager.show(this, FeedbackType.ERROR, "Erro ao carregar dados: " + err.getMessage());
        });
    }

    private void updateCalculatedExpiry() {
        EquipmentDTO eq = (EquipmentDTO) equipmentCombo.getSelectedItem();
        if (eq == null) {
            calculatedExpiryLabel.setText("Aguardando selecção de equipamento...");
            calculatedExpiryLabel.setForeground(UIHelper.TEXT_MUTED);
            return;
        }
        LocalDate date = assignedDateField.getDate();
        if (date == null) date = LocalDate.now();

        LocalDate expiry = calculateExpiryDate(date, eq.periodValue(), eq.periodUnit());
        if (expiry != null) {
            calculatedExpiryLabel.setText(expiry.format(DATE_FMT) + " (Válido por " + eq.periodValue() + " " + eq.periodUnitLabel() + ")");
            calculatedExpiryLabel.setForeground(UIHelper.APPROVED_GREEN);
        } else {
            calculatedExpiryLabel.setText("-");
            calculatedExpiryLabel.setForeground(UIHelper.TEXT_MUTED);
        }
    }

    public static LocalDate calculateExpiryDate(LocalDate assignedDate, int periodValue, PeriodUnit periodUnit) {
        if (assignedDate == null) return null;
        if (periodUnit == null || periodUnit == PeriodUnit.MONTHS) {
            return assignedDate.plusMonths(periodValue);
        } else {
            return assignedDate.plusDays(periodValue);
        }
    }

    private void save() {
        EmployeeDTO emp = (EmployeeDTO) employeeCombo.getSelectedItem();
        EquipmentDTO eq = (EquipmentDTO) equipmentCombo.getSelectedItem();

        if (emp == null) {
            ToastManager.show(this, FeedbackType.WARNING, "Por favor selecione um colaborador.");
            employeeCombo.requestFocus();
            return;
        }
        if (eq == null) {
            ToastManager.show(this, FeedbackType.WARNING, "Por favor selecione o equipamento a atribuir.");
            equipmentCombo.requestFocus();
            return;
        }

        LocalDate delivery = assignedDateField.getDate();
        if (delivery == null) {
            ToastManager.show(this, FeedbackType.WARNING, "Selecione uma data de entrega válida no calendário.");
            assignedDateField.requestFocus();
            return;
        }

        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) companyId = 1L;
        final Long cid = companyId;
        int qty = ((Number) quantitySpinner.getValue()).intValue();
        String size = sizeOrSpecField.getText().trim();
        String sn = serialNumberField.getText().trim();
        String notes = notesArea.getText().trim();

        EquipmentAssignmentRequest req = new EquipmentAssignmentRequest(
                companyId, eq.id(), emp.id(), delivery, qty, size, sn, notes
        );

        if (qualityApiClient == null) {
            dispose();
            if (onSuccess != null) onSuccess.run();
            return;
        }

        Window parent = getOwner();
        boolean print = printSheetCheck.isSelected();

        UIHelper.runWithProgress(this, "A registar atribuição...", () -> {
            return qualityApiClient.assignEquipment(req);
        }, saved -> {
            dispose();
            if (onSuccess != null) onSuccess.run();
            if (print) {
                UIHelper.runWithProgress(parent, "A gerar Termo de Entrega em PDF...", () -> {
                    return qualityApiClient.getDeliverySheetPdf(saved.id(), cid);
                }, pdf -> {
                    PrintPreviewDialog.show(parent, pdf, "termo-entrega-" + saved.id(), "Termo de Entrega de Equipamento / EPI");
                }, err -> ToastManager.show(parent, FeedbackType.ERROR, "Erro ao gerar Termo de Entrega: " + err.getMessage()));
            }
        }, err -> {
            ToastManager.show(this, FeedbackType.ERROR, "Erro ao atribuir: " + err.getMessage());
        });
    }
}
