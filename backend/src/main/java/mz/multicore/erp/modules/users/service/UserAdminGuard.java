package mz.multicore.erp.modules.users.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.users.model.AppUser;

/**
 * Barreiras partilhadas da gestão de utilizadores: perfil administrador da sessão e pertença do
 * utilizador alvo à empresa activa (nunca administradores de plataforma).
 */
public final class UserAdminGuard {

    private UserAdminGuard() {
    }

    public static void requireAdmin() {
        if (!"ADMIN".equalsIgnoreCase(CurrentUserContext.getRole())) {
            throw new BusinessRuleException("Apenas administradores podem gerir utilizadores.");
        }
    }

    public static void requireManagedUser(AppUser user, Long companyId) {
        if (user.isPlatformAdmin() || user.findCompanyAccess(companyId).isEmpty()) {
            throw new BusinessRuleException("O utilizador não pertence à empresa activa.");
        }
    }
}
