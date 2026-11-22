package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.modules.inventory.model.Warehouse;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Resolve a coluna {@code armazem} do stock inicial (IMPORTACAO_DADOS_SPEC §5.1): o código de um armazém
 * da empresa ou o seu nome, sem distinguir maiúsculas, acentos nem espaços nas pontas. O código ganha ao
 * nome; um nome partilhado por dois armazéns é erro da linha — nunca se adivinha o armazém. Classe pura:
 * recebe os armazéns da empresa já lidos pela porta do inventário.
 */
final class ImportWarehouseResolver {

    private final List<Warehouse> warehouses;
    private final ImportValueParser parser = new ImportValueParser();

    ImportWarehouseResolver(List<Warehouse> companyWarehouses) {
        this.warehouses = List.copyOf(companyWarehouses);
    }

    /** Armazém da empresa indicado na célula; {@link ImportValueException} se não houver um só. */
    Warehouse resolve(String raw, int lineNumber, String column) {
        String value = parser.requireText(raw, lineNumber, column);
        String key = ImportText.comparisonKey(value);
        Optional<Warehouse> byCode = warehouses.stream()
                .filter(warehouse -> matches(warehouse.getWarehouseNumber(), key))
                .findFirst();
        if (byCode.isPresent()) {
            return byCode.get();
        }
        List<Warehouse> byName = warehouses.stream().filter(warehouse -> matches(warehouse.getName(), key)).toList();
        if (byName.size() == 1) {
            return byName.get(0);
        }
        if (byName.isEmpty()) {
            throw new ImportValueException(lineNumber, column, "o armazém «" + value
                    + "» não existe na empresa (use o código ou o nome de um armazém existente).");
        }
        throw new ImportValueException(lineNumber, column, "o armazém «" + value + "» é ambíguo ("
                + codes(byName) + " têm esse nome); indique o código do armazém.");
    }

    private static boolean matches(String candidate, String key) {
        return candidate != null && !candidate.isBlank() && ImportText.comparisonKey(candidate).equals(key);
    }

    private static String codes(List<Warehouse> matches) {
        return matches.stream()
                .map(warehouse -> warehouse.getWarehouseNumber() == null
                        ? String.valueOf(warehouse.getId()) : warehouse.getWarehouseNumber())
                .collect(Collectors.joining(", "));
    }
}
