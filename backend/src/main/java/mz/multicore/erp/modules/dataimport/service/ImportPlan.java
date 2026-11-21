package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowStatus;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Classificação de um ficheiro de importação (IMPORTACAO_DADOS_SPEC §3.1): o estado de cada linha e, nas
 * linhas válidas, o pedido do registo manual que a confirmação grava. A mesma classificação serve a
 * pré-visualização e a confirmação, que a refaz do zero em vez de confiar na pré-visualização.
 *
 * @param <T> pedido gravado por linha (ex.: {@code SaveClientRequest})
 */
record ImportPlan<T>(List<Line<T>> lines, List<String> extraColumns, List<String> categoriesToCreate) {

    private static final Set<ImportRowStatus> WRITTEN = Set.of(ImportRowStatus.NOVO, ImportRowStatus.ASSOCIADO);

    ImportPlan {
        lines = List.copyOf(lines);
        extraColumns = List.copyOf(extraColumns);
        categoriesToCreate = List.copyOf(categoriesToCreate);
    }

    ImportPlan(List<Line<T>> lines, List<String> extraColumns) {
        this(lines, extraColumns, List.of());
    }

    /** Linha classificada; {@code item} é {@code null} quando a linha tem erro. */
    record Line<T>(ImportRowDTO row, T item) {

        static <T> Line<T> error(ImportRowDTO row) {
            return new Line<>(row, null);
        }
    }

    ImportPreviewDTO preview() {
        return new ImportPreviewDTO(lines.stream().map(Line::row).toList(), extraColumns, categoriesToCreate);
    }

    /** Tudo ou nada (§2, §3.1): com qualquer {@code ERRO} a confirmação é recusada e nada é gravado. */
    ImportPlan<T> requireNoErrors() {
        List<ImportRowDTO> errors = lines.stream()
                .map(Line::row)
                .filter(row -> row.status() == ImportRowStatus.ERRO)
                .toList();
        if (!errors.isEmpty()) {
            ImportRowDTO first = errors.get(0);
            throw new BusinessRuleException("O ficheiro tem " + errors.size() + " linha(s) com erro; nada foi gravado. "
                    + "Primeiro erro: linha " + first.lineNumber() + ", coluna " + first.column() + ": "
                    + first.message() + " Corrija o ficheiro e pré-visualize de novo.");
        }
        return this;
    }

    /** Pedidos das linhas a gravar ({@code NOVO} e {@code ASSOCIADO}), pela ordem do ficheiro. */
    List<T> toWrite() {
        return lines.stream()
                .filter(line -> WRITTEN.contains(line.row().status()))
                .map(Line::item)
                .filter(Objects::nonNull)
                .toList();
    }
}
