package mz.multicore.erp.gui.components;

import mz.multicore.erp.gui.components.ShelfLabelsDialog.PackagingLevel;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.*;
import java.awt.*;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.List;

/**
 * Motor de paginação física e impressão de etiquetas via PrinterJob.
 */
final class ShelfLabelPrintEngine {

    private ShelfLabelPrintEngine() {}

    static void printLabels(Component parent, List<ProductDTO> allProducts, List<ProductDTO> selectedProducts,
                           PackagingLevel level, int copies, String batch, String expiry) {
        PrinterJob job = PrinterJob.getPrinterJob();
        final PackagingLevel finalLevel = level != null ? level : PackagingLevel.UNIT;
        final List<ProductDTO> productsToPrint = (selectedProducts != null && !selectedProducts.isEmpty())
                ? selectedProducts : allProducts;

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            int x = 10;
            int y = 10;
            for (ProductDTO p : productsToPrint) {
                for (int c = 0; c < copies; c++) {
                    JPanel card = ShelfLabelCardRenderer.createSingleLabelCard(p, finalLevel, batch, expiry);
                    card.setSize(card.getPreferredSize());
                    card.doLayout();
                    Graphics2D gCard = (Graphics2D) g2.create(x, y, card.getWidth(), card.getHeight());
                    card.paint(gCard);
                    gCard.dispose();

                    x += card.getWidth() + 10;
                    if (x + card.getWidth() > pageFormat.getImageableWidth()) {
                        x = 10;
                        y += card.getHeight() + 10;
                    }
                }
            }
            return Printable.PAGE_EXISTS;
        });

        if (job.printDialog()) {
            try {
                job.print();
                ToastManager.show(parent, FeedbackType.SUCCESS, "Etiquetas enviadas para a impressora com sucesso.");
            } catch (PrinterException ex) {
                ToastManager.show(parent, FeedbackType.ERROR, "Erro ao imprimir etiquetas: " + ex.getMessage());
            }
        }
    }
}
