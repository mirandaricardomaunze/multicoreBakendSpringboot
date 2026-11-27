package mz.multicore.erp.modules.webhooks.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs each delivery on the webhooks' own executor so the user's operation never waits for HTTP (§4.2.2, WH-03),
 * where {@link WebhookSender} makes the signed attempt (§4.3.1, WH-04). No error leaves the executor.
 */
@Component
public class AsyncWebhookDispatcher implements WebhookDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AsyncWebhookDispatcher.class);
    private static final int THREADS = 2;

    private final ExecutorService executor = Executors.newFixedThreadPool(THREADS, daemonThreads());
    private final WebhookSender sender;

    public AsyncWebhookDispatcher(WebhookSender sender) {
        this.sender = sender;
    }

    @Override
    public void dispatch(Long deliveryId) {
        executor.execute(() -> send(deliveryId));
    }

    private void send(Long deliveryId) {
        try {
            sender.send(deliveryId);
        } catch (RuntimeException e) {
            log.warn("Envio da entrega de webhook {} falhou: {}", deliveryId, e.getClass().getSimpleName());
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }

    private static ThreadFactory daemonThreads() {
        AtomicInteger sequence = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, "webhook-dispatch-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
