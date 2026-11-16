package mz.multicore.erp.modules.units.service;

import mz.multicore.erp.modules.units.model.UnitDimension;

import java.math.BigDecimal;
import java.util.List;

/**
 * Units provided by Multicore to every company (SA-C02), covering butchers, bakeries, building-materials
 * yards, hardware, beverages and gas. This is the single source; the seeder only copies it to the database.
 */
public final class SystemUnitCatalog {

    public record Definition(String code, String name, String symbol, UnitDimension dimension,
                             BigDecimal dimensionFactor, int decimals) {}

    private static final List<Definition> UNITS = List.of(
            count("UN", "Unidade", "un", "1", 0),
            count("PAR", "Par", "par", "2", 0),
            count("DZ", "Dúzia", "dz", "12", 0),
            physical("KG", "Quilograma", "kg", UnitDimension.MASS, "1", 3),
            physical("G", "Grama", "g", UnitDimension.MASS, "0.001", 0),
            physical("TON", "Tonelada", "t", UnitDimension.MASS, "1000", 3),
            physical("L", "Litro", "L", UnitDimension.VOLUME, "1", 3),
            physical("ML", "Mililitro", "ml", UnitDimension.VOLUME, "0.001", 0),
            physical("M3", "Metro cúbico", "m³", UnitDimension.VOLUME, "1000", 3),
            physical("M", "Metro", "m", UnitDimension.LENGTH, "1", 2),
            physical("CM", "Centímetro", "cm", UnitDimension.LENGTH, "0.01", 0),
            physical("MM", "Milímetro", "mm", UnitDimension.LENGTH, "0.001", 0),
            physical("M2", "Metro quadrado", "m²", UnitDimension.AREA, "1", 2),
            pack("CX", "Caixa", "cx"),
            pack("EMB", "Embalagem", "emb"),
            pack("PACOTE", "Pacote", "pct"),
            pack("SACO", "Saco", "saco"),
            pack("FARDO", "Fardo", "fardo"),
            pack("PALETE", "Palete", "plt"),
            pack("GRADE", "Grade", "grade"),
            pack("GARRAFA", "Garrafa", "gf"),
            pack("LATA", "Lata", "lata"),
            pack("BALDE", "Balde", "balde"),
            pack("BIDAO", "Bidão", "bidão"),
            pack("BOTIJA", "Botija", "botija"),
            pack("BARRA", "Barra", "barra"),
            pack("ROLO", "Rolo", "rolo"),
            pack("FOLHA", "Folha / chapa", "fl"),
            pack("TABULEIRO", "Tabuleiro", "tab"),
            pack("CARGA", "Carga de camião", "carga"));

    private SystemUnitCatalog() {
    }

    public static List<Definition> units() {
        return UNITS;
    }

    private static Definition count(String code, String name, String symbol, String factor, int decimals) {
        return new Definition(code, name, symbol, UnitDimension.COUNT, new BigDecimal(factor), decimals);
    }

    private static Definition physical(String code, String name, String symbol, UnitDimension dimension,
                                       String factor, int decimals) {
        return new Definition(code, name, symbol, dimension, new BigDecimal(factor), decimals);
    }

    private static Definition pack(String code, String name, String symbol) {
        return new Definition(code, name, symbol, UnitDimension.PACKAGE, null, 0);
    }
}
