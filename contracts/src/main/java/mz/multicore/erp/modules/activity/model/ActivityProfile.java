package mz.multicore.erp.modules.activity.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static mz.multicore.erp.modules.activity.model.ActivityCapability.*;

/**
 * Company activity profile (SA-C01). A company may hold several profiles and the active capabilities are
 * the union of their capabilities. Without a chosen profile {@link #GENERAL_RETAIL} applies, matching the
 * system's previous behaviour.
 */
public enum ActivityProfile {
    GENERAL_RETAIL("Comércio geral",
            "Loja de retalho com POS, stock e faturação.",
            EnumSet.of(PRICE_LISTS)),
    GROCERY("Mercearia e supermercado",
            "Retalho alimentar com perecíveis e venda ao grosso.",
            EnumSet.of(MULTI_UNIT_SALES, PRICE_LISTS, PERISHABLES, WEIGHED_SALES, MARKDOWNS)),
    WHOLESALE("Armazém grossista",
            "Venda em caixa, fardo e palete a revendedores, com crédito.",
            EnumSet.of(MULTI_UNIT_SALES, PRICE_LISTS, DELIVERIES, PENDING_PICKUP, ADVANCE_PAYMENTS)),
    BUTCHER("Talho e charcutaria",
            "Venda ao peso, desmancha de carcaças, quebras e encomendas.",
            EnumSet.of(WEIGHED_SALES, PERISHABLES, PRODUCTION, CARCASS_BREAKDOWN, MARKDOWNS, PRICE_LISTS,
                    CUSTOM_ORDERS, ADVANCE_PAYMENTS, MULTI_UNIT_SALES)),
    BAKERY("Padaria e pastelaria",
            "Receitas, plano de produção diário, sobras, revenda em rota e bolos por encomenda.",
            EnumSet.of(PRODUCTION, PERISHABLES, MARKDOWNS, CUSTOM_ORDERS, ADVANCE_PAYMENTS,
                    CONSIGNMENT, PRICE_LISTS, MULTI_UNIT_SALES)),
    BUILDING_MATERIALS("Estaleiro e materiais de construção",
            "Cimento, varão, areia e blocos: várias unidades, levantamentos, obras e frete.",
            EnumSet.of(MULTI_UNIT_SALES, PRICE_LISTS, PENDING_PICKUP, DELIVERIES, CUSTOMER_PROJECTS,
                    ADVANCE_PAYMENTS, BULK_STOCK, RETURNABLES)),
    BLOCK_FACTORY("Fábrica de blocos e pavê",
            "Produção com tempo de cura, entregas em obra e paletes retornáveis.",
            EnumSet.of(PRODUCTION, MULTI_UNIT_SALES, PRICE_LISTS, DELIVERIES, CUSTOMER_PROJECTS,
                    PENDING_PICKUP, ADVANCE_PAYMENTS, RETURNABLES)),
    HARDWARE("Ferragem e electricidade",
            "Venda ao metro e ao rolo, variantes e clientes empreiteiros.",
            EnumSet.of(MULTI_UNIT_SALES, PRICE_LISTS, VARIANTS, CUSTOMER_PROJECTS)),
    FRUIT_VEG("Frutaria e hortícolas",
            "Venda ao peso, perdas e remarcação diária.",
            EnumSet.of(WEIGHED_SALES, PERISHABLES, MARKDOWNS, MULTI_UNIT_SALES, PRICE_LISTS)),
    BEVERAGES("Distribuição de bebidas",
            "Grades e garrafas retornáveis, rotas de entrega e revendedores.",
            EnumSet.of(RETURNABLES, CONSIGNMENT, DELIVERIES, MULTI_UNIT_SALES, PRICE_LISTS)),
    GAS("Revenda de gás",
            "Botijas com caução, troca e entrega.",
            EnumSet.of(RETURNABLES, DELIVERIES, PRICE_LISTS)),
    FASHION("Roupa e calçado",
            "Tamanhos e cores do mesmo artigo.",
            EnumSet.of(VARIANTS, PRICE_LISTS)),
    AGRO("Loja agrícola e veterinária",
            "Sementes, adubos e rações em saco e a granel, com validades.",
            EnumSet.of(MULTI_UNIT_SALES, PERISHABLES, PRICE_LISTS, VARIANTS)),
    REPACKING("Reembalamento",
            "Granel transformado em pacotes com etiqueta, lote e validade.",
            EnumSet.of(PRODUCTION, MULTI_UNIT_SALES, PERISHABLES, WEIGHED_SALES, PRICE_LISTS)),
    SERVICES("Serviços",
            "Prestação de serviços com faturação e contratos.",
            EnumSet.of(PRICE_LISTS, ADVANCE_PAYMENTS, CUSTOMER_PROJECTS));

    private final String label;
    private final String description;
    private final Set<ActivityCapability> capabilities;

    ActivityProfile(String label, String description, Set<ActivityCapability> capabilities) {
        this.label = label;
        this.description = description;
        this.capabilities = Collections.unmodifiableSet(EnumSet.copyOf(capabilities));
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public Set<ActivityCapability> capabilities() {
        return capabilities;
    }
}
