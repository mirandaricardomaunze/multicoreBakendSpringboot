package mz.multicore.erp.architecture.warnings;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Non-blocking warnings of the current operation (request thread), returned to the client with the response
 * ({@link OperationWarningsResponseAdvice}, header {@value OperationWarningsResponseAdvice#HEADER}).
 *
 * <p>A warning raised inside a transaction only counts once that transaction commits: a rolled-back operation
 * never warns about something that did not happen (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.3, OR-06).
 */
public final class OperationWarnings {

    private static final ThreadLocal<List<String>> WARNINGS = ThreadLocal.withInitial(ArrayList::new);

    private OperationWarnings() {
    }

    /** Adds a PT-MZ warning; inside an active transaction it is added only after the commit. */
    public static void add(String message) {
        if (message == null || message.isBlank()) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    WARNINGS.get().add(message);
                }
            });
            return;
        }
        WARNINGS.get().add(message);
    }

    /** Returns the warnings collected so far and forgets them. */
    public static List<String> drain() {
        List<String> warnings = List.copyOf(WARNINGS.get());
        WARNINGS.remove();
        return warnings;
    }

    /** Forgets the warnings of the thread (end of request). */
    public static void clear() {
        WARNINGS.remove();
    }
}
