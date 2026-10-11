package mz.multicore.erp.modules.assets.model;

/**
 * Categorias fiscais canónicas de activos fixos / imobilizado segundo o Código do IRPC de Moçambique.
 */
public enum AssetCategory {
    BUILDINGS("Edifícios & Construções", 0.02),
    VEHICLES("Viaturas & Meios de Transporte", 0.20),
    MACHINERY("Equipamentos & Máquinas Industriais", 0.15),
    IT_EQUIPMENT("Equipamento Informático & Servidores", 0.3333),
    FURNITURE("Mobiliário & Instalações de Escritório", 0.10),
    OTHER("Outros Investimentos Corpóreos", 0.10);

    private final String label;
    private final double defaultAnnualRate;

    AssetCategory(String label, double defaultAnnualRate) {
        this.label = label;
        this.defaultAnnualRate = defaultAnnualRate;
    }

    public String label() {
        return label;
    }

    public double defaultAnnualRate() {
        return defaultAnnualRate;
    }
}
