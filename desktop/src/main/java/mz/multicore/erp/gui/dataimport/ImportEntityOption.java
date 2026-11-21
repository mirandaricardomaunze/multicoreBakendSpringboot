package mz.multicore.erp.gui.dataimport;

/**
 * Entidades que o ecrã "Importar dados" oferece (IMPORTACAO_DADOS_SPEC §3.4, §4): o caminho na API
 * ({@code /api/data-import/{path}}), o nome mostrado e o nome sugerido para o modelo descarregado.
 */
public enum ImportEntityOption {
    CLIENTS("clients", "Clientes", "clientes"),
    SUPPLIERS("suppliers", "Fornecedores", "fornecedores"),
    PRODUCTS("products", "Artigos", "artigos");

    private final String path;
    private final String label;
    private final String fileSlug;

    ImportEntityOption(String path, String label, String fileSlug) {
        this.path = path;
        this.label = label;
        this.fileSlug = fileSlug;
    }

    public String path() {
        return path;
    }

    /** Nome do modelo, igual ao que o servidor anuncia no {@code Content-Disposition}. */
    public String templateFileName() {
        return "modelo-" + fileSlug + ".csv";
    }

    /** Plural em minúsculas para as frases ("Importação de artigos concluída"). */
    String lowerLabel() {
        return fileSlug;
    }

    @Override
    public String toString() {
        return label;
    }
}
