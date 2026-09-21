package mz.multicore.erp.gui.performance;

import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.FormField;
import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.performance.model.BonusType;
import mz.multicore.erp.modules.performance.model.GoalPeriod;
import mz.multicore.erp.modules.performance.model.GoalScope;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.function.Function;

/**
 * Fábrica e utilitário centralizado de controlos visuais (Inputs e Selects) para o Centro de Desempenho Comercial.
 * Garante reusabilidade, altura unificada (38px), tipografia consistente e renderização profissional em PT-MZ.
 */
public final class PerformanceControls {

    public static final int CONTROL_HEIGHT = UIHelper.FORM_CONTROL_HEIGHT; // 38px
    public static final int DEFAULT_INPUT_WIDTH = 220;
    public static final int FILTER_SELECT_WIDTH = 160;

    private PerformanceControls() {}

    /** Cria um JComboBox tipado reutilizável com altura uniforme de 38px e estilo padronizado. */
    public static <T> JComboBox<T> createSelect(T[] items, int preferredWidth, Function<T, String> labelMapper) {
        JComboBox<T> combo = new JComboBox<>(items);
        if (labelMapper != null) {
            combo.setRenderer(UIHelper.labelRenderer(labelMapper));
        }
        UIHelper.styleComboBox(combo);
        unifyControlSize(combo, preferredWidth);
        return combo;
    }

    public static <T> JComboBox<T> createSelect(T[] items, int preferredWidth) {
        return createSelect(items, preferredWidth, null);
    }

    public static <T> JComboBox<T> createSelect(T[] items) {
        return createSelect(items, DEFAULT_INPUT_WIDTH, null);
    }

    /** Cria um JTextField reutilizável com altura uniforme de 38px e estilo padronizado. */
    public static JTextField createTextInput(String initialValue, String placeholder, int preferredWidth) {
        JTextField field = new JTextField(initialValue != null ? initialValue : "");
        UIHelper.styleTextField(field);
        if (placeholder != null && !placeholder.isBlank()) {
            field.setToolTipText(placeholder);
            field.getAccessibleContext().setAccessibleDescription(placeholder);
        }
        unifyControlSize(field, preferredWidth);
        return field;
    }

    public static JTextField createTextInput(String placeholder) {
        return createTextInput("", placeholder, DEFAULT_INPUT_WIDTH);
    }

    /** Cria um DateField reutilizável com altura uniforme de 38px e formato ISO. */
    public static DateField createDateField(LocalDate initialDate, int preferredWidth) {
        DateField field = new DateField(initialDate);
        unifyControlSize(field, preferredWidth);
        return field;
    }

    public static DateField createDateField(LocalDate initialDate) {
        return createDateField(initialDate, DEFAULT_INPUT_WIDTH);
    }

    /** Cria um MoneyField reutilizável com altura uniforme de 38px e formatação monetária. */
    public static MoneyField createMoneyField(String initialValue, int preferredWidth) {
        MoneyField field = new MoneyField(initialValue != null ? initialValue : "");
        unifyControlSize(field, preferredWidth);
        return field;
    }

    public static MoneyField createMoneyField(String initialValue) {
        return createMoneyField(initialValue, DEFAULT_INPUT_WIDTH);
    }

    /** Cria um grupo de filtro de toolbar (Label + Select/Input) perfeitamente alinhado verticalmente. */
    public static JPanel createFilterGroup(String labelText, JComponent control) {
        JPanel group = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        group.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        label.setForeground(UIHelper.TEXT_LIGHT);
        group.add(label);
        group.add(control);
        return group;
    }

    /** Força o controlo a ter exatamente a altura padrão (38px) e a largura indicada. */
    public static void unifyControlSize(JComponent component, int preferredWidth) {
        int width = preferredWidth > 0 ? preferredWidth : component.getPreferredSize().width;
        Dimension d = new Dimension(width, CONTROL_HEIGHT);
        component.setPreferredSize(d);
        component.setMinimumSize(new Dimension(preferredWidth > 0 ? Math.min(preferredWidth, 60) : 0, CONTROL_HEIGHT));
        if (preferredWidth > 0) {
            component.setMaximumSize(new Dimension(preferredWidth, CONTROL_HEIGHT));
        }
    }

    public static String humanPeriod(GoalPeriod period) {
        if (period == null) return "Todos";
        return switch (period) {
            case MONTHLY -> "Mensal";
            case QUARTERLY -> "Trimestral";
            case SEMIANNUAL -> "Semestral";
            case ANNUAL -> "Anual";
        };
    }

    public static String humanScope(GoalScope scope) {
        if (scope == null) return "Todos";
        return switch (scope) {
            case COMPANY -> "Empresa Inteira";
            case WAREHOUSE -> "Por Armazém";
            case CATEGORY -> "Por Categoria";
            case PRODUCT -> "Por Produto";
            case EMPLOYEE -> "Por Vendedor / Trabalhador";
        };
    }

    public static String humanBonusType(BonusType type) {
        if (type == null) return "-";
        return switch (type) {
            case FIXED -> "Valor Fixo (MZN)";
            case PERCENTAGE_OF_REVENUE -> "Percentual da Receita (%)";
            case PERCENTAGE_OF_MARGIN -> "Percentual da Margem Bruta (%)";
        };
    }
}
