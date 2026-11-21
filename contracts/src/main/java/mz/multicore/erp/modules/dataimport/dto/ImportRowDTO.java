package mz.multicore.erp.modules.dataimport.dto;

/**
 * Resultado de uma linha do ficheiro na pré-visualização (IMPORTACAO_DADOS_SPEC §3.1).
 *
 * @param lineNumber número da linha no ficheiro, com o cabeçalho como linha 1
 * @param status     {@code NOVO}, {@code JÁ EXISTE}, {@code ASSOCIADO} ou {@code ERRO}
 * @param column     coluna com o erro (só em {@code ERRO})
 * @param message    mensagem em PT-MZ (só em {@code ERRO})
 */
public record ImportRowDTO(int lineNumber, ImportRowStatus status, String column, String message) {

    public static ImportRowDTO of(int lineNumber, ImportRowStatus status) {
        return new ImportRowDTO(lineNumber, status, null, null);
    }

    public static ImportRowDTO error(int lineNumber, String column, String message) {
        return new ImportRowDTO(lineNumber, ImportRowStatus.ERRO, column, message);
    }
}
