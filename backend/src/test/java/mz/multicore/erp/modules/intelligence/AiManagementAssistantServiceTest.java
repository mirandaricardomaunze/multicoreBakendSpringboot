package mz.multicore.erp.modules.intelligence;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.intelligence.dto.AiAssistantRequest;
import mz.multicore.erp.modules.intelligence.service.AiManagementAssistantService;
import mz.multicore.erp.modules.intelligence.service.ManagementAiGateway;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.purchases.model.PurchaseOrder;
import mz.multicore.erp.modules.purchases.repository.PurchaseOrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiManagementAssistantServiceTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock StockRepository stockRepository;
    @Mock PurchaseOrderRepository purchaseOrderRepository;
    @Mock ManagementAiGateway aiGateway;
    @Mock AuditLogService auditLogService;

    private AiManagementAssistantService service;

    @BeforeEach
    void setUp() {
        service = new AiManagementAssistantService(invoiceRepository, stockRepository,
                purchaseOrderRepository, aiGateway, auditLogService);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
        CurrentUserContext.setCurrentCompanyId(41L);
    }

    @AfterEach void clearContext() { CurrentUserContext.clear(); }

    @Test
    void aggregatesOnlyActiveTenantAndFallsBackWithoutExternalProvider() {
        Invoice invoice = mock(Invoice.class);
        when(invoice.getStatus()).thenReturn(InvoiceStatus.PAID);
        when(invoice.getTotalAmount()).thenReturn(new BigDecimal("1500.00"));
        when(invoiceRepository.findByCompanyIdAndCreatedAtBetween(eq(41L), any(), any())).thenReturn(List.of(invoice));

        Product product = mock(Product.class);
        when(product.getMinStock()).thenReturn(BigDecimal.TEN);
        Stock low = mock(Stock.class);
        when(low.getQuantity()).thenReturn(BigDecimal.valueOf(4));
        when(low.getProduct()).thenReturn(product);
        when(stockRepository.findByWarehouseCompanyId(41L)).thenReturn(List.of(low));

        PurchaseOrder delayed = mock(PurchaseOrder.class);
        when(delayed.getStatus()).thenReturn(PurchaseOrder.ORDERED);
        when(delayed.getExpectedDate()).thenReturn(LocalDate.now().minusDays(1));
        when(delayed.getTotalAmount()).thenReturn(new BigDecimal("800.00"));
        when(purchaseOrderRepository.findByCompanyIdOrderByOrderDateDesc(41L)).thenReturn(List.of(delayed));
        when(aiGateway.answer(anyString(), any())).thenReturn(Optional.empty());

        var response = service.ask(new AiAssistantRequest("Como está o stock?"));

        assertThat(response.externalModelUsed()).isFalse();
        assertThat(response.answer()).contains("1 abaixo do stock mínimo");
        assertThat(response.evidence()).extracting(item -> item.metric())
                .contains("sales_total", "low_stock", "overdue_purchase_orders");
        verify(invoiceRepository).findByCompanyIdAndCreatedAtBetween(eq(41L), any(), any());
        verify(stockRepository).findByWarehouseCompanyId(41L);
        verify(purchaseOrderRepository).findByCompanyIdOrderByOrderDateDesc(41L);
        ArgumentCaptor<String> externalPrompt = ArgumentCaptor.forClass(String.class);
        verify(aiGateway).answer(externalPrompt.capture(), any());
        assertThat(externalPrompt.getValue()).contains("risco agregado de ruptura")
                .doesNotContain("Como está o stock?");

        ArgumentCaptor<String> auditDetails = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).logCurrent(eq("AI_MANAGEMENT_QUERY"), auditDetails.capture());
        assertThat(auditDetails.getValue()).contains("tema=stock").doesNotContain("Como está o stock?");
    }

    @Test
    void refusesRolesWithoutManagementPermissionBeforeReadingRepositories() {
        CurrentUserContext.setCurrentUser("vendedor", "SELLER");

        assertThatThrownBy(() -> service.ask(new AiAssistantRequest("Mostre as vendas")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("MANAGER ou ADMIN");
        verifyNoInteractions(invoiceRepository, stockRepository, purchaseOrderRepository, aiGateway);
    }
}
