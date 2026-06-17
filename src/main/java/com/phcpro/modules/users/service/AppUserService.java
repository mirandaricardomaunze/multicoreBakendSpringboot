package com.phcpro.modules.users.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.users.model.AppUser;
import com.phcpro.modules.users.model.AppUserCompanyAccess;
import com.phcpro.modules.users.model.UserRole;
import com.phcpro.modules.users.repository.AppUserCompanyAccessRepository;
import com.phcpro.modules.users.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompanyRepository companyRepository;
    private final AppUserCompanyAccessRepository companyAccessRepository;

    public AppUserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
                          CompanyRepository companyRepository,
                          AppUserCompanyAccessRepository companyAccessRepository) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.companyRepository = companyRepository;
        this.companyAccessRepository = companyAccessRepository;
    }

    @Transactional(readOnly = true)
    public List<AppUser> getAllUsers() {
        requireAdmin();
        return appUserRepository.findDistinctByCompanyAccessesCompanyIdOrderByName(
                CurrentUserContext.getCurrentCompanyId());
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

        String stored = user.getPassword();
        boolean ok;
        if (isBcryptHash(stored)) {
            ok = passwordEncoder.matches(password, stored);
        } else {
            // Migração suave: password legada em texto-plano. Aceita uma vez e re-encripta.
            ok = stored.equals(password);
            if (ok) {
                user.setPassword(passwordEncoder.encode(password));
                appUserRepository.save(user);
            }
        }

        if (!ok) {
            throw new BusinessRuleException("Senha incorreta.");
        }
        return user;
    }

    @Transactional
    public AppUser createUser(String username, String name, String password, String role) {
        requireAdmin();
        Optional<AppUser> existing = appUserRepository.findByUsername(username);
        if (existing.isPresent()) {
            throw new BusinessRuleException("Utilizador já existe.");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setName(name);
        user.setPassword(passwordEncoder.encode(password));
        String normalizedRole = UserRole.normalize(role);
        user.setRole(normalizedRole);
        user.setActive(true);
        Company company = companyRepository.findById(CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa ativa não encontrada."));
        user.grantCompany(company, normalizedRole);
        user.setCreatedBy(CurrentUserContext.getUsername());

        return appUserRepository.save(user);
    }

    @Transactional
    public AppUser grantCompanyAccess(String username, Long companyId) {
        return grantCompanyAccess(username, companyId, "EMPLOYEE");
    }

    @Transactional
    public AppUser grantCompanyAccess(String username, Long companyId, String role) {
        requireAdmin();
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessRuleException("Utilizador não encontrado."));
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));
        user.grantCompany(company, role);
        return appUserRepository.save(user);
    }

    @Transactional
    public AppUser updateCompanyRole(String username, String role) {
        requireAdmin();
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        String normalizedRole = UserRole.normalize(role);
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessRuleException("Utilizador não encontrado."));
        AppUserCompanyAccess access = user.findCompanyAccess(companyId)
                .orElseThrow(() -> new BusinessRuleException("O utilizador não pertence à empresa ativa."));

        boolean removingAdmin = "ADMIN".equalsIgnoreCase(access.getRole())
                && !"ADMIN".equals(normalizedRole);
        if (removingAdmin && companyAccessRepository.countByCompanyIdAndRoleIgnoreCase(companyId, "ADMIN") <= 1) {
            throw new BusinessRuleException("A empresa deve manter pelo menos um administrador.");
        }

        access.setRole(normalizedRole);
        return appUserRepository.save(user);
    }

    private void requireAdmin() {
        if (!"ADMIN".equalsIgnoreCase(CurrentUserContext.getRole())) {
            throw new BusinessRuleException("Apenas administradores podem gerir utilizadores.");
        }
    }

    private boolean isBcryptHash(String value) {
        return value != null
                && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }
}
