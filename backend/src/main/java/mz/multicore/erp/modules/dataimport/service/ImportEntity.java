package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Entidades que se importam por CSV: cadastros (IMPORTACAO_DADOS_SPEC §4) e saldos iniciais (§5), com o nome usado na
 * rota, o nome do modelo descarregável, o nome em PT-MZ e as colunas pela ordem do modelo (obrigatórias primeiro).
 */
public enum ImportEntity {

    CLIENTS("clients", "clientes", "clientes",
            ClientImportService.REQUIRED_COLUMNS, ClientImportService.OPTIONAL_COLUMNS),
    SUPPLIERS("suppliers", "fornecedores", "fornecedores",
            SupplierImportService.REQUIRED_COLUMNS, SupplierImportService.OPTIONAL_COLUMNS),
    PRODUCTS("products", "artigos", "artigos",
            ProductImportService.REQUIRED_COLUMNS, ProductImportService.OPTIONAL_COLUMNS),
    OPENING_STOCK("opening-stock", "stock-inicial", "stock inicial",
            StockOpeningImportService.REQUIRED_COLUMNS, StockOpeningImportService.OPTIONAL_COLUMNS),
    OPENING_RECEIVABLES("opening-receivables", "dividas-clientes", "dívidas de clientes",
            OpeningReceivableImportService.REQUIRED_COLUMNS, OpeningDebtColumns.OPTIONAL_COLUMNS),
    OPENING_PAYABLES("opening-payables", "dividas-fornecedores", "dívidas a fornecedores",
            OpeningPayableImportService.REQUIRED_COLUMNS, OpeningDebtColumns.OPTIONAL_COLUMNS);

    private final String pathName;
    private final String templateName;
    private final String label;
    private final List<String> columns;

    ImportEntity(String pathName, String templateName, String label, List<String> requiredColumns,
                 List<String> optionalColumns) {
        this.pathName = pathName;
        this.templateName = templateName;
        this.label = label;
        this.columns = Stream.concat(requiredColumns.stream(), optionalColumns.stream()).toList();
    }

    /** Nome do ficheiro do modelo, ex.: {@code modelo-clientes.csv}, {@code modelo-stock-inicial.csv}. */
    public String templateFileName() {
        return "modelo-" + templateName + ".csv";
    }

    /** Nome da entidade em PT-MZ, ex.: {@code clientes}, {@code dívidas de clientes}. */
    public String label() {
        return label;
    }

    public List<String> columns() {
        return columns;
    }

    /** Resolve a entidade pelo nome da rota ({@code clients}, …, {@code opening-payables}). */
    public static ImportEntity fromPath(String pathName) {
        return Arrays.stream(values())
                .filter(entity -> entity.pathName.equalsIgnoreCase(pathName == null ? "" : pathName.strip()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Entidade de importação desconhecida: " + pathName
                        + ". Use clients, suppliers, products, opening-stock, opening-receivables ou opening-payables."));
    }
}
