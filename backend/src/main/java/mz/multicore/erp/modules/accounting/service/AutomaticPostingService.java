package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.events.AssetDepreciatedEvent;
import mz.multicore.erp.architecture.events.CreditNoteApprovedEvent;
import mz.multicore.erp.architecture.events.DebitNoteApprovedEvent;
import mz.multicore.erp.architecture.events.FleetFuelLoggedEvent;
import mz.multicore.erp.architecture.events.InvoiceCancelledEvent;
import mz.multicore.erp.architecture.events.PaymentReceivedEvent;
import mz.multicore.erp.architecture.events.PayrollLiabilityDeliveredEvent;
import mz.multicore.erp.architecture.events.PayslipPaidEvent;
import mz.multicore.erp.architecture.events.ProductionCompletedEvent;
import mz.multicore.erp.architecture.events.PurchaseRegisteredEvent;
import mz.multicore.erp.architecture.events.ReceiptCancelledEvent;
import mz.multicore.erp.architecture.events.SaleRegisteredEvent;
import mz.multicore.erp.architecture.events.SupplierPaymentRegisteredEvent;
import mz.multicore.erp.modules.audit.service.AuditLogService;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Traduz factos de negócio em partidas dobradas.
 * Orquestrador desacoplado ouvindo eventos e delegando para handlers especializados.
 */
@Service
public class AutomaticPostingService {

    private final CommercialPostingHandler commercialPostingHandler;
    private final ProcurementPostingHandler procurementPostingHandler;
    private final OperationsPostingHandler operationsPostingHandler;

    @org.springframework.beans.factory.annotation.Autowired
    public AutomaticPostingService(CommercialPostingHandler commercialPostingHandler,
                                   ProcurementPostingHandler procurementPostingHandler,
                                   OperationsPostingHandler operationsPostingHandler) {
        this.commercialPostingHandler = commercialPostingHandler;
        this.procurementPostingHandler = procurementPostingHandler;
        this.operationsPostingHandler = operationsPostingHandler;
    }

    /** Construtor para retrocompatibilidade com testes legados. */
    public AutomaticPostingService(ChartOfAccountsService chartOfAccountsService,
                                   JournalService journalService,
                                   AuditLogService auditLogService) {
        PostingHelper helper = new PostingHelper(chartOfAccountsService, journalService);
        this.commercialPostingHandler = new CommercialPostingHandler(helper, journalService);
        this.procurementPostingHandler = new ProcurementPostingHandler(helper, journalService);
        this.operationsPostingHandler = new OperationsPostingHandler(helper, journalService, auditLogService);
    }

    // ─── Comercial e Vendas ───────────────────────────────────────────────────

    @EventListener
    @Transactional
    public void onSaleRegistered(SaleRegisteredEvent event) {
        commercialPostingHandler.postSale(event);
    }

    @EventListener
    @Transactional
    public void onPaymentReceived(PaymentReceivedEvent event) {
        commercialPostingHandler.postPayment(event);
    }

    @EventListener
    @Transactional
    public void onInvoiceCancelled(InvoiceCancelledEvent event) {
        commercialPostingHandler.postInvoiceCancellation(event);
    }

    @EventListener
    @Transactional
    public void onReceiptCancelled(ReceiptCancelledEvent event) {
        commercialPostingHandler.postReceiptCancellation(event);
    }

    @EventListener
    @Transactional
    public void onCreditNoteApproved(CreditNoteApprovedEvent event) {
        commercialPostingHandler.postCreditNote(event);
    }

    @EventListener
    @Transactional
    public void onDebitNoteApproved(DebitNoteApprovedEvent event) {
        commercialPostingHandler.postDebitNote(event);
    }

    // ─── Compras e Fornecedores ───────────────────────────────────────────────

    @EventListener
    @Transactional
    public void onPurchaseRegistered(PurchaseRegisteredEvent event) {
        procurementPostingHandler.postPurchase(event);
    }

    @EventListener
    @Transactional
    public void onSupplierPaymentRegistered(SupplierPaymentRegisteredEvent event) {
        procurementPostingHandler.postSupplierPayment(event);
    }

    // ─── Recursos Humanos, Produção e Activos ─────────────────────────────────

    @EventListener
    @Transactional
    public void onPayslipPaid(PayslipPaidEvent event) {
        operationsPostingHandler.postPayslip(event);
    }

    @EventListener
    @Transactional
    public void onPayrollLiabilityDelivered(PayrollLiabilityDeliveredEvent event) {
        operationsPostingHandler.postPayrollLiability(event);
    }

    @EventListener
    @Transactional
    public void onProductionCompleted(ProductionCompletedEvent event) {
        operationsPostingHandler.postProduction(event);
    }

    @EventListener
    @Transactional
    public void onAssetDepreciated(AssetDepreciatedEvent event) {
        operationsPostingHandler.postAssetDepreciation(event);
    }

    @EventListener
    @Transactional
    public void onFleetFuelLogged(FleetFuelLoggedEvent event) {
        operationsPostingHandler.postFleetFuel(event);
    }
}
