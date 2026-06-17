package com.phcpro.architecture.security;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.modules.users.model.AppUser;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthSessionService {

    private static final Duration SESSION_LIFETIME = Duration.ofHours(8);
    private final Map<String, AuthSession> sessions = new ConcurrentHashMap<>();

    public AuthSession create(AppUser user) {
        Instant now = Instant.now();
        AuthSession session = new AuthSession(
                UUID.randomUUID().toString(),
                user.getUsername(),
                now,
                now.plus(SESSION_LIFETIME)
        );
        sessions.put(session.token(), session);
        return session;
    }

    public AuthSession requireValid(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessRuleException("Token de autenticação em falta.");
        }
        AuthSession session = sessions.get(token);
        if (session == null || session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            throw new BusinessRuleException("Sessão inválida ou expirada.");
        }
        return session;
    }

    public void revoke(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    public record AuthSession(String token, String username, Instant issuedAt, Instant expiresAt) {}
}
