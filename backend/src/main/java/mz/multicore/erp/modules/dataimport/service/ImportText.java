package mz.multicore.erp.modules.dataimport.service;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Comparação tolerante de nomes vindos do ficheiro (IMPORTACAO_DADOS_SPEC §3.2, §4.3): sem espaços nas
 * pontas, sem acentos e sem distinguir maiúsculas. Usada nos nomes das colunas e nos nomes de categoria.
 */
final class ImportText {

    private ImportText() {
    }

    /** Chave de comparação: {@code " Lacticínios "} e {@code "LACTICINIOS"} dão o mesmo valor. */
    static String comparisonKey(String value) {
        String withoutAccents = Normalizer.normalize(value.strip(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT);
    }
}
