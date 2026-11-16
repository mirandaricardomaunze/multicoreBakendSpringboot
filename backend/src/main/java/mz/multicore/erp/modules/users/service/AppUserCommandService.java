package mz.multicore.erp.modules.users.service;

import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Escrita de utilizadores pedida por outros módulos (plataforma, seeders), sem expor o
 * {@link AppUserRepository} (docs/MODULE_DECOUPLING_SPEC.md §2). Leituras passam por {@link AppUserLookup}.
 */
@Service
@Transactional
public class AppUserCommandService {

    private final AppUserRepository userRepository;

    public AppUserCommandService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AppUser saveUser(AppUser user) {
        return userRepository.save(user);
    }

    public List<AppUser> saveUsers(List<AppUser> users) {
        return userRepository.saveAll(users);
    }
}
