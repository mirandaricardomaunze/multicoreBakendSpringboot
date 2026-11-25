package mz.multicore.erp.architecture.security;

import jakarta.servlet.http.HttpServletRequest;

/** Caminho do pedido dentro da aplicação (sem o context path), para comparar com rotas fixas. */
final class RequestPaths {

    private RequestPaths() {
    }

    static String withinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)
                ? uri.substring(contextPath.length()) : uri;
    }

    /** {@code path} é {@code prefix} ou está por baixo dele, segmento inteiro ({@code /portalx} não está em {@code /portal}). */
    static boolean isUnder(String path, String prefix) {
        return path != null && (path.equals(prefix) || path.startsWith(prefix + "/"));
    }
}
