package mz.multicore.erp.modules.webhooks.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Webhook URL rules (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.1.1, WH-01): absolute, with a host, without user/password;
 * {@code https} with the {@code prod} profile ({@code http} also accepted elsewhere); and no private or local
 * address, checked on every address the host resolves to (anti-SSRF). Also meant to run before each delivery.
 */
@Component
public class WebhookUrlValidator {

    static final int MAX_URL_LENGTH = 500;
    private static final Pattern IPV4_LITERAL = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");

    private final boolean httpsRequired;
    private final WebhookAddressResolver resolver;

    public WebhookUrlValidator(Environment environment, WebhookAddressResolver resolver) {
        this.httpsRequired = environment.acceptsProfiles(Profiles.of("prod"));
        this.resolver = resolver;
    }

    /** Returns the trimmed URL when it may receive webhooks, else throws with the PT-MZ reason. */
    public String validate(String raw) {
        String url = raw == null ? "" : raw.trim();
        if (url.isEmpty()) {
            throw new BusinessRuleException("Indique o URL do webhook.");
        }
        URI uri = parse(url);
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("https") && (httpsRequired || !scheme.equals("http"))) {
            throw new BusinessRuleException("O URL do webhook tem de usar https://.");
        }
        requirePublicHost(uri.getHost());
        return url;
    }

    private static URI parse(String url) {
        if (url.length() > MAX_URL_LENGTH) {
            throw invalid();
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException ex) {
            throw invalid();
        }
        if (!uri.isAbsolute() || uri.getHost() == null || uri.getHost().isBlank() || uri.getRawUserInfo() != null) {
            throw invalid();
        }
        return uri;
    }

    private void requirePublicHost(String rawHost) {
        String host = rawHost.toLowerCase(Locale.ROOT);
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        if (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        if (host.equals("localhost") || host.endsWith(".localhost")) {
            throw privateAddress();
        }
        List<InetAddress> addresses = resolve(host);
        if (addresses.isEmpty()) {
            throw unresolved();
        }
        if (addresses.stream().anyMatch(WebhookUrlValidator::isPrivateOrLocal)) {
            throw privateAddress();
        }
    }

    private List<InetAddress> resolve(String host) {
        try {
            if (host.contains(":") || IPV4_LITERAL.matcher(host).matches()) {
                // IP literal: parsed by the JVM without any DNS lookup.
                return List.of(InetAddress.getByName(host));
            }
            return resolver.resolve(host);
        } catch (UnknownHostException ex) {
            throw unresolved();
        }
    }

    static boolean isPrivateOrLocal(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            return isPrivateIpv4(bytes);
        }
        if (address instanceof Inet6Address && isIpv4Mapped(bytes)) {
            return isPrivateIpv4(Arrays.copyOfRange(bytes, 12, 16));
        }
        int first = bytes[0] & 0xFF;
        int second = bytes[1] & 0xFF;
        return address.isAnyLocalAddress()                       // ::
                || address.isLoopbackAddress()                   // ::1
                || (first & 0xFE) == 0xFC                        // fc00::/7
                || (first == 0xFE && (second & 0xC0) == 0x80);   // fe80::/10
    }

    private static boolean isIpv4Mapped(byte[] bytes) {
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return (bytes[10] & 0xFF) == 0xFF && (bytes[11] & 0xFF) == 0xFF;
    }

    private static boolean isPrivateIpv4(byte[] bytes) {
        int first = bytes[0] & 0xFF;
        int second = bytes[1] & 0xFF;
        return first == 0 || first == 10 || first == 127
                || (first == 169 && second == 254)
                || (first == 172 && second >= 16 && second <= 31)
                || (first == 192 && second == 168);
    }

    private static BusinessRuleException invalid() {
        return new BusinessRuleException("O URL do webhook é inválido.");
    }

    private static BusinessRuleException privateAddress() {
        return new BusinessRuleException("Não são permitidos endereços privados ou locais no webhook.");
    }

    private static BusinessRuleException unresolved() {
        return new BusinessRuleException("Não foi possível resolver o endereço do webhook.");
    }
}
