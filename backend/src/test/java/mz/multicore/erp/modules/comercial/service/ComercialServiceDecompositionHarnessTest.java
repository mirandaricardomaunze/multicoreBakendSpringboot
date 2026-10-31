package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.comercial.repository.ReceiptRepository;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComercialServiceDecompositionHarnessTest {

    @Test
    void comercialServiceDelegatesToCohesiveCollaboratorsAndRespectsLineLimits() throws Exception {
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        ReceiptRepository receiptRepository = mock(ReceiptRepository.class);
        TreasuryAccountRepository treasuryAccountRepository = mock(TreasuryAccountRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        DocumentNumberService documentNumberService = mock(DocumentNumberService.class);
        FinanceService financeService = mock(FinanceService.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        ComercialClientAndCreditService clientService = mock(ComercialClientAndCreditService.class);
        ComercialProductCatalogService productService = mock(ComercialProductCatalogService.class);
        ComercialInvoiceLifecycleService invoiceService = mock(ComercialInvoiceLifecycleService.class);
        ComercialOrderLifecycleService orderService = mock(ComercialOrderLifecycleService.class);

        ComercialService service = new ComercialService(
                invoiceRepository, receiptRepository, treasuryAccountRepository, orderRepository,
                documentNumberService, financeService, auditLogService, eventPublisher,
                clientService, productService, invoiceService, orderService);

        List<ClientDTO> clients = List.of();
        when(clientService.getAllClients()).thenReturn(clients);
        assertSame(clients, service.getAllClients());
        verify(clientService).getAllClients();

        List<ProductDTO> products = List.of();
        when(productService.getAllProducts()).thenReturn(products);
        assertSame(products, service.getAllProducts());
        verify(productService).getAllProducts();

        List<InvoiceDTO> invoices = List.of();
        when(invoiceService.getAllInvoices()).thenReturn(invoices);
        assertSame(invoices, service.getAllInvoices());
        verify(invoiceService).getAllInvoices();

        List<OrderDTO> orders = List.of();
        when(orderService.getOrdersByCompany(1L)).thenReturn(orders);
        assertSame(orders, service.getOrdersByCompany(1L));
        verify(orderService).getOrdersByCompany(1L);

        Path base = findModuleRoot().resolve("src/main/java/mz/multicore/erp/modules/comercial/service");
        int facadeLines = Files.readAllLines(base.resolve("ComercialService.java")).size();
        int clientLines = Files.readAllLines(base.resolve("ComercialClientAndCreditService.java")).size();
        int productLines = Files.readAllLines(base.resolve("ComercialProductCatalogService.java")).size();
        int invoiceLines = Files.readAllLines(base.resolve("ComercialInvoiceLifecycleService.java")).size();
        int orderLines = Files.readAllLines(base.resolve("ComercialOrderLifecycleService.java")).size();

        assertTrue(facadeLines <= 565, "ComercialService deve ter <= 565 linhas, mas tem " + facadeLines);
        assertTrue(clientLines <= 235, "ComercialClientAndCreditService deve ter <= 235 linhas, mas tem " + clientLines);
        assertTrue(productLines <= 420, "ComercialProductCatalogService deve ter <= 420 linhas, mas tem " + productLines);
        assertTrue(invoiceLines <= 420, "ComercialInvoiceLifecycleService deve ter <= 420 linhas, mas tem " + invoiceLines);
        assertTrue(orderLines <= 600, "ComercialOrderLifecycleService deve ter <= 600 linhas, mas tem " + orderLines);
    }

    private Path findModuleRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (cwd.endsWith("backend")) {
            return cwd;
        }
        return cwd.resolve("backend");
    }
}
