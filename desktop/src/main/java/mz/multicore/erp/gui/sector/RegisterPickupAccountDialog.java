package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.PickupApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.pickup.dto.EligibleInvoiceDTO;
import mz.multicore.erp.modules.pickup.dto.OpenPickupAccountRequest;
import mz.multicore.erp.modules.pickup.dto.PickupAccountDTO;

import javax.swing.*;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/**
 * Registers an issued invoice as paid material the client will collect over time. Only realised invoices
 * that did not leave with a delivery guide are offered; the deadline is optional.
 */
final class RegisterPickupAccountDialog {

    private RegisterPickupAccountDialog() {
    }

    static void show(JComponent parent, PickupApiClient api, InlineFeedbackPanel feedback, Consumer<PickupAccountDTO> onOpened) {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        UIHelper.loadAsync(parent, () -> api.eligibleInvoices(companyId, null),
                invoices -> open(api, companyId, invoices, onOpened, parent),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar as faturas", error.getMessage(),
                        "Tentar novamente", () -> show(parent, api, feedback, onOpened)));
    }

    private static void open(PickupApiClient api, Long companyId, List<EligibleInvoiceDTO> invoices,
                             Consumer<PickupAccountDTO> onOpened, JComponent parent) {
        if (invoices.isEmpty()) {
            ToastManager.show(parent, FeedbackType.INFO, "Não há faturas emitidas por registar como material por levantar.");
            return;
        }
        SearchableComboBox<EligibleInvoiceDTO> combo = new SearchableComboBox<>("Pesquisar por número ou cliente",
                i -> i.invoiceNumber() + "  ·  " + i.clientName() + "  ·  " + UIHelper.formatMzn(i.totalAmount()),
                i -> i.invoiceNumber() + " " + i.clientName());
        combo.setItems(invoices);
        DateField deadline = new DateField();
        JTextField notes = new JTextField();
        UIHelper.styleTextField(notes);
        JLabel hint = new JLabel("<html>O stock já saiu na fatura. A partir daqui cada entrega gera um talão LV, e o "
                + "sistema nunca deixa entregar mais do que foi pago. Deixe o prazo vazio se não houver limite.</html>");
        hint.setForeground(UIHelper.TEXT_MUTED);
        FormField invoiceForm = new FormField("Fatura", combo, true, "Mostram-se as últimas faturas emitidas");
        JPanel form = UIHelper.createDialogForm(1, new Object[]{
                "", invoiceForm,
                "Levantar até (yyyy-MM-dd):", deadline,
                "Observações:", notes,
                "", hint});
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Registar Material por Levantar",
                "fas-people-carry", "O cliente pagou e vai levantar aos poucos", form);
        PickupAccountDTO[] result = new PickupAccountDTO[1];
        dialog.setOnSaveAsync(() -> {
            EligibleInvoiceDTO invoice = combo.selectedValue();
            if (invoice == null) {
                invoiceForm.setError("Escolha a fatura.");
                throw new IllegalArgumentException("Escolha a fatura.");
            }
            LocalDate until = deadline.getText() == null || deadline.getText().isBlank() ? null : deadline.value();
            String text = notes.getText().trim();
            OpenPickupAccountRequest request = new OpenPickupAccountRequest(companyId, invoice.invoiceId(), until,
                    text.isEmpty() ? null : text);
            return () -> { result[0] = api.open(request); return null; };
        });
        if (dialog.showDialog() && result[0] != null) onOpened.accept(result[0]);
    }
}
