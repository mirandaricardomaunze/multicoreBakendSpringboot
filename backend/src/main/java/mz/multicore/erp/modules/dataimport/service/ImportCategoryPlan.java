package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.modules.comercial.model.ProductCategory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Categorias de uma importação de artigos (IMPORTACAO_DADOS_SPEC §4.3): a coluna {@code categoria} reconhece-se
 * pelo nome, sem distinguir maiúsculas nem acentos, entre as categorias da empresa activa. A inexistente não é
 * erro: fica registada como "a criar" — uma vez, com a grafia da primeira linha — e só é criada na confirmação.
 * Classe pura: recebe as categorias da empresa já lidas pela porta do módulo comercial; não grava nada.
 */
final class ImportCategoryPlan {

    private static final int MAX_CODE_LENGTH = 30;

    private final Map<String, ProductCategory> existingByName = new LinkedHashMap<>();
    private final Map<String, String> toCreateByName = new LinkedHashMap<>();

    ImportCategoryPlan(List<ProductCategory> companyCategories) {
        companyCategories.stream()
                .filter(category -> category.getName() != null)
                .forEach(category -> existingByName.putIfAbsent(ImportText.comparisonKey(category.getName()), category));
    }

    /** Categoria da empresa com este nome, se existir. */
    Optional<ProductCategory> existing(String name) {
        return Optional.ofNullable(existingByName.get(ImportText.comparisonKey(name)));
    }

    /** Anota a categoria de uma linha válida que não existe na empresa; repetidas contam uma vez. */
    void requireCreation(String name) {
        String key = ImportText.comparisonKey(name);
        if (!existingByName.containsKey(key)) {
            toCreateByName.putIfAbsent(key, name.strip());
        }
    }

    /**
     * Código da categoria criada na confirmação: o nome sem acentos, em maiúsculas, com {@code _} no lugar de
     * espaços e símbolos, até 30 caracteres ({@code "Lacticínios Frescos"} → {@code LACTICINIOS_FRESCOS}).
     */
    static String codeFor(String name) {
        String code = ImportText.comparisonKey(name).toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (code.isEmpty()) {
            code = "CATEGORIA";
        }
        return code.length() > MAX_CODE_LENGTH ? code.substring(0, MAX_CODE_LENGTH) : code;
    }

    /** Nomes a criar na confirmação, pela ordem em que aparecem no ficheiro. */
    List<String> toCreate() {
        return List.copyOf(toCreateByName.values());
    }
}
