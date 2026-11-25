package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.AccountingApiClient;
import mz.multicore.erp.desktop.session.SignedInUser;
import mz.multicore.erp.gui.NotificationFeed.NotificationItem;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.accounting.dto.BudgetAlertDTO;

import java.util.List;

/**
 * Alertas do orçamento no sino (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.3, OR-UI-02): cada alerta 90%/100% do ano
 * gravado pelo backend vira uma notificação com o texto PT-MZ do backend que abre a Contabilidade. Só ADMIN e MANAGER
 * os vêem; sem perfil não há pedido.
 */
final class NotificationBudgetAlertProvider {

    static final String TYPE = "Orçamento";
    private static final String MODULE_CARD = "contabilidade";

    private NotificationBudgetAlertProvider() {}

    static void addBudgetAlerts(List<NotificationItem> items, AccountingApiClient accountingApiClient, int year) {
        if (accountingApiClient == null || !SignedInUser.isManagerOrAdmin()) return;
        try {
            List<BudgetAlertDTO> alerts = accountingApiClient.getBudgetAlerts(year);
            if (alerts == null) return;
            for (BudgetAlertDTO alert : alerts) {
                String detail = String.format("Real %s de %s orçados em custos", UIHelper.formatMzn(alert.actual()),
                        UIHelper.formatMzn(alert.budgeted()));
                String when = String.format("%02d/%d", alert.month(), alert.year());
                items.add(new NotificationItem(TYPE, alert.message(), detail, when, MODULE_CARD,
                        alert.thresholdPercent() >= 100 ? 3 : 2));
            }
        } catch (Exception ignored) {
            // O sino nunca falha por um fornecedor: sem alertas do orçamento, mostra os restantes.
        }
    }
}
