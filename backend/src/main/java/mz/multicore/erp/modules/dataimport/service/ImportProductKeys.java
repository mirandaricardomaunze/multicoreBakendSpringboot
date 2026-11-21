package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.dataimport.dto.ImportRowDTO;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Artigos já existentes no sistema com as chaves do ficheiro (IMPORTACAO_DADOS_SPEC §4.3): o SKU existente
 * é {@code JÁ EXISTE}; código de barras ou referência já usados por <em>outro</em> artigo (SKU diferente) são
 * {@code ERRO}, porque os três são únicos na base. Classe pura, sem Spring.
 */
final class ImportProductKeys {

    private final Set<String> skus = new HashSet<>();
    private final Map<String, String> skuByBarcode = new HashMap<>();
    private final Map<String, String> skuByReference = new HashMap<>();

    ImportProductKeys(List<Product> existing) {
        for (Product product : existing) {
            skus.add(product.getSku());
            if (product.getBarcode() != null) {
                skuByBarcode.put(product.getBarcode(), product.getSku());
            }
            if (product.getReference() != null) {
                skuByReference.put(product.getReference(), product.getSku());
            }
        }
    }

    boolean skuExists(String sku) {
        return skus.contains(sku);
    }

    Optional<ImportRowDTO> foreignBarcode(int lineNumber, String sku, String barcode, String column) {
        return foreign(skuByBarcode, lineNumber, sku, barcode, column, "Código de barras");
    }

    Optional<ImportRowDTO> foreignReference(int lineNumber, String sku, String reference, String column) {
        return foreign(skuByReference, lineNumber, sku, reference, column, "Referência");
    }

    private static Optional<ImportRowDTO> foreign(Map<String, String> skuByKey, int lineNumber, String sku,
                                                  String key, String column, String label) {
        String owner = key == null ? null : skuByKey.get(key);
        if (owner == null || owner.equals(sku)) {
            return Optional.empty();
        }
        return Optional.of(ImportRowDTO.error(lineNumber, column,
                label + " " + key + " já pertence ao artigo " + owner + "."));
    }
}
