package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowStatus;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * IMPORTACAO_DADOS_HARNESS — IMP-ST-01: no stock inicial o SKU, o armazém, a quantidade e o custo unitário são
 * obrigatórios; o artigo e o armazém têm de existir na empresa activa (armazém pelo código ou pelo nome);
 * quantidade &gt; 0 e custo &ge; 0; artigo perecível sem validade, ou com lote já vencido, é {@code ERRO} como na
 * entrada manual; SKU+armazém+lote repetidos no ficheiro são {@code ERRO} em ambas (SPEC §5.1, §3.3).
 */
class StockOpeningImportServiceTest {

    private static final String HEADER = "sku;armazem;quantidade;custo_unitario;lote;validade";
    private static final long COMPANY_ID = 7L;
    private static final String FUTURE = LocalDate.now().plusYears(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

    private final ProductLookup productLookup = mock(ProductLookup.class);
    private final WarehouseLookup warehouseLookup = mock(WarehouseLookup.class);
    private final StockOpeningImportService service = new StockOpeningImportService(productLookup, warehouseLookup);

    private final Product rice = product(11L, "ART-001", "Arroz 5kg", false);
    private final Product milk = product(12L, "LEI-001", "Leite UHT", true);
    private final Warehouse store = warehouse(21L, "A01", "Loja Central");
    private final Warehouse depot = warehouse(22L, "A02", "Depósito Matola");

    @BeforeEach
    void activeCompany() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        when(productLookup.findBySkuForCompany(anyString(), eq(COMPANY_ID))).thenReturn(Optional.empty());
        when(productLookup.findBySkuForCompany(eq("ART-001"), eq(COMPANY_ID))).thenReturn(Optional.of(rice));
        when(productLookup.findBySkuForCompany(eq("LEI-001"), eq(COMPANY_ID))).thenReturn(Optional.of(milk));
        when(warehouseLookup.listForCompany(COMPANY_ID)).thenReturn(List.of(store, depot));
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    private static Product product(Long id, String sku, String name, boolean tracksExpiry) {
        Product product = new Product();
        product.setId(id);
        product.setSku(sku);
        product.setName(name);
        product.setTracksExpiry(tracksExpiry);
        return product;
    }

    private static Warehouse warehouse(Long id, String number, String name) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        warehouse.setWarehouseNumber(number);
        warehouse.setName(name);
        return warehouse;
    }

    private ImportPreviewDTO preview(String... lines) {
        return service.preview(String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
    }

    private ImportRowDTO singleRow(String line) {
        ImportPreviewDTO preview = preview(HEADER, line);
        assertThat(preview.rows()).hasSize(1);
        return preview.rows().get(0);
    }

    private static void assertError(ImportRowDTO row, String column, String messagePart) {
        assertThat(row.status()).isEqualTo(ImportRowStatus.ERRO);
        assertThat(row.column()).isEqualTo(column);
        assertThat(row.message()).contains(messagePart);
    }

    @ParameterizedTest
    @ValueSource(strings = {"A01", "a01", "Loja Central", " loja central ", "Deposito Matola"})
    void warehouseByCodeOrNameIsNew(String warehouse) {
        ImportRowDTO row = singleRow("ART-001;" + warehouse + ";120;85,50;;");

        assertThat(row.status()).isEqualTo(ImportRowStatus.NOVO);
        assertThat(row.column()).isNull();
    }

    @Test
    void validLineCarriesProductWarehouseQuantityCostBatchAndExpiry() {
        ImportPlan<StockOpeningImportService.StockOpeningLine> plan = service.plan(
                String.join("\n", HEADER, "LEI-001;A02;1.200,5;42.75;L-2026-09;" + FUTURE)
                        .getBytes(StandardCharsets.UTF_8));

        StockOpeningImportService.StockOpeningLine line = plan.lines().get(0).item();
        assertThat(line.product()).isSameAs(milk);
        assertThat(line.warehouse()).isSameAs(depot);
        assertThat(line.quantity()).isEqualByComparingTo("1200.5");
        assertThat(line.unitCost()).isEqualByComparingTo("42.75");
        assertThat(line.batchNumber()).isEqualTo("L-2026-09");
        assertThat(line.expirationDate()).isEqualTo(LocalDate.now().plusYears(1));
    }

    @Test
    void zeroCostIsAccepted() {
        assertThat(singleRow("ART-001;A01;10;0;;").status()).isEqualTo(ImportRowStatus.NOVO);
    }

    @Test
    void missingRequiredValuesAreErrorsInTheirColumn() {
        assertError(singleRow(";A01;10;5;;"), "sku", "obrigatório");
        assertError(singleRow("ART-001;;10;5;;"), "armazem", "obrigatório");
        assertError(singleRow("ART-001;A01;;5;;"), "quantidade", "obrigatório");
        assertError(singleRow("ART-001;A01;10;;;"), "custo_unitario", "obrigatório");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-3", "0,00"})
    void quantityMustBePositive(String quantity) {
        assertError(singleRow("ART-001;A01;" + quantity + ";5;;"), "quantidade", "maior que zero");
    }

    @Test
    void negativeCostIsError() {
        assertError(singleRow("ART-001;A01;10;-0,01;;"), "custo_unitario", "negativo");
    }

    @Test
    void nonNumericValuesAreErrors() {
        assertError(singleRow("ART-001;A01;dez;5;;"), "quantidade", "não numérico");
        assertError(singleRow("ART-001;A01;10;abc;;"), "custo_unitario", "não numérico");
        assertError(singleRow("ART-001;A01;10;5;;31/02/2030"), "validade", "data inválida");
    }

    @Test
    void unknownProductAsksToImportProductsFirst() {
        assertError(singleRow("XYZ-999;A01;10;5;;"), "sku", "importe primeiro os artigos");
    }

    @Test
    void unknownWarehouseIsError() {
        assertError(singleRow("ART-001;Armazém Beira;10;5;;"), "armazem", "não existe");
    }

    @Test
    void ambiguousWarehouseNameIsError() {
        Warehouse twin = warehouse(23L, "A03", "Loja Central");
        when(warehouseLookup.listForCompany(COMPANY_ID)).thenReturn(List.of(store, depot, twin));

        assertError(singleRow("ART-001;Loja Central;10;5;;"), "armazem", "ambíguo");
        assertThat(singleRow("ART-001;A03;10;5;;").status()).isEqualTo(ImportRowStatus.NOVO);
    }

    @Test
    void perishableWithoutExpiryIsError() {
        assertError(singleRow("LEI-001;A01;10;5;L1;"), "validade", "perecível");
    }

    @Test
    void perishableWithFutureExpiryIsNew() {
        assertThat(singleRow("LEI-001;A01;10;5;L1;" + FUTURE).status()).isEqualTo(ImportRowStatus.NOVO);
    }

    @Test
    void expiredBatchIsError() {
        assertError(singleRow("LEI-001;A01;10;5;L1;01/01/2020"), "validade", "vencido");
    }

    @Test
    void sameSkuWarehouseAndBatchTwiceIsErrorInBothLines() {
        ImportPreviewDTO preview = preview(HEADER,
                "ART-001;A01;10;5;L1;",
                "ART-001;loja central;4;5;L1;",
                "ART-001;A01;4;5;L2;",
                "ART-001;A02;4;5;L1;");

        assertThat(preview.rows()).extracting(ImportRowDTO::status).containsExactly(
                ImportRowStatus.ERRO, ImportRowStatus.ERRO, ImportRowStatus.NOVO, ImportRowStatus.NOVO);
        assertThat(preview.rows().get(0).message()).contains("repetid").contains("2, 3");
    }
}
