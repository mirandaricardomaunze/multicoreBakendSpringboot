package mz.multicore.erp.modules.webhooks.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.PermissionGuard;

/** The one rule for who manages webhooks: only the ADMIN (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.1.1, §4.3.3). */
final class WebhookAdmin {

    private WebhookAdmin() {
    }

    static void require() {
        if (!PermissionGuard.isAdmin()) {
            throw new BusinessRuleException("Só o administrador gere os webhooks.");
        }
    }
}
