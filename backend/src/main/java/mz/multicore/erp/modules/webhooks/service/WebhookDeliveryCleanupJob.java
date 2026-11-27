package mz.multicore.erp.modules.webhooks.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Keeps webhook deliveries for 30 days (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.3.3, WH-06): deletes those created
 * before now − 30 days, of any state and any company, in batches of at most {@value WebhookDeliveryService#PURGE_BATCH}
 * so the table is never held for long. Subscriptions are never deleted here.
 */
@Component
@ConditionalOnProperty(name = "webhooks.retention.enabled", havingValue = "true", matchIfMissing = true)
public class WebhookDeliveryCleanupJob {

    static final int RETENTION_DAYS = 30;

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryCleanupJob.class);

    private final WebhookDeliveryService deliveries;
    private final WebhookRetryPolicy clock;

    public WebhookDeliveryCleanupJob(WebhookDeliveryService deliveries, WebhookRetryPolicy clock) {
        this.deliveries = deliveries;
        this.clock = clock;
    }

    @Scheduled(cron = "${webhooks.retention.cron:0 30 3 * * *}")
    public void purge() {
        LocalDateTime cutoff = clock.now().minusDays(RETENTION_DAYS);
        int total = 0;
        try {
            int deleted;
            do {
                deleted = deliveries.deleteBatchCreatedBefore(cutoff);
                total += deleted;
            } while (deleted == WebhookDeliveryService.PURGE_BATCH);
        } catch (RuntimeException e) {
            log.warn("Limpeza das entregas de webhooks falhou: {}", e.getClass().getSimpleName());
        }
        log.info("Limpeza das entregas de webhooks: {} apagadas.", total);
    }
}
