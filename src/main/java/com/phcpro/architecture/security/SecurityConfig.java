package com.phcpro.architecture.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração de segurança baseline. Hoje permite todos os pedidos para não quebrar o desktop
 * (que ainda chama Services directamente, sem passar pela API). À medida que a migração para
 * HTTP avança (ver ARCHITECTURE.md §7), endpoints devem ser progressivamente restringidos:
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(f -> f.sameOrigin())) // H2 console
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
