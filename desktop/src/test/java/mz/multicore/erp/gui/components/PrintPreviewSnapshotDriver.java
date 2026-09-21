package mz.multicore.erp.gui.components;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Fotografa o modal de impressão para inspecção visual — em retrato e em paisagem.
 *
 * <p>Driver manual, como o {@code PanelScreenshotDriver}: <b>não</b> é harness e não corre na
 * suite. Serve para olhar para o diálogo quando se lhe mexe no desenho.</p>
 *
 * <pre>
 *   mvn -o -pl desktop test-compile
 *   java -cp "desktop/target/classes;desktop/target/test-classes;LIBS" \
 *        mz.multicore.erp.gui.components.PrintPreviewSnapshotDriver &lt;pasta-de-saida&gt;
 * </pre>
 */
public final class PrintPreviewSnapshotDriver {

    private static final int WIDTH = 1000;
    private static final int HEIGHT = 660;

    private PrintPreviewSnapshotDriver() {
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : ".");
        JDialog dialog = PrintPreviewDialog.buildForHarness(samplePdf(), "fatura-FT2026-14");
        JComponent content = (JComponent) dialog.getContentPane();
        content.setSize(WIDTH, HEIGHT);
        layoutTree(content);

        Thread.sleep(1500); // deixa o render de fundo terminar
        write(content, new File(out, "modal-impressao.png"));

        selectLandscape(content);
        Thread.sleep(1500);
        write(content, new File(out, "modal-impressao-paisagem.png"));

        dialog.dispose();
        System.exit(0);
    }

    private static void write(JComponent content, File file) throws Exception {
        layoutTree(content);
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        content.paint(graphics);
        graphics.dispose();
        ImageIO.write(image, "png", file);
        System.out.println("Gravado: " + file);
    }

    @SuppressWarnings("unchecked")
    private static void selectLandscape(Container container) {
        for (Component child : container.getComponents()) {
            if (child instanceof JComboBox<?> combo && combo.getItemCount() > 0
                    && combo.getItemAt(0) instanceof PrintOptions.Orientation) {
                ((JComboBox<PrintOptions.Orientation>) combo)
                        .setSelectedItem(PrintOptions.Orientation.LANDSCAPE);
                return;
            }
            if (child instanceof Container nested) {
                selectLandscape(nested);
            }
        }
    }

    private static void layoutTree(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) {
                layoutTree(nested);
            }
        }
    }

    private static byte[] samplePdf() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, output);
        document.open();
        document.add(new Paragraph("MULTICORE ERP — Factura FT2026/14"));
        for (int line = 1; line <= 12; line++) {
            document.add(new Paragraph("Linha " + line + " do documento de teste"));
        }
        document.close();
        return output.toByteArray();
    }
}
