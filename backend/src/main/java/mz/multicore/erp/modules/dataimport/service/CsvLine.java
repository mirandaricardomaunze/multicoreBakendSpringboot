package mz.multicore.erp.modules.dataimport.service;

/**
 * Linha não vazia de um CSV de importação (IMPORTACAO_DADOS_SPEC §3.2).
 *
 * @param number número da linha no ficheiro, com o cabeçalho como linha 1
 * @param text   conteúdo da linha, sem o fim de linha
 */
public record CsvLine(int number, String text) {
}
