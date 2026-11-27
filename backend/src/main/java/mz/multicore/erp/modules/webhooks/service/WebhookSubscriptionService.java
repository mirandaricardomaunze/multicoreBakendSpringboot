package mz.multicore.erp.modules.webhooks.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.AttachmentCrypto;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.webhooks.dto.CreateWebhookSubscriptionRequest;
import mz.multicore.erp.modules.webhooks.dto.CreatedWebhookSubscriptionDTO;
import mz.multicore.erp.modules.webhooks.dto.WebhookSubscriptionDTO;
import mz.multicore.erp.modules.webhooks.model.WebhookEventType;
import mz.multicore.erp.modules.webhooks.model.WebhookSubscription;
import mz.multicore.erp.modules.webhooks.repository.WebhookSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Webhook subscriptions of the active company (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.1, §4.1.1, WH-01): only the
 * ADMIN creates, lists, activates and deactivates them; the URL passes the anti-SSRF check, the {@code whsec_} secret
 * leaves the server only in the creation result and is stored encrypted. Changes are audited without the secret.
 */
@Service
public class WebhookSubscriptionService {

    private final WebhookSubscriptionRepository repository;
    private final WebhookUrlValidator urlValidator;
    private final AttachmentCrypto crypto;
    private final CompanyService companyService;
    private final AuditLogService auditLogService;

    public WebhookSubscriptionService(WebhookSubscriptionRepository repository, WebhookUrlValidator urlValidator,
                                      AttachmentCrypto crypto, CompanyService companyService,
                                      AuditLogService auditLogService) {
        this.repository = repository;
        this.urlValidator = urlValidator;
        this.crypto = crypto;
        this.companyService = companyService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<WebhookSubscriptionDTO> list() {
        requireAdmin();
        return repository.findByCompanyIdOrderByCreatedAtDescIdDesc(CurrentUserContext.requireCurrentCompanyId())
                .stream().map(WebhookSubscriptionService::toDTO).toList();
    }

    @Transactional
    public CreatedWebhookSubscriptionDTO create(CreateWebhookSubscriptionRequest request) {
        requireAdmin();
        String url = urlValidator.validate(request.url());
        Set<WebhookEventType> events = requireEvents(request.events());
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        String secret = WebhookSecrets.generate();

        WebhookSubscription subscription = new WebhookSubscription();
        subscription.setCompany(companyService.getCurrentCompanyReference(companyId));
        subscription.setUrl(url);
        subscription.setEvents(events);
        subscription.setSecretCiphertext(WebhookSecrets.seal(crypto, secret));
        subscription.setActive(true);
        subscription.setCreatedBy(CurrentUserContext.getUsername());
        WebhookSubscription saved = repository.save(subscription);

        auditLogService.logCurrent("WEBHOOK_CREATE", describe(saved) + " com eventos " + codes(saved.getEvents()));
        return new CreatedWebhookSubscriptionDTO(toDTO(saved), secret);
    }

    @Transactional
    public WebhookSubscriptionDTO activate(Long id) {
        return changeActive(id, true, "WEBHOOK_ACTIVATE");
    }

    @Transactional
    public WebhookSubscriptionDTO deactivate(Long id) {
        return changeActive(id, false, "WEBHOOK_DEACTIVATE");
    }

    private WebhookSubscriptionDTO changeActive(Long id, boolean active, String auditAction) {
        requireAdmin();
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        WebhookSubscription subscription = repository.findById(id)
                .filter(found -> Objects.equals(found.getCompany().getId(), companyId))
                .orElseThrow(() -> new BusinessRuleException("Subscrição não encontrada."));
        if (subscription.isActive() == active) {
            return toDTO(subscription);
        }
        subscription.setActive(active);
        WebhookSubscription saved = repository.save(subscription);
        auditLogService.logCurrent(auditAction, describe(saved));
        return toDTO(saved);
    }

    private static void requireAdmin() {
        WebhookAdmin.require();
    }

    private static Set<WebhookEventType> requireEvents(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            throw new BusinessRuleException("Escolha pelo menos um evento.");
        }
        EnumSet<WebhookEventType> events = EnumSet.noneOf(WebhookEventType.class);
        for (String code : raw) {
            events.add(WebhookEventType.fromCode(code).orElseThrow(
                    () -> new BusinessRuleException("Evento de webhook desconhecido: " + code + ".")));
        }
        return events;
    }

    private static String describe(WebhookSubscription subscription) {
        return "Webhook " + subscription.getUrl();
    }

    private static List<String> codes(Set<WebhookEventType> events) {
        return events.stream().map(WebhookEventType::code).toList();
    }

    private static WebhookSubscriptionDTO toDTO(WebhookSubscription subscription) {
        return new WebhookSubscriptionDTO(subscription.getId(), subscription.getUrl(),
                Set.copyOf(subscription.getEvents()), subscription.isActive(),
                subscription.getConsecutiveFailures(), subscription.getCreatedAt(), subscription.getCreatedBy());
    }
}
