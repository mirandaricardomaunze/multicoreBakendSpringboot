package mz.multicore.erp.architecture.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SecurityInterceptor securityInterceptor;
    private final mz.multicore.erp.architecture.version.ClientVersionInterceptor clientVersionInterceptor;

    public WebConfig(SecurityInterceptor securityInterceptor,
                     mz.multicore.erp.architecture.version.ClientVersionInterceptor clientVersionInterceptor) {
        this.securityInterceptor = securityInterceptor;
        this.clientVersionInterceptor = clientVersionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // A verificação de versão vem PRIMEIRO e cobre também o login: o melhor momento para
        // dizer "actualize o programa" é ao entrar, não a meio de uma venda. Exclui-se
        // /api/version para que um cliente bloqueado ainda consiga perguntar qual é a versão boa.
        registry.addInterceptor(clientVersionInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/version");

        registry.addInterceptor(securityInterceptor)
                .addPathPatterns("/api/**")
                // Login e logout só precisam do corpo/token; não exigem empresa (o superadmin não tem).
                .excludePathPatterns("/api/auth/login", "/api/auth/logout", "/api/version");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("Content-Type", "Authorization", "X-Company-Id")
                .exposedHeaders("X-Company-Id");
    }
}
