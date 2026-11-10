package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.AdvancesApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.advances.dto.CustomerAdvanceDTO;
import mz.multicore.erp.modules.advances.model.CustomerAdvanceStatus;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;

import javax.swing.JComponent;
import java.math.BigDecimal;
import java.util.List;

/**
 * After an invoice is issued, reminds the user when the client still has an advance to apply
 * ({@code docs/ADIANTAMENTOS_SPEC.md}). The advances module registers its client; without it, or for walk-in
 * clients, nothing happens. It only reminds: applying the advance stays a deliberate step in Adiantamentos.
 */
public final class AdvanceReminder {

    private static volatile AdvancesApiClient api;

    private AdvanceReminder() {
    }

    public static void register(AdvancesApiClient advancesApi) {
        api = advancesApi;
    }

    /** Checks in the background and shows a notice on {@code scope} when there is an open advance. */
    public static void afterInvoice(JComponent scope, InvoiceDTO invoice) {
        AdvancesApiClient client = api;
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (client == null || invoice == null || invoice.clientId() == null || companyId == null) return;
        UIHelper.loadAsync(scope, () -> client.list(companyId), all -> {
            String message = message(all, invoice);
            if (message != null) ToastManager.show(scope, FeedbackType.INFO, message);
        }, error -> { });
    }

    /** Notice text, or {@code null} when the client of the invoice has no open advance. */
    static String message(List<CustomerAdvanceDTO> advances, InvoiceDTO invoice) {
        BigDecimal open = advances.stream()
                .filter(a -> a.status() == CustomerAdvanceStatus.OPEN && invoice.clientId().equals(a.clientId())
                        && !invoice.id().equals(a.advanceInvoiceId()))
                .map(CustomerAdvanceDTO::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (open.signum() == 0) return null;
        return invoice.clientName() + " tem " + UIHelper.formatMzn(open) + " de sinal por aplicar. Para o descontar na fatura "
                + invoice.invoiceNumber() + ", abra Adiantamentos e use \"Aplicar na Fatura\".";
    }
}
