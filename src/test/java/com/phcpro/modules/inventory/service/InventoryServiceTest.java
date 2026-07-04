package com.phcpro.modules.inventory.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.comercial.repository.ProductRepository;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.inventory.dto.ProductBatchDTO;
import com.phcpro.modules.inventory.repository.StockMovementRepository;
import com.phcpro.modules.inventory.repository.StockRepository;
import com.phcpro.modules.inventory.repository.WarehouseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes do alerta de validade ({@link InventoryService#findExpiringBatches}). Garante que o
 * horizonte é convertido para data de corte (hoje + dias), que a empresa activa é exigida e que
 * a consulta é delegada no {@link ProductBatchService}. Dependências mockadas.
 */
class InventoryServiceTest {

    private static final Long COMPANY_ID = 1L;

    private ProductBatchService productBatchService;
    private WarehouseRepository warehouseRepository;
    private InventoryService service;

    @BeforeEach
    void setUp() {
        productBatchService = mock(ProductBatchService.class);
        warehouseRepository = mock(WarehouseRepository.class);
        service = new InventoryService(
                warehouseRepository,
                mock(StockRepository.class),
                mock(StockMovementRepository.class),
                mock(CompanyRepository.class),
                productBatchService,
                mock(ProductRepository.class),
                mock(AuditLogService.class));

        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void findExpiringBatches_usaCorteHojeMaisDias_eDelega() {
        ProductBatchDTO dto = batch(LocalDate.now().plusDays(5));
        when(productBatchService.findExpiringByCompany(eq(COMPANY_ID), any(LocalDate.class)))
                .thenReturn(List.of(dto));

        List<ProductBatchDTO> result = service.findExpiringBatches(COMPANY_ID, 30);

        assertEquals(1, result.size());
        verify(productBatchService).findExpiringByCompany(COMPANY_ID, LocalDate.now().plusDays(30));
    }

    @Test
    void findExpiringBatches_empresaDiferenteDaActiva_lancaBusinessRule() {
        assertThrows(BusinessRuleException.class,
                () -> service.findExpiringBatches(999L, 30));
        verifyNoInteractions(productBatchService);
    }

    @Test
    void getSalesWarehouses_soActivosEQuePermitemVendas() {
        com.phcpro.modules.inventory.model.Warehouse loja = warehouse("Loja", true, true);
        com.phcpro.modules.inventory.model.Warehouse deposito = warehouse("Depósito", true, false);   // não vende
        com.phcpro.modules.inventory.model.Warehouse inactivo = warehouse("Antigo", false, true);     // inactivo
        when(warehouseRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(loja, deposito, inactivo));

        List<com.phcpro.modules.inventory.model.Warehouse> vendas = service.getSalesWarehousesByCompany(COMPANY_ID);
        assertEquals(1, vendas.size());
        assertEquals("Loja", vendas.get(0).getName());

        // getWarehousesByCompany exclui inactivos, mas mantém depósitos.
        assertEquals(2, service.getWarehousesByCompany(COMPANY_ID).size());
    }

    private com.phcpro.modules.inventory.model.Warehouse warehouse(String name, boolean active, boolean allowsSales) {
        com.phcpro.modules.inventory.model.Warehouse w = new com.phcpro.modules.inventory.model.Warehouse();
        w.setName(name);
        w.setActive(active);
        w.setAllowsSales(allowsSales);
        return w;
    }

    private ProductBatchDTO batch(LocalDate expiration) {
        return new ProductBatchDTO(
                1L, 100L, "SKU-1", "Leite", 10L, "Loja Central",
                "EXP-" + expiration, expiration, LocalDate.now(), new BigDecimal("12"));
    }
}
