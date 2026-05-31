package com.phcpro.modules.users.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.modules.users.model.AppUser;
import com.phcpro.modules.users.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;

    public AppUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<AppUser> getAllUsers() {
        return appUserRepository.findAll();
    }

    @Transactional(readOnly = true)
    public AppUser findByUsername(String username) {
        return appUserRepository.findByUsername(username).orElse(null);
    }

    @Transactional
    public AppUser authenticate(String username, String password) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessRuleException("Utilizador não encontrado."));

        if (!user.isActive()) {
            throw new BusinessRuleException("Utilizador inativo.");
        }

        if (!user.getPassword().equals(password)) {
            throw new BusinessRuleException("Senha incorreta.");
        }

        return user;
    }

    @Transactional
    public AppUser createUser(String username, String name, String password, String role) {
        Optional<AppUser> existing = appUserRepository.findByUsername(username);
        if (existing.isPresent()) {
            throw new BusinessRuleException("Utilizador já existe.");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setName(name);
        user.setPassword(password);
        user.setRole(role);
        user.setActive(true);
        user.setCreatedBy("SYSTEM");

        return appUserRepository.save(user);
    }
}
