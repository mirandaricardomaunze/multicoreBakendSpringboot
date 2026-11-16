package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.DeliveryGuide;
import mz.multicore.erp.modules.comercial.model.DeliveryGuideLine;
import mz.multicore.erp.modules.comercial.model.DeliveryGuideStatus;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.DeliveryGuideRepository;
import mz.multicore.erp.modules.comercial.repository.OrderLineRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** A guia é onde a mercadoria da encomenda A4 sai: também tem de respeitar reservas (RS-06, RS-07). */
class DeliveryGuideApprovalReservationTest {

    private DeliveryGuideRepository guides;
    private DeliveryGuideQueryService queries;
    private InventoryService inventory;
    private OrderLineRepository orderLines;
    private DeliveryGuideLifecycleService service;
    private DeliveryGuide guide;

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(1L);
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
        guides = mock(DeliveryGuideRepository.class);
        queries = mock(DeliveryGuideQueryService.class);
        inventory = mock(InventoryService.class);
        orderLines = mock(OrderLineRepository.class);
        service = new DeliveryGuideLifecycleService(guides, mock(OrderRepository.class), inventory,
                mock(AuditLogService.class), queries, orderLines);

        Warehouse warehouse = new Warehouse();
        warehouse.setId(10L);
        warehouse.setName("Central");
        Product product = new Product();
        product.setId(1L);
        product.setName("Arroz");
        DeliveryGuideLine line = new DeliveryGuideLine();
        line.setProduct(product);
        line.setQuantity(new BigDecimal("5"));
        guide = new DeliveryGuide();
        guide.setId(3L);
        guide.setGuideNumber("GR-2026/3");
        guide.setWarehouse(warehouse);
        guide.setStatus(DeliveryGuideStatus.PENDING_APPROVAL);
        guide.addLine(line);
        when(queries.load(3L)).thenReturn(guide);
        when(inventory.lockAndGetPhysicalQuantity(1L, 10L)).thenReturn(new BigDecimal("8"));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test // RS-06
    void aprovarGuia_stockReservadoParaOutroPedido_recusaENaoMoveStock() {
        when(orderLines.sumActiveReservations(1L, 10L)).thenReturn(new BigDecimal("6"));

        assertThrows(BusinessRuleException.class, () -> service.approve(3L));

        assertEquals(DeliveryGuideStatus.PENDING_APPROVAL, guide.getStatus());
        verify(inventory, never()).registerMovement(any(), any(), any(), any(), any(), any(), any());
    }

    @Test // RS-07
    void aprovarGuia_bloqueiaAGuiaAntesDeALer() {
        when(orderLines.sumActiveReservations(1L, 10L)).thenReturn(BigDecimal.ZERO);
        when(guides.save(any(DeliveryGuide.class))).thenAnswer(inv -> inv.getArgument(0));

        service.approve(3L);

        InOrder order = inOrder(guides, queries);
        order.verify(guides).findForUpdate(3L, 1L);
        order.verify(queries).load(3L);
        assertEquals(DeliveryGuideStatus.APPROVED, guide.getStatus());
    }
}
