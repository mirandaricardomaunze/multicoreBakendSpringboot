package mz.multicore.erp.modules.webhooks.service;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;

/**
 * HTTP port of the webhook sender (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.3.1, WH-04), so tests send without the
 * network. Implementations never follow redirects, ignore the response body and signal a timeout with
 * {@link java.net.http.HttpTimeoutException}.
 */
public interface WebhookHttpClient {

    /** Posts {@code body} with {@code headers} and returns the HTTP status code. */
    int post(URI url, Map<String, String> headers, byte[] body, Duration timeout)
            throws IOException, InterruptedException;
}
