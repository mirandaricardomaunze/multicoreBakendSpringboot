package mz.multicore.erp.gui.components;

import java.awt.Component;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * One way to tell the operator that an action failed (docs/MENSAGENS_ERRO_SPEC.md).
 *
 * <p>Always "Não foi possível {acção}." followed by the reason when the reason is meant for people (server
 * answers, business rules, validation), or by what to do next when it is a programming error, which is
 * logged instead of shown. Before, the same situation read "Erro ao…", "Falha ao…" or "Não foi possível…"
 * depending on the screen, and a {@code NullPointerException} could reach the operator as "null".
 */
public final class UserErrors {

    private static final Logger LOG = Logger.getLogger(UserErrors.class.getName());
    static final String TECHNICAL_HINT = "Tente novamente; se o problema continuar, contacte o suporte.";

    /** Programming errors: their text means nothing to the operator. */
    private static final Set<Class<? extends Throwable>> TECHNICAL = Set.of(
            NullPointerException.class, ClassCastException.class, IndexOutOfBoundsException.class,
            ArrayIndexOutOfBoundsException.class, StringIndexOutOfBoundsException.class,
            UnsupportedOperationException.class, ArithmeticException.class, NumberFormatException.class);

    private UserErrors() {
    }

    /** "Não foi possível carregar o extrato. O servidor não respondeu." */
    public static String message(String action, Throwable error) {
        String what = action == null || action.isBlank() ? "concluir a operação" : action.trim();
        String reason = reason(error);
        return "Não foi possível " + what + "." + (reason.isEmpty() ? "" : " " + reason);
    }

    public static void toast(Component owner, String action, Throwable error) {
        ToastManager.show(owner, FeedbackType.ERROR, message(action, error));
    }

    public static void show(Component owner, String action, Throwable error) {
        ModernMessageDialog.show(owner, FeedbackType.ERROR, "Não foi possível concluir", message(action, error));
    }

    static String reason(Throwable error) {
        Throwable root = error;
        // Descend while the wrapper says nothing of its own (no text, or the "Class: message" Java builds from the cause).
        while (root != null && root.getCause() != null && root.getCause() != root
                && (root.getMessage() == null || root.getMessage().isBlank()
                    || root.getMessage().equals(root.getCause().toString()))) {
            root = root.getCause();
        }
        if (root == null) return "";
        if (TECHNICAL.contains(root.getClass())) {
            LOG.log(Level.WARNING, "Erro técnico mostrado ao operador como mensagem genérica", root);
            return TECHNICAL_HINT;
        }
        String text = root.getMessage();
        if (text == null || text.isBlank() || "null".equalsIgnoreCase(text.trim())) return TECHNICAL_HINT;
        text = text.trim();
        return text.endsWith(".") || text.endsWith("!") || text.endsWith("?") ? text : text + ".";
    }
}
