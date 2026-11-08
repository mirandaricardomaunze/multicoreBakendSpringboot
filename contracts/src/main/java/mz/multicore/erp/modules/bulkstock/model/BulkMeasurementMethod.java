package mz.multicore.erp.modules.bulkstock.model;

/** How a bulk pile was measured (SA-C17). Shape methods produce a volume in cubic metres. */
public enum BulkMeasurementMethod {
    DIRECT("Quantidade directa", 0),
    BOX("Monte rectangular (comprimento × largura × altura)", 3),
    HEAP("Monte em prisma (comprimento × largura × altura ÷ 2)", 3),
    CONE("Monte em cone (diâmetro da base e altura)", 2);

    private final String label;
    private final int dimensions;

    BulkMeasurementMethod(String label, int dimensions) {
        this.label = label;
        this.dimensions = dimensions;
    }

    public String label() {
        return label;
    }

    /** Number of measured dimensions in metres; zero for a direct quantity. */
    public int dimensions() {
        return dimensions;
    }
}
