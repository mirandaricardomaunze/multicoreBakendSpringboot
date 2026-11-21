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
import java.util.List;
import java.util.Map;

/**
 * Importação de artigos por CSV (IMPORTACAO_DADOS_SPEC §4.3). Cada linha é convertida no mesmo
 * {@link CreateProductRequest} do registo manual e validada pelas mesmas regras; o {@code iva} tem de
 * ser uma taxa de IVA já existente na empresa activa, pela percentagem ou pelo código; a {@code categoria}
 * reconhece-se pelo nome e, se não existir, é anunciada em "categorias a criar" (criada só na confirmação).
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

    private static final List<String> REQUIRED_COLUMNS = List.of(SKU, NAME, SALE_PRICE, VAT, CATEGORY);
    private static final List<String> OPTIONAL_COLUMNS =
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
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        CsvTable table = reader.read(content, REQUIRED_COLUMNS, OPTIONAL_COLUMNS);
        ImportVatRateResolver vatRates = new ImportVatRateResolver(taxRateLookup.listActiveForCompany(companyId));
        ImportCategoryPlan categories = new ImportCategoryPlan(productLookup.listCategoriesForCompany(companyId));
        List<ImportRowDTO> rows = table.rows().stream()
                .map(row -> classify(table, row, vatRates, categories))
                .toList();
        return new ImportPreviewDTO(rows, table.header().extraColumns(), categories.toCreate());
    }

    /** Só a categoria de uma linha sem erros entra em "categorias a criar"; nada é gravado aqui. */
    private ImportRowDTO classify(CsvTable table, CsvTable.Row row, ImportVatRateResolver vatRates,
                                  ImportCategoryPlan categories) {
        ProductLine product;
        try {
            product = toProductLine(table, row, vatRates, categories);
        } catch (ImportValueException invalidValue) {
            return ImportRowDTO.error(row.number(), invalidValue.getColumn(), invalidValue.getMessage());
        }
        return requestValidator.firstViolation(row.number(), product.request(), COLUMN_BY_PROPERTY)
                .orElseGet(() -> {
                    categories.requireCreation(product.category());
                    return ImportRowDTO.of(row.number(), ImportRowStatus.NOVO);
                });
    }

    /** Valores pela ordem das colunas do modelo: o primeiro problema da linha é o que se reporta. */
    private ProductLine toProductLine(CsvTable table, CsvTable.Row row, ImportVatRateResolver vatRates,
                                      ImportCategoryPlan categories) {
        int line = row.number();
        String sku = requiredText(table, row, SKU);
        String name = requiredText(table, row, NAME);
        BigDecimal salePrice = requiredDecimal(table, row, SALE_PRICE);
        TaxRate vatRate = vatRates.resolve(table.value(row, VAT), line, VAT);
        String category = requiredText(table, row, CATEGORY);
        Long categoryId = categories.existing(category).map(ProductCategory::getId).orElse(null);
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

    /** Linha convertida: o pedido do registo manual e o nome da categoria como veio no ficheiro. */
    private record ProductLine(String category, CreateProductRequest request) {
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
