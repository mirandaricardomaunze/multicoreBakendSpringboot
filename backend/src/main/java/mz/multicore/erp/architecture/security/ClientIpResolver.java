package mz.multicore.erp.architecture.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/** Aceita X-Forwarded-For apenas da instancia de proxy declarada pelo operador. */
@Component
public class ClientIpResolver {

    private final String trustedProxyHost;

    public ClientIpResolver(@Value("${security.trusted-proxy-host:}") String trustedProxyHost) {
        this.trustedProxyHost = trustedProxyHost == null ? "" : trustedProxyHost.trim();
    }

    public String resolve(HttpServletRequest request) {
        if (request == null) return "local";
        String remote = request.getRemoteAddr();
        if (remote == null || remote.isBlank()) return "local";
        if (isTrustedProxy(remote)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null) {
                String first = forwarded.split(",", 2)[0].trim();
                if (first.length() <= 64 && first.matches("[0-9.]+|[0-9a-fA-F:]+")) {
                    return first;
                }
            }
        }
        return remote;
    }

    private boolean isTrustedProxy(String remote) {
        if (trustedProxyHost.isBlank()) return false;
        try {
            InetAddress address = InetAddress.getByName(remote);
            return Arrays.stream(InetAddress.getAllByName(trustedProxyHost))
                    .anyMatch(trusted -> trusted.equals(address));
        } catch (UnknownHostException ex) {
            return false;
        }
    }
}
