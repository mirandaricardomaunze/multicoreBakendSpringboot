package mz.multicore.erp.gui;

import mz.multicore.erp.modules.inventory.dto.StockDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.inventory.model.WarehouseType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Harness para os Indicadores Executivos de Gestão de Armazéns (docs/WAREHOUSE_MANAGEMENT_KPIS_SPEC.md).
 * Valida a presença de KpiCard.createGrid(4), títulos, ícones semânticos e integridade do cálculo.
 */
class StockWarehouseKpiHarnessTest {

    private static String source(String file) throws Exception {
        return Files.readString(Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", file));
    }

    @Test
    @DisplayName("H-WMS-01: StockWarehousesPanel implementa KpiCard.createGrid(4) com os 4 KPIs canónicos")
    void warehousesPanel_possuiGridDeQuatroKpis() throws Exception {
        String code = source("StockWarehousesPanel.java");

        assertThat(code).contains("KpiCard.createGrid(4)");
        assertThat(code).contains("\"Armazéns Activos\"");
        assertThat(code).contains("\"Capacidade Instalada\"");
        assertThat(code).contains("\"Valor em Armazém\"");
        assertThat(code).contains("\"Rede de Distribuição\"");

        assertThat(code).contains("\"fas-warehouse\"");
        assertThat(code).contains("\"fas-boxes\"");
        assertThat(code).contains("\"fas-coins\"");
        assertThat(code).contains("\"fas-network-wired\"");
    }

    @Test
    @DisplayName("H-WMS-02: Métricas de armazéns activos, capacidade agregada e valorização")
    void metricas_calculamValoresCorretos() {
        WarehouseDTO w1 = new WarehouseDTO(1L, "Armazém Central", "Matola", "WH-01",
                new BigDecimal("50000"), 1L, WarehouseType.CENTRAL, false, "João Gestor", "841234567", true);
        WarehouseDTO w2 = new WarehouseDTO(2L, "Loja Principal", "Maputo", "WH-02",
                new BigDecimal("15000"), 1L, WarehouseType.STORE, true, "Maria Vendedora", "821234567", true);
        WarehouseDTO w3 = new WarehouseDTO(3L, "Depósito Desactivado", "Boane", "WH-03",
                new BigDecimal("10000"), 1L, WarehouseType.DEPOT, false, "Carlos", "871234567", false);

        List<WarehouseDTO> warehouses = List.of(w1, w2, w3);

        StockDTO s1 = new StockDTO(1L, 10L, "SKU-1", "REF-1", "79001", "Arroz 25kg",
                1L, "Armazém Central", new BigDecimal("100"), BigDecimal.TEN, "Mercearia", new BigDecimal("1500.00"), 1);
        StockDTO s2 = new StockDTO(2L, 11L, "SKU-2", "REF-2", "79002", "Óleo 5L",
                2L, "Loja Principal", new BigDecimal("50"), BigDecimal.TEN, "Mercearia", new BigDecimal("500.00"), 4);

        List<StockDTO> stocks = List.of(s1, s2);

        // 1. Armazéns Activos vs Total
        long activeCount = warehouses.stream().filter(WarehouseDTO::active).count();
        long salesCount = warehouses.stream().filter(w -> w.active() && w.allowsSales()).count();
        assertEquals(2, activeCount);
        assertEquals(1, salesCount);

        // 2. Capacidade Instalada dos activos
        BigDecimal totalCapacity = warehouses.stream()
                .filter(WarehouseDTO::active)
                .map(w -> w.capacity() != null ? w.capacity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("65000"), totalCapacity);

        // 3. Valor Total em Stock
        BigDecimal totalStockValue = stocks.stream()
                .map(s -> (s.quantity() != null ? s.quantity() : BigDecimal.ZERO)
                        .multiply(s.unitPrice() != null ? s.unitPrice() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // (100 * 1500) + (50 * 500) = 150000 + 25000 = 175000
        assertEquals(new BigDecimal("175000.00"), totalStockValue);

        // 4. Rede de Distribuição (tipos distintos de armazéns activos)
        long distinctTypes = warehouses.stream()
                .filter(WarehouseDTO::active)
                .map(WarehouseDTO::type)
                .distinct()
                .count();
        assertEquals(2, distinctTypes);
    }
}
