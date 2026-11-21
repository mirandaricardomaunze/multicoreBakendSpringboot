package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;

/**
 * Valor de uma célula do CSV num formato não aceite (IMPORTACAO_DADOS_SPEC §3.2).
 * É erro da linha, não do ficheiro: guarda o número da linha e a coluna para a pré-visualização.
 */
public class ImportValueException extends BusinessRuleException {

    private final int lineNumber;
    private final String column;

    public ImportValueException(int lineNumber, String column, String message) {
        super("Linha " + lineNumber + ", coluna " + column + ": " + message);
        this.lineNumber = lineNumber;
        this.column = column;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getColumn() {
        return column;
    }
}
