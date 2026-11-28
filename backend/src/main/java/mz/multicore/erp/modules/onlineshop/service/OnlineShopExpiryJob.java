package mz.multicore.erp.modules.onlineshop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Runs the automatic cancellation of unbilled online orders (docs/LOJA_ONLINE_SPEC.md §3.4, LO-10) every hour, so an
 * order is cancelled at most one hour after its period ends. The rule itself lives in {@link OnlineShopExpiryService}.
 */
@Component
@ConditionalOnProperty(name = "onlineshop.expiry.enabled", havingValue = "true", matchIfMissing = true)
public class OnlineShopExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(OnlineShopExpiryJob.class);

    private final OnlineShopExpiryService expiryService;

    public OnlineShopExpiryJob(OnlineShopExpiryService expiryService) {
        this.expiryService = expiryService;
    }

    @Scheduled(cron = "${onlineshop.expiry.cron:0 15 * * * *}")
    public void run() {
        try {
            int cancelled = expiryService.cancelExpired(LocalDateTime.now());
            if (cancelled > 0) {
                log.info("Loja online: {} encomenda(s) cancelada(s) automaticamente.", cancelled);
            }
        } catch (RuntimeException e) {
            log.warn("Cancelamento automático das encomendas online falhou: {}", e.getClass().getSimpleName());
        }
    }
}
