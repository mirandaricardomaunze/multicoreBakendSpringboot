package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.purchases.dto.ReorderSuggestionDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes da reposição automática: sugere só produtos abaixo do mínimo, arredonda a caixas
 * inteiras e ignora produtos sem controlo de stock ou sem mínimo definido.
 */
class ReorderServiceTest {

    private static final Long COMPANY_ID = 1L;

    private StockRepository stockRepository;
    private ProductRepository productRepository;
    private ReorderService service;

    @BeforeEach
    void setUp() {
        stockRepository = mock(StockRepository.class);
        productRepository = mock(ProductRepository.class);
        service = new ReorderService(stockRepository, productRepository);
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void suggests_onlyBelowMinimum_roundedUpToWholeBoxes() {
        // Açúcar: 24 und/caixa, mínimo 100, stock 30 → falta 70 → 3 caixas (72 unidades).
        Product acucar = product(2L, "MER-ACUCAR", "Açúcar", new BigDecimal("100"), 24, true);
        // Arroz: mínimo 50, stock 60 → suficiente, não sugere.
        Product arroz = product(1L, "MER-ARROZ", "Arroz", new BigDecimal("50"), 1, true);
        when(productRepository.findDistinctByCompaniesIdOrderByName(COMPANY_ID))
                .thenReturn(List.of(arroz, acucar));
        when(stockRepository.findByWarehouseCompanyId(COMPANY_ID))
                .thenReturn(List.of(stock(acucar, new BigDecimal("30")), stock(arroz, new BigDecimal("60"))));

        List<ReorderSuggestionDTO> out = service.suggestions(COMPANY_ID);

        assertEquals(1, out.size());
        ReorderSuggestionDTO s = out.get(0);
        assertEquals("Açúcar", s.name());
        assertEquals(new BigDecimal("3"), s.suggestedBoxes());
        assertEquals(new BigDecimal("72"), s.suggestedUnits());
    }

    @Test
    void productWithNoStockRow_countsAsZero_andIsSuggested() {
        Product feijao = product(5L, "MER-FEIJAO", "Feijão", new BigDecimal("40"), 10, true);
        when(productRepository.findDistinctByCompaniesIdOrderByName(COMPANY_ID)).thenReturn(List.of(feijao));
        when(stockRepository.findByWarehouseCompanyId(COMPANY_ID)).thenReturn(List.of()); // sem stock

        List<ReorderSuggestionDTO> out = service.suggestions(COMPANY_ID);

        assertEquals(1, out.size());
        assertEquals(new BigDecimal("4"), out.get(0).suggestedBoxes()); // 40/10
        assertEquals(new BigDecimal("40"), out.get(0).suggestedUnits());
        assertEquals(BigDecimal.ZERO, out.get(0).currentStock());
    }

    @Test
    void ignoresProductsWithoutMinimumOrNotStockTracked() {
        Product semMin = product(6L, "SVC", "Serviço", BigDecimal.ZERO, 1, true);        // min 0
        Product servico = product(7L, "SVC2", "Consultoria", new BigDecimal("10"), 1, false); // não stockável
        when(productRepository.findDistinctByCompaniesIdOrderByName(COMPANY_ID))
                .thenReturn(List.of(semMin, servico));
        when(stockRepository.findByWarehouseCompanyId(COMPANY_ID)).thenReturn(List.of());

        assertTrue(service.suggestions(COMPANY_ID).isEmpty());
    }

    private static Product product(long id, String sku, String name, BigDecimal min, int upb, boolean tracked) {
        Product p = new Product();
        p.setId(id);
        p.setSku(sku);
        p.setName(name);
        p.setUnitPrice(BigDecimal.TEN);
        p.setMinStock(min);
        p.setUnitsPerBox(upb);
        p.setStockTracked(tracked);
        return p;
    }

    private static Stock stock(Product p, BigDecimal qty) {
        Stock s = new Stock();
        s.setProduct(p);
        s.setQuantity(qty);
        return s;
    }
}
