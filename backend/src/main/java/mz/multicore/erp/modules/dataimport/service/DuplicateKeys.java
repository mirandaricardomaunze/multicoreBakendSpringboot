package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.modules.dataimport.dto.ImportRowDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Chaves repetidas dentro do mesmo ficheiro (IMPORTACAO_DADOS_SPEC §3.3): duas linhas com a mesma chave
 * são {@code ERRO} em ambas. Classe pura, reutilizável por clientes, fornecedores e artigos.
 */
final class DuplicateKeys {

    private final Map<String, List<Integer>> linesByKey;

    private DuplicateKeys(Map<String, List<Integer>> linesByKey) {
        this.linesByKey = linesByKey;
    }

    /** @param keyOf chave da linha já sem espaços nas pontas; vazia ou {@code null} não conta */
    static DuplicateKeys of(List<CsvTable.Row> rows, Function<CsvTable.Row, String> keyOf) {
        Map<String, List<Integer>> linesByKey = new HashMap<>();
        for (CsvTable.Row row : rows) {
            String key = keyOf.apply(row);
            if (key != null && !key.isEmpty()) {
                linesByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row.number());
            }
        }
        return new DuplicateKeys(linesByKey);
    }

    /**
     * {@code ERRO} na coluna da chave se a chave aparece em mais de uma linha do ficheiro.
     *
     * @param repeatedLabel início da mensagem já com o género certo ("NUIT repetido", "Referência repetida")
     */
    Optional<ImportRowDTO> violation(int lineNumber, String key, String column, String repeatedLabel) {
        List<Integer> lines = key == null ? List.of() : linesByKey.getOrDefault(key, List.of());
        if (lines.size() < 2) {
            return Optional.empty();
        }
        String allLines = lines.stream().map(String::valueOf).collect(Collectors.joining(", "));
        return Optional.of(ImportRowDTO.error(lineNumber, column,
                repeatedLabel + " no ficheiro (linhas " + allLines + ")."));
    }
}
