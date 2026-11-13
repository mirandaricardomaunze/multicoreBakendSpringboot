package mz.multicore.erp.modules.users.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.repository.AppUserCompanyAccessRepository;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Porta de leitura de utilizadores e dos seus acessos a empresas para os outros módulos
 * (docs/MODULE_DECOUPLING_SPEC.md §2). É o único ponto pelo qual um módulo alheio lê {@link AppUser};
 * escritas passam por {@link AppUserCommandService}. As entidades devolvidas servem só para relações JPA
 * dentro do backend e nunca saem para um Controller.
 */
@Service
@Transactional(readOnly = true)
public class AppUserLookup {

    static final String NOT_FOUND = "Utilizador não encontrado.";

    private final AppUserRepository userRepository;
    private final AppUserCompanyAccessRepository accessRepository;

    public AppUserLookup(AppUserRepository userRepository, AppUserCompanyAccessRepository accessRepository) {
        this.userRepository = userRepository;
        this.accessRepository = accessRepository;
    }

    /** Utilizador pelo id; {@code BusinessRuleException("Utilizador não encontrado.")} se não existir. */
    public AppUser require(Long id) {
        return find(id).orElseThrow(() -> new BusinessRuleException(NOT_FOUND));
    }

    public Optional<AppUser> find(Long id) {
        return id == null ? Optional.empty() : userRepository.findById(id);
    }

    /** Utilizador pelo username; {@code BusinessRuleException("Utilizador não encontrado.")} se não existir. */
    public AppUser requireByUsername(String username) {
        return findByUsername(username).orElseThrow(() -> new BusinessRuleException(NOT_FOUND));
    }

    public Optional<AppUser> findByUsername(String username) {
        return username == null ? Optional.empty() : userRepository.findByUsername(username);
    }

    /** Todos os utilizadores da plataforma (visão do superadmin). */
    public List<AppUser> listAll() {
        return userRepository.findAll();
    }

    /** Utilizadores com acesso à empresa, ordenados pelo nome. */
    public List<AppUser> listForCompany(Long companyId) {
        return userRepository.findDistinctByCompanyAccessesCompanyIdOrderByName(companyId);
    }

    /** Acessos à empresa com o papel indicado cujo utilizador está activo. */
    public long countActiveByCompanyAndRole(Long companyId, String role) {
        return accessRepository.countActiveByCompanyIdAndRole(companyId, role);
    }

    /** Número de acessos (utilizadores) concedidos à empresa. */
    public long countAccessesForCompany(Long companyId) {
        return accessRepository.countByCompanyId(companyId);
    }
}
