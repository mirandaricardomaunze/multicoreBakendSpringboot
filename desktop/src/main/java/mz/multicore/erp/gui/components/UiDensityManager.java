package mz.multicore.erp.gui.components;

import javax.swing.JTable;
import javax.swing.UIManager;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * Gestor thread-safe de Densidade de Interface e Escala do Multicore ERP.
 * Permite comutar a densidade de linhas e controlos dinamicamente e persiste a escolha do operador.
 */
public class UiDensityManager {

    private static final Logger LOGGER = Logger.getLogger(UiDensityManager.class.getName());
    private static final Preferences PREFS = Preferences.userRoot().node("mz/multicore/erp/ui");
    private static final UiDensityManager INSTANCE = new UiDensityManager();

    private UiDensity currentDensity = UiDensity.STANDARD;
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private UiDensityManager() {
        loadAndApplySavedDensity();
    }

    public static UiDensityManager getInstance() {
        return INSTANCE;
    }

    public synchronized UiDensity getDensity() {
        return currentDensity;
    }

    public synchronized void setDensity(UiDensity density) {
        if (density == null || density == currentDensity) {
            return;
        }
        this.currentDensity = density;
        PREFS.put("density", density.getId());
        applyDensityToSystem(density);
        notifyListeners();
        restyleAllWindows();
    }

    public synchronized UiDensity cycleDensity() {
        UiDensity next = switch (currentDensity) {
            case STANDARD -> UiDensity.COMPACT;
            case COMPACT -> UiDensity.COMFORTABLE;
            case COMFORTABLE -> UiDensity.STANDARD;
        };
        setDensity(next);
        return next;
    }

    public synchronized void loadAndApplySavedDensity() {
        String savedId = PREFS.get("density", UiDensity.STANDARD.getId());
        this.currentDensity = UiDensity.byId(savedId);
        applyDensityToSystem(this.currentDensity);
    }

    public void applyDensityToSystem(UiDensity density) {
        if (density == null) return;
        UIManager.put("Table.rowHeight", density.getTableRowHeight());
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.remove(listener);
        }
    }

    private void notifyListeners() {
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Erro ao notificar ouvinte de densidade: " + ex.getMessage(), ex);
            }
        }
    }

    private void restyleAllWindows() {
        try {
            int newRowHeight = currentDensity.getTableRowHeight();
            for (Window window : Window.getWindows()) {
                if (window.isShowing()) {
                    updateTablesInTree(window, newRowHeight);
                    window.revalidate();
                    window.repaint();
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Falha ao atualizar janelas para nova densidade: " + ex.getMessage(), ex);
        }
    }

    private void updateTablesInTree(Component comp, int rowHeight) {
        if (comp instanceof JTable table) {
            table.setRowHeight(rowHeight);
        }
        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                updateTablesInTree(child, rowHeight);
            }
        }
    }
}
