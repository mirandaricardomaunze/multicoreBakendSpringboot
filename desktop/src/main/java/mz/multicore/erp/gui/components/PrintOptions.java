package mz.multicore.erp.gui.components;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Escolhas do operador no modal de impressão — impressora, cópias, intervalo de páginas,
 * orientação ("posição do documento" no papel), ajuste e cor.
 *
 * <p>Valor imutável e sem dependências de Swing: pode ser lido, guardado ({@link PrintOptionsStore})
 * e testado sem abrir janela nenhuma. A validação vive aqui — o diálogo só apresenta a mensagem.</p>
 */
public record PrintOptions(
        String printerName,
        int copies,
        Orientation orientation,
        Fit fit,
        String pageRange,
        boolean grayscale
) {

    public static final int MAX_COPIES = 99;

    /** Posição do documento no papel. {@link #AUTO} segue a forma de cada página. */
    public enum Orientation {
        AUTO("Automática"),
        PORTRAIT("Retrato"),
        LANDSCAPE("Paisagem");

        private final String label;

        Orientation(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public static Orientation byName(String name, Orientation fallback) {
            for (Orientation value : values()) {
                if (value.name().equals(name)) return value;
            }
            return fallback;
        }
    }

    /** Como a página ocupa o papel. */
    public enum Fit {
        SHRINK_TO_FIT("Ajustar à página"),
        ACTUAL_SIZE("Tamanho real");

        private final String label;

        Fit(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public static Fit byName(String name, Fit fallback) {
            for (Fit value : values()) {
                if (value.name().equals(name)) return value;
            }
            return fallback;
        }
    }

    /** Normaliza no construtor: cópias dentro dos limites e intervalo sem espaços. */
    public PrintOptions {
        copies = Math.max(1, Math.min(MAX_COPIES, copies));
        orientation = orientation == null ? Orientation.AUTO : orientation;
        fit = fit == null ? Fit.SHRINK_TO_FIT : fit;
        pageRange = pageRange == null ? "" : pageRange.trim();
    }

    /** Uma cópia, orientação automática, ajustado à página, todas as páginas, a cores. */
    public static PrintOptions defaults(String printerName) {
        return new PrintOptions(printerName, 1, Orientation.AUTO, Fit.SHRINK_TO_FIT, "", false);
    }

    public PrintOptions withPrinter(String name) {
        return new PrintOptions(name, copies, orientation, fit, pageRange, grayscale);
    }

    /** Intervalo vazio = documento inteiro. */
    public boolean printsEveryPage() {
        return pageRange.isBlank();
    }

    /**
     * Traduz o intervalo escrito pelo operador ({@code "1,3-5"}, base 1) para índices de página
     * base 0, ordenados e sem repetições. Intervalo vazio devolve o documento inteiro.
     *
     * @throws IllegalArgumentException com mensagem pronta a mostrar, se o intervalo não fizer
     *                                  sentido para um documento de {@code pageCount} páginas.
     */
    public List<Integer> resolvePages(int pageCount) {
        if (pageCount <= 0) {
            throw new IllegalArgumentException("O documento não tem páginas para imprimir.");
        }
        if (printsEveryPage()) {
            List<Integer> all = new ArrayList<>(pageCount);
            for (int index = 0; index < pageCount; index++) all.add(index);
            return all;
        }
        LinkedHashSet<Integer> pages = new LinkedHashSet<>();
        for (String part : pageRange.split(",")) {
            String piece = part.trim();
            if (piece.isEmpty()) continue;
            int dash = piece.indexOf('-');
            if (dash < 0) {
                pages.add(page(piece, pageCount) - 1);
            } else {
                int from = page(piece.substring(0, dash), pageCount);
                int to = page(piece.substring(dash + 1), pageCount);
                if (from > to) {
                    throw new IllegalArgumentException(
                            "Intervalo de páginas invertido: " + piece + ". Escreva por exemplo 2-5.");
                }
                for (int page = from; page <= to; page++) pages.add(page - 1);
            }
        }
        if (pages.isEmpty()) {
            throw new IllegalArgumentException("Indique pelo menos uma página a imprimir.");
        }
        List<Integer> ordered = new ArrayList<>(pages);
        ordered.sort(Integer::compareTo);
        return ordered;
    }

    private static int page(String raw, int pageCount) {
        String text = raw.trim();
        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Intervalo de páginas inválido: \"" + text + "\". Use o formato 1,3-5.");
        }
        if (value < 1 || value > pageCount) {
            throw new IllegalArgumentException(
                    "O documento tem " + pageCount + " página(s); a página " + value + " não existe.");
        }
        return value;
    }

    /** Resumo curto para a barra do modal (ex.: {@code "3 páginas · 2 cópias · Retrato"}). */
    public String summary(int pagesToPrint) {
        String pages = pagesToPrint == 1 ? "1 página" : pagesToPrint + " páginas";
        String copiesText = copies == 1 ? "1 cópia" : copies + " cópias";
        return pages + " · " + copiesText + " · " + orientation.label()
                + (grayscale ? " · Escala de cinzentos" : "");
    }
}
