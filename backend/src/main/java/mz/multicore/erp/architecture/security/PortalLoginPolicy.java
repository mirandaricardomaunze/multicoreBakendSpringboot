package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.users.model.AppUser;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Regras de entrada pelo portal web do gestor (PORTAL_GESTOR_SPEC §3.1).
 * O login do desktop não passa por aqui: só pedidos com o canal {@code PORTAL}.
 */
@Component
public class PortalLoginPolicy {

    public static final String PORTAL_CHANNEL = "PORTAL";
    static final String ROLE_REFUSED_MESSAGE =
            "O portal é só para administradores e gestores. Use o programa da loja.";
    static final String TWO_FACTOR_REFUSED_MESSAGE =
            "Active a verificação em dois passos no programa da loja para usar o portal.";
    static final String COMPANY_REFUSED_MESSAGE =
            "O portal só abre empresas onde é administrador ou gestor.";
    private static final Set<String> PORTAL_ROLES = Set.of("ADMIN", "MANAGER");

    public boolean isPortal(String channel) {
        return channel != null && PORTAL_CHANNEL.equalsIgnoreCase(channel.trim());
    }

    /**
     * PG-01: entra quem é ADMIN ou MANAGER em pelo menos uma empresa a que já tem acesso.
     * PG-02: e só com a verificação em dois passos já activa; o código é pedido depois, como no desktop.
     */
    public void requireAllowed(AppUser user) {
        if (!allowedPortalRole(user)) {
            throw new BusinessRuleException(ROLE_REFUSED_MESSAGE);
        }
        if (user.getTotpSecret() == null) {
            throw new BusinessRuleException(TWO_FACTOR_REFUSED_MESSAGE);
        }
    }

    /** Empresas que entram na sessão de portal: só aquelas onde o perfil é ADMIN ou MANAGER. */
    public boolean allowsCompanyRole(String companyRole) {
        return companyRole != null && PORTAL_ROLES.contains(companyRole.toUpperCase());
    }

    /** PG-06: um pedido do portal só entra numa empresa onde o perfil é ADMIN ou MANAGER. */
    public void requireCompanyRole(String companyRole) {
        if (!allowsCompanyRole(companyRole)) {
            throw new BusinessRuleException(COMPANY_REFUSED_MESSAGE);
        }
    }

    private boolean allowedPortalRole(AppUser user) {
        return user.getCompanyAccesses().stream().anyMatch(access -> allowsCompanyRole(access.getRole()));
    }
}
