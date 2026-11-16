package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.model.ProductBatch;
import mz.multicore.erp.modules.inventory.model.StockMovement;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.model.StockWaste;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.model.WasteStatus;
import mz.multicore.erp.modules.inventory.repository.ProductBatchRepository;
import mz.multicore.erp.modules.inventory.repository.StockMovementRepository;
import mz.multicore.erp.modules.inventory.repository.StockTransferRepository;
import mz.multicore.erp.modules.inventory.repository.StockWasteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * MD-I04 — quebras, transferências, lotes e movimentos lidos fora do inventário só pelos Lookups do
 * inventário (docs/MODULE_DECOUPLING_SPEC.md §2).
 */
class InventoryDocumentLookupsTest {

    private final StockWasteRepository wastes = mock(StockWasteRepository.class);
    private final StockTransferRepository transfers = mock(StockTransferRepository.class);
    private final ProductBatchRepository batches = mock(ProductBatchRepository.class);
    private final StockMovementRepository movements = mock(StockMovementRepository.class);

    private final StockWasteLookup wasteLookup = new StockWasteLookup(wastes);
    private final StockTransferLookup transferLookup = new StockTransferLookup(transfers);
    private final ProductBatchLookup batchLookup = new ProductBatchLookup(batches);
    private final StockMovementLookup movementLookup = new StockMovementLookup(movements);

    @Test
    void quebras_listagensDelegam() {
        List<StockWaste> daEmpresa = List.of(new StockWaste());
        List<StockWaste> pendentes = List.of(new StockWaste());
        List<StockWaste> doPeriodo = List.of(new StockWaste());
        List<StockWaste> todas = List.of(new StockWaste(), new StockWaste());
        LocalDateTime from = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 11, 30, 23, 59);
        when(wastes.findByCompanyIdOrderByCreatedAtDesc(1L)).thenReturn(daEmpresa);
        when(wastes.findByCompanyIdAndStatusOrderByCreatedAtDesc(1L, WasteStatus.PENDING_APPROVAL)).thenReturn(pendentes);
        when(wastes.findByCompanyIdAndPeriod(1L, from, to)).thenReturn(doPeriodo);
        when(wastes.findAll()).thenReturn(todas);

        assertThat(wasteLookup.listForCompany(1L)).isEqualTo(daEmpresa);
        assertThat(wasteLookup.listForCompanyWithStatus(1L, WasteStatus.PENDING_APPROVAL)).isEqualTo(pendentes);
        assertThat(wasteLookup.listForCompanyInPeriod(1L, from, to)).isEqualTo(doPeriodo);
        assertThat(wasteLookup.listAll()).isEqualTo(todas);
    }

    @Test
    void transferencias_eMovimentos_listagensDelegam() {
        List<StockTransfer> t = List.of(new StockTransfer());
        List<StockMovement> m = List.of(new StockMovement());
        when(transfers.findByCompanyId(2L)).thenReturn(t);
        when(movements.findByCompanyId(2L)).thenReturn(m);

        assertThat(transferLookup.listForCompany(2L)).isEqualTo(t);
        assertThat(movementLookup.listForCompany(2L)).isEqualTo(m);
    }

    @Test
    void lotes_consultasDelegam_eIdsNulosNaoTocamNoRepository() {
        ProductBatch b = batch(10L, 1L);
        when(batches.findFirstByProductIdOrderByExpirationDateAsc(5L)).thenReturn(Optional.of(b));
        when(batches.findFirstByProductIdAndBatchNumberOrderByExpirationDateAsc(5L, "L-01")).thenReturn(Optional.of(b));
        when(batches.findAvailableFEFO(5L, 6L)).thenReturn(List.of(b));

        assertThat(batchLookup.findEarliestExpiring(5L)).containsSame(b);
        assertThat(batchLookup.findByBatchNumber(5L, "L-01")).containsSame(b);
        assertThat(batchLookup.listAvailableFefo(5L, 6L)).containsExactly(b);

        ProductBatchRepository untouched = mock(ProductBatchRepository.class);
        ProductBatchLookup lookup = new ProductBatchLookup(untouched);
        assertThat(lookup.findEarliestExpiring(null)).isEmpty();
        assertThat(lookup.findByBatchNumber(null, "L-01")).isEmpty();
        assertThat(lookup.findByBatchNumber(5L, null)).isEmpty();
        assertThat(lookup.findForCompany(null, 1L)).isEmpty();
        verifyNoInteractions(untouched);
    }

    @Test
    void lotes_requireForCompany_mensagensNormalizadas() {
        when(batches.findById(10L)).thenReturn(Optional.of(batch(10L, 1L)));
        when(batches.findById(11L)).thenReturn(Optional.empty());
        ProductBatch semArmazem = new ProductBatch();
        semArmazem.setId(12L);
        when(batches.findById(12L)).thenReturn(Optional.of(semArmazem));

        assertThat(batchLookup.requireForCompany(10L, 1L).getId()).isEqualTo(10L);
        assertThatThrownBy(() -> batchLookup.requireForCompany(11L, 1L))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Lote não encontrado.");
        assertThatThrownBy(() -> batchLookup.requireForCompany(10L, 2L))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Lote não encontrado nesta empresa.");
        assertThatThrownBy(() -> batchLookup.requireForCompany(12L, 1L))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Lote não encontrado nesta empresa.");
    }

    @Test
    void contrato_lookupsSoDeLeitura() {
        for (Class<?> lookup : List.of(StockWasteLookup.class, StockTransferLookup.class,
                ProductBatchLookup.class, StockMovementLookup.class)) {
            assertThat(lookup).hasAnnotation(Service.class);
            Transactional tx = lookup.getAnnotation(Transactional.class);
            assertThat(tx).as(lookup.getSimpleName()).isNotNull();
            assertThat(tx.readOnly()).as(lookup.getSimpleName()).isTrue();
            List<Method> publicMethods = Arrays.stream(lookup.getDeclaredMethods())
                    .filter(m -> Modifier.isPublic(m.getModifiers())).toList();
            assertThat(publicMethods).as(lookup.getSimpleName()).isNotEmpty()
                    .allMatch(m -> m.getName().startsWith("find") || m.getName().startsWith("list")
                            || m.getName().startsWith("require"))
                    .noneMatch(m -> m.getReturnType() == void.class);
        }
    }

    /** Nenhum módulo fora do inventário importa os repositórios de quebras, transferências, lotes ou movimentos. */
    @Test
    void foraDoInventario_naoHaRepositoriosDeQuebrasTransferenciasLotesMovimentos() throws IOException {
        List<String> repos = List.of("inventory.repository.StockWasteRepository",
                "inventory.repository.StockTransferRepository", "inventory.repository.ProductBatchRepository",
                "inventory.repository.StockMovementRepository");
        Path main = moduleRoot().resolve("src/main/java/mz/multicore/erp");
        Path inventory = main.resolve("modules/inventory");
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(main)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.startsWith(inventory)).toList()) {
                String src = Files.readString(file);
                repos.stream().filter(src::contains)
                        .forEach(r -> offenders.add(main.relativize(file) + " -> " + r));
            }
        }
        assertThat(offenders)
                .as("Quebras, transferências, lotes e movimentos lidos fora do inventário passam pelos Lookups")
                .isEmpty();
    }

    private static ProductBatch batch(Long id, Long companyId) {
        Company company = new Company();
        company.setId(companyId);
        Warehouse warehouse = new Warehouse();
        warehouse.setCompany(company);
        ProductBatch b = new ProductBatch();
        b.setId(id);
        b.setWarehouse(warehouse);
        return b;
    }

    private static Path moduleRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return Files.isDirectory(cwd.resolve("src/main/java")) ? cwd : cwd.resolve("backend");
    }
}
