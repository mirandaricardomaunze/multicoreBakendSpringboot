package mz.multicore.erp.architecture.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter.XFrameOptionsMode;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;

/**
 * Cabeçalhos de segurança das páginas do portal web do gestor (PORTAL_GESTOR_SPEC §3.5, PG-09).
 *
 * <p>Em {@code /portal} e {@code /portal/**}: {@code Content-Security-Policy} só com {@code 'self'}/{@code 'none'}
 * (sem {@code unsafe-inline} nem origens externas), {@code X-Frame-Options: DENY} e
 * {@code Referrer-Policy: no-referrer}. O resto da aplicação mantém o {@code SAMEORIGIN} de que a consola
 * H2 precisa em desenvolvimento.
 */
public final class PortalSecurityHeaders {

    public static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self'",
            "img-src 'self'",
            "connect-src 'self'",
            "manifest-src 'self'",
            "font-src 'self'",
            "object-src 'none'",
            "base-uri 'none'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    /** Corresponde a {@code /portal} e a tudo o que está por baixo, segmento inteiro ({@code /portalx} fica de fora). */
    public static final RequestMatcher PORTAL_PAGES = PortalSecurityHeaders::isPortalPath;

    private PortalSecurityHeaders() {
    }

    /** Escritores a registar no {@code HttpSecurity.headers()} em vez do {@code frameOptions} global. */
    public static List<HeaderWriter> writers() {
        RequestMatcher rest = new NegatedRequestMatcher(PORTAL_PAGES);
        return List.of(
                new DelegatingRequestMatcherHeaderWriter(PORTAL_PAGES, new XFrameOptionsHeaderWriter(XFrameOptionsMode.DENY)),
                new DelegatingRequestMatcherHeaderWriter(PORTAL_PAGES,
                        new StaticHeadersWriter("Content-Security-Policy", CONTENT_SECURITY_POLICY)),
                new DelegatingRequestMatcherHeaderWriter(PORTAL_PAGES,
                        new ReferrerPolicyHeaderWriter(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)),
                // Consola H2 (dev) e restantes rotas: comportamento anterior.
                new DelegatingRequestMatcherHeaderWriter(rest, new XFrameOptionsHeaderWriter(XFrameOptionsMode.SAMEORIGIN)));
    }

    static boolean isPortalPath(HttpServletRequest request) {
        String path = RequestPaths.withinApplication(request);
        return path.equals("/portal") || path.startsWith("/portal/");
    }
}
