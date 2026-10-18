package mz.multicore.erp.gui.tv;

import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Gestor de metas financeiras e comerciais por Unidade de Negócio e Global da empresa.
 * Permite selecionar quais negócios participam das metas (seleção múltipla modular),
 * definir os valores e persistir no armazenamento do sistema operacional para uso em salas de reunião.
 */
public final class BusinessUnitGoalManager {

    private static final Preferences PREFS = Preferences.userNodeForPackage(BusinessUnitGoalManager.class);
    private static final String KEY_GLOBAL = "goal_global_monthly";
    private static final String KEY_UNIT_PREFIX = "goal_unit_monthly_";
    private static final String KEY_INCLUDED_PREFIX = "goal_unit_included_";

    // Meta global padrão caso ainda não definida: 5.000.000,00 MT
    private static final BigDecimal DEFAULT_GLOBAL_GOAL = new BigDecimal("5000000.00");
    // Meta por unidade padrão: 1.000.000,00 MT
    private static final BigDecimal DEFAULT_UNIT_GOAL = new BigDecimal("1000000.00");

    private BusinessUnitGoalManager() {}

    public static BigDecimal getGlobalGoal() {
        String val = PREFS.get(KEY_GLOBAL, null);
        if (val == null || val.isBlank()) {
            return DEFAULT_GLOBAL_GOAL;
        }
        try {
            return new BigDecimal(val.trim());
        } catch (Exception e) {
            return DEFAULT_GLOBAL_GOAL;
        }
    }

    public static void setGlobalGoal(BigDecimal goal) {
        if (goal != null && goal.compareTo(BigDecimal.ZERO) >= 0) {
            PREFS.put(KEY_GLOBAL, goal.toPlainString());
        }
    }

    public static BigDecimal getGoalForUnit(Long unitId) {
        if (unitId == null) return DEFAULT_UNIT_GOAL;
        String val = PREFS.get(KEY_UNIT_PREFIX + unitId, null);
        if (val == null || val.isBlank()) {
            return DEFAULT_UNIT_GOAL;
        }
        try {
            return new BigDecimal(val.trim());
        } catch (Exception e) {
            return DEFAULT_UNIT_GOAL;
        }
    }

    public static void setGoalForUnit(Long unitId, BigDecimal goal) {
        if (unitId != null && goal != null && goal.compareTo(BigDecimal.ZERO) >= 0) {
            PREFS.put(KEY_UNIT_PREFIX + unitId, goal.toPlainString());
        }
    }

    public static boolean isUnitIncluded(Long unitId) {
        if (unitId == null) return true;
        return PREFS.getBoolean(KEY_INCLUDED_PREFIX + unitId, true);
    }

    public static void setUnitIncluded(Long unitId, boolean included) {
        if (unitId != null) {
            PREFS.putBoolean(KEY_INCLUDED_PREFIX + unitId, included);
        }
    }

    /**
     * Abre diálogo administrativo moderno para estipular e selecionar as metas de cada Unidade de Negócio.
     */
    public static void showGoalsDialog(Component parent, List<BusinessUnitDTO> units, Runnable onSaved) {
        Window parentWindow = parent instanceof Window w ? w : javax.swing.SwingUtilities.getWindowAncestor(parent);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(10, 12, 10, 12));

        JLabel info = new JLabel("Selecione os módulos de negócio que participam das metas e defina os valores mensais (MT):");
        info.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        info.setForeground(UIHelper.TEXT_MUTED);
        content.add(info);
        content.add(Box.createVerticalStrut(12));

        // 1. Meta Global da Empresa
        JTextField globalField = new JTextField(getGlobalGoal().toPlainString());
        UIHelper.styleTextField(globalField);
        globalField.setPreferredSize(new Dimension(180, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel globalCard = new ModernPanel(10);
        globalCard.setLayout(new BorderLayout(12, 4));
        globalCard.setBorder(new EmptyBorder(10, 14, 10, 14));
        JLabel globalLbl = new JLabel("Meta Global Consolidada da Empresa (MT):");
        globalLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        globalLbl.setForeground(UIHelper.TEXT_LIGHT);
        globalCard.add(globalLbl, BorderLayout.WEST);
        globalCard.add(globalField, BorderLayout.EAST);
        content.add(globalCard);
        content.add(Box.createVerticalStrut(14));

        // 2. Barra de Seleção Rápida de Negócios
        JPanel unitsHeader = new JPanel(new BorderLayout());
        unitsHeader.setOpaque(false);

        JLabel unitsTitle = new JLabel("Módulos de Negócio / Linhas de Distribuição:");
        unitsTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        unitsTitle.setForeground(UIHelper.TEXT_LIGHT);
        unitsHeader.add(unitsTitle, BorderLayout.WEST);

        JPanel quickActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        quickActions.setOpaque(false);

        ModernButton selectAllBtn = UIHelper.createSecondaryButton("Marcar Todos");
        selectAllBtn.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        selectAllBtn.setPreferredSize(new Dimension(110, 26));

        ModernButton deselectAllBtn = UIHelper.createSecondaryButton("Desmarcar Todos");
        deselectAllBtn.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        deselectAllBtn.setPreferredSize(new Dimension(120, 26));

        ModernButton sumToGlobalBtn = UIHelper.createSecondaryButton("Somar na Meta Global");
        sumToGlobalBtn.setIcon(UIHelper.icon("fas-calculator", 11, UIHelper.APPROVED_GREEN));
        sumToGlobalBtn.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        sumToGlobalBtn.setToolTipText("Calcula a Meta Global como a soma de todas as metas dos negócios selecionados");
        sumToGlobalBtn.setPreferredSize(new Dimension(180, 26));

        quickActions.add(selectAllBtn);
        quickActions.add(deselectAllBtn);
        quickActions.add(sumToGlobalBtn);
        unitsHeader.add(quickActions, BorderLayout.EAST);

        content.add(unitsHeader);
        content.add(Box.createVerticalStrut(8));

        // 3. Lista Interativa com Checkbox de Seleção por Negócio
        Map<Long, JCheckBox> unitCheckboxes = new HashMap<>();
        Map<Long, JTextField> unitFields = new HashMap<>();

        JPanel unitsList = new JPanel();
        unitsList.setLayout(new BoxLayout(unitsList, BoxLayout.Y_AXIS));
        unitsList.setOpaque(false);

        if (units != null) {
            for (BusinessUnitDTO bu : units) {
                if (!bu.active()) continue;
                JPanel row = new JPanel(new BorderLayout(10, 0));
                row.setOpaque(false);
                row.setBorder(new EmptyBorder(5, 4, 5, 4));

                boolean included = isUnitIncluded(bu.id());
                JCheckBox cb = new JCheckBox();
                cb.setSelected(included);
                cb.setOpaque(false);
                unitCheckboxes.put(bu.id(), cb);

                String labelText = bu.name();
                if (bu.supplierName() != null && !bu.supplierName().isBlank()) {
                    labelText += "  •  Fornecedor: " + bu.supplierName();
                }
                JLabel buLbl = new JLabel(labelText);
                buLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
                buLbl.setForeground(UIHelper.TEXT_LIGHT);

                JPanel leftBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
                leftBox.setOpaque(false);
                leftBox.add(cb);
                leftBox.add(buLbl);

                JTextField field = new JTextField(getGoalForUnit(bu.id()).toPlainString());
                UIHelper.styleTextField(field);
                field.setPreferredSize(new Dimension(160, UIHelper.FORM_CONTROL_HEIGHT));
                field.setEnabled(included);
                unitFields.put(bu.id(), field);

                cb.addActionListener(e -> {
                    field.setEnabled(cb.isSelected());
                    buLbl.setForeground(cb.isSelected() ? UIHelper.TEXT_LIGHT : UIHelper.TEXT_MUTED);
                });

                row.add(leftBox, BorderLayout.CENTER);
                row.add(field, BorderLayout.EAST);
                unitsList.add(row);
            }
        }

        // Ações dos botões rápidos
        selectAllBtn.addActionListener(e -> {
            unitCheckboxes.values().forEach(cb -> cb.setSelected(true));
            unitFields.values().forEach(f -> f.setEnabled(true));
        });

        deselectAllBtn.addActionListener(e -> {
            unitCheckboxes.values().forEach(cb -> cb.setSelected(false));
            unitFields.values().forEach(f -> f.setEnabled(false));
        });

        sumToGlobalBtn.addActionListener(e -> {
            BigDecimal sum = BigDecimal.ZERO;
            for (Map.Entry<Long, JTextField> entry : unitFields.entrySet()) {
                JCheckBox cb = unitCheckboxes.get(entry.getKey());
                if (cb != null && cb.isSelected()) {
                    try {
                        String text = entry.getValue().getText().trim().replace(" ", "").replace(",", ".");
                        sum = sum.add(new BigDecimal(text));
                    } catch (Exception ignored) {}
                }
            }
            globalField.setText(sum.toPlainString());
        });

        JScrollPane scroll = new JScrollPane(unitsList);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.GRID, 1, true),
                new EmptyBorder(6, 6, 6, 6)
        ));
        scroll.setPreferredSize(new Dimension(600, Math.min(280, Math.max(120, unitsList.getComponentCount() * 40 + 20))));
        content.add(scroll);

        ModernFormDialog dialog = new ModernFormDialog(parentWindow, "Configurar Metas por Módulo de Negócio", "fas-bullseye", content);

        dialog.setOnSave(() -> {
            BigDecimal gGoal;
            try {
                gGoal = new BigDecimal(globalField.getText().trim().replace(" ", "").replace(",", "."));
            } catch (Exception ex) {
                throw new IllegalArgumentException("Por favor insira um valor numérico válido para a meta global.");
            }
            setGlobalGoal(gGoal);

            for (Map.Entry<Long, JTextField> entry : unitFields.entrySet()) {
                Long unitId = entry.getKey();
                JCheckBox cb = unitCheckboxes.get(unitId);
                boolean included = cb != null && cb.isSelected();
                setUnitIncluded(unitId, included);

                try {
                    String text = entry.getValue().getText().trim().replace(" ", "").replace(",", ".");
                    BigDecimal uGoal = new BigDecimal(text);
                    setGoalForUnit(unitId, uGoal);
                } catch (Exception ex) {
                    throw new IllegalArgumentException("Valor numérico inválido numa das metas das unidades.");
                }
            }

            if (onSaved != null) {
                onSaved.run();
            }
        });

        if (dialog.showDialog()) {
            JOptionPane.showMessageDialog(parent, "Metas e módulos de negócio configurados com sucesso!", "Metas Salvas", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
