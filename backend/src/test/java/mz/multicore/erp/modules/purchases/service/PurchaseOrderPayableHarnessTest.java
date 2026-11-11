package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.events.PurchaseRegisteredEvent;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.purchases.dto.ReceivePurchaseOrderRequest;
import mz.multicore.erp.modules.purchases.dto.ReceivePurchaseOrderRequest.ReceiveLine;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.PurchaseOrder;
import mz.multicore.erp.modules.purchases.model.PurchaseOrderLine;
import mz.multicore.erp.modules.purchases.model.PurchaseOrderReceipt;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.GoodsReceiptDiscrepancyRepository;
import mz.multicore.erp.modules.purchases.repository.PurchaseOrderReceiptRepository;
import mz.multicore.erp.modules.purchases.repository.PurchaseOrderRepository;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Harness CD-01..CD-07: a recepção de uma encomenda cria a dívida ao fornecedor. */
class PurchaseOrderPayableHarnessTest {

    private static final Long ORDER_ID = 99L;
    private static final Long LINE_ID = 501L;

    private PurchaseOrderRepository orderRepository;
    private PurchaseRepository purchaseRepository;
    private PurchaseOrderReceiptRepository receiptRepository;
    private InventoryService inventoryService;
    private ApplicationEventPublisher eventPublisher;
    private PurchaseOrderReceiptExecutor executor;
    private PurchaseOrder order;

    @BeforeEach
    void setUp() {
        orderRepository = mock(PurchaseOrderRepository.class);
        purchaseRepository = mock(PurchaseRepository.class);
        receiptRepository = mock(PurchaseOrderReceiptRepository.class);
        inventoryService = mock(InventoryService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        DocumentNumberService numbers = mock(DocumentNumberService.class);
        AuditLogService audit = mock(AuditLogService.class);
        GoodsReceiptDiscrepancyRepository discrepancies = mock(GoodsReceiptDiscrepancyRepository.class);

        when(numbers.next(anyString())).thenReturn("CP-2026/1");
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        executor = new PurchaseOrderReceiptExecutor(orderRepository, inventoryService, audit, discrepancies,
                receiptRepository, new PurchaseOrderPayableRegistrar(purchaseRepository, numbers, eventPublisher, audit));

        order = order();
        when(orderRepository.lockByIdAndCompanyId(anyLong(), anyLong())).thenReturn(Optional.of(order));
        CurrentUserContext.setCurrentCompanyId(1L);
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    private PurchaseOrder order() {
        Company company = new Company();
        company.setId(1L);
        Supplier supplier = new Supplier();
        supplier.setId(7L);
        supplier.setName("Acme Lda");
        Product product = new Product();
        product.setId(11L);
        product.setName("Leite");

        PurchaseOrder result = new PurchaseOrder();
        result.setId(ORDER_ID);
        result.setOrderNumber("EC-F-2026/1");
        result.setCompany(company);
        result.setSupplier(supplier);
        result.setWarehouse(new Warehouse());
        PurchaseOrderLine line = new PurchaseOrderLine();
        line.setId(LINE_ID);
        line.setProduct(product);
        line.setQuantity(new BigDecimal("10"));
        line.setUnitPrice(new BigDecimal("25.00"));
        line.setTaxRate(new BigDecimal("0.16"));
        result.addLine(line);
        return result;
    }

    private ReceivePurchaseOrderRequest receive(String good, String damaged, String missing, String key) {
        return new ReceivePurchaseOrderRequest(List.of(new ReceiveLine(LINE_ID, new BigDecimal(good),
                new BigDecimal(damaged), new BigDecimal(missing), null)), key);
    }

    private List<Purchase> savedPurchases(int count) {
        ArgumentCaptor<Purchase> saved = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository, times(count)).save(saved.capture());
        return saved.getAllValues();
    }

    @Test // CD-01, CD-02, CD-07
    void receiveOrder_criaDividaLigadaAEncomenda_semSegundaEntradaDeStock() {
        executor.receiveOrder(order);

        Purchase purchase = savedPurchases(1).get(0);
        assertEquals(new BigDecimal("290.00"), purchase.getTotalAmount());
        assertEquals(new BigDecimal("40.00"), purchase.getTaxAmount());
        assertEquals(new BigDecimal("290.00"), purchase.getOutstanding());
        assertEquals(0, purchase.getAmountPaid().signum());
        assertSame(order, purchase.getPurchaseOrder());
        verify(inventoryService, times(1)).registerPurchase(any(), any(), any(), any(),
                any(), any(), anyString(), any());
    }

    @Test // CD-03
    void receivePartial_danificadosEFaltasNaoGeramDivida() {
        executor.receivePartial(ORDER_ID, receive("6", "1", "3", null));

        Purchase purchase = savedPurchases(1).get(0);
        assertEquals(0, new BigDecimal("6").compareTo(purchase.getLines().get(0).getQuantity()));
        assertEquals(new BigDecimal("174.00"), purchase.getTotalAmount());
    }

    @Test // CD-04
    void duasRecepcoes_criamDuasCompras() {
        executor.receivePartial(ORDER_ID, receive("4", "0", "0", null));
        executor.receivePartial(ORDER_ID, receive("6", "0", "0", null));

        List<Purchase> purchases = savedPurchases(2);
        assertEquals(new BigDecimal("116.00"), purchases.get(0).getTotalAmount());
        assertEquals(new BigDecimal("174.00"), purchases.get(1).getTotalAmount());
    }

    @Test // CD-05
    void mesmaChaveDeIdempotencia_naoCriaNovaDivida() {
        ArgumentCaptor<PurchaseOrderReceipt> receipt = ArgumentCaptor.forClass(PurchaseOrderReceipt.class);
        executor.receivePartial(ORDER_ID, receive("4", "0", "0", "chave-1"));
        verify(receiptRepository).save(receipt.capture());
        when(receiptRepository.findByPurchaseOrderIdAndIdempotencyKey(ORDER_ID, "chave-1"))
                .thenReturn(Optional.of(receipt.getValue()));

        executor.receivePartial(ORDER_ID, receive("4", "0", "0", "chave-1"));

        verify(purchaseRepository, times(1)).save(any());
    }

    @Test // CD-06
    void dividaPublicaFactoContabilistico() {
        executor.receiveOrder(order);

        ArgumentCaptor<PurchaseRegisteredEvent> event = ArgumentCaptor.forClass(PurchaseRegisteredEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals(new BigDecimal("250.00"), event.getValue().netAmount());
        assertEquals(new BigDecimal("40.00"), event.getValue().taxAmount());
        assertEquals(new BigDecimal("290.00"), event.getValue().totalAmount());
        assertEquals(0, event.getValue().amountPaidNow().signum());
    }

    @Test
    void recepcaoRecusada_naoCriaDivida() {
        order.setStatus(PurchaseOrder.CANCELLED);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> executor.receiveOrder(order));

        verify(purchaseRepository, never()).save(any());
    }
}
