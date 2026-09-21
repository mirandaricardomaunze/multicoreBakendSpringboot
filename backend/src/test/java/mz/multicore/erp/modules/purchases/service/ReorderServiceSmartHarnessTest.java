package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.purchases.dto.ReorderSuggestionDTO;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.PurchaseLine;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.PurchaseLineRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes analíticos do motor inteligente de reposição automática:
 * Valida velocidade de vendas, dias restantes, status de urgência (ESGOTADO, CRÍTICO, BAIXO),
 * fornecedor habitual e custos estimados.
 */
class ReorderServiceSmartHarnessTest {

    private static final Long COMPANY_ID = 1L;

    private StockRepository stockRepository;
    private ProductRepository productRepository;
    private InvoiceRepository invoiceRepository;
    private PurchaseLineRepository purchaseLineRepository;
    private ReorderService service;

    @BeforeEach
    void setUp() {
        stockRepository = mock(StockRepository.class);
        productRepository = mock(ProductRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        purchaseLineRepository = mock(PurchaseLineRepository.class);
        service = new ReorderService(stockRepository, productRepository, invoiceRepository, purchaseLineRepository);
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void calculates_smartMetrics_urgencyLevels_and_supplierAttribution() {
        // P1: Esgotado (stock 0, min 50, 10 und/cx, vendas 30/mês = 1.00/dia)
        Product p1 = product(101L, "SKU-ESGOTADO", "Óleo 1L", new BigDecimal("50"), 10, new BigDecimal("100.00"));
        // P2: Crítico (stock 10, min 60, 12 und/cx, vendas 60/mês = 2.00/dia -> 5 dias restantes <= 7)
        Product p2 = product(102L, "SKU-CRITICO", "Farinha 1kg", new BigDecimal("60"), 12, new BigDecimal("80.00"));
        // P3: Baixo (stock 30, min 50, 1 und/cx, vendas 30/mês = 1.00/dia -> 30 dias restantes > 7)
        Product p3 = product(103L, "SKU-BAIXO", "Arroz 5kg", new BigDecimal("50"), 1, new BigDecimal("250.00"));

        when(productRepository.findDistinctByCompaniesIdOrderByName(COMPANY_ID))
                .thenReturn(List.of(p1, p2, p3));

        when(stockRepository.findByWarehouseCompanyId(COMPANY_ID))
                .thenReturn(List.of(
                        stock(p2, new BigDecimal("10")),
                        stock(p3, new BigDecimal("30"))
                        // p1 sem stock row -> 0
                ));

        // Vendas faturadas nos últimos 30 dias
        when(invoiceRepository.sumQuantitySoldByProductSince(eq(COMPANY_ID), anyCollection(), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        new Object[]{101L, new BigDecimal("30")},
                        new Object[]{102L, new BigDecimal("60")},
                        new Object[]{103L, new BigDecimal("30")}
                ));

        // Histórico de compras para Fornecedor Habitual
        Supplier supA = supplier(1L, "Distribuidora Central");
        PurchaseLine plP1 = purchaseLine(p1, supA, new BigDecimal("95.00")); // preço na compra foi 95.00
        when(purchaseLineRepository.findRecentPurchasesByCompany(COMPANY_ID))
                .thenReturn(List.of(plP1));

        List<ReorderSuggestionDTO> suggestions = service.suggestions(COMPANY_ID);

        assertEquals(3, suggestions.size());

        // 1º colocado: ESGOTADO (P1)
        ReorderSuggestionDTO s1 = suggestions.get(0);
        assertEquals("SKU-ESGOTADO", s1.sku());
        assertEquals("ESGOTADO", s1.urgencyStatus());
        assertEquals(0, s1.daysRemaining());
        assertEquals(new BigDecimal("1.00"), s1.dailySalesRate());
        assertEquals(new BigDecimal("5"), s1.suggestedBoxes()); // 50 / 10 = 5 caixas
        assertEquals(new BigDecimal("50"), s1.suggestedUnits());
        assertEquals("Distribuidora Central", s1.supplierName());
        assertEquals(new BigDecimal("95.00"), s1.estimatedUnitPrice());
        assertEquals(new BigDecimal("4750.00"), s1.estimatedTotalCost()); // 50 * 95.00

        // 2º colocado: CRÍTICO (P2)
        ReorderSuggestionDTO s2 = suggestions.get(1);
        assertEquals("SKU-CRITICO", s2.sku());
        assertEquals("CRÍTICO", s2.urgencyStatus());
        assertEquals(5, s2.daysRemaining()); // 10 stock / 2.00 diário = 5 dias
        assertEquals(new BigDecimal("2.00"), s2.dailySalesRate());
        // Défice = 60 - 10 = 50 -> 50/12 = 4.16 -> 5 caixas (60 unidades)
        assertEquals(new BigDecimal("5"), s2.suggestedBoxes());
        assertEquals(new BigDecimal("60"), s2.suggestedUnits());
        assertNull(s2.supplierName()); // sem compra prévia -> fallback para preço do produto (80.00)
        assertEquals(new BigDecimal("80.00"), s2.estimatedUnitPrice());
        assertEquals(new BigDecimal("4800.00"), s2.estimatedTotalCost()); // 60 * 80.00

        // 3º colocado: BAIXO (P3)
        ReorderSuggestionDTO s3 = suggestions.get(2);
        assertEquals("SKU-BAIXO", s3.sku());
        assertEquals("BAIXO", s3.urgencyStatus());
        assertEquals(30, s3.daysRemaining()); // 30 stock / 1.00 diário = 30 dias
        assertEquals(new BigDecimal("20"), s3.suggestedUnits()); // 50 - 30 = 20
        assertEquals(new BigDecimal("5000.00"), s3.estimatedTotalCost()); // 20 * 250.00
    }

    private static Product product(long id, String sku, String name, BigDecimal min, int upb, BigDecimal purchasePrice) {
        Product p = new Product();
        p.setId(id);
        p.setSku(sku);
        p.setName(name);
        p.setUnitPrice(BigDecimal.TEN);
        p.setMinStock(min);
        p.setUnitsPerBox(upb);
        p.setStockTracked(true);
        p.setPurchasePrice(purchasePrice);
        return p;
    }

    private static Stock stock(Product p, BigDecimal qty) {
        Stock s = new Stock();
        s.setProduct(p);
        s.setQuantity(qty);
        return s;
    }

    private static Supplier supplier(Long id, String name) {
        Supplier s = new Supplier();
        s.setId(id);
        s.setName(name);
        return s;
    }

    private static PurchaseLine purchaseLine(Product p, Supplier s, BigDecimal unitPrice) {
        Purchase purchase = new Purchase();
        purchase.setSupplier(s);
        purchase.setStatus("COMPLETED");
        purchase.setPurchaseDate(LocalDateTime.now().minusDays(2));

        PurchaseLine line = new PurchaseLine();
        line.setProduct(p);
        line.setPurchase(purchase);
        line.setUnitPrice(unitPrice);
        return line;
    }
}
