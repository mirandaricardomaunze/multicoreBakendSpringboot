package mz.multicore.erp.modules.webhooks.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.webhooks.dto.WebhookDeliveryDTO;
import mz.multicore.erp.modules.webhooks.model.WebhookDelivery;
import mz.multicore.erp.modules.webhooks.model.WebhookDeliveryStatus;
import mz.multicore.erp.modules.webhooks.model.WebhookSubscription;
import mz.multicore.erp.modules.webhooks.repository.WebhookDeliveryRepository;
import mz.multicore.erp.modules.webhooks.repository.WebhookSubscriptionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Records webhook deliveries and their send attempts, retry schedule included (docs/API_PUBLICA_WEBHOOKS_SPEC.md
 * §4.2.2, §4.3.1, §4.3.2); shows the ADMIN the delivery log, resends failed deliveries and purges old ones (§4.3.3).
 */
@Service
public class WebhookDeliveryService {

    /** Most deliveries deleted by one cleanup statement (§4.3.3). */
    static final int PURGE_BATCH = 500;
    static final int DEFAULT_PAGE_SIZE = 50;
    static final int MAX_PAGE_SIZE = 100;

    private static final int MAX_ERROR = 500;

    private final WebhookDeliveryRepository deliveries;
    private final WebhookSubscriptionRepository subscriptions;
    private final WebhookRetryPolicy retryPolicy;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher events;

    public WebhookDeliveryService(WebhookDeliveryRepository deliveries, WebhookSubscriptionRepository subscriptions,
                                  WebhookRetryPolicy retryPolicy, AuditLogService auditLogService,
                                  ApplicationEventPublisher events) {
        this.deliveries = deliveries;
        this.subscriptions = subscriptions;
        this.retryPolicy = retryPolicy;
        this.auditLogService = auditLogService;
        this.events = events;
    }

    /**
     * Delivery log of one subscription of the active company, newest first, optionally of one state (WH-06): one line
     * per delivery with its last attempt, never the body nor the secret. {@code size} null means 50, at most 100.
     */
    @Transactional(readOnly = true)
    public List<WebhookDeliveryDTO> listDeliveries(Long subscriptionId, String status, int page, Integer size) {
        WebhookAdmin.require();
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        WebhookSubscription subscription = subscriptions.findById(subscriptionId)
                .filter(found -> Objects.equals(found.getCompany().getId(), companyId))
                .orElseThrow(() -> new BusinessRuleException("Subscrição não encontrada."));
        Optional<WebhookDeliveryStatus> wanted = parseStatus(status);
        PageRequest pageRequest = pageRequest(page, size);
        List<WebhookDelivery> found = wanted.isPresent()
                ? deliveries.findBySubscriptionIdAndStatusOrderByCreatedAtDescIdDesc(subscription.getId(),
                        wanted.get(), pageRequest)
                : deliveries.findBySubscriptionIdOrderByCreatedAtDescIdDesc(subscription.getId(), pageRequest);
        return found.stream().map(WebhookDeliveryService::toDTO).toList();
    }

    /**
     * Manual resend of a {@code FAILED} delivery of the active company (WH-06): the same delivery (id, body, event)
     * goes back to {@code PENDING} with the retry schedule restarted, and is dispatched after this commit. The last
     * attempt's code/duration/error stay until the new attempt. Audited without body nor secret.
     */
    @Transactional
    public WebhookDeliveryDTO resend(Long deliveryId) {
        WebhookAdmin.require();
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        WebhookDelivery delivery = deliveries.findById(deliveryId)
                .filter(found -> Objects.equals(found.getCompany().getId(), companyId))
                .orElseThrow(() -> new BusinessRuleException("Entrega não encontrada."));
        if (delivery.getStatus() != WebhookDeliveryStatus.FAILED) {
            throw new BusinessRuleException("Só se reenviam entregas falhadas.");
        }
        if (!delivery.getSubscription().isActive()) {
            throw new BusinessRuleException("Active a subscrição antes de reenviar.");
        }
        delivery.setStatus(WebhookDeliveryStatus.PENDING);
        delivery.setAttempts(0);
        delivery.setNextAttemptAt(retryPolicy.now());
        WebhookDelivery saved = deliveries.save(delivery);
        auditLogService.logCurrent("WEBHOOK_RESEND", "Webhook " + saved.getSubscription().getUrl()
                + ": reenvio da entrega " + saved.getId() + " (evento " + saved.getEventId() + ")");
        events.publishEvent(new WebhookResendRequested(saved.getId()));
        return toDTO(saved);
    }

    /** Deletes one batch (at most {@value #PURGE_BATCH}) of deliveries created before the cutoff (WH-06). */
    @Transactional
    public int deleteBatchCreatedBefore(LocalDateTime cutoff) {
        List<Long> ids = deliveries.findIdsCreatedBefore(cutoff, PageRequest.of(0, PURGE_BATCH));
        if (!ids.isEmpty()) {
            deliveries.deleteAllByIdInBatch(ids);
        }
        return ids.size();
    }

    /**
     * Records one {@code PENDING} delivery with the already serialized body, in a transaction of its own: the
     * business transaction has committed already, and one subscription failing must not stop the others.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long registerPending(PreparedWebhookEvent prepared) {
        WebhookSubscription subscription = subscriptions.getReferenceById(prepared.subscriptionId());
        WebhookDelivery delivery = new WebhookDelivery();
        delivery.setSubscription(subscription);
        delivery.setCompany(subscription.getCompany());
        delivery.setEventId(prepared.payload().eventId());
        delivery.setEventType(prepared.payload().type());
        delivery.setBody(prepared.payload().body());
        delivery.setStatus(WebhookDeliveryStatus.PENDING);
        delivery.setAttempts(0);
        delivery.setNextAttemptAt(retryPolicy.now());
        return deliveries.save(delivery).getId();
    }

    /** Ids of the due {@code PENDING} deliveries of active subscriptions, oldest due first (WH-05). */
    @Transactional(readOnly = true)
    public List<Long> dueDeliveryIds(int limit) {
        return deliveries.findDueIds(WebhookDeliveryStatus.PENDING, retryPolicy.now(), PageRequest.of(0, limit));
    }

    /**
     * Claims the next attempt and reads what it needs (WH-04, WH-05); empty when the delivery is gone, no longer
     * {@code PENDING}, not yet due, already claimed by another thread, or its subscription is inactive — then nothing
     * is sent and nothing changes. The claim reserves the attempt for {@link WebhookRetryPolicy#CLAIM_LEASE}.
     */
    @Transactional
    public Optional<WebhookAttempt> startAttempt(Long deliveryId) {
        LocalDateTime now = retryPolicy.now();
        return deliveries.findById(deliveryId)
                .filter(delivery -> delivery.getStatus() == WebhookDeliveryStatus.PENDING)
                .filter(delivery -> delivery.getSubscription().isActive())
                .filter(delivery -> deliveries.claim(delivery.getId(), WebhookDeliveryStatus.PENDING, now,
                        retryPolicy.claimLeaseUntil(now)) == 1)
                .map(delivery -> new WebhookAttempt(delivery.getId(), delivery.getSubscription().getUrl(),
                        delivery.getEventType().code(), delivery.getBody(),
                        delivery.getSubscription().getSecretCiphertext()));
    }

    /** 2xx: the delivery is done (WH-04). */
    @Transactional
    public void recordDelivered(Long deliveryId, int httpStatus, long durationMs) {
        deliveries.findById(deliveryId).ifPresent(delivery -> {
            countAttempt(delivery, httpStatus, durationMs, null);
            delivery.setStatus(WebhookDeliveryStatus.DELIVERED);
            delivery.setDeliveredAt(retryPolicy.now());
            delivery.setNextAttemptAt(null);
            deliveries.save(delivery);
        });
    }

    /**
     * Failed attempt (WH-04): the attempt is recorded and the next one scheduled from the end of this one; after the
     * last allowed attempt the delivery becomes {@code FAILED} (WH-05).
     */
    @Transactional
    public void recordFailure(Long deliveryId, Integer httpStatus, long durationMs, String error) {
        deliveries.findById(deliveryId).ifPresent(delivery -> {
            countAttempt(delivery, httpStatus, durationMs, error);
            Optional<LocalDateTime> next = retryPolicy.nextAttemptAt(delivery.getAttempts(), retryPolicy.now());
            delivery.setNextAttemptAt(next.orElse(null));
            if (next.isEmpty()) {
                delivery.setStatus(WebhookDeliveryStatus.FAILED);
            }
            deliveries.save(delivery);
        });
    }

    private static void countAttempt(WebhookDelivery delivery, Integer httpStatus, long durationMs, String error) {
        delivery.setAttempts(delivery.getAttempts() + 1);
        delivery.setLastHttpStatus(httpStatus);
        delivery.setLastDurationMs(durationMs);
        delivery.setLastError(error == null || error.length() <= MAX_ERROR ? error : error.substring(0, MAX_ERROR));
    }

    private static Optional<WebhookDeliveryStatus> parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(WebhookDeliveryStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Estado de entrega desconhecido: " + raw.trim() + ".");
        }
    }

    private static PageRequest pageRequest(int page, Integer size) {
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : size;
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BusinessRuleException("Tamanho de página inválido (1 a 100).");
        }
        if (page < 0) {
            throw new BusinessRuleException("Página inválida.");
        }
        return PageRequest.of(page, pageSize);
    }

    private static WebhookDeliveryDTO toDTO(WebhookDelivery delivery) {
        return new WebhookDeliveryDTO(delivery.getId(), delivery.getEventId(), delivery.getEventType().code(),
                delivery.getStatus(), delivery.getAttempts(), delivery.getLastHttpStatus(),
                delivery.getLastDurationMs(), delivery.getLastError(), delivery.getNextAttemptAt(),
                delivery.getCreatedAt(), delivery.getDeliveredAt());
    }
}
