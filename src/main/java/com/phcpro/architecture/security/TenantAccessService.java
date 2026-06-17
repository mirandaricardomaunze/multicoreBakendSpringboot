package com.phcpro.architecture.security;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.users.model.AppUser;
import com.phcpro.modules.users.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class TenantAccessService {

    private final AppUserRepository appUserRepository;

    public TenantAccessService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public AppUser requireActiveUser(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessRuleException("Utilizador autenticado em falta.");
        }
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessRuleException("Utilizador não encontrado."));
        if (!user.isActive()) {
            throw new BusinessRuleException("Utilizador inativo.");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public AppUser requireAccess(String username, Long companyId) {
        AppUser user = requireActiveUser(username);
        if (!user.hasCompany(companyId)) {
            throw new BusinessRuleException("O utilizador não tem acesso à empresa selecionada.");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public List<Company> getAccessibleCompanies(String username) {
        return requireActiveUser(username).getCompanies().stream()
                .sorted(Comparator.comparing(Company::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public void selectCompany(Long companyId) {
        AppUser user = requireAccess(CurrentUserContext.getUsername(), companyId);
        CurrentUserContext.setCurrentUser(user.getUsername(), user.getRoleForCompany(companyId));
        CurrentUserContext.setCurrentCompanyId(companyId);
    }
}
