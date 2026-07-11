package com.phcpro.modules.inventory.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.comercial.model.Product;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.inventory.dto.CreateStockAdjustmentRequest;
import com.phcpro.modules.inventory.dto.InventoryCountDTO;
import com.phcpro.modules.inventory.model.InventoryCount;
import com.phcpro.modules.inventory.model.InventoryCountLine;
import com.phcpro.modules.inventory.model.InventoryCountStatus;
import com.phcpro.modules.inventory.model.Stock;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.repository.InventoryCountRepository;
import com.phcpro.modules.inventory.repository.WarehouseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes da orquestração de sessões de contagem (IC-01..05): criar DRAFT com linha por artigo,
 * aplicar (só as linhas contadas geram ajuste e a sessão fecha), guardas de estado, cancelar.
 * Dependências mockadas; {@code InventoryService.adjustStock} é o já testado — aqui verifica-se a
 * orquestração, não o ajuste em si.
 */
class InventoryCountServiceTest {

    private InventoryCountRepository inventoryCountRepository;
    private WarehouseRepository warehouseRepository;
    private InventoryService inventoryService;
    private AuditLogService auditLogService;
    private InventoryCountService service;

    private static final Long COMPANY_ID = 1L;
    private static final Long WAREHOUSE_ID = 10L;
    private Company company;
    private Warehouse warehouse;
    private Product p1;
    private Product p2;

    @BeforeEach
    void setUp() {
        inventoryCountRepository = mock(InventoryCountRepository.class);
        warehouseRepository = mock(WarehouseRepository.class);
        inventoryService = mock(InventoryService.class);
        auditLogService = mock(AuditLogService.class);
        service = new InventoryCountService(inventoryCountRepository, warehouseRepository, inventoryService, auditLogService);

        company = new Company();
        company.setId(COMPANY_ID);
        warehouse = new Warehouse();
        warehouse.setId(WAREHOUSE_ID);
        warehouse.setName("Loja");
        warehouse.setCompany(company);
        p1 = product(100L, "SKU1", "Arroz");
        p2 = product(200L, "SKU2", "Óleo");

        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gestor", "ADMIN");

        when(inventoryCountRepository.save(any(InventoryCount.class))).thenAnswer(inv -> {
            InventoryCount c = inv.getArgument(0);
            if (c.getId() == null) c.setId(5L);
            return c;
        });
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void createSession_criaDraftComLinhaPorArtigo() {
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(warehouse));
        when(inventoryService.getStocksByWarehouse(WAREHOUSE_ID))
                .thenReturn(List.of(stock(p1, new BigDecimal("10")), stock(p2, new BigDecimal("5"))));

        InventoryCountDTO dto = service.createSession(WAREHOUSE_ID, null);

        assertEquals("DRAFT", dto.status());
        assertEquals(2, dto.totalItems());
        assertEquals(0, dto.countedItems());
        verify(inventoryCountRepository).save(any(InventoryCount.class));
    }

    @Test
    void applySession_ajustaSoLinhasContadas_eFechaSessao() {
        InventoryCount count = draftWith(line(p1, new BigDecimal("8")), line(p2, null));
        when(inventoryCountRepository.findById(5L)).thenReturn(Optional.of(count));
        when(inventoryService.getStocksByWarehouse(WAREHOUSE_ID))
                .thenReturn(List.of(stock(p1, new BigDecimal("10")), stock(p2, new BigDecimal("5"))));

        InventoryCountDTO dto = service.applySession(5L);

        assertEquals("APPLIED", dto.status());
        ArgumentCaptor<CreateStockAdjustmentRequest> cap = ArgumentCaptor.forClass(CreateStockAdjustmentRequest.class);
        verify(inventoryService, times(1)).adjustStock(cap.capture());
        assertEquals(0, new BigDecimal("8").compareTo(cap.getValue().countedQuantity()));
        assertEquals(100L, cap.getValue().productId());
    }

    @Test
    void applySession_naoDraft_lanca() {
        InventoryCount count = draftWith(line(p1, new BigDecimal("8")));
        count.setStatus(InventoryCountStatus.APPLIED);
        when(inventoryCountRepository.findById(5L)).thenReturn(Optional.of(count));

        assertThrows(BusinessRuleException.class, () -> service.applySession(5L));
        verify(inventoryService, never()).adjustStock(any());
    }

    @Test
    void saveCounts_atualizaContagemDoArtigo() {
        InventoryCount count = draftWith(line(p1, null), line(p2, null));
        when(inventoryCountRepository.findById(5L)).thenReturn(Optional.of(count));

        InventoryCountDTO dto = service.saveCounts(5L, Map.of(100L, new BigDecimal("7")));

        assertEquals(1, dto.countedItems());
    }

    @Test
    void cancelSession_marcaCancelada() {
        InventoryCount count = draftWith(line(p1, null));
        when(inventoryCountRepository.findById(5L)).thenReturn(Optional.of(count));

        service.cancelSession(5L);

        assertEquals(InventoryCountStatus.CANCELLED, count.getStatus());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Product product(Long id, String sku, String name) {
        Product p = new Product();
        p.setId(id);
        p.setSku(sku);
        p.setName(name);
        return p;
    }

    private Stock stock(Product p, BigDecimal qty) {
        Stock s = new Stock();
        s.setProduct(p);
        s.setQuantity(qty);
        return s;
    }

    private InventoryCountLine line(Product p, BigDecimal counted) {
        InventoryCountLine l = new InventoryCountLine();
        l.setProduct(p);
        l.setCountedQuantity(counted);
        return l;
    }

    private InventoryCount draftWith(InventoryCountLine... lines) {
        InventoryCount c = new InventoryCount();
        c.setId(5L);
        c.setCompany(company);
        c.setWarehouse(warehouse);
        c.setStatus(InventoryCountStatus.DRAFT);
        for (InventoryCountLine l : lines) {
            l.setInventoryCount(c);
            c.getLines().add(l);
        }
        return c;
    }
}
