package mz.multicore.erp.gui.components;

import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * Geometria da pré-visualização: onde fica a folha dentro da área de desenho, se a página precisa
 * de ser virada e onde assenta dentro da folha.
 *
 * <p>Está separada do diálogo de propósito — é a parte da pré-visualização que se pode errar em
 * silêncio (uma página encostada ao canto, cortada, ou "de lado" sem o operador reparar) e é a
 * única que se testa sem abrir janela nenhuma.</p>
 */
public final class PaperLayout {

    /** Proporção largura/altura de uma folha A4 em retrato. */
    public static final double A4_ASPECT = 210.0 / 297.0;

    /** Margem entre a folha e os limites da área de desenho, em pixéis. */
    private static final int MARGIN = 24;

    private PaperLayout() {
    }

    /**
     * Proporção da folha depois de aplicada a posição escolhida. Retrato e paisagem <b>viram</b> a
     * folha do próprio documento em vez de a forçar a A4 — um recibo térmico de 80&nbsp;mm continua
     * estreito, como sai da impressora.
     */
    public static double paperAspect(double pageAspect, PrintOptions.Orientation orientation) {
        double safe = pageAspect > 0 ? pageAspect : A4_ASPECT;
        return switch (orientation) {
            case AUTO -> safe;
            case PORTRAIT -> safe <= 1 ? safe : 1 / safe;
            case LANDSCAPE -> safe >= 1 ? safe : 1 / safe;
        };
    }

    /** {@code true} quando a página tem de ser rodada 90° para assentar na folha escolhida. */
    public static boolean rotates(double pageAspect, PrintOptions.Orientation orientation) {
        double safe = pageAspect > 0 ? pageAspect : A4_ASPECT;
        return Math.abs(paperAspect(safe, orientation) - safe) > 1e-9;
    }

    /** Proporção da página <i>já com a rotação aplicada</i> — é esta que se desenha na folha. */
    public static double contentAspect(double pageAspect, PrintOptions.Orientation orientation) {
        double safe = pageAspect > 0 ? pageAspect : A4_ASPECT;
        return rotates(safe, orientation) ? 1 / safe : safe;
    }

    /**
     * Rectângulo da folha, centrado na área disponível.
     *
     * @param area        área de desenho, em pixéis
     * @param pageAspect  proporção largura/altura da página do PDF
     * @param orientation posição escolhida pelo operador
     * @param zoom        1.0 = folha ajustada à área; &gt;1 amplia
     */
    public static Rectangle paper(Dimension area, double pageAspect,
                                  PrintOptions.Orientation orientation, double zoom) {
        double aspect = paperAspect(pageAspect, orientation);
        int availableWidth = Math.max(1, area.width - 2 * MARGIN);
        int availableHeight = Math.max(1, area.height - 2 * MARGIN);

        int width = availableWidth;
        int height = (int) Math.round(width / aspect);
        if (height > availableHeight) {
            height = availableHeight;
            width = (int) Math.round(height * aspect);
        }
        width = Math.max(1, (int) Math.round(width * zoom));
        height = Math.max(1, (int) Math.round(height * zoom));

        int x = Math.max(MARGIN, (area.width - width) / 2);
        int y = Math.max(MARGIN, (area.height - height) / 2);
        return new Rectangle(x, y, width, height);
    }

    /**
     * Rectângulo da página dentro da folha. Com {@link PrintOptions.Fit#SHRINK_TO_FIT} a página é
     * reduzida até caber inteira, com a margem que a impressora não consegue imprimir; com
     * {@code ACTUAL_SIZE} mantém a escala real e fica centrada — se transbordar, o excesso sai
     * cortado, exactamente como sairia no papel.
     *
     * @param contentAspect proporção da página já rodada ({@link #contentAspect})
     * @param actualScale   tamanho real da página em fracção da largura da folha (1.0 = folha cheia)
     */
    public static Rectangle content(Rectangle paper, double contentAspect, PrintOptions.Fit fit,
                                    double actualScale) {
        double aspect = contentAspect > 0 ? contentAspect : A4_ASPECT;
        int width;
        int height;
        if (fit == PrintOptions.Fit.ACTUAL_SIZE) {
            width = (int) Math.round(paper.width * (actualScale > 0 ? actualScale : 1.0));
            height = (int) Math.round(width / aspect);
        } else {
            int margin = innerMargin(paper);
            int usableWidth = Math.max(1, paper.width - 2 * margin);
            int usableHeight = Math.max(1, paper.height - 2 * margin);
            width = usableWidth;
            height = (int) Math.round(width / aspect);
            if (height > usableHeight) {
                height = usableHeight;
                width = (int) Math.round(height * aspect);
            }
        }
        width = Math.max(1, width);
        height = Math.max(1, height);
        int x = paper.x + (paper.width - width) / 2;
        int y = paper.y + (paper.height - height) / 2;
        return new Rectangle(x, y, width, height);
    }

    private static int innerMargin(Rectangle paper) {
        return Math.max(4, Math.round(paper.width * 0.04f));
    }
}
