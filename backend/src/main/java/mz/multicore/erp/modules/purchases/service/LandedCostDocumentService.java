package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.events.LandedCostConfirmedEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.purchases.dto.CreateLandedCostRequest;
import mz.multicore.erp.modules.purchases.dto.LandedCostAllocationDTO;
import mz.multicore.erp.modules.purchases.dto.LandedCostChargeDTO;
import mz.multicore.erp.modules.purchases.dto.LandedCostDocumentDTO;
import mz.multicore.erp.modules.purchases.dto.LandedCostPreviewDTO;
import mz.multicore.erp.modules.purchases.model.LandedCostAllocation;
import mz.multicore.erp.modules.purchases.model.LandedCostAllocationMethod;
import mz.multicore.erp.modules.purchases.model.LandedCostCharge;
import mz.multicore.erp.modules.purchases.model.LandedCostDocument;
import mz.multicore.erp.modules.purchases.model.LandedCostDocumentCharge;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.PurchaseLine;
import mz.multicore.erp.modules.purchases.repository.LandedCostDocumentRepository;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;

/**
 * Registers "Custos de Importação" documents (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.1): numbered in the CI
 * series of the active company, linked only to received purchases of that company, and with one debt per cost line
 * to that line's supplier, shown in accounts payable and paid like a purchase. The capitalizable cost is spread over
 * the purchase lines and applied to the stock still on hand or to cost of goods sold ({@link LandedCostStockEffect}),
 * and the confirmation publishes the base-currency amounts of its journal entry (§4.4).
 */
@Service
public class LandedCostDocumentService {

    private static final String RECEIVED = "COMPLETED";

    private final LandedCostDocumentRepository documentRepository;
    private final PurchaseRepository purchaseRepository;
    private final DocumentNumberService documentNumberService;
    private final LandedCostChargeResolver chargeResolver;
    private final LandedCostStockEffect stockEffect;
    private final ApplicationEventPublisher eventPublisher;
    private final LandedCostCalculator calculator = new LandedCostCalculator();

    public LandedCostDocumentService(LandedCostDocumentRepository documentRepository,
                                     PurchaseRepository purchaseRepository,
                                     DocumentNumberService documentNumberService,
                                     LandedCostChargeResolver chargeResolver,
                                     LandedCostStockEffect stockEffect,
                                     ApplicationEventPublisher eventPublisher) {
        this.documentRepository = documentRepository;
        this.purchaseRepository = purchaseRepository;
        this.documentNumberService = documentNumberService;
        this.chargeResolver = chargeResolver;
        this.stockEffect = stockEffect;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Pré-visualização (§4.3): as mesmas validações e o mesmo cálculo da confirmação, sem número, sem dívidas e sem
     * mexer no custo médio dos artigos.
     */
    @Transactional(readOnly = true)
    public LandedCostPreviewDTO preview(CreateLandedCostRequest request) {
        Draft draft = draft(request);
        LandedCostCalculator.Totals totals = draft.totals();
        List<LandedCostAllocation> allocations = stockEffect.preview(draft.method(), totals.capitalizableCost(),
                draft.purchases());
        return new LandedCostPreviewDTO(draft.method(), totals.capitalizableCost(), totals.deductibleVat(),
                totals.payableTotal(), allocations.stream().map(LandedCostDocumentService::toDto).toList());
    }

    /** Confirmação (§4.4): grava o CI, as dívidas, a fotografia por linha e o novo custo médio numa transacção. */
    @Transactional
    public LandedCostDocumentDTO register(CreateLandedCostRequest request) {
        Draft draft = draft(request);
        List<LandedCostDocumentCharge> charges = draft.charges();

        LandedCostDocument document = new LandedCostDocument();
        document.setCompany(draft.company());
        document.setDocumentDate(draft.documentDate());
        document.setAllocationMethod(draft.method());
        document.getPurchases().addAll(draft.purchases());
        document.getAllocations().addAll(
                stockEffect.apply(draft.method(), draft.totals().capitalizableCost(), draft.purchases()));
        document.setDocumentNumber(documentNumberService.next(DocumentSeries.LANDED_COST));
        for (LandedCostDocumentCharge charge : charges) {
            Purchase payable = charge.createPayable(document, document.getCharges().size() + 1);
            payable.setCreatedBy(CurrentUserContext.getUsername());
            charge.setPayable(purchaseRepository.save(payable));
            document.getCharges().add(charge);
        }
        LandedCostDocument saved = documentRepository.save(document);
        eventPublisher.publishEvent(postingEvent(saved));
        return toDto(saved);
    }

    /** Pedido validado na empresa activa: compras recebidas, linhas de custo resolvidas, método e totais. */
    private record Draft(Company company, LocalDate documentDate, LandedCostAllocationMethod method,
                         List<Purchase> purchases, List<LandedCostDocumentCharge> charges,
                         LandedCostCalculator.Totals totals) {
    }

    private Draft draft(CreateLandedCostRequest request) {
        PermissionGuard.requireManagerOrAdmin("registar custos de importação");
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        List<Purchase> purchases = receivedPurchases(companyId, request.purchaseIds());
        Company company = purchases.get(0).getCompany();
        LocalDate documentDate = request.documentDate() == null ? LocalDate.now() : request.documentDate();
        List<LandedCostDocumentCharge> charges = chargeResolver.resolve(company, documentDate, request.charges());
        LandedCostCalculator.Totals totals = calculator.totals(
                charges.stream().map(LandedCostDocumentCharge::charge).toList());
        return new Draft(company, documentDate, LandedCostAllocationMethod.orDefault(request.allocationMethod()),
                purchases, charges, totals);
    }

    /**
     * Valores do lançamento (§4.4): a parte do stock e a já vendida vêm das quotas fotografadas; cada dívida entra
     * pelo seu total em moeda base (o que se vai pagar) e o IVA dedutível é a diferença, para o lançamento fechar.
     */
    private static LandedCostConfirmedEvent postingEvent(LandedCostDocument document) {
        BigDecimal stockAmount = sum(document.getAllocations().stream().map(LandedCostAllocation::getStockAmount));
        BigDecimal soldAmount = sum(document.getAllocations().stream().map(LandedCostAllocation::getSoldAmount));
        List<LandedCostConfirmedEvent.Payable> payables = document.getCharges().stream()
                .map(line -> new LandedCostConfirmedEvent.Payable(line.getPayable().getPurchaseNumber(),
                        line.getSupplier().getName(), line.getPayable().baseTotal()))
                .toList();
        BigDecimal payableTotal = sum(payables.stream().map(LandedCostConfirmedEvent.Payable::amount));
        return new LandedCostConfirmedEvent(document.getCompany().getId(), document.getId(),
                document.getDocumentNumber(), document.getDocumentDate(), stockAmount, soldAmount,
                payableTotal.subtract(stockAmount).subtract(soldAmount), payables);
    }

    private static BigDecimal sum(Stream<BigDecimal> values) {
        return values.reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Purchase> receivedPurchases(Long companyId, List<Long> purchaseIds) {
        if (purchaseIds == null || purchaseIds.isEmpty()) {
            throw new BusinessRuleException("Escolha pelo menos uma compra recebida.");
        }
        return new LinkedHashSet<>(purchaseIds).stream()
                .map(id -> receivedPurchase(companyId, id))
                .toList();
    }

    private Purchase receivedPurchase(Long companyId, Long purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .filter(p -> p.getCompany() != null && companyId.equals(p.getCompany().getId()))
                .orElseThrow(() -> new BusinessRuleException("Compra não encontrada na empresa activa."));
        if (!purchase.isGoodsPurchase() || !RECEIVED.equals(purchase.getStatus())) {
            throw new BusinessRuleException("A compra " + purchase.getPurchaseNumber()
                    + " não é uma compra recebida: só compras recebidas levam custos de importação.");
        }
        return purchase;
    }

    private static LandedCostDocumentDTO toDto(LandedCostDocument document) {
        return new LandedCostDocumentDTO(document.getId(), document.getDocumentNumber(), document.getDocumentDate(),
                document.getAllocationMethod(),
                document.getPurchases().stream().map(Purchase::getId).toList(),
                document.getPurchases().stream().map(Purchase::getPurchaseNumber).toList(),
                document.getCharges().stream().map(LandedCostDocumentService::toDto).toList(),
                document.getAllocations().stream().map(LandedCostDocumentService::toDto).toList());
    }

    private static LandedCostAllocationDTO toDto(LandedCostAllocation allocation) {
        PurchaseLine line = allocation.getPurchaseLine();
        return new LandedCostAllocationDTO(line.getId(), line.getPurchase().getPurchaseNumber(),
                line.getProduct().getId(), line.getProduct().getName(), allocation.getReceivedQuantity(),
                allocation.getStockQuantity(), allocation.getShareAmount(), allocation.getStockAmount(),
                allocation.getSoldAmount(), allocation.getUnitCostBefore(), allocation.getUnitCostAfter());
    }

    private static LandedCostChargeDTO toDto(LandedCostDocumentCharge line) {
        LandedCostCharge charge = line.charge();
        return new LandedCostChargeDTO(line.getChargeType(), line.getSupplier().getId(), line.getSupplier().getName(),
                line.getCurrency(), line.getExchangeRate(), line.getAmount(), line.getVatRatePercent(),
                charge.documentVatAmount(), charge.documentTotalAmount(), charge.baseCostAmount(),
                charge.baseVatAmount(), line.getPayable().getId(), line.getPayable().getPurchaseNumber());
    }
}
