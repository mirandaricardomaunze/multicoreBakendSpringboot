package mz.multicore.erp.modules.webhooks.service;

import mz.multicore.erp.architecture.security.AttachmentCrypto;
import mz.multicore.erp.modules.webhooks.model.WebhookDelivery;
import mz.multicore.erp.modules.webhooks.model.WebhookDeliveryStatus;
import mz.multicore.erp.modules.webhooks.model.WebhookEventType;
import mz.multicore.erp.modules.webhooks.model.WebhookSubscription;
import mz.multicore.erp.modules.webhooks.repository.WebhookDeliveryRepository;
import mz.multicore.erp.modules.webhooks.repository.WebhookSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
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

    private WebhookDeliveryService deliveries;
    private WebhookSender sender;

    @BeforeEach
    void setUp() throws Exception {
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
        deliveries = new WebhookDeliveryService(deliveryRepository, subscriptionRepository, retryPolicy);
        sender = new WebhookSender(deliveries, validator, http, crypto);
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

    // ---- helpers ----

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
        subscription.setUrl(url);
        subscription.setActive(active);
        subscription.setEvents(EnumSet.of(WebhookEventType.INVOICE_ISSUED));
        subscription.setSecretCiphertext(WebhookSecrets.seal(crypto, SECRET));
        WebhookDelivery delivery = new WebhookDelivery();
        delivery.setId(id);
        delivery.setSubscription(subscription);
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
