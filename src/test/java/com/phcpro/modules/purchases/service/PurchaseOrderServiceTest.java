package com.phcpro.modules.purchases.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.comercial.model.Product;
import com.phcpro.modules.comercial.repository.ProductRepository;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.repository.WarehouseRepository;
import com.phcpro.modules.inventory.service.InventoryService;
import com.phcpro.modules.numbering.service.DocumentNumberService;
import com.phcpro.modules.purchases.dto.CreatePurchaseOrderLineRequest;
import com.phcpro.modules.purchases.dto.CreatePurchaseOrderRequest;
import com.phcpro.modules.purchases.dto.PurchaseOrderDTO;
import com.phcpro.modules.purchases.dto.ReceivePurchaseOrderRequest;
import com.phcpro.modules.purchases.dto.ReceivePurchaseOrderRequest.ReceiveLine;
import com.phcpro.modules.purchases.model.PurchaseOrder;
import com.phcpro.modules.purchases.model.PurchaseOrderLine;
import com.phcpro.modules.purchases.model.Supplier;
import com.phcpro.modules.purchases.repository.PurchaseOrderRepository;
import com.phcpro.modules.purchases.repository.SupplierRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Harness PO-01..PO-08: ciclo da encomenda a fornecedor (criar/receber/cancelar/pesquisar). */
class PurchaseOrderServiceTest {

    private static final Long COMPANY_ID = 1L;

    private PurchaseOrderRepository orderRepository;
    private SupplierRepository supplierRepository;
    private ProductRepository productRepository;
    private WarehouseRepository warehouseRepository;
    private CompanyRepository companyRepository;
    private InventoryService inventoryService;
    private DocumentNumberService documentNumberService;
    private PurchaseOrderService service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(PurchaseOrderRepository.class);
        supplierRepository = mock(SupplierRepository.class);
        productRepository = mock(ProductRepository.class);
        warehouseRepository = mock(WarehouseRepository.class);
        companyRepository = mock(CompanyRepository.class);
        inventoryService = mock(InventoryService.class);
        documentNumberService = mock(DocumentNumberService.class);
        service = new PurchaseOrderService(orderRepository, supplierRepository, productRepository,
                warehouseRepository, companyRepository, inventoryService, documentNumberService,
                mock(AuditLogService.class));

        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");

        when(orderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(documentNumberService.next(anyString())).thenReturn("EC-F-2026/1");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    private Company company() { Company c = new Company(); c.setId(COMPANY_ID); return c; }

    private Supplier supplier(boolean active) {
        Supplier s = new Supplier();
        s.setId(7L); s.setName("Acme Lda"); s.setTaxId("123456789");
        s.setActive(active); s.setCompany(company());
        return s;
    }

    private Warehouse warehouse() {
        Warehouse w = new Warehouse(); w.setId(3L); w.setName("Central"); w.setCompany(company());
        return w;
    }

    private Product product() {
        Product p = new Product(); p.setId(11L); p.setName("Leite"); p.setSku("SKU-1"); return p;
    }

    private void stubRefs(boolean supplierActive) {
        when(supplierRepository.findById(7L)).thenReturn(Optional.of(supplier(supplierActive)));
        when(warehouseRepository.findById(3L)).thenReturn(Optional.of(warehouse()));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company()));
        when(productRepository.findByIdAndCompaniesId(11L, COMPANY_ID)).thenReturn(Optional.of(product()));
    }

    private CreatePurchaseOrderRequest request() {
        return new CreatePurchaseOrderRequest(7L, 3L, COMPANY_ID, LocalDate.now().plusDays(7), "urgente",
                List.of(new CreatePurchaseOrderLineRequest(11L, new BigDecimal("10"), new BigDecimal("25"),
                        "L1", LocalDate.now().plusMonths(6), null)));
    }

    private PurchaseOrder ordered() {
        stubRefs(true);
        PurchaseOrderDTO dto = service.createOrder(request());
        PurchaseOrder o = new PurchaseOrder();
        o.setId(99L); o.setOrderNumber(dto.orderNumber()); o.setStatus(PurchaseOrder.ORDERED);
        o.setSupplier(supplier(true)); o.setWarehouse(warehouse()); o.setCompany(company());
        PurchaseOrderLine line = new PurchaseOrderLine();
        line.setProduct(product()); line.setQuantity(new BigDecimal("10"));
        line.setUnitPrice(new BigDecimal("25")); line.setTaxRate(BigDecimal.ZERO);
        line.setLineTotal(new BigDecimal("250")); line.setBatchNumber("L1");
        o.addLine(line);
        return o;
    }

    @Test // PO-01
    void createOrder_valida_ficaOrderedSemStock() {
        stubRefs(true);
        PurchaseOrderDTO dto = service.createOrder(request());
        assertEquals(PurchaseOrder.ORDERED, dto.status());
        assertEquals("EC-F-2026/1", dto.orderNumber());
        verifyNoInteractions(inventoryService);
    }

    @Test // PO-02
    void createOrder_fornecedorInactivo_lanca() {
        stubRefs(false);
        assertThrows(BusinessRuleException.class, () -> service.createOrder(request()));
    }

    // ── IVA da compra: manda a factura do fornecedor; sem ela, a taxa do artigo ──
    // Regressão: aplicava-se 16% cego a tudo, o que inflava o IVA dedutível em bens isentos.

    @Test // IV-11
    void createOrder_semTaxaIndicada_usaAdoArtigo_eNaoOsDezasseisPorCento() {
        Product isento = product();
        isento.setTaxRate(taxRateOf("0.00"));
        when(supplierRepository.findById(7L)).thenReturn(Optional.of(supplier(true)));
        when(warehouseRepository.findById(3L)).thenReturn(Optional.of(warehouse()));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company()));
        when(productRepository.findByIdAndCompaniesId(11L, COMPANY_ID)).thenReturn(Optional.of(isento));

        PurchaseOrderDTO dto = service.createOrder(request()); // linha sem taxa

        assertEquals(0, dto.taxAmount().compareTo(BigDecimal.ZERO),
                "comprar bem isento não pode gerar IVA dedutível");
    }

    @Test // IV-12
    void createOrder_comTaxaDaFactura_usaEssa() {
        stubRefs(true);
        CreatePurchaseOrderRequest comIva = new CreatePurchaseOrderRequest(7L, 3L, COMPANY_ID,
                LocalDate.now().plusDays(7), "urgente",
                List.of(new CreatePurchaseOrderLineRequest(11L, new BigDecimal("10"), new BigDecimal("25"),
                        "L1", LocalDate.now().plusMonths(6), null, new BigDecimal("0.05"))));

        PurchaseOrderDTO dto = service.createOrder(comIva);

        // 250 líquido a 5% = 12,50 (a factura do fornecedor manda, mesmo que o artigo diga outra coisa).
        assertEquals(0, dto.taxAmount().compareTo(new BigDecimal("12.50")));
    }

    private static com.phcpro.modules.fiscal.model.TaxRate taxRateOf(String rate) {
        com.phcpro.modules.fiscal.model.TaxRate taxRate = new com.phcpro.modules.fiscal.model.TaxRate();
        taxRate.setRate(new BigDecimal(rate));
        return taxRate;
    }

    @Test // PO-03
    void receiveOrder_geraEntradaPorLinha() {
        PurchaseOrder o = ordered();
        when(orderRepository.findById(99L)).thenReturn(Optional.of(o));
        PurchaseOrderDTO dto = service.receiveOrder(99L);
        assertEquals(PurchaseOrder.RECEIVED, dto.status());
        verify(inventoryService, times(1)).registerMovement(
                any(), any(), eq(new BigDecimal("10")), eq("PURCHASE"), eq("L1"), any(), anyString(), any());
    }

    @Test // PO-04
    void receiveOrder_semPermissao_naoMoveStock() {
        PurchaseOrder o = ordered();
        when(orderRepository.findById(99L)).thenReturn(Optional.of(o));
        CurrentUserContext.setCurrentUser("func", "EMPLOYEE");
        assertThrows(BusinessRuleException.class, () -> service.receiveOrder(99L));
        verifyNoInteractions(inventoryService);
    }

    @Test // PO-05
    void receiveOrder_jaRecebida_lanca() {
        PurchaseOrder o = ordered();
        o.setStatus(PurchaseOrder.RECEIVED);
        when(orderRepository.findById(99L)).thenReturn(Optional.of(o));
        assertThrows(BusinessRuleException.class, () -> service.receiveOrder(99L));
    }

    @Test // PO-06
    void cancelOrder_comMotivo_cancela() {
        PurchaseOrder o = ordered();
        when(orderRepository.findById(99L)).thenReturn(Optional.of(o));
        PurchaseOrderDTO dto = service.cancelOrder(99L, "fornecedor sem stock");
        assertEquals(PurchaseOrder.CANCELLED, dto.status());
        verifyNoInteractions(inventoryService);
    }

    @Test // PO-07
    void cancelOrder_semMotivo_lanca() {
        PurchaseOrder o = ordered();
        when(orderRepository.findById(99L)).thenReturn(Optional.of(o));
        assertThrows(BusinessRuleException.class, () -> service.cancelOrder(99L, "  "));
    }

    @Test // PO-08
    void searchOrders_filtraPorNumeroOuFornecedor() {
        PurchaseOrder o = ordered();
        when(orderRepository.findByCompanyIdOrderByOrderDateDesc(COMPANY_ID)).thenReturn(List.of(o));
        assertEquals(1, service.searchOrders(COMPANY_ID, "ec-f").size());
        assertEquals(1, service.searchOrders(COMPANY_ID, "acme").size());
        assertEquals(0, service.searchOrders(COMPANY_ID, "zzz").size());
    }

    // ── Recepção parcial (RP-01..RP-12) ──────────────────────────────────────

    /** Encomenda num estado dado com uma linha (qty/recebido/lineId controlados). */
    private PurchaseOrder orderInState(String status, BigDecimal qty, BigDecimal received, Long lineId) {
        PurchaseOrder o = new PurchaseOrder();
        o.setId(99L); o.setOrderNumber("EC-F-2026/1"); o.setStatus(status);
        o.setSupplier(supplier(true)); o.setWarehouse(warehouse()); o.setCompany(company());
        PurchaseOrderLine line = new PurchaseOrderLine();
        line.setId(lineId); line.setProduct(product());
        line.setQuantity(qty); line.setReceivedQuantity(received);
        line.setUnitPrice(new BigDecimal("25")); line.setTaxRate(BigDecimal.ZERO);
        line.setLineTotal(new BigDecimal("250")); line.setBatchNumber("L1");
        o.addLine(line);
        when(orderRepository.findById(99L)).thenReturn(Optional.of(o));
        return o;
    }

    private ReceivePurchaseOrderRequest receive(Long lineId, String qty) {
        return new ReceivePurchaseOrderRequest(List.of(new ReceiveLine(lineId, new BigDecimal(qty))));
    }

    @Test // RP-01
    void receivePartial_parteDeUmaLinha_ficaPartiallyReceived() {
        orderInState(PurchaseOrder.ORDERED, new BigDecimal("10"), BigDecimal.ZERO, 5L);
        PurchaseOrderDTO dto = service.receivePartial(99L, receive(5L, "6"));
        assertEquals(PurchaseOrder.PARTIALLY_RECEIVED, dto.status());
        assertEquals(0, new BigDecimal("6").compareTo(dto.lines().get(0).receivedQuantity()));
        verify(inventoryService, times(1)).registerMovement(
                any(), any(), eq(new BigDecimal("6")), eq("PURCHASE"), eq("L1"), any(), anyString(), any());
    }

    @Test // RP-02
    void receivePartial_completaRestante_ficaReceived() {
        orderInState(PurchaseOrder.PARTIALLY_RECEIVED, new BigDecimal("10"), new BigDecimal("6"), 5L);
        PurchaseOrderDTO dto = service.receivePartial(99L, receive(5L, "4"));
        assertEquals(PurchaseOrder.RECEIVED, dto.status());
        assertNotNull(dto.receivedAt());
        verify(inventoryService, times(1)).registerMovement(
                any(), any(), eq(new BigDecimal("4")), eq("PURCHASE"), any(), any(), anyString(), any());
    }

    @Test // RP-03
    void receivePartial_acimaDoEmFalta_lanca() {
        orderInState(PurchaseOrder.ORDERED, new BigDecimal("10"), BigDecimal.ZERO, 5L);
        assertThrows(BusinessRuleException.class, () -> service.receivePartial(99L, receive(5L, "12")));
        verifyNoInteractions(inventoryService);
    }

    @Test // RP-04
    void receivePartial_quantidadeZero_lanca() {
        orderInState(PurchaseOrder.ORDERED, new BigDecimal("10"), BigDecimal.ZERO, 5L);
        assertThrows(BusinessRuleException.class, () -> service.receivePartial(99L, receive(5L, "0")));
    }

    @Test // RP-05
    void receivePartial_semPermissao_naoMoveStock() {
        orderInState(PurchaseOrder.ORDERED, new BigDecimal("10"), BigDecimal.ZERO, 5L);
        CurrentUserContext.setCurrentUser("func", "EMPLOYEE");
        assertThrows(BusinessRuleException.class, () -> service.receivePartial(99L, receive(5L, "5")));
        verifyNoInteractions(inventoryService);
    }

    @Test // RP-06
    void receivePartial_jaRecebida_lanca() {
        orderInState(PurchaseOrder.RECEIVED, new BigDecimal("10"), new BigDecimal("10"), 5L);
        assertThrows(BusinessRuleException.class, () -> service.receivePartial(99L, receive(5L, "1")));
    }

    @Test // RP-07
    void receivePartial_cancelada_lanca() {
        orderInState(PurchaseOrder.CANCELLED, new BigDecimal("10"), BigDecimal.ZERO, 5L);
        assertThrows(BusinessRuleException.class, () -> service.receivePartial(99L, receive(5L, "1")));
    }

    @Test // RP-08
    void receiveOrder_apartirDePartial_recebeSoOEmFalta() {
        orderInState(PurchaseOrder.PARTIALLY_RECEIVED, new BigDecimal("10"), new BigDecimal("6"), 5L);
        PurchaseOrderDTO dto = service.receiveOrder(99L);
        assertEquals(PurchaseOrder.RECEIVED, dto.status());
        verify(inventoryService, times(1)).registerMovement(
                any(), any(), eq(new BigDecimal("4")), eq("PURCHASE"), any(), any(), anyString(), any());
    }

    @Test // RP-09
    void receiveOrder_apartirDeOrdered_recebeTudo() {
        orderInState(PurchaseOrder.ORDERED, new BigDecimal("10"), BigDecimal.ZERO, 5L);
        PurchaseOrderDTO dto = service.receiveOrder(99L);
        assertEquals(PurchaseOrder.RECEIVED, dto.status());
        verify(inventoryService, times(1)).registerMovement(
                any(), any(), eq(new BigDecimal("10")), eq("PURCHASE"), any(), any(), anyString(), any());
    }

    @Test // RP-10
    void cancelOrder_apartirDePartial_cancela() {
        orderInState(PurchaseOrder.PARTIALLY_RECEIVED, new BigDecimal("10"), new BigDecimal("6"), 5L);
        PurchaseOrderDTO dto = service.cancelOrder(99L, "fornecedor não entrega o resto");
        assertEquals(PurchaseOrder.CANCELLED, dto.status());
        verifyNoInteractions(inventoryService);
    }

    @Test // RP-11
    void receivePartial_multiLinha_umaCompletaOutraParcial_ficaPartial() {
        PurchaseOrder o = orderInState(PurchaseOrder.ORDERED, new BigDecimal("5"), BigDecimal.ZERO, 5L);
        PurchaseOrderLine b = new PurchaseOrderLine();
        b.setId(6L); b.setProduct(product()); b.setQuantity(new BigDecimal("5"));
        b.setReceivedQuantity(BigDecimal.ZERO); b.setUnitPrice(new BigDecimal("25"));
        b.setTaxRate(BigDecimal.ZERO); b.setLineTotal(new BigDecimal("125"));
        o.addLine(b);
        ReceivePurchaseOrderRequest req = new ReceivePurchaseOrderRequest(List.of(
                new ReceiveLine(5L, new BigDecimal("5")), new ReceiveLine(6L, new BigDecimal("2"))));
        PurchaseOrderDTO dto = service.receivePartial(99L, req);
        assertEquals(PurchaseOrder.PARTIALLY_RECEIVED, dto.status());
        verify(inventoryService, times(2)).registerMovement(
                any(), any(), any(), eq("PURCHASE"), any(), any(), anyString(), any());
    }

    @Test // RP-12
    void receivePartial_lineIdInexistente_lanca() {
        orderInState(PurchaseOrder.ORDERED, new BigDecimal("10"), BigDecimal.ZERO, 5L);
        assertThrows(BusinessRuleException.class, () -> service.receivePartial(99L, receive(999L, "1")));
    }
}
