package mz.multicore.erp.modules.activity.model;

/**
 * Business capability enabled by an activity profile (SA-C01, {@code docs/SECTORES_ACTIVIDADE_SPEC.md}).
 * Capabilities are generic and serve any sector that needs them. Disabling one only simplifies the interface;
 * it never deletes data or changes fiscal, stock or cash rules.
 */
public enum ActivityCapability {
    MULTI_UNIT_SALES("Várias unidades de medida",
            "Comprar e vender o mesmo artigo em saco, palete, kg, m³, barra, metro…"),
    PRICE_LISTS("Tabelas de preço por cliente",
            "Preços diferentes para revendedor, empreiteiro, funcionário ou cliente especial."),
    WEIGHED_SALES("Venda ao peso",
            "Balança no balcão e etiquetas de peso variável."),
    PERISHABLES("Perecíveis e validades",
            "Lotes com validade, saída do mais antigo e alertas."),
    PRODUCTION("Produção e transformação",
            "Receitas, ordens de produção, rendimento e desperdício."),
    MARKDOWNS("Remarcação de sobras",
            "Preço reduzido para artigos do dia anterior ou perto da validade."),
    CUSTOM_ORDERS("Encomendas personalizadas",
            "Encomendas com data de entrega e notas do cliente."),
    ADVANCE_PAYMENTS("Adiantamentos e sinais",
            "Receber parte do valor antes de entregar."),
    PENDING_PICKUP("Material pago por levantar",
            "O cliente paga tudo e levanta aos poucos."),
    DELIVERIES("Entregas e frete",
            "Agenda de entregas, viatura e frete cobrado."),
    CUSTOMER_PROJECTS("Obras do cliente",
            "Extracto, entregas e crédito por obra ou projecto."),
    BULK_STOCK("Stock a granel",
            "Inventário por medição (areia, brita, pedra)."),
    CONSIGNMENT("Consignação e rota",
            "Entregar a revendedores, recolher sobras e cobrar o vendido."),
    RETURNABLES("Vasilhame e retornáveis",
            "Grades, garrafas, botijas e paletes com conta-corrente."),
    VARIANTS("Variantes de artigo",
            "Tamanho, cor, espessura ou sabor do mesmo artigo.");

    private final String label;
    private final String description;

    ActivityCapability(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }
}
