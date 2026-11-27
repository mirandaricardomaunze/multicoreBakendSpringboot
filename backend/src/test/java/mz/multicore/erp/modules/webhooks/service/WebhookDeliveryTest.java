package mz.multicore.erp.modules.webhooks.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.AttachmentCrypto;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.webhooks.dto.WebhookDeliveryDTO;
import mz.multicore.erp.modules.webhooks.model.WebhookDelivery;
import mz.multicore.erp.modules.webhooks.model.WebhookDeliveryStatus;
import mz.multicore.erp.modules.webhooks.model.WebhookEventType;
import mz.multicore.erp.modules.webhooks.model.WebhookSubscription;
import mz.multicore.erp.modules.webhooks.repository.WebhookDeliveryRepository;
import mz.multicore.erp.modules.webhooks.repository.WebhookSubscriptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.env.MockEnvironment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Harness WH-04 (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.3, §4.3.1): one attempt posts the recorded body as is, with the
 * event, the stable delivery id and an HMAC-SHA256 signature made with the decrypted secret; 2xx delivers, any failure
 * keeps the delivery {@code PENDING} with the attempt recorded; the URL is checked again before connecting; inactive
 * subscriptions and non-pending deliveries send nothing.
 * <p>
 * Harness WH-05 (§4.3.2), with a controlled clock: failures schedule +1 min, +5 min, +30 min, +2 h, +12 h from the end
 * of each attempt, the 6th is {@code FAILED}; the scheduler dispatches only due {@code PENDING} deliveries of active
 * subscriptions, at most 50; a claimed attempt is not sent again until its lease expires. The real queries are
 * checked against Hibernate in {@code WebhookRetryJpaTest}.
 * <p>
 * Harness WH-06 (§4.3.3): the ADMIN sees the delivery log of a subscription of the active company, newest first,
 * filtered by state, without the body; a {@code FAILED} delivery is resent as the same delivery, back to
 * {@code PENDING} with the schedule restarted, dispatched after commit and audited; the cleanup deletes deliveries
 * older than 30 days.
 */
class WebhookDeliveryTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String SECRET = "whsec_0123456789abcdefghijABCDEFGHIJklmnopqrstu";
    private static final String BODY = "{\"id\":\"evt_abc\",\"type\":\"invoice.issued\",\"companyId\":1,"
            + "\"data\":{\"invoiceNumber\":\"FT 2026/00001\",\"customer\":{\"name\":\"Lojas Ndlovu, Lda\"},"
            + "\"totalAmount\":1160.00}}";

    private final WebhookDeliveryRepository deliveryRepository = mock(WebhookDeliveryRepository.class);
    private final WebhookSubscriptionRepository subscriptionRepository = mock(WebhookSubscriptionRepository.class);
    private final AttachmentCrypto crypto = new AttachmentCrypto(KEY);
    private final Map<String, List<InetAddress>> dns = new HashMap<>();
    private final FakeHttpClient http = new FakeHttpClient();
    private final Map<Long, WebhookDelivery> byId = new HashMap<>();
    private final MutableClock clock = new MutableClock(LocalDateTime.of(2026, 11, 28, 9, 0));
    private final WebhookRetryPolicy retryPolicy = new WebhookRetryPolicy(clock);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final List<Object> published = new ArrayList<>();
    private final Company company = company(1L);

    private WebhookDeliveryService deliveries;
    private WebhookSender sender;

    @BeforeEach
    void setUp() throws Exception {
        CurrentUserContext.setCurrentCompanyId(1L);
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        when(deliveryRepository.save(any(WebhookDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriptionRepository.findById(any())).thenAnswer(invocation -> byId.values().stream()
                .map(WebhookDelivery::getSubscription)
                .filter(s -> s.getId().equals(invocation.getArgument(0)))
                .findFirst());
        // Fakes of the delivery-log queries (same WHERE / ORDER BY / page) and of the cleanup batch.
        when(deliveryRepository.findBySubscriptionIdOrderByCreatedAtDescIdDesc(any(), any()))
                .thenAnswer(invocation -> log(invocation.getArgument(0), null, invocation.getArgument(1)));
        when(deliveryRepository.findBySubscriptionIdAndStatusOrderByCreatedAtDescIdDesc(any(), any(), any()))
                .thenAnswer(invocation -> log(invocation.getArgument(0), invocation.getArgument(1),
                        invocation.getArgument(2)));
        when(deliveryRepository.findIdsCreatedBefore(any(), any())).thenAnswer(invocation -> {
            LocalDateTime cutoff = invocation.getArgument(0);
            Pageable page = invocation.getArgument(1);
            return byId.values().stream().filter(d -> d.getCreatedAt().isBefore(cutoff))
                    .map(WebhookDelivery::getId).sorted().limit(page.getPageSize()).toList();
        });
        org.mockito.Mockito.doAnswer(invocation -> {
            invocation.<Iterable<Long>>getArgument(0).forEach(byId::remove);
            return null;
        }).when(deliveryRepository).deleteAllByIdInBatch(any());
        dns.put("loja.example.com", List.of(InetAddress.getByName("93.184.216.34")));
        when(deliveryRepository.findById(any()))
                .thenAnswer(invocation -> Optional.ofNullable(byId.get(invocation.<Long>getArgument(0))));
        // Fake of the conditional UPDATE in WebhookDeliveryRepository.claim (same WHERE clause).
        when(deliveryRepository.claim(anyLong(), any(), any(), any())).thenAnswer(invocation -> {
            WebhookDelivery delivery = byId.get(invocation.<Long>getArgument(0));
            LocalDateTime now = invocation.getArgument(2);
            if (delivery == null || delivery.getStatus() != invocation.getArgument(1)
                    || delivery.getNextAttemptAt() == null || delivery.getNextAttemptAt().isAfter(now)) {
                return 0;
            }
            delivery.setNextAttemptAt(invocation.getArgument(3));
            return 1;
        });
        // Fake of the due query in WebhookDeliveryRepository.findDueIds (same WHERE / ORDER BY / page size).
        when(deliveryRepository.findDueIds(any(), any(), any())).thenAnswer(invocation -> {
            WebhookDeliveryStatus status = invocation.getArgument(0);
            LocalDateTime now = invocation.getArgument(1);
            Pageable page = invocation.getArgument(2);
            return byId.values().stream()
                    .filter(d -> d.getStatus() == status && d.getNextAttemptAt() != null
                            && !d.getNextAttemptAt().isAfter(now) && d.getSubscription().isActive())
                    .sorted(Comparator.comparing(WebhookDelivery::getNextAttemptAt)
                            .thenComparing(WebhookDelivery::getId))
                    .map(WebhookDelivery::getId)
                    .limit(page.getPageSize())
                    .toList();
        });
        WebhookAddressResolver resolver = host -> {
            List<InetAddress> addresses = dns.get(host);
            if (addresses == null) throw new UnknownHostException(host);
            return addresses;
        };
        WebhookUrlValidator validator = new WebhookUrlValidator(new MockEnvironment(), resolver);
        deliveries = new WebhookDeliveryService(deliveryRepository, subscriptionRepository, retryPolicy,
                auditLogService, published::add);
        sender = new WebhookSender(deliveries, validator, http, crypto);
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void wh04_enviaOCorpoGravadoTalEQualComCabecalhosEAssinatura() throws Exception {
        WebhookDelivery delivery = delivery(41L, "https://loja.example.com/hooks", true);
        http.status = 200;

        sender.send(41L);

        assertThat(http.requests).hasSize(1);
        SentRequest request = http.requests.get(0);
        assertThat(request.url()).isEqualTo(URI.create("https://loja.example.com/hooks"));
        assertThat(new String(request.body(), StandardCharsets.UTF_8)).isEqualTo(BODY);
        assertThat(request.timeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(request.headers())
                .containsEntry("Content-Type", "application/json; charset=utf-8")
                .containsEntry("User-Agent", "Multicore-Webhooks/1")
                .containsEntry("X-Multicore-Event", "invoice.issued")
                .containsEntry("X-Multicore-Delivery", "41")
                .containsEntry("X-Multicore-Signature", "sha256=" + hmacHex(SECRET, BODY));
        assertThat(request.headers().values()).noneMatch(value -> value.contains(SECRET));
        assertThat(delivery.getStatus()).isEqualTo(WebhookDeliveryStatus.DELIVERED);
    }

    @Test
    void wh04_assinaturaHmacSha256HexMinusculasComVectorConhecido() {
        // RFC 4231, test case 2.
        assertThat(WebhookSignature.sign("Jefe", "what do ya want for nothing?".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("sha256=5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843");
    }

    @Test
    void wh04_resposta2xxFicaEntregueComCodigoEDuracao() {
        WebhookDelivery delivery = delivery(42L, "https://loja.example.com/hooks", true);
        http.status = 204;

        sender.send(42L);

        assertThat(delivery.getStatus()).isEqualTo(WebhookDeliveryStatus.DELIVERED);
        assertThat(delivery.getAttempts()).isEqualTo(1);
        assertThat(delivery.getLastHttpStatus()).isEqualTo(204);
        assertThat(delivery.getLastDurationMs()).isNotNull().isGreaterThanOrEqualTo(0L);
        assertThat(delivery.getLastError()).isNull();
        assertThat(delivery.getDeliveredAt()).isNotNull();
        assertThat(delivery.getNextAttemptAt()).isNull();
    }

    @Test
    void wh04_resposta500FicaPendenteComATentativaRegistada() {
        WebhookDelivery delivery = delivery(43L, "https://loja.example.com/hooks", true);
        http.status = 500;

        sender.send(43L);

        assertPendingFailure(delivery, 500, "Resposta HTTP 500.");
    }

    @Test
    void wh04_redireccaoNaoESeguidaEContaComoFalha() {
        WebhookDelivery delivery = delivery(44L, "https://loja.example.com/hooks", true);
        http.status = 302;

        sender.send(44L);

        assertThat(http.requests).hasSize(1);
        assertPendingFailure(delivery, 302, "Resposta HTTP 302.");
    }

    @Test
    void wh04_timeoutFicaPendenteSemCodigo() {
        WebhookDelivery delivery = delivery(45L, "https://loja.example.com/hooks", true);
        http.failure = new HttpTimeoutException("request timed out");

        sender.send(45L);

        assertPendingFailure(delivery, null, "Tempo de resposta excedido (10 s).");
    }

    @Test
    void wh04_erroDeRedeFicaPendenteComAClasseDoErro() {
        WebhookDelivery delivery = delivery(46L, "https://loja.example.com/hooks", true);
        http.failure = new ConnectException("Connection refused");

        sender.send(46L);

        assertPendingFailure(delivery, null, "Erro de rede: ConnectException.");
    }

    @Test
    void wh04_urlQuePassouAResolverParaEnderecoPrivadoNaoLigaERegistaFalha() throws Exception {
        WebhookDelivery delivery = delivery(47L, "https://loja.example.com/hooks", true);
        dns.put("loja.example.com", List.of(InetAddress.getByName("10.0.0.7")));

        sender.send(47L);

        assertThat(http.requests).isEmpty();
        assertPendingFailure(delivery, null, "Endereço do webhook recusado.");
    }

    @Test
    void wh04_nomeQueDeixouDeResolverNaoLigaERegistaFalha() {
        WebhookDelivery delivery = delivery(48L, "https://loja.example.com/hooks", true);
        dns.clear();

        sender.send(48L);

        assertThat(http.requests).isEmpty();
        assertPendingFailure(delivery, null, "Endereço do webhook recusado.");
    }

    @Test
    void wh04_subscricaoInactivaNaoEnviaNemMudaAEntrega() {
        WebhookDelivery delivery = delivery(49L, "https://loja.example.com/hooks", false);

        sender.send(49L);

        assertThat(http.requests).isEmpty();
        assertUntouched(delivery, WebhookDeliveryStatus.PENDING);
    }

    @Test
    void wh04_entregaJaEntregueNaoVoltaASerEnviada() {
        WebhookDelivery delivery = delivery(50L, "https://loja.example.com/hooks", true);
        delivery.setStatus(WebhookDeliveryStatus.DELIVERED);

        sender.send(50L);

        assertThat(http.requests).isEmpty();
        assertUntouched(delivery, WebhookDeliveryStatus.DELIVERED);
    }

    @Test
    void wh04_entregaInexistenteNaoEnvia() {
        sender.send(999L);

        assertThat(http.requests).isEmpty();
    }

    @Test
    void wh04_oIdDaEntregaNoCabecalhoEEstavelEntreTentativas() {
        delivery(51L, "https://loja.example.com/hooks", true);
        http.status = 503;

        sender.send(51L);
        clock.advance(Duration.ofMinutes(1));
        sender.send(51L);

        assertThat(http.requests).hasSize(2);
        assertThat(http.requests).extracting(r -> r.headers().get("X-Multicore-Delivery")).containsOnly("51");
        assertThat(http.requests).extracting(r -> r.headers().get("X-Multicore-Signature"))
                .containsOnly(http.requests.get(0).headers().get("X-Multicore-Signature"));
        assertThat(byId.get(51L).getAttempts()).isEqualTo(2);
    }

    // ---- WH-05: tentativas e FAILED ----

    @Test
    void wh05_falhasSeguidasAgendamONovoEnvioDoFimDeCadaTentativaEASextaFicaFailed() {
        WebhookDelivery delivery = delivery(60L, "https://loja.example.com/hooks", true);
        http.status = 500;
        List<Duration> expected = List.of(Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(30),
                Duration.ofHours(2), Duration.ofHours(12));

        for (int failure = 1; failure <= 5; failure++) {
            sender.send(60L);
            assertThat(delivery.getStatus()).isEqualTo(WebhookDeliveryStatus.PENDING);
            assertThat(delivery.getAttempts()).isEqualTo(failure);
            assertThat(delivery.getNextAttemptAt()).as("depois da falha %d", failure)
                    .isEqualTo(clock.now().plus(expected.get(failure - 1)));
            clock.advance(expected.get(failure - 1));
        }
        sender.send(60L);

        assertThat(http.requests).hasSize(6);
        assertThat(delivery.getStatus()).isEqualTo(WebhookDeliveryStatus.FAILED);
        assertThat(delivery.getAttempts()).isEqualTo(6);
        assertThat(delivery.getNextAttemptAt()).isNull();
        assertThat(delivery.getLastHttpStatus()).isEqualTo(500);
        assertThat(delivery.getLastError()).isEqualTo("Resposta HTTP 500.");
        assertThat(delivery.getLastDurationMs()).isNotNull();

        clock.advance(Duration.ofDays(1));
        sender.send(60L);
        new WebhookRetryScheduler(deliveries, sender::send).dispatchDue();
        assertThat(http.requests).as("FAILED é final").hasSize(6);
    }

    @Test
    void wh05_oPrazoContaDoFimDaTentativaQueFalhou() {
        WebhookDelivery delivery = delivery(61L, "https://loja.example.com/hooks", true);
        http.failure = new HttpTimeoutException("request timed out");
        http.onPost = () -> clock.advance(Duration.ofSeconds(10));

        sender.send(61L);

        assertThat(delivery.getStatus()).isEqualTo(WebhookDeliveryStatus.PENDING);
        assertThat(delivery.getNextAttemptAt()).isEqualTo(LocalDateTime.of(2026, 11, 28, 9, 1, 10));
        assertThat(delivery.getLastError()).isEqualTo("Tempo de resposta excedido (10 s).");
    }

    @Test
    void wh05_sucessoNumaRepeticaoFicaEntregueSemMaisTentativas() {
        WebhookDelivery delivery = delivery(62L, "https://loja.example.com/hooks", true);
        http.status = 503;
        sender.send(62L);
        clock.advance(Duration.ofMinutes(1));
        http.status = 200;

        sender.send(62L);
        clock.advance(Duration.ofHours(13));
        new WebhookRetryScheduler(deliveries, sender::send).dispatchDue();

        assertThat(http.requests).hasSize(2);
        assertThat(delivery.getStatus()).isEqualTo(WebhookDeliveryStatus.DELIVERED);
        assertThat(delivery.getAttempts()).isEqualTo(2);
        assertThat(delivery.getNextAttemptAt()).isNull();
        assertThat(delivery.getLastError()).isNull();
    }

    @Test
    void wh05_tentativaAntesDoPrazoNaoEnvia() {
        WebhookDelivery delivery = delivery(63L, "https://loja.example.com/hooks", true);
        http.status = 500;
        sender.send(63L);
        clock.advance(Duration.ofSeconds(59));

        sender.send(63L);

        assertThat(http.requests).hasSize(1);
        assertThat(delivery.getAttempts()).isEqualTo(1);
    }

    @Test
    void wh05_agendadorSoDespachaPendentesVencidasDeSubscricoesActivas() {
        delivery(70L, "https://loja.example.com/hooks", true);
        delivery(71L, "https://loja.example.com/hooks", true).setNextAttemptAt(clock.now().minusMinutes(5));
        delivery(72L, "https://loja.example.com/hooks", true).setNextAttemptAt(clock.now().plusSeconds(1));
        delivery(73L, "https://loja.example.com/hooks", true).setStatus(WebhookDeliveryStatus.DELIVERED);
        WebhookDelivery failed = delivery(74L, "https://loja.example.com/hooks", true);
        failed.setStatus(WebhookDeliveryStatus.FAILED);
        failed.setNextAttemptAt(null);
        delivery(75L, "https://loja.example.com/hooks", false);
        List<Long> dispatched = new ArrayList<>();

        new WebhookRetryScheduler(deliveries, dispatched::add).dispatchDue();

        assertThat(dispatched).containsExactly(71L, 70L);
    }

    @Test
    void wh05_agendadorDespachaNoMaximo50PorVolta() {
        IntStream.rangeClosed(1, 60).forEach(i -> delivery(100L + i, "https://loja.example.com/hooks", true)
                .setNextAttemptAt(clock.now().minusMinutes(61 - i)));
        List<Long> dispatched = new ArrayList<>();

        new WebhookRetryScheduler(deliveries, dispatched::add).dispatchDue();

        assertThat(dispatched).hasSize(50).startsWith(101L).endsWith(150L);
    }

    @Test
    void wh05_agendadorRetomaAPrimeiraTentativaPerdidaNumReinicio() {
        WebhookDelivery delivery = delivery(76L, "https://loja.example.com/hooks", true);
        clock.advance(Duration.ofMinutes(10));
        http.status = 200;

        new WebhookRetryScheduler(deliveries, sender::send).dispatchDue();

        assertThat(http.requests).hasSize(1);
        assertThat(delivery.getStatus()).isEqualTo(WebhookDeliveryStatus.DELIVERED);
    }

    @Test
    void wh05_tentativaReclamadaNaoVoltaASerEnviadaDentroDoPrazoESimDepoisDeEleExpirar() {
        WebhookDelivery delivery = delivery(77L, "https://loja.example.com/hooks", true);

        assertThat(deliveries.startAttempt(77L)).as("primeira reclamação").isPresent();
        assertThat(delivery.getNextAttemptAt()).isEqualTo(clock.now().plusMinutes(2));
        sender.send(77L);
        clock.advance(Duration.ofSeconds(119));
        sender.send(77L);
        assertThat(http.requests).as("dentro do prazo de 2 min").isEmpty();

        clock.advance(Duration.ofSeconds(1));
        http.status = 200;
        sender.send(77L);

        assertThat(http.requests).as("prazo expirou: a tentativa repete").hasSize(1);
        assertThat(delivery.getStatus()).isEqualTo(WebhookDeliveryStatus.DELIVERED);
    }

    @Test
    void wh05_subscricaoInactivaEsperaSemContarFalhaESegueQuandoVoltaAFicarActiva() {
        WebhookDelivery delivery = delivery(78L, "https://loja.example.com/hooks", false);
        List<Long> dispatched = new ArrayList<>();
        clock.advance(Duration.ofDays(2));

        new WebhookRetryScheduler(deliveries, dispatched::add).dispatchDue();
        sender.send(78L);

        assertThat(dispatched).isEmpty();
        assertThat(http.requests).isEmpty();
        assertUntouched(delivery, WebhookDeliveryStatus.PENDING);

        delivery.getSubscription().setActive(true);
        http.status = 200;
        new WebhookRetryScheduler(deliveries, sender::send).dispatchDue();

        assertThat(delivery.getStatus()).isEqualTo(WebhookDeliveryStatus.DELIVERED);
    }

    @Test
    void wh05_calendarioFixoNaPolitica() {
        LocalDateTime end = LocalDateTime.of(2026, 11, 28, 12, 0);
        assertThat(IntStream.rangeClosed(1, 6).mapToObj(n -> retryPolicy.nextAttemptAt(n, end)).toList())
                .containsExactly(Optional.of(end.plusMinutes(1)), Optional.of(end.plusMinutes(5)),
                        Optional.of(end.plusMinutes(30)), Optional.of(end.plusHours(2)),
                        Optional.of(end.plusHours(12)), Optional.empty());
    }


    // ---- WH-06: registo de entregas, reenvio e limpeza ----

    @Test
    void wh06_registoMostraSoAsEntregasDaSubscricaoMaisRecentesPrimeiroSemCorpo() {
        WebhookDelivery older = delivery(80L, "https://loja.example.com/hooks", true);
        older.setCreatedAt(clock.now().minusHours(2));
        WebhookDelivery newer = delivery(81L, "https://loja.example.com/hooks", true);
        newer.setSubscription(older.getSubscription());
        newer.setCreatedAt(clock.now().minusHours(1));
        newer.setStatus(WebhookDeliveryStatus.DELIVERED);
        newer.setAttempts(2);
        newer.setLastHttpStatus(200);
        newer.setLastDurationMs(35L);
        WebhookDelivery sameTime = delivery(82L, "https://loja.example.com/hooks", true);
        sameTime.setSubscription(older.getSubscription());
        sameTime.setCreatedAt(clock.now().minusHours(1));
        delivery(83L, "https://outra.example.com/hooks", true).setCreatedAt(clock.now());

        List<WebhookDeliveryDTO> log = deliveries.listDeliveries(older.getSubscription().getId(), null, 0, null);

        assertThat(log).extracting(WebhookDeliveryDTO::id).containsExactly(82L, 81L, 80L);
        WebhookDeliveryDTO delivered = log.get(1);
        assertThat(delivered.eventId()).isEqualTo("evt_abc");
        assertThat(delivered.eventType()).isEqualTo("invoice.issued");
        assertThat(delivered.status()).isEqualTo(WebhookDeliveryStatus.DELIVERED);
        assertThat(delivered.attempts()).isEqualTo(2);
        assertThat(delivered.lastHttpStatus()).isEqualTo(200);
        assertThat(delivered.lastDurationMs()).isEqualTo(35L);
        assertThat(Arrays.stream(WebhookDeliveryDTO.class.getRecordComponents()).map(c -> c.getName()))
                .doesNotContain("body", "secret", "secretCiphertext");
        assertThat(log.toString()).doesNotContain("Lojas Ndlovu").doesNotContain(SECRET);
    }

    @Test
    void wh06_registoFiltraPorEstadoEPagina() {
        WebhookDelivery first = delivery(84L, "https://loja.example.com/hooks", true);
        first.setStatus(WebhookDeliveryStatus.FAILED);
        IntStream.rangeClosed(85, 87).forEach(i -> {
            WebhookDelivery d = delivery((long) i, "https://loja.example.com/hooks", true);
            d.setSubscription(first.getSubscription());
            d.setCreatedAt(clock.now().plusMinutes(i));
        });
        Long subscriptionId = first.getSubscription().getId();

        assertThat(deliveries.listDeliveries(subscriptionId, "failed", 0, null))
                .extracting(WebhookDeliveryDTO::id).containsExactly(84L);
        assertThat(deliveries.listDeliveries(subscriptionId, "PENDING", 0, 2))
                .extracting(WebhookDeliveryDTO::id).containsExactly(87L, 86L);
        assertThat(deliveries.listDeliveries(subscriptionId, "PENDING", 1, 2))
                .extracting(WebhookDeliveryDTO::id).containsExactly(85L);
        assertMessage("Estado de entrega desconhecido: PERDIDA.",
                () -> deliveries.listDeliveries(subscriptionId, "PERDIDA", 0, null));
        assertMessage("Tamanho de página inválido (1 a 100).",
                () -> deliveries.listDeliveries(subscriptionId, null, 0, 101));
        assertMessage("Tamanho de página inválido (1 a 100).",
                () -> deliveries.listDeliveries(subscriptionId, null, 0, 0));
    }

    @Test
    void wh06_registoSoParaOAdministradorENaEmpresaActiva() {
        WebhookDelivery delivery = delivery(88L, "https://loja.example.com/hooks", true);
        Long subscriptionId = delivery.getSubscription().getId();

        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
        assertMessage("Só o administrador gere os webhooks.",
                () -> deliveries.listDeliveries(subscriptionId, null, 0, null));

        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(2L);
        assertMessage("Subscrição não encontrada.", () -> deliveries.listDeliveries(subscriptionId, null, 0, null));
        assertMessage("Subscrição não encontrada.", () -> deliveries.listDeliveries(9999L, null, 0, null));
    }

    @Test
    void wh06_reenviarFalhadaVoltaAPendenteComAMesmaEntregaDespachoEAuditoria() {
        WebhookDelivery delivery = failed(90L, true);
        clock.advance(Duration.ofDays(1));

        WebhookDeliveryDTO result = deliveries.resend(90L);

        assertThat(result.id()).isEqualTo(90L);
        assertThat(result.status()).isEqualTo(WebhookDeliveryStatus.PENDING);
        assertThat(result.attempts()).isZero();
        assertThat(delivery.getStatus()).isEqualTo(WebhookDeliveryStatus.PENDING);
        assertThat(delivery.getAttempts()).isZero();
        assertThat(delivery.getNextAttemptAt()).isEqualTo(clock.now());
        assertThat(delivery.getBody()).isEqualTo(BODY);
        assertThat(delivery.getEventId()).isEqualTo("evt_abc");
        assertThat(delivery.getLastError()).as("fica até à nova tentativa").isEqualTo("Resposta HTTP 500.");
        assertThat(delivery.getLastHttpStatus()).isEqualTo(500);
        assertThat(published).containsExactly(new WebhookResendRequested(90L));
        ArgumentCaptor<String> details = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).logCurrent(eq("WEBHOOK_RESEND"), details.capture());
        assertThat(details.getValue()).contains("https://loja.example.com/hooks", "90", "evt_abc")
                .doesNotContain(SECRET).doesNotContain("Lojas Ndlovu");

        http.status = 500;
        sender.send(90L);
        assertThat(http.requests).hasSize(1);
        assertThat(http.requests.get(0).headers()).containsEntry("X-Multicore-Delivery", "90");
        assertThat(new String(http.requests.get(0).body(), StandardCharsets.UTF_8)).isEqualTo(BODY);
        assertThat(delivery.getAttempts()).isEqualTo(1);
        assertThat(delivery.getNextAttemptAt()).as("calendário recomeça").isEqualTo(clock.now().plusMinutes(1));
    }

    @Test
    void wh06_soSeReenviamFalhadasDeSubscricoesActivasDaEmpresa() {
        WebhookDelivery delivered = delivery(91L, "https://loja.example.com/hooks", true);
        delivered.setStatus(WebhookDeliveryStatus.DELIVERED);
        delivery(92L, "https://loja.example.com/hooks", true);
        failed(93L, false);
        failed(94L, true);

        assertMessage("Só se reenviam entregas falhadas.", () -> deliveries.resend(91L));
        assertMessage("Só se reenviam entregas falhadas.", () -> deliveries.resend(92L));
        assertMessage("Active a subscrição antes de reenviar.", () -> deliveries.resend(93L));
        assertMessage("Entrega não encontrada.", () -> deliveries.resend(9999L));
        CurrentUserContext.setCurrentCompanyId(2L);
        assertMessage("Entrega não encontrada.", () -> deliveries.resend(94L));
        CurrentUserContext.setCurrentCompanyId(1L);
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
        assertMessage("Só o administrador gere os webhooks.", () -> deliveries.resend(94L));

        assertThat(delivered.getStatus()).isEqualTo(WebhookDeliveryStatus.DELIVERED);
        assertThat(byId.get(93L).getStatus()).isEqualTo(WebhookDeliveryStatus.FAILED);
        assertThat(byId.get(94L).getStatus()).isEqualTo(WebhookDeliveryStatus.FAILED);
        assertThat(byId.get(94L).getAttempts()).isEqualTo(6);
        assertThat(published).isEmpty();
        verify(auditLogService, never()).logCurrent(anyString(), anyString());
    }

    @Test
    void wh06_reenvioDespachaSoDepoisDoCommitPeloListener() {
        List<Long> dispatched = new ArrayList<>();
        WebhookEventListener listener = new WebhookEventListener(mock(WebhookEventService.class), deliveries,
                dispatched::add);

        listener.onResend(new WebhookResendRequested(95L));

        assertThat(dispatched).containsExactly(95L);
    }

    @Test
    void wh06_limpezaApagaAsDeMaisDe30DiasEMantemAsMaisNovas() {
        delivery(96L, "https://loja.example.com/hooks", true).setCreatedAt(clock.now().minusDays(31));
        failed(97L, false).setCreatedAt(clock.now().minusDays(31));
        delivery(98L, "https://loja.example.com/hooks", false).setCreatedAt(clock.now().minusDays(45));
        delivery(99L, "https://loja.example.com/hooks", true).setCreatedAt(clock.now().minusDays(29));
        delivery(100L, "https://loja.example.com/hooks", true).setCreatedAt(clock.now());

        new WebhookDeliveryCleanupJob(deliveries, retryPolicy).purge();

        assertThat(byId.keySet()).containsExactlyInAnyOrder(99L, 100L);
    }

    @Test
    void wh06_limpezaApagaEmLotesDeNoMaximo500() {
        IntStream.rangeClosed(1, 1203).forEach(i -> delivery(10_000L + i, "https://loja.example.com/hooks", true)
                .setCreatedAt(clock.now().minusDays(40)));
        List<Integer> batches = new ArrayList<>();
        org.mockito.Mockito.doAnswer(invocation -> {
            LocalDateTime cutoff = invocation.getArgument(0);
            Pageable page = invocation.getArgument(1);
            List<Long> ids = byId.values().stream().filter(d -> d.getCreatedAt().isBefore(cutoff))
                    .map(WebhookDelivery::getId).sorted().limit(page.getPageSize()).toList();
            batches.add(ids.size());
            return ids;
        }).when(deliveryRepository).findIdsCreatedBefore(any(), any());

        new WebhookDeliveryCleanupJob(deliveries, retryPolicy).purge();

        assertThat(byId).isEmpty();
        assertThat(batches).containsExactly(500, 500, 203);
    }

    // ---- helpers ----

    private List<WebhookDelivery> log(Long subscriptionId, WebhookDeliveryStatus status, Pageable page) {
        return byId.values().stream()
                .filter(d -> d.getSubscription().getId().equals(subscriptionId))
                .filter(d -> status == null || d.getStatus() == status)
                .sorted(Comparator.comparing(WebhookDelivery::getCreatedAt).thenComparing(WebhookDelivery::getId)
                        .reversed())
                .skip(page.getOffset())
                .limit(page.getPageSize())
                .toList();
    }

    private WebhookDelivery failed(Long id, boolean activeSubscription) {
        WebhookDelivery delivery = delivery(id, "https://loja.example.com/hooks", activeSubscription);
        delivery.setStatus(WebhookDeliveryStatus.FAILED);
        delivery.setAttempts(6);
        delivery.setNextAttemptAt(null);
        delivery.setLastHttpStatus(500);
        delivery.setLastDurationMs(12L);
        delivery.setLastError("Resposta HTTP 500.");
        return delivery;
    }

    private static Company company(Long id) {
        Company company = new Company();
        company.setId(id);
        return company;
    }

    private static void assertMessage(String expected, Runnable action) {
        BusinessRuleException error = assertThrows(BusinessRuleException.class, action::run);
        assertThat(error.getMessage()).isEqualTo(expected);
    }

    private void assertPendingFailure(WebhookDelivery delivery, Integer httpStatus, String error) {
        assertThat(delivery.getStatus()).isEqualTo(WebhookDeliveryStatus.PENDING);
        assertThat(delivery.getAttempts()).isEqualTo(1);
        assertThat(delivery.getLastHttpStatus()).isEqualTo(httpStatus);
        assertThat(delivery.getLastDurationMs()).isNotNull().isGreaterThanOrEqualTo(0L);
        assertThat(delivery.getLastError()).isEqualTo(error).doesNotContain(SECRET);
        assertThat(delivery.getDeliveredAt()).isNull();
    }

    private static void assertUntouched(WebhookDelivery delivery, WebhookDeliveryStatus status) {
        assertThat(delivery.getStatus()).isEqualTo(status);
        assertThat(delivery.getAttempts()).isZero();
        assertThat(delivery.getLastHttpStatus()).isNull();
        assertThat(delivery.getLastError()).isNull();
    }

    private WebhookDelivery delivery(Long id, String url, boolean active) {
        WebhookSubscription subscription = new WebhookSubscription();
        subscription.setId(id + 1000);
        subscription.setCompany(company);
        subscription.setUrl(url);
        subscription.setActive(active);
        subscription.setEvents(EnumSet.of(WebhookEventType.INVOICE_ISSUED));
        subscription.setSecretCiphertext(WebhookSecrets.seal(crypto, SECRET));
        WebhookDelivery delivery = new WebhookDelivery();
        delivery.setId(id);
        delivery.setSubscription(subscription);
        delivery.setCompany(company);
        delivery.setCreatedAt(clock.now());
        delivery.setEventId("evt_abc");
        delivery.setEventType(WebhookEventType.INVOICE_ISSUED);
        delivery.setBody(BODY);
        delivery.setStatus(WebhookDeliveryStatus.PENDING);
        delivery.setNextAttemptAt(clock.now());
        byId.put(id, delivery);
        return delivery;
    }

    private static String hmacHex(String secret, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    private record SentRequest(URI url, Map<String, String> headers, byte[] body, Duration timeout) {
    }

    private static final class FakeHttpClient implements WebhookHttpClient {
        private final List<SentRequest> requests = new ArrayList<>();
        private int status = 200;
        private IOException failure;
        private Runnable onPost = () -> { };

        @Override
        public int post(URI url, Map<String, String> headers, byte[] body, Duration timeout) throws IOException {
            requests.add(new SentRequest(url, Map.copyOf(headers), body.clone(), timeout));
            onPost.run();
            if (failure != null) {
                throw failure;
            }
            return status;
        }
    }

    /** Clock the test moves by hand; the policy reads local date-times from it. */
    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(LocalDateTime start) {
            this.instant = start.toInstant(ZoneOffset.UTC);
        }

        LocalDateTime now() {
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
