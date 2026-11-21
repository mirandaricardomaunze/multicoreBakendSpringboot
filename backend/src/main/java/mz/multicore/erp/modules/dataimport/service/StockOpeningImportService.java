package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowStatus;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.StockMovementLookup;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Importação do stock inicial por CSV (IMPORTACAO_DADOS_SPEC §5.1). Cada linha indica um artigo (pelo SKU)
 * e um armazém (pelo código ou nome) da empresa activa, a quantidade (&gt; 0), o custo unitário (&ge; 0) e,
 * opcionalmente, o lote e a validade, com as regras da entrada manual: artigo perecível exige validade e
 * lote já vencido não entra. A chave é SKU + armazém + lote: repetida no ficheiro é {@code ERRO} em ambas.
 * Artigo que já tem movimentos no armazém é {@code ERRO} ("use um ajuste"): reimportar nunca duplica stock.
 */
@Service
public class StockOpeningImportService {

    static final String SKU = "sku";
    static final String WAREHOUSE = "armazem";
    static final String QUANTITY = "quantidade";
    static final String UNIT_COST = "custo_unitario";
    static final String BATCH = "lote";
    static final String EXPIRY = "validade";

    static final List<String> REQUIRED_COLUMNS = List.of(SKU, WAREHOUSE, QUANTITY, UNIT_COST);
    static final List<String> OPTIONAL_COLUMNS = List.of(BATCH, EXPIRY);
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CsvImportReader reader = new CsvImportReader();
    private final ImportValueParser parser = new ImportValueParser();
    private final ProductLookup productLookup;
    private final WarehouseLookup warehouseLookup;
    private final StockMovementLookup movementLookup;

    public StockOpeningImportService(ProductLookup productLookup, WarehouseLookup warehouseLookup,
                                     StockMovementLookup movementLookup) {
        this.productLookup = productLookup;
        this.warehouseLookup = warehouseLookup;
        this.movementLookup = movementLookup;
    }

    /** Lê o ficheiro e classifica cada linha na empresa activa, sem gravar nada (§3.1 passo 2). */
    public ImportPreviewDTO preview(byte[] content) {
        return plan(content).preview();
    }

    /** Classificação com a entrada de cada linha válida; base da pré-visualização e da confirmação. */
    ImportPlan<StockOpeningLine> plan(byte[] content) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        CsvTable table = reader.read(content, REQUIRED_COLUMNS, OPTIONAL_COLUMNS);
        FileContext context = new FileContext(table, companyId,
                new ImportWarehouseResolver(warehouseLookup.listForCompany(companyId)), new HashMap<>(),
                new HashMap<>());
        List<ImportPlan.Line<StockOpeningLine>> converted = table.rows().stream()
                .map(row -> convert(context, row))
                .toList();
        Map<Integer, String> keyByLine = new HashMap<>();
        converted.stream()
                .filter(line -> line.item() != null)
                .forEach(line -> keyByLine.put(line.row().lineNumber(), line.item().key()));
        DuplicateKeys duplicates = DuplicateKeys.of(table.rows(), row -> keyByLine.get(row.number()));
        List<ImportPlan.Line<StockOpeningLine>> lines = converted.stream()
                .map(line -> line.item() == null ? line : withDuplicateCheck(line, duplicates))
                .toList();
        return new ImportPlan<>(lines, table.header().extraColumns());
    }

    private static ImportPlan.Line<StockOpeningLine> withDuplicateCheck(ImportPlan.Line<StockOpeningLine> line,
                                                                       DuplicateKeys duplicates) {
        int number = line.row().lineNumber();
        return duplicates.violation(number, line.item().key(), SKU, "SKU, armazém e lote repetidos")
                .map(ImportPlan.Line::<StockOpeningLine>error)
                .orElse(line);
    }

    /** Valores pela ordem das colunas do modelo: o primeiro problema da linha é o que se reporta. */
    private ImportPlan.Line<StockOpeningLine> convert(FileContext context, CsvTable.Row row) {
        try {
            StockOpeningLine line = toLine(context, row);
            return new ImportPlan.Line<>(ImportRowDTO.of(row.number(), ImportRowStatus.NOVO), line);
        } catch (ImportValueException invalidValue) {
            return ImportPlan.Line.error(
                    ImportRowDTO.error(row.number(), invalidValue.getColumn(), invalidValue.getMessage()));
        }
    }

    private StockOpeningLine toLine(FileContext context, CsvTable.Row row) {
        CsvTable table = context.table();
        int line = row.number();
        Product product = product(context, parser.requireText(table.value(row, SKU), line, SKU), line);
        Warehouse warehouse = context.warehouses().resolve(table.value(row, WAREHOUSE), line, WAREHOUSE);
        requireNoMovements(context, product, warehouse, line);
        BigDecimal quantity = parser.requireDecimal(table.value(row, QUANTITY), line, QUANTITY);
        if (quantity.signum() <= 0) {
            throw new ImportValueException(line, QUANTITY, "a quantidade tem de ser maior que zero.");
        }
        BigDecimal unitCost = parser.requireDecimal(table.value(row, UNIT_COST), line, UNIT_COST);
        if (unitCost.signum() < 0) {
            throw new ImportValueException(line, UNIT_COST, "o custo unitário não pode ser negativo.");
        }
        String batch = parser.parseText(table.value(row, BATCH));
        LocalDate expiry = parser.parseDate(table.value(row, EXPIRY), line, EXPIRY);
        requireValidExpiry(product, expiry, line);
        return new StockOpeningLine(product, warehouse, quantity, unitCost, batch, expiry);
    }

    private Product product(FileContext context, String sku, int line) {
        return context.productsBySku()
                .computeIfAbsent(sku, key -> productLookup.findBySkuForCompany(key, context.companyId()))
                .orElseThrow(() -> new ImportValueException(line, SKU, "o artigo «" + sku
                        + "» não existe na empresa; importe primeiro os artigos."));
    }

    /**
     * Stock inicial só entra onde o artigo ainda não tem movimentos; assim reimportar o mesmo ficheiro nunca
     * duplica stock (§5.1). Consulta o ledger uma vez por armazém do ficheiro.
     */
    private void requireNoMovements(FileContext context, Product product, Warehouse warehouse, int line) {
        Set<Long> productsWithMovements = context.productsWithMovementsByWarehouse().computeIfAbsent(
                warehouse.getId(), movementLookup::listProductIdsWithMovements);
        if (productsWithMovements.contains(product.getId())) {
            throw new ImportValueException(line, WAREHOUSE, "o artigo «" + product.getName()
                    + "» já tem stock neste armazém; use um ajuste.");
        }
    }

    /** Regras da entrada manual: perecível exige validade; lote já vencido não entra (§5.1). */
    private static void requireValidExpiry(Product product, LocalDate expiry, int line) {
        if (expiry == null && product.isTracksExpiry()) {
            throw new ImportValueException(line, EXPIRY, "o artigo «" + product.getName()
                    + "» é perecível: indique a validade do lote.");
        }
        if (expiry != null && expiry.isBefore(LocalDate.now())) {
            throw new ImportValueException(line, EXPIRY, "lote já vencido (validade "
                    + expiry.format(DISPLAY_DATE) + "); não é possível dar entrada de stock.");
        }
    }

    /**
     * Entrada de stock inicial de uma linha válida. As entidades servem só para a escrita pela porta do
     * inventário e nunca saem deste módulo.
     */
    record StockOpeningLine(Product product, Warehouse warehouse, BigDecimal quantity, BigDecimal unitCost,
                            String batchNumber, LocalDate expirationDate) {

        /**
         * Chave SKU + armazém + lote (§5.1). O lote compara-se tal como fica gravado; lote vazio conta como o
         * mesmo lote por omissão.
         */
        String key() {
            return product.getId() + "|" + warehouse.getId() + "|" + (batchNumber == null ? "" : batchNumber);
        }
    }

    /** O que a classificação de cada linha precisa de saber sobre o ficheiro inteiro e a empresa activa. */
    private record FileContext(CsvTable table, Long companyId, ImportWarehouseResolver warehouses,
                               Map<String, Optional<Product>> productsBySku,
                               Map<Long, Set<Long>> productsWithMovementsByWarehouse) {
    }
}
