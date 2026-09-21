package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.dto.*;
import mz.multicore.erp.modules.inventory.model.*;
import mz.multicore.erp.modules.inventory.repository.ProductBatchRepository;
import mz.multicore.erp.modules.inventory.repository.StockWasteRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
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

class StockWasteHarnessTest {

    private StockWasteRepository wasteRepository;
    private ProductRepository productRepository;
    private WarehouseRepository warehouseRepository;
    private ProductBatchRepository batchRepository;
    private InventoryService inventoryService;
    private InvoiceRepository invoiceRepository;
    private CompanyRepository companyRepository;
    private StockWasteService service;

    private Company company;
    private Warehouse warehouse;
    private Product product;

    @BeforeEach
    void setUp() {
        wasteRepository = mock(StockWasteRepository.class);
        productRepository = mock(ProductRepository.class);
        warehouseRepository = mock(WarehouseRepository.class);
        batchRepository = mock(ProductBatchRepository.class);
        inventoryService = mock(InventoryService.class);
        invoiceRepository = mock(InvoiceRepository.class);
        companyRepository = mock(CompanyRepository.class);

        service = new StockWasteService(
                wasteRepository,
                productRepository,
                warehouseRepository,
                batchRepository,
                inventoryService,
                invoiceRepository,
                companyRepository
        );

        company = new Company();
        company.setId(1L);
        company.setName("Empresa Teste");

        warehouse = new Warehouse();
        warehouse.setId(10L);
        warehouse.setName("Armazém Central");
        warehouse.setCompany(company);

        product = new Product();
        product.setId(100L);
        product.setSku("PRD-001");
        product.setName("Leite Pasteurizado");
        product.setPurchasePrice(new BigDecimal("100.00"));
        product.setUnitPrice(new BigDecimal("150.00"));

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(wasteRepository.save(any(StockWaste.class))).thenAnswer(i -> {
            StockWaste sw = i.getArgument(0);
            if (sw.getId() == null) sw.setId(999L);
            return sw;
        });

        CurrentUserContext.setCurrentUser("operador", "OPERATOR");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.setCurrentUser(null, null);
    }

    @Test
    void lowValueWaste_autoApprovesAndDeductsStock() {
        // 10 unidades * 100 MZN = 1.000 MZN (<= 2.500 MZN)
        CreateStockWasteRequest req = new CreateStockWasteRequest(
                1L, 10L, 100L, null, new BigDecimal("10"), WasteReason.EXPIRED, "Vencido nas prateleiras"
        );

        StockMovement mockMovement = new StockMovement();
        mockMovement.setId(555L);
        when(inventoryService.registerMovement(any(), any(), any(), eq("WASTE"), any(), any(), any(), any()))
                .thenReturn(mockMovement);

        StockWasteDTO result = service.registerWaste(req);

        assertEquals(WasteStatus.APPROVED, result.status());
        assertEquals(new BigDecimal("100.00"), result.unitCost());
        assertEquals(new BigDecimal("1000.00"), result.totalCost());
        assertEquals("operador", result.approvedBy());
        assertNotNull(result.approvedAt());

        // Garante que o stock foi abatido
        verify(inventoryService, times(1)).registerMovement(
                eq(product), eq(warehouse), eq(new BigDecimal("-10")), eq("WASTE"), isNull(), isNull(), anyString(), isNull()
        );
    }

    @Test
    void highValueWaste_byOperator_entersPendingApproval_doesNotDeductStock() {
        // 30 unidades * 100 MZN = 3.000 MZN (> 2.500 MZN)
        CreateStockWasteRequest req = new CreateStockWasteRequest(
                1L, 10L, 100L, null, new BigDecimal("30"), WasteReason.DAMAGED, "Caixa tombou no descarregamento"
        );

        StockWasteDTO result = service.registerWaste(req);

        assertEquals(WasteStatus.PENDING_APPROVAL, result.status());
        assertEquals(new BigDecimal("3000.00"), result.totalCost());
        assertNull(result.approvedBy());
        assertNull(result.approvedAt());

        // Stock não pode ser deduzido enquanto pendente
        verify(inventoryService, never()).registerMovement(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void highValueWaste_byManager_autoApprovesAndDeductsStock() {
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");

        // 50 unidades * 100 MZN = 5.000 MZN (> 2.500 MZN, mas é gerente)
        CreateStockWasteRequest req = new CreateStockWasteRequest(
                1L, 10L, 100L, null, new BigDecimal("50"), WasteReason.COLD_CHAIN_FAILURE, "Falha no frigorífico central"
        );

        StockMovement mockMovement = new StockMovement();
        mockMovement.setId(777L);
        when(inventoryService.registerMovement(any(), any(), any(), eq("WASTE"), any(), any(), any(), any()))
                .thenReturn(mockMovement);

        StockWasteDTO result = service.registerWaste(req);

        assertEquals(WasteStatus.APPROVED, result.status());
        assertEquals(new BigDecimal("5000.00"), result.totalCost());
        assertEquals("gerente", result.approvedBy());

        verify(inventoryService, times(1)).registerMovement(
                eq(product), eq(warehouse), eq(new BigDecimal("-50")), eq("WASTE"), isNull(), isNull(), anyString(), isNull()
        );
    }

    @Test
    void approveWaste_pendingRecord_deductsStockAndMarksApproved() {
        StockWaste pending = new StockWaste();
        pending.setId(42L);
        pending.setCompany(company);
        pending.setWarehouse(warehouse);
        pending.setProduct(product);
        pending.setQuantity(new BigDecimal("30"));
        pending.setUnitCost(new BigDecimal("100.00"));
        pending.setTotalCost(new BigDecimal("3000.00"));
        pending.setStatus(WasteStatus.PENDING_APPROVAL);
        pending.setReason(WasteReason.THEFT_OR_SHRINKAGE);

        when(wasteRepository.findById(42L)).thenReturn(Optional.of(pending));

        StockMovement mockMovement = new StockMovement();
        mockMovement.setId(888L);
        when(inventoryService.registerMovement(any(), any(), any(), eq("WASTE"), any(), any(), any(), any()))
                .thenReturn(mockMovement);

        CurrentUserContext.setCurrentUser("admin", "ADMIN");

        ApproveWasteRequest req = new ApproveWasteRequest(true, "Confirmado e auditado");
        StockWasteDTO result = service.approveWaste(42L, req);

        assertEquals(WasteStatus.APPROVED, result.status());
        assertEquals("admin", result.approvedBy());
        assertTrue(result.notes().contains("Confirmado e auditado"));

        verify(inventoryService, times(1)).registerMovement(
                eq(product), eq(warehouse), eq(new BigDecimal("-30")), eq("WASTE"), isNull(), isNull(), anyString(), isNull()
        );
    }

    @Test
    void rejectWaste_marksRejected_doesNotDeductStock() {
        StockWaste pending = new StockWaste();
        pending.setId(42L);
        pending.setCompany(company);
        pending.setWarehouse(warehouse);
        pending.setProduct(product);
        pending.setQuantity(new BigDecimal("30"));
        pending.setStatus(WasteStatus.PENDING_APPROVAL);
        pending.setReason(WasteReason.DAMAGED);

        when(wasteRepository.findById(42L)).thenReturn(Optional.of(pending));

        CurrentUserContext.setCurrentUser("gerente", "MANAGER");

        ApproveWasteRequest req = new ApproveWasteRequest(false, "Mercadoria recuperável após triagem");
        StockWasteDTO result = service.approveWaste(42L, req);

        assertEquals(WasteStatus.REJECTED, result.status());
        assertEquals("gerente", result.approvedBy());
        assertTrue(result.notes().contains("Rejeitado: Mercadoria recuperável após triagem"));

        verify(inventoryService, never()).registerMovement(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void approveWaste_alreadyProcessed_throwsBusinessRuleException() {
        StockWaste approved = new StockWaste();
        approved.setId(42L);
        approved.setStatus(WasteStatus.APPROVED);

        when(wasteRepository.findById(42L)).thenReturn(Optional.of(approved));

        assertThrows(BusinessRuleException.class, () ->
                service.approveWaste(42L, new ApproveWasteRequest(true, "re-aprovar")));
    }

    @Test
    void registerWaste_zeroOrNegativeQuantity_throwsBusinessRuleException() {
        CreateStockWasteRequest zeroQty = new CreateStockWasteRequest(
                1L, 10L, 100L, null, BigDecimal.ZERO, WasteReason.EXPIRED, "Zero"
        );
        assertThrows(BusinessRuleException.class, () -> service.registerWaste(zeroQty));

        CreateStockWasteRequest negQty = new CreateStockWasteRequest(
                1L, 10L, 100L, null, new BigDecimal("-5"), WasteReason.EXPIRED, "Negativo"
        );
        assertThrows(BusinessRuleException.class, () -> service.registerWaste(negQty));
    }

    @Test
    void expiringRadar_correctlyClassifiesUrgency() {
        ProductBatch b1 = new ProductBatch();
        b1.setId(1L);
        b1.setBatchNumber("LOT-001");
        b1.setProduct(product);
        b1.setWarehouse(warehouse);
        b1.setQuantity(new BigDecimal("20"));
        b1.setExpirationDate(LocalDate.now().plusDays(2)); // <= 3 -> CRÍTICO

        ProductBatch b2 = new ProductBatch();
        b2.setId(2L);
        b2.setBatchNumber("LOT-002");
        b2.setProduct(product);
        b2.setWarehouse(warehouse);
        b2.setQuantity(new BigDecimal("15"));
        b2.setExpirationDate(LocalDate.now().plusDays(6)); // <= 7 -> ALTO

        when(batchRepository.findExpiringByCompanyId(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of(b1, b2));

        List<ExpiringBatchAlertDTO> alerts = service.getExpiringBatchesRadar(1L, 30);

        assertEquals(2, alerts.size());
        assertEquals("CRÍTICO", alerts.get(0).alertLevel());
        assertEquals(new BigDecimal("2000.00"), alerts.get(0).potentialLossValue());
        assertEquals("ALTO", alerts.get(1).alertLevel());
        assertEquals(new BigDecimal("1500.00"), alerts.get(1).potentialLossValue());
    }
}
