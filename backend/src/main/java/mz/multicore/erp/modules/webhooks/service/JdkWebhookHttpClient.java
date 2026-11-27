package mz.multicore.erp.modules.webhooks.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** {@link WebhookHttpClient} over {@code java.net.http}: no redirects, response body discarded (§4.3.1). */
@Component
public class JdkWebhookHttpClient implements WebhookHttpClient {

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(WebhookSender.TIMEOUT)
            .build();

    @Override
    public int post(URI url, Map<String, String> headers, byte[] body, Duration timeout)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(url)
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        headers.forEach(request::header);
        return client.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
