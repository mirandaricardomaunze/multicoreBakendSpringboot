package mz.multicore.erp.desktop.session;

import mz.multicore.erp.architecture.security.CurrentUserContext;

import java.util.Locale;

/**
 * O perfil de quem está com sessão iniciada <b>neste cliente</b>, para decidir o que se desenha.
 *
 * <p><b>Isto não é uma verificação de segurança, e a distinção é a razão de esta classe existir.</b>
 * Quem decide o que é permitido é o backend, que verifica o perfil no pedido HTTP; o que aqui se
 * decide é apenas se vale a pena mostrar um botão ou pedir dados que o servidor vai recusar. Um
 * cliente a perguntar a si próprio se é gestor responde sempre o que lhe convier — por isso a
 * resposta só pode governar pixels.
 *
 * <p>Antes disto o desktop chamava o {@code PermissionGuard} do backend, que vivia no artefacto
 * partilhado. Funcionava, mas punha o vocabulário da autorização dentro do jar instalado em cada
 * máquina de cliente, e tornava fácil alguém "verificar a permissão" no cliente e acreditar que
 * tinha feito alguma coisa. Ver docs/MULTI_MODULE_ARCHITECTURE_SPEC.md §3.
 *
 * <p>O papel vem do {@link CurrentUserContext}, que o próprio desktop popula no login. Contexto de
 * sessão é vocabulário partilhado; a decisão de autorização não é.
 */
public final class SignedInUser {

    public static final String SUPERADMIN = "SUPERADMIN";

    private SignedInUser() {
    }

    /** Papel activo, em maiúsculas e nunca nulo. */
    public static String role() {
        String role = CurrentUserContext.getRole();
        return role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(role());
    }

    public static boolean isManagerOrAdmin() {
        String role = role();
        return "ADMIN".equals(role) || "MANAGER".equals(role);
    }

    public static boolean isSuperAdmin() {
        return SUPERADMIN.equals(role());
    }
}
