package mz.multicore.erp.modules.manufacturing.service;

import mz.multicore.erp.architecture.events.ProductionCompletedEvent;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.manufacturing.dto.*;
import mz.multicore.erp.modules.manufacturing.model.*;
import mz.multicore.erp.modules.manufacturing.repository.BillOfMaterialsRepository;
import mz.multicore.erp.modules.manufacturing.repository.ProductionOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Testes Unitários do Serviço de Produção (Manufacturing)")
class ProductionServiceTest {

    private BillOfMaterialsRepository bomRepository;
    private ProductionOrderRepository productionOrderRepository;
    private CompanyRepository companyRepository;
    private ProductRepository productRepository;
    private WarehouseRepository warehouseRepository;
    private StockRepository stockRepository;
    private ApplicationEventPublisher eventPublisher;

    private ProductionService service;

    private Company company;
    private Product finishedProduct;
    private Product rawMaterial;
    private Warehouse rawWh;
    private Warehouse finishedWh;

    @BeforeEach
    void setUp() {
        bomRepository = mock(BillOfMaterialsRepository.class);
        productionOrderRepository = mock(ProductionOrderRepository.class);
        companyRepository = mock(CompanyRepository.class);
        productRepository = mock(ProductRepository.class);
        warehouseRepository = mock(WarehouseRepository.class);
        stockRepository = mock(StockRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        service = new ProductionService(
                bomRepository,
                productionOrderRepository,
                companyRepository,
                productRepository,
                warehouseRepository,
                stockRepository,
                eventPublisher
        );

        company = new Company();
        company.setId(1L);
        company.setName("Multicore Test Factory");

        finishedProduct = new Product();
        finishedProduct.setId(10L);
        finishedProduct.setSku("PROD-FIN-01");
        finishedProduct.setName("Cadeira Ergonómica");

        rawMaterial = new Product();
        rawMaterial.setId(20L);
        rawMaterial.setSku("MAT-RAW-01");
        rawMaterial.setName("Tubo de Aço");
        rawMaterial.setPurchasePrice(new BigDecimal("150.00"));

        rawWh = new Warehouse();
        rawWh.setId(100L);
        rawWh.setName("Armazém Matéria-Prima");
        rawWh.setCompany(company);

        finishedWh = new Warehouse();
        finishedWh.setId(200L);
        finishedWh.setName("Armazém Produtos Acabados");
        finishedWh.setCompany(company);

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(productRepository.findById(10L)).thenReturn(Optional.of(finishedProduct));
        when(productRepository.findById(20L)).thenReturn(Optional.of(rawMaterial));
        when(warehouseRepository.findById(100L)).thenReturn(Optional.of(rawWh));
        when(warehouseRepository.findById(200L)).thenReturn(Optional.of(finishedWh));
    }

    @Test
    @DisplayName("Cria Ficha Técnica (BOM) com cálculo correto de estimativas")
    void testCreateBom() {
        when(bomRepository.existsByCompanyIdAndBomCode(1L, "BOM-001")).thenReturn(false);
        when(bomRepository.save(any(BillOfMaterials.class))).thenAnswer(inv -> {
            BillOfMaterials b = inv.getArgument(0);
            b.setId(50L);
            return b;
        });

        CreateBomRequest req = new CreateBomRequest(
                "BOM-001",
                "Receita Cadeira Ergonómica",
                10L,
                new BigDecimal("1.00"),
                BomType.STANDARD,
                new BigDecimal("200.00"),
                new BigDecimal("50.00"),
                List.of(new CreateBomRequest.CreateBomItemRequest(20L, new BigDecimal("2.00"), BigDecimal.ZERO))
        );

        BillOfMaterialsDTO dto = service.createBom(1L, req);
        assertNotNull(dto);
        assertEquals("BOM-001", dto.bomCode());
        assertEquals(0, new BigDecimal("550.00").compareTo(dto.totalEstimatedCost()));
    }

    @Test
    @DisplayName("Ciclo completo de Ordem de Produção: Criar, Iniciar, Concluir e Publicar Evento")
    void testProductionOrderFullLifecycle() {
        BillOfMaterials bom = new BillOfMaterials();
        bom.setId(50L);
        bom.setCompany(company);
        bom.setBomCode("BOM-001");
        bom.setFinishedProduct(finishedProduct);
        bom.setBaseQuantity(new BigDecimal("1.00"));
        bom.setEstimatedLaborCost(new BigDecimal("200.00"));
        bom.setEstimatedOverheadCost(new BigDecimal("50.00"));

        BomItem item = new BomItem();
        item.setRawMaterialProduct(rawMaterial);
        item.setRequiredQuantity(new BigDecimal("2.00"));
        item.setScrapPercentage(BigDecimal.ZERO);
        bom.addItem(item);

        when(bomRepository.findByIdAndCompanyId(50L, 1L)).thenReturn(Optional.of(bom));
        when(productionOrderRepository.save(any(ProductionOrder.class))).thenAnswer(inv -> {
            ProductionOrder o = inv.getArgument(0);
            if (o.getId() == null) o.setId(500L);
            return o;
        });

        // 1. Criar Ordem
        CreateProductionOrderRequest createReq = new CreateProductionOrderRequest(
                50L,
                new BigDecimal("10.00"),
                100L,
                200L,
                LocalDate.now().plusDays(5)
        );
        ProductionOrderDTO created = service.createProductionOrder(1L, createReq);
        assertNotNull(created);
        assertEquals(ProductionOrderStatus.PLANNED, created.status());
        assertEquals(new BigDecimal("10.00"), created.plannedQuantity());

        // 2. Iniciar Ordem
        ProductionOrder orderEntity = new ProductionOrder();
        orderEntity.setId(500L);
        orderEntity.setCompany(company);
        orderEntity.setBom(bom);
        orderEntity.setTargetProduct(finishedProduct);
        orderEntity.setSourceWarehouse(rawWh);
        orderEntity.setTargetWarehouse(finishedWh);
        orderEntity.setPlannedQuantity(new BigDecimal("10.00"));
        orderEntity.setStatus(ProductionOrderStatus.PLANNED);

        ProductionOrderLine line = new ProductionOrderLine();
        line.setComponentProduct(rawMaterial);
        line.setPlannedQuantity(new BigDecimal("20.00"));
        line.setUnitCost(new BigDecimal("150.00"));
        orderEntity.addLine(line);

        when(productionOrderRepository.findByIdAndCompanyId(500L, 1L)).thenReturn(Optional.of(orderEntity));

        ProductionOrderDTO started = service.startProduction(1L, 500L);
        assertEquals(ProductionOrderStatus.IN_PROGRESS, started.status());

        // 3. Concluir Ordem (com movimentação de stock e evento)
        Stock rawStock = new Stock();
        rawStock.setProduct(rawMaterial);
        rawStock.setWarehouse(rawWh);
        rawStock.setQuantity(new BigDecimal("50.00"));
        when(stockRepository.findForReservation(20L, 100L)).thenReturn(Optional.of(rawStock));

        Stock finishedStock = new Stock();
        finishedStock.setProduct(finishedProduct);
        finishedStock.setWarehouse(finishedWh);
        finishedStock.setQuantity(BigDecimal.ZERO);
        when(stockRepository.findForReservation(10L, 200L)).thenReturn(Optional.of(finishedStock));

        ProductionOrderDTO completed = service.completeProduction(1L, 500L, new BigDecimal("10.00"));
        assertEquals(ProductionOrderStatus.COMPLETED, completed.status());
        assertEquals(new BigDecimal("10.00"), completed.producedQuantity());

        // Verificar baixa no estoque de matéria prima: 50 - 20 = 30
        assertEquals(new BigDecimal("30.00"), rawStock.getQuantity());

        // Verificar entrada no estoque de produto acabado: 0 + 10 = 10
        assertEquals(new BigDecimal("10.00"), finishedStock.getQuantity());

        // Verificar publicação de ProductionCompletedEvent
        verify(eventPublisher, times(1)).publishEvent(any(ProductionCompletedEvent.class));
    }
}
