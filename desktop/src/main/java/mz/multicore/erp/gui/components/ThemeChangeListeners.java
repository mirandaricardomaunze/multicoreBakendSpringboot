package mz.multicore.erp.gui.components;

import java.awt.EventQueue;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Quem precisa de saber que o tema da aplicação mudou. Para janelas que <b>não</b> são reconstruídas
 * com a janela principal, como o Painel Executivo: sem isto, trocar de tema fechava-o (era filho da
 * janela principal, que é descartada) ou deixava-o com as cores antigas.
 */
public final class ThemeChangeListeners {

    private static final List<Consumer<Theme>> LISTENERS = new CopyOnWriteArrayList<>();

    private ThemeChangeListeners() {}

    public static void add(Consumer<Theme> listener) {
        if (listener != null) LISTENERS.add(listener);
    }

    public static void remove(Consumer<Theme> listener) {
        LISTENERS.remove(listener);
    }

    /** Avisa no EDT, depois da reconstrução da janela principal (que também é agendada no EDT). */
    static void fire(Theme theme) {
        for (Consumer<Theme> listener : LISTENERS) {
            EventQueue.invokeLater(() -> listener.accept(theme));
        }
    }

    static int count() {
        return LISTENERS.size();
    }
}
