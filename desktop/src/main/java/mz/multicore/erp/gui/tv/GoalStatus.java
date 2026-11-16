package mz.multicore.erp.gui.tv;

import mz.multicore.erp.gui.components.UIHelper;

import java.awt.Color;

/**
 * Estado de uma meta de vendas. Partilhado pelo crachá, pela barra e pelos números do painel de TV,
 * para que a mesma percentagem tenha sempre a mesma cor e o mesmo texto.
 */
public enum GoalStatus {
    NO_GOAL("SEM META"),
    ALERT("ALERTA"),
    WARNING("ATENÇÃO"),
    ON_TRACK("NO RITMO"),
    ACHIEVED("META SUPERADA");

    private final String label;

    GoalStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Regra de negócio: &lt;50% alerta, 50–79,9% atenção, 80–99,9% no ritmo, ≥100% superada. */
    public static GoalStatus of(boolean hasGoal, double pct) {
        if (!hasGoal) return NO_GOAL;
        if (pct >= 100.0) return ACHIEVED;
        if (pct >= 80.0) return ON_TRACK;
        if (pct >= 50.0) return WARNING;
        return ALERT;
    }

    public Color color(boolean lightMode) {
        return switch (this) {
            case ACHIEVED -> UIHelper.APPROVED_GREEN;
            case ON_TRACK -> UIHelper.ACCENT_BLUE;
            case WARNING -> UIHelper.PENDING_YELLOW;
            case ALERT -> UIHelper.REJECTED_RED;
            case NO_GOAL -> ExecutiveTvTheme.getTextMutedColor(lightMode);
        };
    }
}
