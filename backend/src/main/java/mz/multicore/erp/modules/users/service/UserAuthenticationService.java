package mz.multicore.erp.modules.users.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autenticação por utilizador/senha com mensagem genérica (sem enumeração de contas) e migração
 * suave de senhas legadas em texto-plano para BCrypt no primeiro login válido.
 */
@Service
public class UserAuthenticationService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAuthenticationService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser authenticate(String username, String password) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessRuleException("Utilizador ou senha incorrectos."));

        if (!user.isActive()) {
            throw new BusinessRuleException("Utilizador ou senha incorrectos.");
        }

        String stored = user.getPassword();
        boolean ok;
        if (isBcryptHash(stored)) {
            ok = passwordEncoder.matches(password, stored);
        } else {
            // Migração suave: password legada em texto-plano. Aceita uma vez e re-encripta.
            ok = stored != null && stored.equals(password);
            if (ok) {
                user.setPassword(passwordEncoder.encode(password));
                appUserRepository.save(user);
            }
        }

        if (!ok) {
            throw new BusinessRuleException("Utilizador ou senha incorrectos.");
        }
        return user;
    }

    private static boolean isBcryptHash(String value) {
        return value != null
                && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }
}
