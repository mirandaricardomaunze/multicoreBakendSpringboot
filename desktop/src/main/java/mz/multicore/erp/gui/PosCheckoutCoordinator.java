package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.CustomerCreditValidator;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.pos.dto.POSCheckoutLineRequest;
import mz.multicore.erp.modules.pos.dto.POSCheckoutRequest;
import mz.multicore.erp.modules.pos.dto.PosPaymentRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Coordenador do fluxo de finalização de venda (checkout) no POS.
 */
public class PosCheckoutCoordinator {

    private final POSPanel owner;

    public PosCheckoutCoordinator(POSPanel owner) {
        this.owner = owner;
    }

    public void runCheckout() {
        if (owner.activeSession == null) {
            owner.showPosNotice(FeedbackType.WARNING, "Sessão de caixa necessária", "Abra uma sessão de caixa antes de finalizar a venda.");
            return;
        }
        if (owner.cartItems.isEmpty()) {
            owner.showPosNotice(FeedbackType.WARNING, "Carrinho vazio", "Adicione pelo menos um artigo antes de finalizar a venda.");
            return;
        }
        if (owner.warehousesList.isEmpty() || owner.accountsList.isEmpty()) {
            owner.showPosNotice(FeedbackType.WARNING, "Configuração incompleta", "Registe um armazém e uma conta de tesouraria.");
            return;
        }

        int clientIdx = owner.clientCombo.getSelectedIndex();
        int whIdx = owner.warehouseCombo.getSelectedIndex();
        int accIdx = owner.accountCombo.getSelectedIndex();
        if (whIdx < 0 || accIdx < 0) {
            owner.showPosNotice(FeedbackType.WARNING, "Selecção incompleta", "Seleccione o armazém e a conta de tesouraria.");
            return;
        }

        ClientDTO client = (clientIdx >= 0 && clientIdx < owner.filteredClients.size()) ? owner.filteredClients.get(clientIdx) : null;
        String walkInName = (client == null && owner.clientSearchField != null) ? owner.clientSearchField.getText().trim() : null;
        if (client == null && (walkInName == null || walkInName.isBlank())) {
            walkInName = UIHelper.promptRequiredText("Identificação do Cliente", "fas-user",
                    "Venda a Cliente Não Cadastrado", "Introduza o nome do cliente:");
            if (walkInName == null || walkInName.isBlank()) {
                owner.showPosNotice(FeedbackType.WARNING, "Nome do cliente obrigatório",
                        "É obrigatório indicar o nome do cliente para efetuar a venda no POS.");
                if (owner.clientSearchField != null) owner.clientSearchField.requestFocusInWindow();
                return;
            }
            walkInName = walkInName.trim();
            if (owner.clientSearchField != null) owner.clientSearchField.setText(walkInName);
        }
        WarehouseDTO wh = owner.warehousesList.get(whIdx);
        TreasuryAccountDTO acc = owner.accountsList.get(accIdx);

        List<POSCheckoutLineRequest> lines = owner.cartItems.stream()
                .map(i -> new POSCheckoutLineRequest(i.product.id(), i.qty, i.discount, i.batch, i.serial, i.customUnitPrice,
                        i.weightSource()))
                .toList();

        String operator = CurrentUserContext.getUsername();
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        boolean fiado = owner.creditCheck != null && owner.creditCheck.isSelected();
        BigDecimal cartTotal = owner.cartItems.stream().map(POSPanel.CartItem::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        if (fiado) {
            CustomerCreditValidator.CreditAssessment assessment =
                    CustomerCreditValidator.validateCreditPreFlight(client, cartTotal);
            if (!assessment.isApproved()) {
                owner.showPosNotice(FeedbackType.WARNING, "Crédito não autorizado", assessment.details());
                return;
            }
        }

        List<PosPaymentRequest> payments;
        Long treasuryAccountId;
        if (fiado) {
            payments = List.of(new PosPaymentRequest(
                    "CREDIT", cartTotal, BigDecimal.ZERO, "Venda a crédito", null));
            treasuryAccountId = null;
        } else {
            PosPaymentRequest payment = PosPaymentDialog.show(cartTotal, acc.id(), owner.posApiClient);
            if (payment == null) return;
            payments = List.of(payment);
            treasuryAccountId = null;
        }

        String idempotencyKey = "POS-" + UUID.randomUUID();
        POSCheckoutRequest request = new POSCheckoutRequest(operator, companyId, client != null ? client.id() : null,
                walkInName, wh.id(), treasuryAccountId, lines, payments, idempotencyKey, owner.currentQuotationId, owner.discountAuthorization);

        UIHelper.runWithProgress(owner, "A finalizar venda",
                () -> owner.posApiClient.checkout(request),
                inv -> {
                    String paymentLabel = fiado ? "EM DÍVIDA (fiado)" : "PAGO";
                    ToastManager.success(owner, "Venda concluída · " + inv.invoiceNumber() + " · "
                            + inv.totalAmount() + " MT · " + paymentLabel + ".");

                    if (!fiado) mz.multicore.erp.modules.pos.drawer.CashDrawer.afterSale(payments.get(0).method());
                    PosReceiptPrinter.printReceiptIfConfirmed(owner, inv);
                    owner.clearCart();
                    owner.refreshSessionState();
                    owner.todaySummaryView.loadTodaySalesSummary(owner);
                    owner.loadMetadata();
                },
                ex -> {
                    if (owner.contingencyManager.handleContingencyCheckout(owner, request, cartTotal, ex, owner::clearCart)) return;
                    owner.showPosNotice(FeedbackType.ERROR, "Não foi possível concluir a venda", ex.getMessage());
                });
    }
}
