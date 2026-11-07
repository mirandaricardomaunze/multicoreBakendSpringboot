package mz.multicore.erp.gui.tv;

import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;

import java.awt.Component;
import java.math.BigDecimal;
import java.util.List;
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
     * Abre o modal das metas <b>no tema actual do Painel Executivo</b> (ver {@link ExecutiveTvGoalsDialog}).
     */
    public static void showGoalsDialog(Component parent, List<BusinessUnitDTO> units, Runnable onSaved, boolean lightMode) {
        ExecutiveTvGoalsDialog.show(parent, units, onSaved, lightMode);
    }
}
