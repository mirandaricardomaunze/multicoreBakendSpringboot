package mz.multicore.erp.gui.components;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.JobName;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Envia um PDF já gerado para uma impressora concreta, com as opções escolhidas no modal.
 *
 * <p>Não desenha nada e não decide nada: recebe {@link PrintOptions} e cumpre. Cada trabalho abre a
 * sua própria cópia do documento — a pré-visualização e a impressão nunca partilham o
 * {@code PDDocument}, que não é seguro entre threads.</p>
 *
 * <p>A <b>posição do documento</b> é resolvida folha a folha: em automático, cada página escolhe
 * retrato ou paisagem consoante a sua forma; em retrato/paisagem, o operador manda.</p>
 */
public final class PdfPrinter {

    private PdfPrinter() {
    }

    /** Impressoras instaladas no sistema, pela ordem em que o SO as devolve. */
    public static List<String> printerNames() {
        List<String> names = new ArrayList<>();
        for (PrintService service : PrintServiceLookup.lookupPrintServices(null, null)) {
            names.add(service.getName());
        }
        return names;
    }

    /** Impressora por defeito do sistema, ou {@code null} se não houver nenhuma instalada. */
    public static String defaultPrinterName() {
        PrintService service = PrintServiceLookup.lookupDefaultPrintService();
        return service == null ? null : service.getName();
    }

    public static boolean hasPrinters() {
        return !printerNames().isEmpty();
    }

    /**
     * Imprime. Chamar <b>fora do EDT</b> — o spool bloqueia.
     *
     * @param pdf      bytes do documento
     * @param options  escolhas do operador (impressora, cópias, intervalo, posição, ajuste, cor)
     * @param jobName  nome do trabalho, como aparece na fila de impressão
     * @throws IllegalArgumentException se a impressora não existir ou o intervalo não fizer sentido
     * @throws PrinterException         se o spool recusar o trabalho
     */
    public static void print(byte[] pdf, PrintOptions options, String jobName)
            throws PrinterException, IOException {
        if (pdf == null || pdf.length == 0) {
            throw new IllegalArgumentException("O documento veio vazio — nada para imprimir.");
        }
        PrintService service = resolveService(options.printerName());

        try (PDDocument source = Loader.loadPDF(pdf)) {
            List<Integer> pages = options.resolvePages(source.getNumberOfPages());
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(service);
            job.setJobName(jobName == null || jobName.isBlank() ? "Multicore ERP" : jobName);
            job.setCopies(options.copies());

            if (options.printsEveryPage()) {
                spool(job, source, options);
                return;
            }
            try (PDDocument selection = subset(source, pages)) {
                spool(job, selection, options);
            }
        }
    }

    /** Vira o que houver a virar, monta o {@link Book} e entrega ao spool. */
    private static void spool(PrinterJob job, PDDocument document, PrintOptions options)
            throws PrinterException {
        turnPages(document, options.orientation());
        job.setPageable(book(document, allPages(document), job, options));
        job.print(attributes(options, job.getJobName()));
    }

    private static PrintService resolveService(String printerName) {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        if (services.length == 0) {
            throw new IllegalArgumentException(
                    "Não há nenhuma impressora instalada neste posto. Instale uma, ou guarde o PDF.");
        }
        if (printerName != null && !printerName.isBlank()) {
            for (PrintService service : services) {
                if (service.getName().equals(printerName)) return service;
            }
            throw new IllegalArgumentException("A impressora \"" + printerName
                    + "\" já não está disponível. Escolha outra na lista.");
        }
        PrintService fallback = PrintServiceLookup.lookupDefaultPrintService();
        return fallback != null ? fallback : services[0];
    }

    /**
     * Um {@link Book} com um {@link PageFormat} por página: é assim que a posição escolhida chega
     * ao spool página a página, em vez de um único formato para o documento inteiro.
     */
    private static Book book(PDDocument document, List<Integer> pages, PrinterJob job,
                             PrintOptions options) {
        Scaling scaling = options.fit() == PrintOptions.Fit.ACTUAL_SIZE
                ? Scaling.ACTUAL_SIZE
                : Scaling.SHRINK_TO_FIT;
        PDFPrintable printable = new PDFPrintable(document, scaling, false, 0, true);
        PageFormat base = job.defaultPage();
        Book book = new Book();
        for (int page : pages) {
            PageFormat format = (PageFormat) base.clone();
            format.setOrientation(orientationFor(document.getPage(page)));
            book.append(printable, format);
        }
        return book;
    }

    /**
     * Vira as páginas que a posição escolhida obriga a virar, <b>antes</b> de as enviar.
     *
     * <p>Sem isto, escolher "Paisagem" mandava o documento em pé para uma folha deitada: o
     * {@code PDFPrintable} encolhe a página para caber e sairia um documento pequeno ao meio da
     * folha, com margens enormes — <i>diferente do que a pré-visualização mostra</i>. Um modal que
     * mostra uma coisa e imprime outra é pior do que não ter modal nenhum.</p>
     *
     * <p>A decisão de virar é a mesma que a pré-visualização usa ({@link PaperLayout#rotates}):
     * uma única fonte de verdade, para que a folha no ecrã e a folha na bandeja não possam
     * divergir.</p>
     */
    static void turnPages(PDDocument document, PrintOptions.Orientation orientation) {
        if (orientation == PrintOptions.Orientation.AUTO) {
            return;
        }
        for (PDPage page : document.getPages()) {
            if (PaperLayout.rotates(aspectOf(page), orientation)) {
                page.setRotation((page.getRotation() + 90) % 360);
            }
        }
    }

    /** Depois de {@link #turnPages}, a folha segue simplesmente a forma da página. */
    private static int orientationFor(PDPage page) {
        return aspectOf(page) > 1 ? PageFormat.LANDSCAPE : PageFormat.PORTRAIT;
    }

    /** Proporção largura/altura da página, já com a rotação declarada no PDF aplicada. */
    static double aspectOf(PDPage page) {
        PDRectangle box = page.getCropBox();
        boolean quarterTurn = (page.getRotation() / 90) % 2 != 0;
        float width = quarterTurn ? box.getHeight() : box.getWidth();
        float height = quarterTurn ? box.getWidth() : box.getHeight();
        return height <= 0 ? PaperLayout.A4_ASPECT : (double) width / height;
    }

    private static PrintRequestAttributeSet attributes(PrintOptions options, String jobName) {
        PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
        attributes.add(new Copies(options.copies()));
        attributes.add(new JobName(jobName, null));
        attributes.add(options.grayscale() ? Chromaticity.MONOCHROME : Chromaticity.COLOR);
        return attributes;
    }

    private static List<Integer> allPages(PDDocument document) {
        List<Integer> pages = new ArrayList<>(document.getNumberOfPages());
        for (int index = 0; index < document.getNumberOfPages(); index++) pages.add(index);
        return pages;
    }

    /** Documento novo só com as páginas pedidas — mantém os índices alinhados com o {@link Book}. */
    private static PDDocument subset(PDDocument source, List<Integer> pages) throws IOException {
        PDDocument selection = new PDDocument();
        for (int page : pages) {
            selection.importPage(source.getPage(page));
        }
        return selection;
    }
}
