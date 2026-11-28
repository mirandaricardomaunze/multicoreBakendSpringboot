package mz.multicore.erp.architecture.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
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
                // A API pública é para sistemas externos, não para o desktop: sem verificação de versão.
                .excludePathPatterns("/api/version", PublicApiRoutes.PATTERN, OnlineShopRoutes.PATTERN);

        registry.addInterceptor(securityInterceptor)
                .addPathPatterns("/api/**")
                // Login e logout só precisam do corpo/token; não exigem empresa (o superadmin não tem).
                // A API pública autentica por X-Api-Key no ApiKeyAuthenticationFilter; a empresa é a da chave. API-03.
                // A loja online é pública (sem login): a empresa é a da loja do código no endereço. LO-04.
                .excludePathPatterns("/api/auth/**", "/api/version", PublicApiRoutes.PATTERN, OnlineShopRoutes.PATTERN);
    }

    /**
     * Portal web do gestor (PORTAL_GESTOR_SPEC §3.5): {@code /portal} e {@code /portal/} abrem a página estática.
     * Forward (não redirect) para não gerar um {@code Location} http atrás do proxy HTTPS.
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/portal").setViewName("forward:/portal/index.html");
        registry.addViewController("/portal/").setViewName("forward:/portal/index.html");
        // Loja online (LOJA_ONLINE_SPEC §3.1–3.2, LO-11): /loja/{código} abre a mesma página, que lê o código do
        // endereço. O código só tem minúsculas, números e hífen, por isso /loja/loja.js e afins não são apanhados.
        registry.addViewController("/loja/{code:[a-z0-9-]+}").setViewName("forward:/loja/index.html");
        registry.addViewController("/loja/{code:[a-z0-9-]+}/").setViewName("forward:/loja/index.html");
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
