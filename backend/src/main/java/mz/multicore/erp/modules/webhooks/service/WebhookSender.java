package mz.multicore.erp.modules.webhooks.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.AttachmentCrypto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Makes one signed send attempt of a recorded delivery (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.3.1, WH-04): the body
 * exactly as stored, the event, the stable delivery id and the HMAC signature; the URL is checked again (DNS
 * included) before connecting. No database transaction is held while waiting for the receiver.
 */
@Service
public class WebhookSender {

    static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final WebhookDeliveryService deliveries;
    private final WebhookUrlValidator urlValidator;
    private final WebhookHttpClient http;
    private final AttachmentCrypto crypto;

    public WebhookSender(WebhookDeliveryService deliveries, WebhookUrlValidator urlValidator,
                         WebhookHttpClient http, AttachmentCrypto crypto) {
        this.deliveries = deliveries;
        this.urlValidator = urlValidator;
        this.http = http;
        this.crypto = crypto;
    }

    /** One attempt; nothing happens when the delivery is no longer pending or its subscription is inactive. */
    public void send(Long deliveryId) {
        Optional<WebhookAttempt> next = deliveries.startAttempt(deliveryId);
        if (next.isEmpty()) {
            return;
        }
        WebhookAttempt attempt = next.get();
        long started = System.nanoTime();
        URI url;
        try {
            url = URI.create(urlValidator.validate(attempt.url()));
        } catch (BusinessRuleException ex) {
            deliveries.recordFailure(deliveryId, null, elapsedMs(started), "Endereço do webhook recusado.");
            return;
        }
        byte[] body = attempt.body().getBytes(StandardCharsets.UTF_8);
        Map<String, String> headers = headers(attempt, body);
        try {
            int status = http.post(url, headers, body, TIMEOUT);
            if (status >= 200 && status < 300) {
                deliveries.recordDelivered(deliveryId, status, elapsedMs(started));
            } else {
                deliveries.recordFailure(deliveryId, status, elapsedMs(started), "Resposta HTTP " + status + ".");
            }
        } catch (HttpTimeoutException ex) {
            deliveries.recordFailure(deliveryId, null, elapsedMs(started), "Tempo de resposta excedido (10 s).");
        } catch (IOException ex) {
            deliveries.recordFailure(deliveryId, null, elapsedMs(started), networkError(ex));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            deliveries.recordFailure(deliveryId, null, elapsedMs(started), networkError(ex));
        }
    }

    private Map<String, String> headers(WebhookAttempt attempt, byte[] body) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json; charset=utf-8");
        headers.put("User-Agent", "Multicore-Webhooks/1");
        headers.put("X-Multicore-Event", attempt.eventCode());
        headers.put("X-Multicore-Delivery", String.valueOf(attempt.deliveryId()));
        headers.put("X-Multicore-Signature",
                WebhookSignature.sign(WebhookSecrets.open(crypto, attempt.secretCiphertext()), body));
        return headers;
    }

    private static String networkError(Exception ex) {
        return "Erro de rede: " + ex.getClass().getSimpleName() + ".";
    }

    private static long elapsedMs(long startedNanos) {
        return Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();
    }
}
