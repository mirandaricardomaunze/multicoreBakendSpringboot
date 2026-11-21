package mz.multicore.erp.modules.dataimport.service;

import java.util.List;

/**
 * Ficheiro CSV de importação já lido (IMPORTACAO_DADOS_SPEC §3.2): cabeçalho reconhecido e linhas de
 * dados não vazias, cada uma com o seu número no ficheiro.
 */
public record CsvTable(CsvHeader header, List<Row> rows) {

    public CsvTable {
        rows = List.copyOf(rows);
    }

    /** Linha de dados com os campos já separados; o número conta o cabeçalho como linha 1. */
    public record Row(int number, List<String> fields) {

        public Row {
            fields = List.copyOf(fields);
        }
    }

    /** Valor da coluna na linha, sem espaços nas pontas; vazio se a coluna ou o campo faltarem. */
    public String value(Row row, String column) {
        return header.value(row.fields(), column);
    }
}
