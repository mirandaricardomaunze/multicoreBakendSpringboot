package mz.multicore.erp.modules.users.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.users.model.AppUser;

/**
 * A senha, o PIN, o nome e o estado valem em todas as empresas da conta. Quem os altera tem de
 * administrar todas elas: de outro modo, um administrador assumia acessos a empresas que não são suas.
 */
final class SharedAccountGuard {

    private SharedAccountGuard() {}

    static boolean controlsAccount(AppUser target, AppUser actor, Long companyId) {
        return target.getCompanyAccesses().stream()
                .map(access -> access.getCompany().getId())
                .filter(id -> !id.equals(companyId))
                .allMatch(id -> actor != null && actor.findCompanyAccess(id)
                        .filter(access -> "ADMIN".equalsIgnoreCase(access.getRole())).isPresent());
    }

    static void requireControlledAccount(AppUser target, AppUser actor, Long companyId) {
        if (!controlsAccount(target, actor, companyId)) {
            throw new BusinessRuleException("Esta conta também pertence a outra empresa. "
                    + "Só o administrador da plataforma a pode alterar.");
        }
    }
}
