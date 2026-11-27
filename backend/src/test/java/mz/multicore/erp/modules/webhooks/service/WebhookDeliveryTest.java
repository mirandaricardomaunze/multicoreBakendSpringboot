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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Harness WH-04 (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.3, §4.3.1): one attempt posts the recorded body as is, with the
 * event, the stable delivery id and an HMAC-SHA256 signature made with the decrypted secret; 2xx delivers, any failure
 * keeps the delivery {@code PENDING} with the attempt recorded; the URL is checked again before connecting; inactive
 * subscriptions and non-pending deliveries send nothing.
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

    private WebhookSender sender;

    @BeforeEach
    void setUp() throws Exception {
        dns.put("loja.example.com", List.of(InetAddress.getByName("93.184.216.34")));
        when(deliveryRepository.findById(any()))
                .thenAnswer(invocation -> Optional.ofNullable(byId.get(invocation.<Long>getArgument(0))));
        WebhookAddressResolver resolver = host -> {
            List<InetAddress> addresses = dns.get(host);
            if (addresses == null) throw new UnknownHostException(host);
            return addresses;
        };
        WebhookUrlValidator validator = new WebhookUrlValidator(new MockEnvironment(), resolver);
        WebhookDeliveryService deliveries = new WebhookDeliveryService(deliveryRepository, subscriptionRepository);
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
        sender.send(51L);

        assertThat(http.requests).hasSize(2);
        assertThat(http.requests).extracting(r -> r.headers().get("X-Multicore-Delivery")).containsOnly("51");
        assertThat(http.requests).extracting(r -> r.headers().get("X-Multicore-Signature"))
                .containsOnly(http.requests.get(0).headers().get("X-Multicore-Signature"));
        assertThat(byId.get(51L).getAttempts()).isEqualTo(2);
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
        delivery.setNextAttemptAt(LocalDateTime.now());
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

        @Override
        public int post(URI url, Map<String, String> headers, byte[] body, Duration timeout) throws IOException {
            requests.add(new SentRequest(url, Map.copyOf(headers), body.clone(), timeout));
            if (failure != null) {
                throw failure;
            }
            return status;
        }
    }
}
