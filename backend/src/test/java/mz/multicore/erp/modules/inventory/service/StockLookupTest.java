package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * MD-I02 — porta de leitura de stock para os outros módulos e escrita alheia só pelo serviço de movimentos
 * (docs/MODULE_DECOUPLING_SPEC.md §2).
 */
class StockLookupTest {

    private static final Pattern STOCK_REPO_FIELD =
            Pattern.compile("StockRepository\\s+(\\w+)\\s*[;,)=]");

    private final StockRepository stocks = mock(StockRepository.class);
    private final StockLookup lookup = new StockLookup(stocks);

    private static Stock stock(String quantity) {
        Stock s = new Stock();
        s.setQuantity(new BigDecimal(quantity));
        return s;
    }

    @Test
    void find_porProdutoEArmazem_delega() {
        Stock s = stock("12.000");
        when(stocks.findByProductIdAndWarehouseId(3L, 4L)).thenReturn(Optional.of(s));

        assertThat(lookup.find(3L, 4L)).containsSame(s);
    }

    @Test
    void find_idsNulos_vazioSemTocarNoRepository() {
        assertThat(lookup.find(null, 4L)).isEmpty();
        assertThat(lookup.find(3L, null)).isEmpty();
        assertThat(lookup.quantityOf(null, null)).isEqualByComparingTo(BigDecimal.ZERO);
        verifyNoInteractions(stocks);
    }

    @Test
    void quantityOf_semRegisto_ouQuantidadeNula_eZero() {
        when(stocks.findByProductIdAndWarehouseId(1L, 2L)).thenReturn(Optional.of(stock("7.500")));
        when(stocks.findByProductIdAndWarehouseId(1L, 3L)).thenReturn(Optional.empty());
        Stock semQuantidade = new Stock();
        semQuantidade.setQuantity(null);
        when(stocks.findByProductIdAndWarehouseId(1L, 4L)).thenReturn(Optional.of(semQuantidade));

        assertThat(lookup.quantityOf(1L, 2L)).isEqualByComparingTo("7.5");
        assertThat(lookup.quantityOf(1L, 3L)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(lookup.quantityOf(1L, 4L)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void listagens_delegam() {
        List<Stock> daEmpresa = List.of(stock("1"), stock("2"));
        List<Stock> doArmazem = List.of(stock("3"));
        List<Stock> doProduto = List.of(stock("4"));
        when(stocks.findByWarehouseCompanyId(9L)).thenReturn(daEmpresa);
        when(stocks.findByWarehouseId(8L)).thenReturn(doArmazem);
        when(stocks.findByProductId(7L)).thenReturn(doProduto);

        assertThat(lookup.listForCompany(9L)).isEqualTo(daEmpresa);
        assertThat(lookup.listForWarehouse(8L)).isEqualTo(doArmazem);
        assertThat(lookup.listForProduct(7L)).isEqualTo(doProduto);
    }

    @Test
    void contrato_serviceSoDeLeitura_semMetodosDeEscrita() {
        assertThat(StockLookup.class).hasAnnotation(Service.class);
        Transactional tx = StockLookup.class.getAnnotation(Transactional.class);
        assertThat(tx).isNotNull();
        assertThat(tx.readOnly()).isTrue();

        List<String> publicMethods = Arrays.stream(StockLookup.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .map(Method::getName)
                .toList();
        assertThat(publicMethods).isNotEmpty().allMatch(name ->
                name.startsWith("find") || name.startsWith("list") || name.startsWith("quantity"));
        assertThat(Arrays.stream(StockLookup.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers())))
                .noneMatch(m -> m.getReturnType() == void.class);
    }

    @Test
    void escritaDeStockForaDoInventario_soPeloServicoDeMovimentos() throws IOException {
        Path main = moduleRoot().resolve("src/main/java/mz/multicore/erp");
        Path inventory = main.resolve("modules/inventory");
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(main)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.startsWith(inventory)).toList()) {
                String src = Files.readString(file);
                if (!src.contains("inventory.repository.StockRepository")) continue;
                Matcher field = STOCK_REPO_FIELD.matcher(src);
                while (field.find()) {
                    String name = field.group(1);
                    if (Pattern.compile("\\b" + name + "\\.(save|saveAll|saveAndFlush|delete|deleteAll|deleteById)\\b")
                            .matcher(src).find()) {
                        violations.add(main.relativize(file) + " escreve stock por " + name);
                    }
                }
            }
        }
        assertThat(violations)
                .as("Escrita de stock alheia tem de passar pelo StockMovementService (ledger)")
                .isEmpty();
    }

    @Test
    void seederDemo_gravaStockInicialPeloLedger() throws IOException {
        String seeder = Files.readString(moduleRoot()
                .resolve("src/main/java/mz/multicore/erp/architecture/DemoEnterpriseDataSeeder.java"));
        assertThat(seeder).doesNotContain("inventory.repository.StockRepository")
                .contains("StockMovementService");
    }

    private static Path moduleRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return Files.isDirectory(cwd.resolve("src/main/java")) ? cwd : cwd.resolve("backend");
    }
}
