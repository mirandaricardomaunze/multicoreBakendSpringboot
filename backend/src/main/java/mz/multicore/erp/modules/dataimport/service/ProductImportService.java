package mz.multicore.erp.modules.dataimport.service;

import jakarta.validation.Validator;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.CreateProductRequest;
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
 * ser uma taxa de IVA já existente na empresa activa, pela percentagem ou pelo código.
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

    public ProductImportService(Validator validator, TaxRateLookup taxRateLookup) {
        this.requestValidator = new ImportRequestValidator(validator);
        this.taxRateLookup = taxRateLookup;
    }

    /** Lê o ficheiro e classifica cada linha na empresa activa, sem gravar nada (§3.1 passo 2). */
    public ImportPreviewDTO preview(byte[] content) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        CsvTable table = reader.read(content, REQUIRED_COLUMNS, OPTIONAL_COLUMNS);
        ImportVatRateResolver vatRates = new ImportVatRateResolver(taxRateLookup.listActiveForCompany(companyId));
        List<ImportRowDTO> rows = table.rows().stream()
                .map(row -> classify(table, row, vatRates))
                .toList();
        return new ImportPreviewDTO(rows, table.header().extraColumns());
    }

    private ImportRowDTO classify(CsvTable table, CsvTable.Row row, ImportVatRateResolver vatRates) {
        CreateProductRequest request;
        try {
            request = toRequest(table, row, vatRates);
        } catch (ImportValueException invalidValue) {
            return ImportRowDTO.error(row.number(), invalidValue.getColumn(), invalidValue.getMessage());
        }
        return requestValidator.firstViolation(row.number(), request, COLUMN_BY_PROPERTY)
                .orElseGet(() -> ImportRowDTO.of(row.number(), ImportRowStatus.NOVO));
    }

    /** Valores pela ordem das colunas do modelo: o primeiro problema da linha é o que se reporta. */
    private CreateProductRequest toRequest(CsvTable table, CsvTable.Row row, ImportVatRateResolver vatRates) {
        int line = row.number();
        String sku = requiredText(table, row, SKU);
        String name = requiredText(table, row, NAME);
        BigDecimal salePrice = requiredDecimal(table, row, SALE_PRICE);
        TaxRate vatRate = vatRates.resolve(table.value(row, VAT), line, VAT);
        requiredText(table, row, CATEGORY);
        BigDecimal wholesalePrice = parser.parseDecimal(table.value(row, WHOLESALE_PRICE), line, WHOLESALE_PRICE);
        return new CreateProductRequest(
                sku,
                parser.parseText(table.value(row, REFERENCE)),
                parser.parseText(table.value(row, BARCODE)),
                name,
                salePrice,
                null, null, 1, null, null,
                null, // categoria: resolvida pelo nome (IMP-AR-02)
                null, true,
                vatRate.getId(),
                parser.parseText(table.value(row, DESCRIPTION)),
                wholesalePrice,
                null, null, null);
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
