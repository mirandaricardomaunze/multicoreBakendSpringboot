package mz.multicore.erp.gui.pos.contingency;

import mz.multicore.erp.modules.pos.dto.POSCheckoutLineRequest;
import mz.multicore.erp.modules.pos.dto.PosPaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.print.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Renderizador e impressor de talões provisórios de contingência para o POS.
 * Utiliza Java AWT Printable padrão, sem necessidade de bibliotecas externas de composição de PDF.
 */
public class PosThermalReceiptPrinter implements Printable {

    private static final Logger log = LoggerFactory.getLogger(PosThermalReceiptPrinter.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final PosContingencySale sale;

    public PosThermalReceiptPrinter(PosContingencySale sale) {
        this.sale = sale;
    }

    /**
     * Envia o talão directamente para a impressora padrão do sistema de forma assíncrona.
     */
    public static void printSilent(PosContingencySale sale) {
        if (sale == null) return;
        Thread.ofVirtual().start(() -> {
            try {
                PrinterJob job = PrinterJob.getPrinterJob();
                if (job.getPrintService() == null) {
                    log.warn("Nenhuma impressora configurada no sistema para talão de contingência.");
                    return;
                }
                PageFormat pf = job.defaultPage();
                Paper paper = pf.getPaper();
                // 80mm térmico = aproximadamente 226 pt de largura, comprimento variável
                paper.setSize(226, 842);
                paper.setImageableArea(5, 5, 216, 832);
                pf.setPaper(paper);

                job.setPrintable(new PosThermalReceiptPrinter(sale), pf);
                job.print();
                log.info("Talão de contingência impresso com sucesso: {}", sale.contingencyReference());
            } catch (PrinterException e) {
                log.warn("Não foi possível imprimir talão térmico de contingência: {}", e.getMessage());
            } catch (Throwable t) {
                log.error("Erro inesperado ao imprimir talão de contingência", t);
            }
        });
    }

    /**
     * Formata o talão como texto simples para pré-visualização ou fallback.
     */
    public static String formatReceiptText(PosContingencySale sale) {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("            MULTICORE ERP               \n");
        sb.append("      TALÃO PROVISÓRIO DE CAIXA         \n");
        sb.append("       (REGIME DE CONTINGÊNCIA)         \n");
        sb.append("========================================\n");
        sb.append("Ref. Contingência: ").append(sale.contingencyReference()).append("\n");
        sb.append("Data/Hora:         ").append(sale.createdAt().format(DATE_FORMAT)).append("\n");
        sb.append("Operador:          ").append(sale.request().operator()).append("\n");
        String cliente = sale.request().walkInName() != null && !sale.request().walkInName().isBlank()
                ? sale.request().walkInName() : "Consumidor Final";
        sb.append("Cliente:           ").append(cliente).append("\n");
        sb.append("----------------------------------------\n");
        sb.append(String.format("%-4s %-20s %12s\n", "Qtd", "Item (ID)", "Total MT"));
        sb.append("----------------------------------------\n");

        for (POSCheckoutLineRequest line : sale.request().lines()) {
            BigDecimal qty = line.quantity() != null ? line.quantity() : BigDecimal.ONE;
            sb.append(String.format("%-4.0f Artigo ID #%-16d\n", qty, line.productId()));
        }

        sb.append("----------------------------------------\n");
        sb.append(String.format("TOTAL DA VENDA:           %12.2f MT\n", sale.cartTotal()));
        sb.append("PAGAMENTO: ").append(formatPaymentSummary(sale.request().payments())).append("\n");
        sb.append("========================================\n");
        sb.append("      AVISO FISCAL REGULAMENTAR:        \n");
        sb.append("   DOCUMENTO EMITIDO EM CONTINGÊNCIA    \n");
        sb.append("  AGUARDA SINCRONIZAÇÃO COM O SERVIDOR  \n");
        sb.append("   FATURA FISCAL OFICIAL SERÁ GERADA    \n");
        sb.append("      ASSIM QUE A REDE RETOMAR          \n");
        sb.append("========================================\n");
        return sb.toString();
    }

    private static String formatPaymentSummary(List<PosPaymentRequest> payments) {
        if (payments == null || payments.isEmpty()) return "Numerário (Caixa)";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < payments.size(); i++) {
            PosPaymentRequest p = payments.get(i);
            if (i > 0) sb.append(", ");
            sb.append(p.method()).append(" (").append(p.amount()).append(" MT)");
        }
        return sb.toString();
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) return NO_SUCH_PAGE;

        Graphics2D g2 = (Graphics2D) graphics;
        g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        g2.setColor(Color.BLACK);

        Font titleFont = new Font(Font.MONOSPACED, Font.BOLD, 10);
        Font boldFont = new Font(Font.MONOSPACED, Font.BOLD, 8);
        Font normalFont = new Font(Font.MONOSPACED, Font.PLAIN, 7);

        int y = 15;
        g2.setFont(titleFont);
        drawCentered(g2, "MULTICORE ERP", 216, y);
        y += 12;
        g2.setFont(boldFont);
        drawCentered(g2, "TALÃO PROVISÓRIO DE CAIXA", 216, y);
        y += 10;
        drawCentered(g2, "(REGIME DE CONTINGÊNCIA)", 216, y);
        y += 12;

        g2.drawLine(0, y, 216, y);
        y += 10;

        g2.setFont(normalFont);
        g2.drawString("Ref: " + sale.contingencyReference(), 2, y);
        y += 9;
        g2.drawString("Data: " + sale.createdAt().format(DATE_FORMAT), 2, y);
        y += 9;
        g2.drawString("Operador: " + sale.request().operator(), 2, y);
        y += 9;
        String cliente = sale.request().walkInName() != null && !sale.request().walkInName().isBlank()
                ? sale.request().walkInName() : "Consumidor Final";
        g2.drawString("Cliente: " + cliente, 2, y);
        y += 10;

        g2.drawLine(0, y, 216, y);
        y += 10;

        g2.setFont(boldFont);
        g2.drawString("Qtd  Artigo", 2, y);
        g2.drawString("Total MT", 160, y);
        y += 9;
        g2.setFont(normalFont);

        for (POSCheckoutLineRequest line : sale.request().lines()) {
            BigDecimal qty = line.quantity() != null ? line.quantity() : BigDecimal.ONE;
            String itemText = String.format("%.0fx Artigo #%d", qty, line.productId());
            g2.drawString(itemText, 2, y);
            y += 9;
            if (y > 750) break; // limite de segurança
        }

        y += 4;
        g2.drawLine(0, y, 216, y);
        y += 12;

        g2.setFont(titleFont);
        g2.drawString("TOTAL:", 2, y);
        String totalFormatted = String.format("%.2f MT", sale.cartTotal());
        g2.drawString(totalFormatted, 130, y);
        y += 14;

        g2.setFont(normalFont);
        g2.drawString("Pgto: " + formatPaymentSummary(sale.request().payments()), 2, y);
        y += 12;

        g2.drawLine(0, y, 216, y);
        y += 10;

        g2.setFont(boldFont);
        drawCentered(g2, "* REGIME DE CONTINGÊNCIA *", 216, y);
        y += 9;
        g2.setFont(normalFont);
        drawCentered(g2, "Aguardando sincronização fiscal.", 216, y);
        y += 9;
        drawCentered(g2, "Obrigado pela sua preferência!", 216, y);

        return PAGE_EXISTS;
    }

    private void drawCentered(Graphics2D g2, String text, int width, int y) {
        FontMetrics fm = g2.getFontMetrics();
        int x = Math.max(2, (width - fm.stringWidth(text)) / 2);
        g2.drawString(text, x, y);
    }
}
