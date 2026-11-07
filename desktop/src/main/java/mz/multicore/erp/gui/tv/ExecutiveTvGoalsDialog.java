package mz.multicore.erp.gui.tv;

import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modal "Metas por módulo de negócio" do Painel Executivo, no tema em que o painel está.
 * A gravação continua no {@link BusinessUnitGoalManager}; aqui fica só o desenho.
 */
final class ExecutiveTvGoalsDialog {

    private ExecutiveTvGoalsDialog() {}

    static void show(Component parent, List<BusinessUnitDTO> units, Runnable onSaved, boolean lightMode) {
        Window owner = parent instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent);
        ExecutiveTvDialog dialog = new ExecutiveTvDialog(owner, "Metas por Módulo de Negócio", "fas-bullseye",
                "Escolha os negócios que entram nas metas e defina os valores mensais (MT).", lightMode);

        JPanel content = ExecutiveTvComponents.transparent(null);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JTextField globalField = ExecutiveTvFormControls.textField(
                BusinessUnitGoalManager.getGlobalGoal().toPlainString(), 200, lightMode);
        JPanel globalCard = ExecutiveTvComponents.card(lightMode, new BorderLayout(12, 0), 12);
        globalCard.add(ExecutiveTvComponents.label("Meta global consolidada da empresa (MT)", Font.BOLD, 13,
                ExecutiveTvTheme.getTextLightColor(lightMode)), BorderLayout.CENTER);
        globalCard.add(globalField, BorderLayout.EAST);
        content.add(globalCard);
        content.add(Box.createVerticalStrut(16));

        Map<Long, JCheckBox> checks = new LinkedHashMap<>();
        Map<Long, JTextField> fields = new LinkedHashMap<>();
        JPanel list = ExecutiveTvComponents.card(lightMode, null, 10);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        for (BusinessUnitDTO unit : units == null ? List.<BusinessUnitDTO>of() : units) {
            if (!unit.active()) continue;
            list.add(unitRow(unit, checks, fields, lightMode));
        }
        if (fields.isEmpty()) {
            list.add(ExecutiveTvComponents.emptyState("fas-layer-group", "Sem módulos de negócio activos",
                    "Crie-os em Compras para lhes atribuir metas.", lightMode));
        }

        content.add(unitsHeader(checks, fields, globalField, lightMode));
        content.add(Box.createVerticalStrut(8));
        content.add(list);

        dialog.content(content).onSave("Gravar metas", () -> save(globalField, checks, fields, onSaved));
        if (dialog.show(760, 600)) {
            ToastManager.success(parent, "Metas e módulos de negócio gravados.");
        }
    }

    private static JPanel unitRow(BusinessUnitDTO unit, Map<Long, JCheckBox> checks,
                                  Map<Long, JTextField> fields, boolean lightMode) {
        boolean included = BusinessUnitGoalManager.isUnitIncluded(unit.id());
        JCheckBox check = ExecutiveTvFormControls.checkBox(included, lightMode);
        String text = unit.name() + (unit.supplierName() == null || unit.supplierName().isBlank()
                ? "" : "  ·  Fornecedor: " + unit.supplierName());
        JLabel name = ExecutiveTvComponents.label(text, Font.PLAIN, 13, color(included, lightMode));
        JTextField field = ExecutiveTvFormControls.textField(
                BusinessUnitGoalManager.getGoalForUnit(unit.id()).toPlainString(), 170, lightMode);
        field.setEnabled(included);
        check.addActionListener(e -> {
            field.setEnabled(check.isSelected());
            name.setForeground(color(check.isSelected(), lightMode));
        });
        checks.put(unit.id(), check);
        fields.put(unit.id(), field);

        JPanel left = ExecutiveTvComponents.transparent(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.add(check);
        left.add(name);
        JPanel row = ExecutiveTvComponents.transparent(new BorderLayout(10, 0));
        row.setBorder(new EmptyBorder(5, 2, 5, 2));
        row.add(left, BorderLayout.CENTER);
        row.add(field, BorderLayout.EAST);
        return row;
    }

    private static JPanel unitsHeader(Map<Long, JCheckBox> checks, Map<Long, JTextField> fields,
                                      JTextField globalField, boolean lightMode) {
        JPanel header = ExecutiveTvComponents.transparent(new BorderLayout());
        header.add(ExecutiveTvComponents.overline("Módulos de negócio", lightMode), BorderLayout.WEST);
        JPanel actions = ExecutiveTvComponents.transparent(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        Runnable all = () -> setAll(checks, fields, true);
        Runnable none = () -> setAll(checks, fields, false);
        Runnable sum = () -> globalField.setText(sumIncluded(checks, fields).toPlainString());
        actions.add(ExecutiveTvComponents.ghostButton("Marcar todos", "fas-check-double",
                ExecutiveTvTheme.getTextMutedColor(lightMode), all, lightMode));
        actions.add(ExecutiveTvComponents.ghostButton("Desmarcar todos", "fas-minus-square",
                ExecutiveTvTheme.getTextMutedColor(lightMode), none, lightMode));
        ModernButton sumButton = ExecutiveTvComponents.ghostButton("Somar na meta global", "fas-calculator",
                UIHelper.APPROVED_GREEN, sum, lightMode);
        sumButton.setToolTipText("A meta global passa a ser a soma das metas dos negócios marcados");
        actions.add(sumButton);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private static void setAll(Map<Long, JCheckBox> checks, Map<Long, JTextField> fields, boolean on) {
        checks.values().forEach(check -> { if (check.isSelected() != on) check.doClick(0); });
        fields.values().forEach(field -> field.setEnabled(on));
    }

    private static BigDecimal sumIncluded(Map<Long, JCheckBox> checks, Map<Long, JTextField> fields) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Map.Entry<Long, JTextField> entry : fields.entrySet()) {
            JCheckBox check = checks.get(entry.getKey());
            if (check == null || !check.isSelected()) continue;
            try {
                sum = sum.add(new BigDecimal(MoneyField.normalize(entry.getValue().getText().trim())));
            } catch (RuntimeException ignored) {
                // Valor por preencher: não entra na soma; a gravação é que recusa valores inválidos.
            }
        }
        return sum;
    }

    private static void save(JTextField globalField, Map<Long, JCheckBox> checks,
                             Map<Long, JTextField> fields, Runnable onSaved) {
        BigDecimal global;
        try {
            global = new BigDecimal(MoneyField.normalize(globalField.getText()));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Indique um valor numérico válido para a meta global.");
        }
        Map<Long, BigDecimal> unitGoals = new LinkedHashMap<>();
        for (Map.Entry<Long, JTextField> entry : fields.entrySet()) {
            try {
                unitGoals.put(entry.getKey(), new BigDecimal(MoneyField.normalize(entry.getValue().getText().trim())));
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("Há uma meta de módulo com valor inválido.");
            }
        }
        // Valida tudo antes de gravar: um erro a meio não deixa metas meio gravadas.
        BusinessUnitGoalManager.setGlobalGoal(global);
        unitGoals.forEach((unitId, goal) -> {
            JCheckBox check = checks.get(unitId);
            BusinessUnitGoalManager.setUnitIncluded(unitId, check != null && check.isSelected());
            BusinessUnitGoalManager.setGoalForUnit(unitId, goal);
        });
        if (onSaved != null) onSaved.run();
    }

    private static java.awt.Color color(boolean included, boolean lightMode) {
        return included ? ExecutiveTvTheme.getTextLightColor(lightMode) : ExecutiveTvTheme.getTextMutedColor(lightMode);
    }
}
