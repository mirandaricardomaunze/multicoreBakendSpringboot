package mz.multicore.erp.modules.activity.service;

import mz.multicore.erp.modules.activity.model.ActivityProfile;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Starting product categories for each activity profile (SA-C01), added when a profile is first chosen so a
 * new butcher, bakery or yard does not start from an empty catalogue. Codes are shared across companies: two
 * profiles that need "Bebidas" use the same category. Units need no seed: every unit a sector uses (kg, saco,
 * palete, grade, botija, m³…) is a system unit visible to every company.
 */
final class ActivityProfileSeeds {

    record CategorySeed(String code, String name, String colorHex) { }

    private static final String MEAT = "#B91C1C";
    private static final String FOOD = "#B45309";
    private static final String DRINK = "#0369A1";
    private static final String BUILD = "#57534E";
    private static final String GREEN = "#15803D";
    private static final String TOOLS = "#4338CA";
    private static final String FASHION = "#BE185D";
    private static final String NEUTRAL = "#475569";

    private static final CategorySeed GROCERY_DRY = seed("SA-MERCEARIA", "Mercearia seca", FOOD);
    private static final CategorySeed DRINKS = seed("SA-BEBIDAS", "Bebidas", DRINK);
    private static final CategorySeed CLEANING = seed("SA-HIGIENE", "Higiene e limpeza", NEUTRAL);
    private static final CategorySeed FRUIT = seed("SA-FRUTAS", "Frutas", GREEN);
    private static final CategorySeed VEG = seed("SA-LEGUMES", "Legumes e hortícolas", GREEN);
    private static final CategorySeed CEMENT = seed("SA-CIMENTO", "Cimento e argamassas", BUILD);
    private static final CategorySeed AGGREGATES = seed("SA-INERTES", "Inertes (areia, brita, pedra)", BUILD);
    private static final CategorySeed BLOCKS = seed("SA-BLOCOS", "Blocos e pavê", BUILD);
    private static final CategorySeed RETURNABLE = seed("SA-VASILHAME", "Vasilhame e paletes", DRINK);

    private static final Map<ActivityProfile, List<CategorySeed>> CATEGORIES = new EnumMap<>(ActivityProfile.class);

    static {
        CATEGORIES.put(ActivityProfile.GENERAL_RETAIL, List.of(seed("SA-GERAL", "Geral", NEUTRAL)));
        CATEGORIES.put(ActivityProfile.GROCERY, List.of(GROCERY_DRY, DRINKS,
                seed("SA-LACTICINIOS", "Lacticínios e frios", FOOD), CLEANING, FRUIT, VEG));
        CATEGORIES.put(ActivityProfile.WHOLESALE, List.of(GROCERY_DRY, DRINKS, CLEANING));
        CATEGORIES.put(ActivityProfile.BUTCHER, List.of(
                seed("SA-CARNE-VACA", "Carne de vaca", MEAT),
                seed("SA-CARNE-PORCO", "Carne de porco", MEAT),
                seed("SA-AVES", "Frango e aves", MEAT),
                seed("SA-ENCHIDOS", "Enchidos e charcutaria", MEAT),
                seed("SA-MIUDEZAS", "Miudezas", MEAT)));
        CATEGORIES.put(ActivityProfile.BAKERY, List.of(
                seed("SA-PAO", "Pão", FOOD),
                seed("SA-PASTELARIA", "Pastelaria", FOOD),
                seed("SA-BOLOS", "Bolos por encomenda", FOOD),
                seed("SA-MP-PADARIA", "Matérias-primas de padaria", NEUTRAL)));
        CATEGORIES.put(ActivityProfile.BUILDING_MATERIALS, List.of(CEMENT,
                seed("SA-FERRO", "Ferro e varão", BUILD), AGGREGATES, BLOCKS,
                seed("SA-COBERTURAS", "Chapas e coberturas", BUILD)));
        CATEGORIES.put(ActivityProfile.BLOCK_FACTORY, List.of(BLOCKS, CEMENT, AGGREGATES, RETURNABLE));
        CATEGORIES.put(ActivityProfile.HARDWARE, List.of(
                seed("SA-FERRAGENS", "Ferragens", TOOLS),
                seed("SA-ELECTRICO", "Material eléctrico", TOOLS),
                seed("SA-CANALIZACAO", "Canalização", TOOLS),
                seed("SA-FERRAMENTAS", "Ferramentas", TOOLS),
                seed("SA-TINTAS", "Tintas", TOOLS)));
        CATEGORIES.put(ActivityProfile.FRUIT_VEG, List.of(FRUIT, VEG));
        CATEGORIES.put(ActivityProfile.BEVERAGES, List.of(
                seed("SA-REFRIGERANTES", "Refrigerantes", DRINK),
                seed("SA-CERVEJAS", "Cervejas", DRINK),
                seed("SA-AGUAS", "Águas", DRINK), RETURNABLE));
        CATEGORIES.put(ActivityProfile.GAS, List.of(
                seed("SA-GAS", "Gás em botija", NEUTRAL),
                seed("SA-ACESS-GAS", "Acessórios de gás", NEUTRAL), RETURNABLE));
        CATEGORIES.put(ActivityProfile.FASHION, List.of(
                seed("SA-ROUPA", "Roupa", FASHION),
                seed("SA-CALCADO", "Calçado", FASHION),
                seed("SA-ACESSORIOS", "Acessórios de moda", FASHION)));
        CATEGORIES.put(ActivityProfile.AGRO, List.of(
                seed("SA-SEMENTES", "Sementes", GREEN),
                seed("SA-ADUBOS", "Adubos e fertilizantes", GREEN),
                seed("SA-RACOES", "Rações", GREEN),
                seed("SA-VETERINARIA", "Produtos veterinários", GREEN)));
        CATEGORIES.put(ActivityProfile.REPACKING, List.of(
                seed("SA-GRANEL", "Granel para reembalar", FOOD),
                seed("SA-EMBALADO", "Produto embalado", FOOD),
                seed("SA-EMBALAGENS", "Embalagens", NEUTRAL)));
        CATEGORIES.put(ActivityProfile.SERVICES, List.of(seed("SA-SERVICOS", "Serviços", NEUTRAL)));
    }

    private ActivityProfileSeeds() {
    }

    static List<CategorySeed> categories(ActivityProfile profile) {
        return CATEGORIES.getOrDefault(profile, List.of());
    }

    private static CategorySeed seed(String code, String name, String colorHex) {
        return new CategorySeed(code, name, colorHex);
    }
}
