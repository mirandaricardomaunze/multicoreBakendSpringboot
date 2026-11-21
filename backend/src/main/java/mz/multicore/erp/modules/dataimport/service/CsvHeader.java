package mz.multicore.erp.modules.dataimport.service;

import java.util.List;
import java.util.Map;

/**
 * Cabeçalho reconhecido de um CSV de importação (IMPORTACAO_DADOS_SPEC §3.2).
 *
 * @param columnIndex  coluna do modelo (nome canónico) → posição no ficheiro
 * @param extraColumns colunas do ficheiro que o modelo não conhece; ignoradas e anunciadas
 */
public record CsvHeader(Map<String, Integer> columnIndex, List<String> extraColumns) {

    public CsvHeader {
        columnIndex = Map.copyOf(columnIndex);
        extraColumns = List.copyOf(extraColumns);
    }

    public boolean has(String column) {
        return columnIndex.containsKey(column);
    }

    /** Valor da coluna na linha, sem espaços nas pontas; vazio se a coluna ou o campo faltarem. */
    public String value(List<String> fields, String column) {
        Integer index = columnIndex.get(column);
        if (index == null || index >= fields.size()) {
            return "";
        }
        String raw = fields.get(index);
        return raw == null ? "" : raw.strip();
    }
}
