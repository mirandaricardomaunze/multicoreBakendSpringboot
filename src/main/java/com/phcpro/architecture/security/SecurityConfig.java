package com.phcpro.architecture.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração de segurança endurecida para hospedagem: o {@link TokenAuthenticationFilter} valida o
 * token opaco e o filtro recusa {@code /api/**} sem token válido (login/logout e health públicos).
 * O {@link SecurityInterceptor} continua por cima a resolver empresa/papel e a auditar. As chamadas
 * em processo do desktop não passam por este filtro (não são HTTP), pelo que não são afectadas.
 * Exemplo do endurecimento aplicado:
 *
 *   .authorizeHttpRequests(auth -> auth
 *       .requestMatchers("/api/auth/**").permitAll()
 *       .requestMatchers("/api/admin/**").hasRole("ADMIN")
 *       .anyRequest().authenticated()
 *   )
 *
 * O {@link PasswordEncoder} já é BCrypt — usado em {@code AppUserService} para hash novo
 * e migração suave dos hashes em texto-plano legados.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Stub vazio para suprimir {@code UserDetailsServiceAutoConfiguration} (que gera uma
     * password aleatória no arranque). A autenticação real é feita pelo desktop via
     * {@code AppUserService.authenticate(...)}. Quando existir login HTTP, substituir este
     * stub por um adaptador que delega ao {@code AppUserService}.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   TokenAuthenticationFilter tokenAuthenticationFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(f -> f.sameOrigin())) // H2 console (dev)
                // Defense-in-depth: o Spring Security recusa /api/** sem token válido (o filtro popula
                // o SecurityContext). Login/logout e o health check ficam públicos. O SecurityInterceptor
                // continua a resolver empresa/papel e a auditar por cima disto.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/logout").permitAll()
                        // Versão é pública como o health: o desktop pergunta-a ANTES do login,
                        // e um cliente bloqueado por ser antigo tem de a poder consultar.
                        .requestMatchers("/api/version").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(e -> e.authenticationEntryPoint((request, response, ex) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Autenticação necessária.")))
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
