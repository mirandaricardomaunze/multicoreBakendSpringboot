package com.phcpro;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.modules.comercial.dto.ClientDTO;
import com.phcpro.modules.comercial.dto.CreateInvoiceLineRequest;
import com.phcpro.modules.comercial.dto.CreateInvoiceRequest;
import com.phcpro.modules.comercial.dto.InvoiceDTO;
import com.phcpro.modules.comercial.model.Client;
import com.phcpro.modules.comercial.model.InvoiceStatus;
import com.phcpro.modules.comercial.model.Product;
import com.phcpro.modules.comercial.repository.ClientRepository;
import com.phcpro.modules.comercial.repository.ProductRepository;
import com.phcpro.modules.comercial.service.ComercialService;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.financeira.model.TreasuryAccount;
import com.phcpro.modules.financeira.repository.TreasuryAccountRepository;
import com.phcpro.modules.inventory.model.Stock;
import com.phcpro.modules.inventory.repository.StockRepository;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.repository.WarehouseRepository;
import com.phcpro.modules.inventory.service.InventoryService;
import com.phcpro.modules.pos.dto.POSCheckoutLineRequest;
import com.phcpro.modules.pos.dto.POSCheckoutRequest;
import com.phcpro.modules.pos.model.TillSession;
import com.phcpro.modules.pos.service.POSService;
import com.phcpro.modules.purchases.model.Supplier;
import com.phcpro.modules.purchases.service.PurchaseService;
import com.phcpro.modules.backup.service.BackupService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
public class MulticoreServicesTest {

    static {
        // Prevent headless exception when Swing GUI beans are initialized in Spring Boot Test
        System.setProperty("java.awt.headless", "false");
    }

    @Autowired
    private ComercialService comercialService;

    @Autowired
    private POSService posService;

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private BackupService backupService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private TreasuryAccountRepository treasuryAccountRepository;

    @Autowired
    private StockRepository stockRepository;

    @Test
    public void testTaxIdValidation() {
        // Valid NUIT / NIF: exactly 9 digits
        assertDoesNotThrow(() -> {
            ClientDTO c = comercialService.createClient("Test Client Valid", "999888777", "valid@email.com", "Test Addr");
            assertNotNull(c);
        });

        // Invalid NUIT: non-digit
        assertThrows(BusinessRuleException.class, () -> {
            comercialService.createClient("Test Client Invalid 1", "12345678a", "invalid@email.com", "Test Addr");
        });

        // Invalid NUIT: too short
        assertThrows(BusinessRuleException.class, () -> {
            comercialService.createClient("Test Client Invalid 2", "12345678", "invalid@email.com", "Test Addr");
        });

        // Invalid NUIT: too long
        assertThrows(BusinessRuleException.class, () -> {
            comercialService.createClient("Test Client Invalid 3", "1234567890", "invalid@email.com", "Test Addr");
        });
    }

    @Test
    public void testPOSSessionAndCheckout() {
        Company company = companyRepository.findAll().get(0);
        Client client = clientRepository.findAll().get(0);
        Product product = productRepository.findAll().get(0);
        Warehouse warehouse = warehouseRepository.findAll().stream()
                .filter(w -> w.getCompany().getId().equals(company.getId()))
                .findFirst()
                .orElseThrow();
        TreasuryAccount account = treasuryAccountRepository.findAll().get(0);

        String operatorUnopened = "maria_unopened";

        // 1. POS Checkout without open session should throw BusinessRuleException
        POSCheckoutLineRequest line = new POSCheckoutLineRequest(product.getId(), 1, BigDecimal.ZERO, "LOTE1", "SERIE1");
        POSCheckoutRequest requestUnopened = new POSCheckoutRequest(
                operatorUnopened,
                company.getId(),
                client.getId(),
                warehouse.getId(),
                account.getId(),
                List.of(line),
                null
        );

        assertThrows(BusinessRuleException.class, () -> posService.checkout(requestUnopened));

        // 2. Open session for a different operator (or same if we want to test flow)
        String operator = "maria_pos";
        POSCheckoutRequest request = new POSCheckoutRequest(
                operator,
                company.getId(),
                client.getId(),
                warehouse.getId(),
                account.getId(),
                List.of(line),
                null
        );
        TillSession session = posService.openSession(operator, new BigDecimal("100.00"), company.getId());
        assertNotNull(session);
        assertEquals("OPEN", session.getStatus());

        // 3. Checkout should now succeed
        assertDoesNotThrow(() -> {
            var invoice = posService.checkout(request);
            assertNotNull(invoice);
            assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        });

        // 4. Close session
        TillSession closedSession = posService.closeSession(session.getId(), new BigDecimal("150.00"));
        assertNotNull(closedSession);
        assertEquals("CLOSED", closedSession.getStatus());
    }

    @Test
    public void testDiscountApprovalThreshold() {
        Company company = companyRepository.findAll().get(0);
        Client client = clientRepository.findAll().get(0);
        Product product = productRepository.findAll().get(0);
        Warehouse warehouse = warehouseRepository.findAll().stream()
                .filter(w -> w.getCompany().getId().equals(company.getId()))
                .findFirst()
                .orElseThrow();

        // 1. Discount = 5% (<= 10%), status should be PENDING_APPROVAL (standard approval flow)
        CreateInvoiceLineRequest lineNormal = new CreateInvoiceLineRequest(
                product.getId(),
                1,
                BigDecimal.valueOf(0.23),
                BigDecimal.valueOf(5.0),
                "LOTE-N",
                "SERIE-N"
        );
        CreateInvoiceRequest reqNormal = new CreateInvoiceRequest(
                client.getId(),
                warehouse.getId(),
                company.getId(),
                List.of(lineNormal)
        );

        InvoiceDTO invNormal = comercialService.createInvoice(reqNormal);
        assertEquals(InvoiceStatus.PENDING_APPROVAL, invNormal.status());

        // 2. Discount = 15% (> 10%), status should be PENDING_DISCOUNT_APPROVAL
        CreateInvoiceLineRequest lineHigh = new CreateInvoiceLineRequest(
                product.getId(),
                1,
                BigDecimal.valueOf(0.23),
                BigDecimal.valueOf(15.0),
                "LOTE-H",
                "SERIE-H"
        );
        CreateInvoiceRequest reqHigh = new CreateInvoiceRequest(
                client.getId(),
                warehouse.getId(),
                company.getId(),
                List.of(lineHigh)
        );

        InvoiceDTO invHigh = comercialService.createInvoice(reqHigh);
        assertEquals(InvoiceStatus.PENDING_DISCOUNT_APPROVAL, invHigh.status());
    }

    @Test
    public void testCancelInvoiceAndStockReversal() {
        Company company = companyRepository.findAll().get(0);
        Client client = clientRepository.findAll().get(0);
        Product product = productRepository.findAll().get(0);
        Warehouse warehouse = warehouseRepository.findAll().stream()
                .filter(w -> w.getCompany().getId().equals(company.getId()))
                .findFirst()
                .orElseThrow();
        TreasuryAccount account = treasuryAccountRepository.findAll().get(0);

        // Make sure a POS checkout is completed to deduct stock
        String operator = "maria";
        try {
            posService.openSession(operator, new BigDecimal("100.00"), company.getId());
        } catch (Exception ignored) {
            // Might already be open in other tests
        }

        BigDecimal stockBefore = stockRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .map(Stock::getQuantity).orElse(BigDecimal.ZERO);

        POSCheckoutLineRequest line = new POSCheckoutLineRequest(product.getId(), 5, BigDecimal.ZERO, "LOTE-REV", "SERIE-REV");
        POSCheckoutRequest request = new POSCheckoutRequest(
                operator,
                company.getId(),
                client.getId(),
                warehouse.getId(),
                account.getId(),
                List.of(line),
                null
        );

        var invoice = posService.checkout(request);
        BigDecimal stockAfterCheckout = stockRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .map(Stock::getQuantity).orElse(BigDecimal.ZERO);
        assertEquals(stockBefore.subtract(new BigDecimal("5.000")), stockAfterCheckout);

        // Cancel the invoice with motive
        assertDoesNotThrow(() -> {
            comercialService.cancelInvoice(invoice.getId(), "Cliente desistiu");
        });

        BigDecimal stockAfterCancel = stockRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .map(Stock::getQuantity).orElse(BigDecimal.ZERO);
        assertEquals(stockBefore, stockAfterCancel); // Stock should be fully restored!
    }

    @Test
    public void testOrderAndBillingFlow() {
        Company company = companyRepository.findAll().get(0);
        Client client = clientRepository.findAll().get(0);
        Product product = productRepository.findAll().get(0);
        Warehouse warehouse = warehouseRepository.findAll().stream()
                .filter(w -> w.getCompany().getId().equals(company.getId()))
                .findFirst()
                .orElseThrow();

        // 1. Check stock before order creation
        BigDecimal stockBefore = stockRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .map(Stock::getQuantity).orElse(BigDecimal.ZERO);

        // 2. Create customer order (EC)
        CreateInvoiceLineRequest lineRequest = new CreateInvoiceLineRequest(
                product.getId(),
                3,
                BigDecimal.valueOf(0.23),
                BigDecimal.ZERO,
                "LOTE-EC",
                "SERIE-EC"
        );
        CreateInvoiceRequest request = new CreateInvoiceRequest(
                client.getId(),
                company.getId(),
                warehouse.getId(),
                List.of(lineRequest)
        );

        var order = comercialService.createOrder(request);
        assertNotNull(order);
        assertEquals("PENDING", order.status());
        assertTrue(order.orderNumber().startsWith("EC-2026/"));

        // 3. Stock should NOT be deducted upon order creation
        BigDecimal stockAfterOrder = stockRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .map(Stock::getQuantity).orElse(BigDecimal.ZERO);
        assertEquals(stockBefore, stockAfterOrder);

        // 4. Bill the order (FT)
        var invoice = comercialService.billOrder(order.id());
        assertNotNull(invoice);
        assertEquals(InvoiceStatus.APPROVED, invoice.status());
        
        // 5. Shared sequence validation (EC-2026/X should match FT-2026/X)
        String orderSeq = order.orderNumber().substring(order.orderNumber().lastIndexOf('/') + 1);
        String invoiceSeq = invoice.invoiceNumber().substring(invoice.invoiceNumber().lastIndexOf('/') + 1);
        assertEquals(orderSeq, invoiceSeq);
        assertTrue(invoice.invoiceNumber().startsWith("FT-2026/"));

        // 6. Stock should now be deducted by the ordered quantity (3)
        BigDecimal stockAfterBilling = stockRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .map(Stock::getQuantity).orElse(BigDecimal.ZERO);
        assertEquals(stockBefore.subtract(new BigDecimal("3.000")), stockAfterBilling);
    }

    @Test
    public void testProductRegistration() {
        String sku = "TEST-PROD-" + System.currentTimeMillis();
        BigDecimal salesPrice = new BigDecimal("120.00");
        BigDecimal purchasePrice = new BigDecimal("45.00");
        BigDecimal minStock = new BigDecimal("15.00");

        var created = comercialService.createProduct(sku, "Test Product Registration", salesPrice, purchasePrice, minStock, "Description test");
        assertNotNull(created);
        assertEquals(sku, created.sku());
        assertEquals(salesPrice, created.unitPrice());
        assertEquals(purchasePrice, created.purchasePrice());
        assertEquals(minStock, created.minStock());
    }

    @Test
    public void testBackupService() {
        assertDoesNotThrow(() -> {
            String path = backupService.executeBackup();
            assertNotNull(path);
            File f = new File(path);
            assertTrue(f.exists());
            assertTrue(f.length() > 0);
        });
    }
}

