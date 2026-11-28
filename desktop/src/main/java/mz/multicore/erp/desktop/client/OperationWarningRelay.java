package mz.multicore.erp.desktop.client;

import java.net.URLDecoder;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

/**
 * Passes on the operation warnings the backend sends in the {@value #HEADER} header of a successful response
 * (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.3, e.g. a purchase that took a cost centre past 90% of its budget).
 * The HTTP layer only decodes them; the screen that registers the listener decides how to show them (a toast).
 * Nothing is blocked: the operation already succeeded.
 */
public final class OperationWarningRelay {

    public static final String HEADER = "X-Operation-Warning";

    private static volatile Consumer<String> listener;

    private OperationWarningRelay() {}

    /** Registers who shows the warnings; {@code null} stops passing them on. */
    public static void setListener(Consumer<String> warningListener) {
        listener = warningListener;
    }

    /** Hands every warning header of the response (UTF-8, URL-encoded) to the listener, decoded. */
    static void relay(HttpHeaders headers) {
        if (headers == null) return;
        publish(headers.allValues(HEADER));
    }

    static void publish(List<String> encodedWarnings) {
        Consumer<String> current = listener;
        if (current == null || encodedWarnings == null) return;
        for (String encoded : encodedWarnings) {
            if (encoded == null || encoded.isBlank()) continue;
            current.accept(URLDecoder.decode(encoded, StandardCharsets.UTF_8));
        }
    }
}
