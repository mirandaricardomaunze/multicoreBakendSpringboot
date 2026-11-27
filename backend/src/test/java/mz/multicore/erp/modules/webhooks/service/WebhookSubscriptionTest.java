package mz.multicore.erp.modules.webhooks.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.AttachmentCrypto;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.webhooks.dto.CreateWebhookSubscriptionRequest;
import mz.multicore.erp.modules.webhooks.dto.CreatedWebhookSubscriptionDTO;
import mz.multicore.erp.modules.webhooks.dto.WebhookSubscriptionDTO;
import mz.multicore.erp.modules.webhooks.model.WebhookEventType;
import mz.multicore.erp.modules.webhooks.model.WebhookSubscription;
import mz.multicore.erp.modules.webhooks.repository.WebhookSubscriptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Harness WH-01 (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.1, §4.1.1): only the ADMIN of the active company manages
 * subscriptions; URL + at least one known event; https in prod; no user/password; private/local addresses refused,
 * also after DNS; the {@code whsec_} secret is shown once and stored encrypted; audit never carries the secret.
 */
class WebhookSubscriptionTest {

    private static final Long COMPANY_ID = 1L;
    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    private final WebhookSubscriptionRepository repository = mock(WebhookSubscriptionRepository.class);
    private final CompanyService companyService = mock(CompanyService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final AttachmentCrypto crypto = new AttachmentCrypto(KEY);
    private final Map<String, List<InetAddress>> dns = new HashMap<>();
    private final Map<Long, WebhookSubscription> byId = new HashMap<>();

    private WebhookSubscriptionService service = serviceWithProfiles();

    @BeforeEach
    void setUp() throws Exception {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        when(companyService.getCurrentCompanyReference(COMPANY_ID)).thenReturn(company(COMPANY_ID));
        when(repository.save(any(WebhookSubscription.class))).thenAnswer(invocation -> {
            WebhookSubscription subscription = invocation.getArgument(0);
            if (subscription.getId() == null) subscription.setId(100L + byId.size());
            byId.put(subscription.getId(), subscription);
            return subscription;
        });
        when(repository.findById(any()))
                .thenAnswer(invocation -> Optional.ofNullable(byId.get(invocation.<Long>getArgument(0))));
        dns.put("loja.example.com", List.of(InetAddress.getByName("93.184.216.34")));
        dns.put("interno.example.com", List.of(InetAddress.getByName("93.184.216.34"),
                InetAddress.getByName("10.0.0.5")));
        dns.put("v6interno.example.com", List.of(InetAddress.getByName("fd12:3456::1")));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void wh01_criaSubscricaoActivaComSegredoWhsecMostradoUmaVez() {
        CreatedWebhookSubscriptionDTO created = create("https://loja.example.com/hooks",
                "invoice.issued", "order.status_changed");

        assertThat(created.secret()).startsWith("whsec_");
        assertThat(created.secret().substring("whsec_".length())).hasSize(43).matches("[0-9A-Za-z]+");
        assertTrue(created.subscription().active());
        assertEquals("https://loja.example.com/hooks", created.subscription().url());
        assertEquals(EnumSet.of(WebhookEventType.INVOICE_ISSUED, WebhookEventType.ORDER_STATUS_CHANGED),
                created.subscription().events());
        assertThat(Arrays.stream(WebhookSubscriptionDTO.class.getRecordComponents()).map(c -> c.getName()))
                .doesNotContain("secret", "secretCiphertext");
    }

    @Test
    void wh01_segredoGuardadoCifradoERecuperavelParaAssinar() {
        CreatedWebhookSubscriptionDTO created = create("https://loja.example.com/hooks", "invoice.issued");

        ArgumentCaptor<WebhookSubscription> saved = ArgumentCaptor.forClass(WebhookSubscription.class);
        verify(repository).save(saved.capture());
        String stored = saved.getValue().getSecretCiphertext();
        assertThat(stored).doesNotContain(created.secret());
        assertTrue(AttachmentCrypto.isEncrypted(Base64.getDecoder().decode(stored)));
        assertEquals(created.secret(), WebhookSecrets.open(crypto, stored));
        assertEquals(COMPANY_ID, saved.getValue().getCompany().getId());
        assertEquals(0, saved.getValue().getConsecutiveFailures());
    }

    @Test
    void wh01_listagemNaoDevolveSegredo() {
        CreatedWebhookSubscriptionDTO created = create("https://loja.example.com/hooks", "invoice.issued");
        when(repository.findByCompanyIdOrderByCreatedAtDescIdDesc(COMPANY_ID)).thenReturn(List.copyOf(byId.values()));

        List<WebhookSubscriptionDTO> listed = service.list();

        assertThat(listed).hasSize(1);
        assertThat(listed.get(0).toString()).doesNotContain(created.secret());
    }

    @Test
    void wh01_soOAdministradorGere() {
        create("https://loja.example.com/hooks", "invoice.issued");
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");

        assertMessage("Só o administrador gere os webhooks.",
                () -> service.create(request("https://loja.example.com/x", "invoice.issued")));
        assertMessage("Só o administrador gere os webhooks.", () -> service.list());
        assertMessage("Só o administrador gere os webhooks.", () -> service.deactivate(100L));
    }

    @Test
    void wh01_urlEEventosObrigatorios() {
        assertMessage("Indique o URL do webhook.", () -> service.create(request("  ", "invoice.issued")));
        assertMessage("Indique o URL do webhook.", () -> service.create(request(null, "invoice.issued")));
        assertMessage("Escolha pelo menos um evento.",
                () -> service.create(new CreateWebhookSubscriptionRequest("https://loja.example.com/h", List.of())));
        assertMessage("Escolha pelo menos um evento.",
                () -> service.create(new CreateWebhookSubscriptionRequest("https://loja.example.com/h", null)));
        assertMessage("Evento de webhook desconhecido: sale.deleted.",
                () -> service.create(request("https://loja.example.com/h", "invoice.issued", "sale.deleted")));
        verify(repository, never()).save(any());
    }

    @Test
    void wh01_eventosRepetidosFicamUmaVez() {
        CreatedWebhookSubscriptionDTO created = create("https://loja.example.com/hooks",
                "invoice.issued", "invoice.issued");

        assertEquals(EnumSet.of(WebhookEventType.INVOICE_ISSUED), created.subscription().events());
    }

    @Test
    void wh01_urlInvalidaOuComUtilizadorSenhaRecusada() {
        assertMessage("O URL do webhook é inválido.", () -> service.create(request("loja.example.com/h", "invoice.issued")));
        assertMessage("O URL do webhook é inválido.", () -> service.create(request("https://", "invoice.issued")));
        assertMessage("O URL do webhook é inválido.",
                () -> service.create(request("https://u:p@loja.example.com/h", "invoice.issued")));
        assertMessage("O URL do webhook é inválido.",
                () -> service.create(request("https://loja.example.com/" + "a".repeat(500), "invoice.issued")));
    }

    @Test
    void wh01_httpsObrigatorioEmProd_httpSoForaDeProd() {
        assertThat(create("http://loja.example.com/hooks", "invoice.issued").subscription().url())
                .startsWith("http://");
        assertMessage("O URL do webhook tem de usar https://.",
                () -> service.create(request("ftp://loja.example.com/h", "invoice.issued")));

        service = serviceWithProfiles("prod");
        assertMessage("O URL do webhook tem de usar https://.",
                () -> service.create(request("http://loja.example.com/h", "invoice.issued")));
        assertTrue(create("https://loja.example.com/hooks", "invoice.issued").subscription().active());
    }

    @Test
    void wh01_enderecosPrivadosELocaisRecusados() {
        String message = "Não são permitidos endereços privados ou locais no webhook.";
        for (String url : List.of("https://localhost/h", "https://api.localhost/h", "https://127.0.0.1/h",
                "https://10.1.2.3/h", "https://192.168.1.10/h", "https://172.16.0.1/h", "https://172.31.255.1/h",
                "https://169.254.169.254/latest", "https://0.0.0.0/h", "https://[::1]/h", "https://[::]/h",
                "https://[fc00::1]/h", "https://[fe80::1]/h", "https://[::ffff:127.0.0.1]/h")) {
            assertMessage(message, () -> service.create(request(url, "invoice.issued")));
        }
        assertTrue(create("https://172.32.0.1/h", "invoice.issued").subscription().active());
        verify(repository).save(any());
    }

    @Test
    void wh01_dnsVerificaTodosOsEnderecos_nomeQueNaoResolveRecusado() {
        assertMessage("Não são permitidos endereços privados ou locais no webhook.",
                () -> service.create(request("https://interno.example.com/h", "invoice.issued")));
        assertMessage("Não são permitidos endereços privados ou locais no webhook.",
                () -> service.create(request("https://v6interno.example.com/h", "invoice.issued")));
        assertMessage("Não foi possível resolver o endereço do webhook.",
                () -> service.create(request("https://nao-existe.example.com/h", "invoice.issued")));
        verify(repository, never()).save(any());
    }

    @Test
    void wh01_activarDesactivarComAuditoriaSemSegredo() {
        CreatedWebhookSubscriptionDTO created = create("https://loja.example.com/hooks", "invoice.issued");
        Long id = created.subscription().id();

        assertFalse(service.deactivate(id).active());
        assertTrue(service.activate(id).active());

        ArgumentCaptor<String> details = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).logCurrent(eq("WEBHOOK_CREATE"), details.capture());
        verify(auditLogService).logCurrent(eq("WEBHOOK_DEACTIVATE"), details.capture());
        verify(auditLogService).logCurrent(eq("WEBHOOK_ACTIVATE"), details.capture());
        assertThat(details.getAllValues()).allSatisfy(detail -> {
            assertThat(detail).contains("https://loja.example.com/hooks");
            assertThat(detail).doesNotContain(created.secret());
        });
    }

    @Test
    void wh01_subscricaoDeOutraEmpresaNaoEncontrada() {
        Long id = create("https://loja.example.com/hooks", "invoice.issued").subscription().id();
        CurrentUserContext.setCurrentCompanyId(2L);

        assertMessage("Subscrição não encontrada.", () -> service.deactivate(id));
        assertTrue(byId.get(id).isActive());
        verify(auditLogService, never()).logCurrent(eq("WEBHOOK_DEACTIVATE"), anyString());
    }

    private CreatedWebhookSubscriptionDTO create(String url, String... events) {
        return service.create(request(url, events));
    }

    private static CreateWebhookSubscriptionRequest request(String url, String... events) {
        return new CreateWebhookSubscriptionRequest(url, List.of(events));
    }

    private WebhookSubscriptionService serviceWithProfiles(String... profiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profiles);
        WebhookAddressResolver resolver = host -> {
            List<InetAddress> addresses = dns.get(host);
            if (addresses == null) throw new UnknownHostException(host);
            return addresses;
        };
        return new WebhookSubscriptionService(repository, new WebhookUrlValidator(environment, resolver), crypto,
                companyService, auditLogService);
    }

    private static void assertMessage(String expected, Runnable action) {
        BusinessRuleException error = assertThrows(BusinessRuleException.class, action::run);
        assertEquals(expected, error.getMessage());
    }

    private static Company company(Long id) {
        Company company = new Company();
        company.setId(id);
        return company;
    }
}
