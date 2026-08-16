package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.ReprintAuthorizationRequest;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.repository.OrderEventRepository;
import mz.multicore.erp.modules.comercial.repository.OrderLineRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.printing.OrderPickingPrintService;
import mz.multicore.erp.modules.users.service.AppUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CustomerOrderFulfillmentHarnessTest {
    @AfterEach void clear() { CurrentUserContext.clear(); }

    @Test
    void reprintRequiresDifferentApproverBeforePasswordValidation() {
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
        CurrentUserContext.setCurrentCompanyId(1L);
        Order order = new Order();
        order.setId(7L); order.setStatus("IN_SEPARATION");
        Company company = new Company(); company.setId(1L); order.setCompany(company);
        OrderRepository orders = mock(OrderRepository.class);
        when(orders.findById(7L)).thenReturn(Optional.of(order));
        CustomerOrderFulfillmentService service = new CustomerOrderFulfillmentService(
                mock(ComercialService.class), orders, mock(OrderLineRepository.class),
                mock(OrderEventRepository.class), mock(InventoryService.class),
                mock(OrderPickingPrintService.class), mock(AppUserService.class),
                mock(mz.multicore.erp.modules.approvals.service.ApprovalService.class));

        assertThrows(BusinessRuleException.class, () -> service.reprint(7L,
                new ReprintAuthorizationRequest("gerente", "senha", "papel danificado", "POS-1")));
    }
}
