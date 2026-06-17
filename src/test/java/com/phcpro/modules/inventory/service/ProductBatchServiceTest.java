package com.phcpro.modules.inventory.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.modules.comercial.model.Product;
import com.phcpro.modules.inventory.model.ProductBatch;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.repository.ProductBatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Testes unitários focados na lógica FEFO. Não levantam o Spring — repositório é mockado.
 */
class ProductBatchServiceTest {

    private ProductBatchRepository repo;
    private ProductBatchService service;
    private Product product;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        repo = mock(ProductBatchRepository.class);
        service = new ProductBatchService(repo);
        product = product(1L, "SKU-1", "Arroz Tio João");
        warehouse = warehouse(10L, "Loja Maputo");
    }

    // ────────────────────────── findNextFEFO ──────────────────────────

    @Test
    void findNextFEFO_loteDisponivel_devolveOMaisProximoAVencer() {
        ProductBatch antigo = batch(100L, "LOTE-A", LocalDate.now().plusDays(10), new BigDecimal("5"));
        ProductBatch novo   = batch(101L, "LOTE-B", LocalDate.now().plusDays(90), new BigDecimal("20"));
        when(repo.findAvailableFEFO(1L, 10L)).thenReturn(List.of(antigo, novo));

        var result = service.findNextFEFO(1L, 10L);

        assertTrue(result.isPresent());
        assertEquals("LOTE-A", result.get().batchNumber());
    }

    @Test
    void findNextFEFO_semStock_devolveEmpty() {
        when(repo.findAvailableFEFO(1L, 10L)).thenReturn(List.of());

        assertEquals(Optional.empty(), service.findNextFEFO(1L, 10L));
    }

    // ────────────────────────── consumeFEFO ───────────────────────────

    @Test
    void consumeFEFO_quantidadeMenorQueOLote_consomeApenasUmLote() {
        ProductBatch b = batch(100L, "LOTE-A", LocalDate.now().plusDays(10), new BigDecimal("20"));
        when(repo.findAvailableFEFO(1L, 10L)).thenReturn(List.of(b));
        when(repo.save(any(ProductBatch.class))).thenAnswer(inv -> inv.getArgument(0));

        var debits = service.consumeFEFO(product, warehouse, new BigDecimal("5"));

        assertEquals(1, debits.size());
        assertEquals(new BigDecimal("5"), debits.get(0).quantity());
        assertEquals("LOTE-A", debits.get(0).batch().getBatchNumber());
        assertEquals(new BigDecimal("15"), b.getQuantity(), "Quantidade restante no lote");
    }

    @Test
    void consumeFEFO_atravessaVariosLotes_emOrdemDeValidade() {
        ProductBatch a = batch(100L, "LOTE-A", LocalDate.now().plusDays(10), new BigDecimal("3"));
        ProductBatch b = batch(101L, "LOTE-B", LocalDate.now().plusDays(30), new BigDecimal("4"));
        ProductBatch c = batch(102L, "LOTE-C", LocalDate.now().plusDays(60), new BigDecimal("10"));
        when(repo.findAvailableFEFO(1L, 10L)).thenReturn(List.of(a, b, c));
        when(repo.save(any(ProductBatch.class))).thenAnswer(inv -> inv.getArgument(0));

        // Precisa 8 unidades → 3 de A + 4 de B + 1 de C
        var debits = service.consumeFEFO(product, warehouse, new BigDecimal("8"));

        assertEquals(3, debits.size());
        assertEquals("LOTE-A", debits.get(0).batch().getBatchNumber());
        assertEquals(new BigDecimal("3"), debits.get(0).quantity());
        assertEquals("LOTE-B", debits.get(1).batch().getBatchNumber());
        assertEquals(new BigDecimal("4"), debits.get(1).quantity());
        assertEquals("LOTE-C", debits.get(2).batch().getBatchNumber());
        assertEquals(new BigDecimal("1"), debits.get(2).quantity());

        assertEquals(BigDecimal.ZERO, a.getQuantity());
        assertEquals(BigDecimal.ZERO, b.getQuantity());
        assertEquals(new BigDecimal("9"), c.getQuantity());
    }

    @Test
    void consumeFEFO_stockInsuficiente_lancaBusinessRuleExceptionSemMexerEmLotes() {
        ProductBatch a = batch(100L, "LOTE-A", LocalDate.now().plusDays(10), new BigDecimal("2"));
        when(repo.findAvailableFEFO(1L, 10L)).thenReturn(List.of(a));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.consumeFEFO(product, warehouse, new BigDecimal("5")));
        assertTrue(ex.getMessage().contains("Stock insuficiente"));
        verify(repo, never()).save(any());
        assertEquals(new BigDecimal("2"), a.getQuantity(), "Lote intacto após falha");
    }

    @Test
    void consumeFEFO_quantidadeZeroOuNegativa_lancaBusinessRuleException() {
        assertThrows(BusinessRuleException.class,
                () -> service.consumeFEFO(product, warehouse, BigDecimal.ZERO));
        assertThrows(BusinessRuleException.class,
                () -> service.consumeFEFO(product, warehouse, new BigDecimal("-1")));
        verifyNoInteractions(repo);
    }

    // ────────────────────────── addToBatch ────────────────────────────

    @Test
    void addToBatch_loteNovo_criaERegistaQuantidade() {
        when(repo.findByProductIdAndWarehouseIdAndBatchNumber(1L, 10L, "LOTE-X"))
                .thenReturn(Optional.empty());
        when(repo.save(any(ProductBatch.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductBatch saved = service.addToBatch(product, warehouse, "LOTE-X",
                LocalDate.now().plusDays(180), new BigDecimal("12"));

        ArgumentCaptor<ProductBatch> captor = ArgumentCaptor.forClass(ProductBatch.class);
        verify(repo).save(captor.capture());
        ProductBatch persisted = captor.getValue();
        assertEquals("LOTE-X", persisted.getBatchNumber());
        assertEquals(new BigDecimal("12"), persisted.getQuantity());
        assertSame(persisted, saved);
    }

    @Test
    void addToBatch_loteExistente_acumulaQuantidade() {
        ProductBatch existing = batch(50L, "LOTE-Y", LocalDate.now().plusDays(180), new BigDecimal("5"));
        when(repo.findByProductIdAndWarehouseIdAndBatchNumber(1L, 10L, "LOTE-Y"))
                .thenReturn(Optional.of(existing));
        when(repo.save(any(ProductBatch.class))).thenAnswer(inv -> inv.getArgument(0));

        service.addToBatch(product, warehouse, "LOTE-Y",
                LocalDate.now().plusDays(180), new BigDecimal("3"));

        assertEquals(new BigDecimal("8"), existing.getQuantity());
    }

    @Test
    void addToBatch_quantidadeNaoPositiva_lancaBusinessRuleException() {
        assertThrows(BusinessRuleException.class, () -> service.addToBatch(
                product, warehouse, "LOTE-X", LocalDate.now().plusDays(10), BigDecimal.ZERO));
        verify(repo, never()).save(any());
    }

    // ────────────────────────── helpers ────────────────────────────────

    private static Product product(long id, String sku, String name) {
        Product p = new Product();
        p.setId(id);
        p.setSku(sku);
        p.setName(name);
        return p;
    }

    private static Warehouse warehouse(long id, String name) {
        Warehouse w = new Warehouse();
        w.setId(id);
        w.setName(name);
        return w;
    }

    private ProductBatch batch(long id, String number, LocalDate exp, BigDecimal qty) {
        ProductBatch b = new ProductBatch();
        b.setId(id);
        b.setProduct(product);
        b.setWarehouse(warehouse);
        b.setBatchNumber(number);
        b.setExpirationDate(exp);
        b.setEntryDate(LocalDate.now());
        b.setQuantity(qty);
        return b;
    }
}
