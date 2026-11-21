package mz.multicore.erp.gui.dataimport;

/**
 * Entidades que o ecrã "Importar dados" oferece (IMPORTACAO_DADOS_SPEC §3.4, §4, §5): o caminho na API
 * ({@code /api/data-import/{path}}), o nome mostrado, o nome sugerido para o modelo descarregado e as frases
 * PT-MZ. Os cadastros vêm primeiro; os saldos iniciais dependem deles (artigos, clientes, fornecedores).
 */
public enum ImportEntityOption {
    CLIENTS("clients", "Clientes", "clientes", "clientes", "os clientes"),
    SUPPLIERS("suppliers", "Fornecedores", "fornecedores", "fornecedores", "os fornecedores"),
    PRODUCTS("products", "Artigos", "artigos", "artigos", "os artigos"),
    OPENING_STOCK("opening-stock", "Stock inicial", "stock-inicial", "stock inicial", "o stock inicial"),
    OPENING_RECEIVABLES("opening-receivables", "Dívidas de clientes", "dividas-clientes",
            "dívidas de clientes", "as dívidas de clientes"),
    OPENING_PAYABLES("opening-payables", "Dívidas a fornecedores", "dividas-fornecedores",
            "dívidas a fornecedores", "as dívidas a fornecedores");

    private final String path;
    private final String label;
    private final String fileSlug;
    private final String lowerLabel;
    private final String objectPhrase;

    ImportEntityOption(String path, String label, String fileSlug, String lowerLabel, String objectPhrase) {
        this.path = path;
        this.label = label;
        this.fileSlug = fileSlug;
        this.lowerLabel = lowerLabel;
        this.objectPhrase = objectPhrase;
    }

    public String path() {
        return path;
    }

    /** Nome do modelo, igual ao que o servidor anuncia no {@code Content-Disposition}. */
    public String templateFileName() {
        return "modelo-" + fileSlug + ".csv";
    }

    /** Nome em minúsculas para as frases ("Importação de dívidas de clientes concluída"). */
    String lowerLabel() {
        return lowerLabel;
    }

    /** Complemento directo com artigo, para "Gravar o stock inicial do ficheiro …". */
    public String objectPhrase() {
        return objectPhrase;
    }

    @Override
    public String toString() {
        return label;
    }
}
