package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Entidades que se importam por CSV (IMPORTACAO_DADOS_SPEC §4), com o nome usado na rota, o nome do
 * modelo descarregável e as colunas pela ordem do modelo (obrigatórias primeiro).
 */
public enum ImportEntity {

    CLIENTS("clients", "clientes", ClientImportService.REQUIRED_COLUMNS, ClientImportService.OPTIONAL_COLUMNS),
    SUPPLIERS("suppliers", "fornecedores", SupplierImportService.REQUIRED_COLUMNS, SupplierImportService.OPTIONAL_COLUMNS),
    PRODUCTS("products", "artigos", ProductImportService.REQUIRED_COLUMNS, ProductImportService.OPTIONAL_COLUMNS);

    private final String pathName;
    private final String templateName;
    private final List<String> columns;

    ImportEntity(String pathName, String templateName, List<String> requiredColumns, List<String> optionalColumns) {
        this.pathName = pathName;
        this.templateName = templateName;
        this.columns = Stream.concat(requiredColumns.stream(), optionalColumns.stream()).toList();
    }

    /** Nome do ficheiro do modelo, ex.: {@code modelo-clientes.csv}. */
    public String templateFileName() {
        return "modelo-" + templateName + ".csv";
    }

    public List<String> columns() {
        return columns;
    }

    /** Resolve a entidade pelo nome da rota ({@code clients}, {@code suppliers}, {@code products}). */
    public static ImportEntity fromPath(String pathName) {
        return Arrays.stream(values())
                .filter(entity -> entity.pathName.equalsIgnoreCase(pathName == null ? "" : pathName.strip()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Entidade de importação desconhecida: " + pathName
                        + ". Use clients, suppliers ou products."));
    }
}
