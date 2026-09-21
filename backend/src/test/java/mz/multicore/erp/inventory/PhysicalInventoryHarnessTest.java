package mz.multicore.erp.inventory;

import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.dto.*;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.InventoryPhysicalCountingService;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PhysicalInventoryHarnessTest {

    @Autowired
    private InventoryPhysicalCountingService countingService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private Company testCompany;
    private Warehouse testWarehouse;
    private Product prodA;
    private Product prodB;

    @BeforeEach
    void setUp() {
        testCompany = new Company();
        testCompany.setName("Empresa Teste Inventário Físico");
        testCompany.setTaxId("999888777");
        testCompany = companyRepository.save(testCompany);

        CurrentUserContext.setCurrentCompanyId(testCompany.getId());
        CurrentUserContext.setCurrentUser("admin", "ADMIN");

        testWarehouse = inventoryService.createWarehouse("Armazém Principal", "WH-01", BigDecimal.valueOf(1000), "Maputo", testCompany);

        prodA = new Product();
        prodA.setName("Arroz 5kg");
        prodA.setSku("ARR-001");
        prodA.setBarcode("560123456789");
        prodA.setPurchasePrice(BigDecimal.valueOf(300));
        prodA.setUnitPrice(BigDecimal.valueOf(400));
        prodA.setStockTracked(true);
        prodA.getCompanies().add(testCompany);
        prodA = productRepository.save(prodA);

        prodB = new Product();
        prodB.setName("Óleo 1L");
        prodB.setSku("OLE-001");
        prodB.setBarcode("560987654321");
        prodB.setPurchasePrice(BigDecimal.valueOf(120));
        prodB.setUnitPrice(BigDecimal.valueOf(150));
        prodB.setStockTracked(true);
        prodB.getCompanies().add(testCompany);
        prodB = productRepository.save(prodB);

        // Inicializar stock inicial
        inventoryService.registerMovement(prodA, testWarehouse, BigDecimal.valueOf(10), "ENTRY", null, null, "Stock Inicial Arroz");
        inventoryService.registerMovement(prodB, testWarehouse, BigDecimal.valueOf(5), "ENTRY", null, null, "Stock Inicial Óleo");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void testFullPhysicalInventoryLifecycle() {
        // 1. Criar sessão de inventário
        CreateInventorySessionRequest createReq = new CreateInventorySessionRequest("Inventário Mensal Armazém", false, null);
        InventorySessionDTO session = countingService.createSession(createReq, testCompany.getId());

        assertNotNull(session);
        assertEquals(InventoryStatus.DRAFT, session.status());
        assertEquals(2, session.totalItems());

        // 2. Iniciar contagem
        session = countingService.startCounting(session.id(), testCompany.getId());
        assertEquals(InventoryStatus.IN_PROGRESS, session.status());

        // 3. Registar contagem (Arroz: esperado 10, contado 12 -> sobra de 2)
        InventoryItemDTO itemA = session.items().stream()
                .filter(i -> i.productId().equals(prodA.getId()))
                .findFirst()
                .orElseThrow();

        UpdateInventoryItemCountRequest countReqA = new UpdateInventoryItemCountRequest(itemA.id(), BigDecimal.valueOf(12), null, false, "Contagem c/ sobra");
        session = countingService.recordCount(session.id(), countReqA, testCompany.getId());

        // 4. Registar contagem por código de barras (Óleo: esperado 5, contado 3 -> falta de 2)
        UpdateInventoryItemCountRequest countReqB = new UpdateInventoryItemCountRequest(null, BigDecimal.valueOf(3), "560987654321", false, "Contagem c/ falta");
        session = countingService.recordCount(session.id(), countReqB, testCompany.getId());

        // Verificar divergências
        assertEquals(BigDecimal.valueOf(600).setScale(4), session.totalSurplusValue().setScale(4)); // 2 * 300
        assertEquals(BigDecimal.valueOf(240).setScale(4), session.totalDeficitValue().setScale(4)); // 2 * 120
        assertEquals(BigDecimal.valueOf(360).setScale(4), session.netFinancialImpact().setScale(4)); // 600 - 240

        // 5. Concluir inventário e ajustar stock
        session = countingService.closeAndAdjustStock(session.id(), testCompany.getId());
        assertEquals(InventoryStatus.CLOSED, session.status());
        assertNotNull(session.endDate());

        // 6. Verificar se o stock real dos produtos foi ajustado
        BigDecimal newStockA = inventoryService.getStocksByWarehouse(testWarehouse.getId()).stream()
                .filter(s -> s.getProduct().getId().equals(prodA.getId()))
                .map(s -> s.getQuantity())
                .findFirst()
                .orElse(BigDecimal.ZERO);

        BigDecimal newStockB = inventoryService.getStocksByWarehouse(testWarehouse.getId()).stream()
                .filter(s -> s.getProduct().getId().equals(prodB.getId()))
                .map(s -> s.getQuantity())
                .findFirst()
                .orElse(BigDecimal.ZERO);

        assertEquals(BigDecimal.valueOf(12).setScale(4), newStockA.setScale(4));
        assertEquals(BigDecimal.valueOf(3).setScale(4), newStockB.setScale(4));
    }
}
