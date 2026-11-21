package mz.multicore.erp.modules.dataimport.service;

import jakarta.validation.Validator;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.CreateProductRequest;
import mz.multicore.erp.modules.comercial.model.ProductCategory;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowStatus;
import mz.multicore.erp.modules.fiscal.model.TaxRate;
import mz.multicore.erp.modules.fiscal.service.TaxRateLookup;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Importação de artigos por CSV (IMPORTACAO_DADOS_SPEC §4.3). Cada linha é convertida no mesmo
 * {@link CreateProductRequest} do registo manual e validada pelas mesmas regras; o {@code iva} tem de
 * ser uma taxa de IVA já existente na empresa activa, pela percentagem ou pelo código; a {@code categoria}
 * reconhece-se pelo nome e, se não existir, é anunciada em "categorias a criar" (criada só na confirmação).
 * SKU já existente no sistema é {@code JÁ EXISTE}; código de barras ou referência de outro artigo é {@code ERRO}.
 */
@Service
public class ProductImportService {

    static final String SKU = "sku";
    static final String NAME = "nome";
    static final String SALE_PRICE = "preco_venda";
    static final String VAT = "iva";
    static final String CATEGORY = "categoria";
    static final String BARCODE = "codigo_barras";
    static final String REFERENCE = "referencia";
    static final String WHOLESALE_PRICE = "preco_grosso";
    static final String UNIT = "unidade";
    static final String DESCRIPTION = "descricao";

    static final List<String> REQUIRED_COLUMNS = List.of(SKU, NAME, SALE_PRICE, VAT, CATEGORY);
    static final List<String> OPTIONAL_COLUMNS =
            List.of(BARCODE, REFERENCE, WHOLESALE_PRICE, UNIT, DESCRIPTION);
    private static final Map<String, String> COLUMN_BY_PROPERTY = columnByProperty();
    private static final String REQUIRED_VALUE = "valor obrigatório em falta.";

    private final CsvImportReader reader = new CsvImportReader();
    private final ImportValueParser parser = new ImportValueParser();
    private final ImportRequestValidator requestValidator;
    private final TaxRateLookup taxRateLookup;
    private final ProductLookup productLookup;

    public ProductImportService(Validator validator, TaxRateLookup taxRateLookup, ProductLookup productLookup) {
        this.requestValidator = new ImportRequestValidator(validator);
        this.taxRateLookup = taxRateLookup;
        this.productLookup = productLookup;
    }

    /** Lê o ficheiro e classifica cada linha na empresa activa, sem gravar nada (§3.1 passo 2). */
    public ImportPreviewDTO preview(byte[] content) {
        return plan(content).preview();
    }

    /** Classificação com o artigo de cada linha válida; base da pré-visualização e da confirmação. */
    ImportPlan<ProductLine> plan(byte[] content) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        CsvTable table = reader.read(content, REQUIRED_COLUMNS, OPTIONAL_COLUMNS);
        FileContext context = new FileContext(
                table,
                new ImportVatRateResolver(taxRateLookup.listActiveForCompany(companyId)),
                new ImportCategoryPlan(productLookup.listCategoriesForCompany(companyId)),
                new ImportProductKeys(productLookup.findByAnyKey(
                        valuesOf(table, SKU), valuesOf(table, BARCODE), valuesOf(table, REFERENCE))),
                duplicatesOf(table, SKU),
                duplicatesOf(table, BARCODE),
                duplicatesOf(table, REFERENCE));
        List<ImportPlan.Line<ProductLine>> lines = table.rows().stream()
                .map(row -> classify(context, row))
                .toList();
        return new ImportPlan<>(lines, table.header().extraColumns(), context.categories().toCreate());
    }

    /**
     * Ordem: valores, regras do registo manual, chaves repetidas no ficheiro, chaves de outro artigo e, por
     * fim, o estado. Só a categoria de uma linha {@code NOVO} entra em "categorias a criar"; nada é gravado.
     */
    private ImportPlan.Line<ProductLine> classify(FileContext context, CsvTable.Row row) {
        ProductLine product;
        try {
            product = toProductLine(context, row);
        } catch (ImportValueException invalidValue) {
            return ImportPlan.Line.error(
                    ImportRowDTO.error(row.number(), invalidValue.getColumn(), invalidValue.getMessage()));
        }
        int line = row.number();
        CreateProductRequest request = product.request();
        ImportProductKeys existing = context.existing();
        return requestValidator.firstViolation(line, request, COLUMN_BY_PROPERTY)
                .or(() -> context.skus().violation(line, request.sku(), SKU, "SKU repetido"))
                .or(() -> context.barcodes().violation(line, request.barcode(), BARCODE, "Código de barras repetido"))
                .or(() -> context.references().violation(line, request.reference(), REFERENCE, "Referência repetida"))
                .or(() -> existing.foreignBarcode(line, request.sku(), request.barcode(), BARCODE))
                .or(() -> existing.foreignReference(line, request.sku(), request.reference(), REFERENCE))
                .map(ImportPlan.Line::<ProductLine>error)
                .orElseGet(() -> {
                    if (existing.skuExists(request.sku())) {
                        // ignorado, nunca alterado
                        return new ImportPlan.Line<>(ImportRowDTO.of(line, ImportRowStatus.JA_EXISTE), product);
                    }
                    context.categories().requireCreation(product.category());
                    return new ImportPlan.Line<>(ImportRowDTO.of(line, ImportRowStatus.NOVO), product);
                });
    }

    /** Valores pela ordem das colunas do modelo: o primeiro problema da linha é o que se reporta. */
    private ProductLine toProductLine(FileContext context, CsvTable.Row row) {
        CsvTable table = context.table();
        int line = row.number();
        String sku = requiredText(table, row, SKU);
        String name = requiredText(table, row, NAME);
        BigDecimal salePrice = requiredDecimal(table, row, SALE_PRICE);
        TaxRate vatRate = context.vatRates().resolve(table.value(row, VAT), line, VAT);
        String category = requiredText(table, row, CATEGORY);
        Long categoryId = context.categories().existing(category).map(ProductCategory::getId).orElse(null);
        BigDecimal wholesalePrice = parser.parseDecimal(table.value(row, WHOLESALE_PRICE), line, WHOLESALE_PRICE);
        return new ProductLine(category, new CreateProductRequest(
                sku,
                parser.parseText(table.value(row, REFERENCE)),
                parser.parseText(table.value(row, BARCODE)),
                name,
                salePrice,
                null, null, 1, null, null,
                categoryId, // null: categoria nova, criada só na confirmação
                null, true,
                vatRate.getId(),
                parser.parseText(table.value(row, DESCRIPTION)),
                wholesalePrice,
                null, null, null));
    }

    /**
     * Linha convertida: o pedido do registo manual e o nome da categoria como veio no ficheiro. Com categoria
     * nova o {@code categoryId} do pedido é {@code null} até a confirmação a criar.
     */
    record ProductLine(String category, CreateProductRequest request) {

        /** O mesmo pedido com a categoria criada na confirmação. */
        CreateProductRequest withCategory(Long categoryId) {
            CreateProductRequest r = request;
            return new CreateProductRequest(r.sku(), r.reference(), r.barcode(), r.name(), r.unitPrice(),
                    r.purchasePrice(), r.minStock(), r.unitsPerBox(), r.packagesPerBox(), r.unitsPerPackage(),
                    categoryId, r.saleType(), r.stockTracked(), r.taxRateId(), r.description(), r.wholesalePrice(),
                    r.wholesaleMinQty(), r.netUnitWeightKg(), r.grossUnitWeightKg(), r.businessUnitId(),
                    r.tracksExpiry());
        }
    }

    /** O que a classificação de cada linha precisa de saber sobre o ficheiro inteiro e a empresa activa. */
    private record FileContext(CsvTable table, ImportVatRateResolver vatRates, ImportCategoryPlan categories,
                               ImportProductKeys existing, DuplicateKeys skus, DuplicateKeys barcodes,
                               DuplicateKeys references) {
    }

    private Set<String> valuesOf(CsvTable table, String column) {
        return table.rows().stream()
                .map(row -> parser.parseText(table.value(row, column)))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private DuplicateKeys duplicatesOf(CsvTable table, String column) {
        return DuplicateKeys.of(table.rows(), row -> parser.parseText(table.value(row, column)));
    }

    private String requiredText(CsvTable table, CsvTable.Row row, String column) {
        String value = parser.parseText(table.value(row, column));
        if (value == null) {
            throw new ImportValueException(row.number(), column, REQUIRED_VALUE);
        }
        return value;
    }

    private BigDecimal requiredDecimal(CsvTable table, CsvTable.Row row, String column) {
        BigDecimal value = parser.parseDecimal(table.value(row, column), row.number(), column);
        if (value == null) {
            throw new ImportValueException(row.number(), column, REQUIRED_VALUE);
        }
        return value;
    }

    private static Map<String, String> columnByProperty() {
        Map<String, String> columns = new LinkedHashMap<>();
        columns.put("sku", SKU);
        columns.put("name", NAME);
        columns.put("unitPrice", SALE_PRICE);
        columns.put("taxRateId", VAT);
        columns.put("barcode", BARCODE);
        columns.put("reference", REFERENCE);
        columns.put("wholesalePrice", WHOLESALE_PRICE);
        columns.put("description", DESCRIPTION);
        return Collections.unmodifiableMap(columns); // mantém a ordem das colunas do modelo
    }
}
