package mz.multicore.erp.modules.dataimport.dto;

import java.util.List;

/**
 * Pré-visualização de um ficheiro de importação (IMPORTACAO_DADOS_SPEC §3.1): o estado de cada linha
 * e as colunas do ficheiro que o modelo não conhece (ignoradas e anunciadas).
 */
public record ImportPreviewDTO(List<ImportRowDTO> rows, List<String> extraColumns) {

    public ImportPreviewDTO {
        rows = List.copyOf(rows);
        extraColumns = List.copyOf(extraColumns);
    }

    public long count(ImportRowStatus status) {
        return rows.stream().filter(row -> row.status() == status).count();
    }

    /** Com qualquer erro nada pode ser gravado (tudo ou nada). */
    public boolean hasErrors() {
        return count(ImportRowStatus.ERRO) > 0;
    }
}
