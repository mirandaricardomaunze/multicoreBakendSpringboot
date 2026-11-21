package mz.multicore.erp.modules.dataimport.dto;

import java.util.List;

/**
 * Pré-visualização de um ficheiro de importação (IMPORTACAO_DADOS_SPEC §3.1): o estado de cada linha,
 * as colunas do ficheiro que o modelo não conhece (ignoradas e anunciadas) e, nos artigos, as categorias
 * que serão criadas na confirmação (§4.3).
 */
public record ImportPreviewDTO(List<ImportRowDTO> rows, List<String> extraColumns, List<String> categoriesToCreate) {

    public ImportPreviewDTO {
        rows = List.copyOf(rows);
        extraColumns = List.copyOf(extraColumns);
        categoriesToCreate = categoriesToCreate == null ? List.of() : List.copyOf(categoriesToCreate);
    }

    /** Pré-visualização sem categorias a criar (clientes, fornecedores). */
    public ImportPreviewDTO(List<ImportRowDTO> rows, List<String> extraColumns) {
        this(rows, extraColumns, List.of());
    }

    public long count(ImportRowStatus status) {
        return rows.stream().filter(row -> row.status() == status).count();
    }

    /** Com qualquer erro nada pode ser gravado (tudo ou nada). */
    public boolean hasErrors() {
        return count(ImportRowStatus.ERRO) > 0;
    }
}
