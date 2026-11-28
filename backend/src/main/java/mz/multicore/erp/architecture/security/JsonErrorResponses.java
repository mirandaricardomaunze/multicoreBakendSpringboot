package mz.multicore.erp.architecture.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** JSON {@code {code, message}} refusals written by the security filters, before any controller runs. */
final class JsonErrorResponses {

    private JsonErrorResponses() {
    }

    /** {@code message} is a fixed PT-MZ text of the filter, never user input. */
    static void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }

    /** 429 {@code RATE_LIMIT_EXCEEDED} with {@code Retry-After} in seconds. */
    static void tooManyRequests(HttpServletResponse response, long retryAfterSeconds, String message)
            throws IOException {
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        write(response, 429, "RATE_LIMIT_EXCEEDED", message);
    }
}
