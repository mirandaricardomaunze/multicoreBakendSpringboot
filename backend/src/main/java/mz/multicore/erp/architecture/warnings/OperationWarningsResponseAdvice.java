package mz.multicore.erp.architecture.warnings;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Sends the {@link OperationWarnings} of a successful request back as one {@value #HEADER} header per warning
 * (UTF-8, URL-encoded); the desktop shows each one as a toast. Nothing is blocked: the body is untouched.
 * Failed requests (status ≥ 400) discard the warnings.
 */
@ControllerAdvice
public class OperationWarningsResponseAdvice implements ResponseBodyAdvice<Object> {

    public static final String HEADER = "X-Operation-Warning";

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        List<String> warnings = OperationWarnings.drain();
        if (warnings.isEmpty() || failed(response)) return body;
        warnings.forEach(warning -> response.getHeaders().add(HEADER, URLEncoder.encode(warning, StandardCharsets.UTF_8)));
        return body;
    }

    private static boolean failed(ServerHttpResponse response) {
        return response instanceof ServletServerHttpResponse servlet && servlet.getServletResponse().getStatus() >= 400;
    }
}
