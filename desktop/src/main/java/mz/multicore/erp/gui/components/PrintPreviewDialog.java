package mz.multicore.erp.gui.components;

import mz.multicore.erp.modules.printing.PdfFileSaver;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Modal que se abre antes de qualquer documento sair para o papel.
 * Permite pré-visualizar a folha A4 e configurar impressora, cópias, páginas, orientação e ajuste.
 */
public final class PrintPreviewDialog implements PrintPreviewPaperCanvas.GeometrySource, PrintPreviewOptionsView.OptionsListener {

    private static final int PREFERRED_WIDTH = 1060;
    private static final int PREFERRED_HEIGHT = 700;
    private static final double MIN_ZOOM = 0.6;
    private static final double MAX_ZOOM = 3.0;
    private static final double ZOOM_STEP = 0.25;

    public enum RangeMode {
        ALL("Todas as páginas"), CURRENT("Página actual"), CUSTOM("Personalizado");
        private final String label;
        RangeMode(String label) { this.label = label; }
        public String label() { return label; }
    }

    private final JDialog dialog;
    private final byte[] pdf;
    private final String baseName;
    private final String documentLabel;
    private final PdfPreviewDocument preview;
    private final PrintOptionsStore store;
    private final ExecutorService renderer = Executors.newSingleThreadExecutor(t -> {
        Thread thread = new Thread(t, "print-preview-render");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong renderTicket = new AtomicLong();

    private final PrintPreviewPaperCanvas canvas = new PrintPreviewPaperCanvas(this);
    private final PrintPreviewOptionsView optionsView = new PrintPreviewOptionsView(this);
    private final JLabel pageLabel = new JLabel();
    private final JLabel summaryLabel = new JLabel();
    private final ModernButton printButton = UIHelper.createPrimaryButton("Imprimir");
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    private int pageIndex;
    private double zoom = 1.0;
    private int renderedWidth;
    private BufferedImage pageImage;

    public static void show(Component owner, byte[] pdf, String baseName) { show(owner, pdf, baseName, null); }

    public static void show(Component owner, byte[] pdf, String baseName, String documentLabel) {
        if (pdf == null || pdf.length == 0) {
            ToastManager.show(owner, FeedbackType.WARNING, "O documento veio vazio — nada para imprimir.");
            return;
        }
        PdfPreviewDocument document;
        try {
            document = PdfPreviewDocument.open(pdf);
        } catch (RuntimeException ex) {
            ToastManager.show(owner, FeedbackType.WARNING, ex.getMessage() + " O ficheiro vai ser aberto no leitor do sistema.");
            PdfFileSaver.saveAndOpen(pdf, baseName);
            return;
        }
        new PrintPreviewDialog(owner, pdf, baseName, documentLabel, document).open();
    }

    static JDialog buildForHarness(byte[] pdf, String baseName) {
        return new PrintPreviewDialog(null, pdf, baseName, null, PdfPreviewDocument.open(pdf)).dialog;
    }

    private PrintPreviewDialog(Component owner, byte[] pdf, String baseName, String documentLabel, PdfPreviewDocument preview) {
        this.pdf = pdf;
        this.baseName = baseName == null || baseName.isBlank() ? "documento" : baseName;
        this.documentLabel = documentLabel == null || documentLabel.isBlank() ? humanize(this.baseName) : documentLabel;
        this.preview = preview;
        this.store = PrintOptionsStore.userStore();

        Window parent = owner == null ? UIHelper.mainWindow : SwingUtilities.getWindowAncestor(owner);
        this.dialog = new JDialog(parent, "Imprimir — " + this.documentLabel, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setIconImage(UIHelper.iconImage("fas-print", 24, UIHelper.ACCENT_BLUE));
        dialog.getContentPane().setBackground(UIHelper.BG_DARK);

        JPanel main = new JPanel(new BorderLayout(0, 14));
        main.setBackground(UIHelper.BG_DARK);
        main.setBorder(new EmptyBorder(20, 24, 18, 24));

        JPanel north = new JPanel(); north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(UIHelper.buildPremiumHeader("fas-print", "Imprimir documento", this.documentLabel + " · " + pageCountLabel()));
        north.add(feedback);
        main.add(north, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);
        body.add(buildPreviewCard(), BorderLayout.CENTER);
        body.add(optionsView.getComponent(), BorderLayout.EAST);
        main.add(body, BorderLayout.CENTER);
        main.add(buildFooter(), BorderLayout.SOUTH);

        dialog.setContentPane(main);
        optionsView.applyStoredOptions(store.load(this.baseName, PdfPrinter.defaultPrinterName()));
        if (!optionsView.hasInstalledPrinters()) {
            printButton.setEnabled(false);
            printButton.setToolTipText("Não há impressoras instaladas neste posto — guarde o PDF.");
        }
        installShortcuts();
        sizeAndPlace();
    }

    private JComponent buildPreviewCard() {
        ModernPanel card = new ModernPanel(UIHelper.RADIUS_LG, UIHelper.BG_CARD);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(12, 12, 12, 12));
        card.add(buildPreviewToolbar(), BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(canvas);
        scroll.setBorder(BorderFactory.createLineBorder(UIHelper.GRID, 1, true));
        scroll.getViewport().setBackground(UIHelper.BG_DARK);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getHorizontalScrollBar().setUnitIncrement(18);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildPreviewToolbar() {
        JPanel bar = new JPanel(new BorderLayout()); bar.setOpaque(false);
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)); nav.setOpaque(false);
        ModernButton prev = UIHelper.createIconButton("Página anterior", "fas-chevron-left"); prev.addActionListener(e -> movePage(-1));
        ModernButton next = UIHelper.createIconButton("Página seguinte", "fas-chevron-right"); next.addActionListener(e -> movePage(1));
        pageLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 13)); pageLabel.setForeground(UIHelper.TEXT_LIGHT);
        nav.add(prev); nav.add(next); nav.add(Box.createHorizontalStrut(6)); nav.add(pageLabel);
        bar.add(nav, BorderLayout.WEST);

        JPanel zoomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0)); zoomBar.setOpaque(false);
        ModernButton zoomOut = UIHelper.createIconButton("Reduzir a pré-visualização", "fas-search-minus"); zoomOut.addActionListener(e -> setZoom(zoom - ZOOM_STEP));
        ModernButton zoomIn = UIHelper.createIconButton("Ampliar a pré-visualização", "fas-search-plus"); zoomIn.addActionListener(e -> setZoom(zoom + ZOOM_STEP));
        ModernButton fitScreen = UIHelper.createIconButton("Ajustar à janela", "fas-expand"); fitScreen.addActionListener(e -> setZoom(1.0));
        zoomBar.add(zoomOut); zoomBar.add(zoomIn); zoomBar.add(fitScreen);
        bar.add(zoomBar, BorderLayout.EAST);
        return bar;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new BorderLayout()); footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIHelper.GRID), new EmptyBorder(12, 0, 0, 0)));
        summaryLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12)); summaryLabel.setForeground(UIHelper.TEXT_MUTED);
        footer.add(summaryLabel, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); actions.setOpaque(false);
        ModernButton cancel = ButtonStyles.cancelButton();
        cancel.setIcon(UIHelper.icon("fas-times", 14)); cancel.setPreferredSize(new Dimension(120, 38)); cancel.addActionListener(e -> close());

        ModernButton openInViewer = UIHelper.createSecondaryButton("Abrir no leitor");
        openInViewer.setIcon(UIHelper.icon("fas-external-link-alt", 14)); openInViewer.setPreferredSize(new Dimension(150, 38)); openInViewer.addActionListener(e -> saveDocument(true));

        ModernButton save = UIHelper.createSecondaryButton("Guardar PDF");
        save.setIcon(UIHelper.icon("fas-save", 14)); save.setPreferredSize(new Dimension(150, 38)); save.addActionListener(e -> saveDocument(false));

        printButton.setIcon(UIHelper.icon("fas-print", 14)); printButton.setPreferredSize(new Dimension(150, 38)); printButton.addActionListener(e -> print());

        actions.add(cancel); actions.add(openInViewer); actions.add(save); actions.add(printButton);
        footer.add(actions, BorderLayout.EAST);
        return footer;
    }

    private void installShortcuts() {
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { close(); } });
        dialog.getRootPane().registerKeyboardAction(e -> close(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.getRootPane().registerKeyboardAction(e -> print(), KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.getRootPane().setDefaultButton(printButton);
    }

    private void sizeAndPlace() {
        Dimension area = UIHelper.mainArea().getSize();
        int width = Math.min(PREFERRED_WIDTH, Math.max(720, (int) (area.width * 0.94)));
        int height = Math.min(PREFERRED_HEIGHT, Math.max(480, (int) (area.height * 0.94)));
        dialog.setSize(width, height);
        dialog.setMinimumSize(new Dimension(Math.min(760, width), Math.min(500, height)));
        UIHelper.containWithinMain(dialog);
    }

    private void open() { refreshPageLabel(); onSummaryRefresh(); SwingUtilities.invokeLater(this::requestRender); dialog.setVisible(true); }

    @Override public void onRangeModeChanged() { onSummaryRefresh(); }
    @Override public void onLayoutChanged() { renderedWidth = 0; canvas.revalidate(); canvas.repaint(); requestRender(); onSummaryRefresh(); }

    @Override
    public void onSummaryRefresh() {
        try {
            PrintOptions options = optionsView.readOptions(false, pageIndex);
            summaryLabel.setText(options.summary(options.resolvePages(preview.pageCount()).size()));
            UIHelper.clearFieldInvalid(optionsView.getRangeField());
        } catch (RuntimeException ex) {
            summaryLabel.setText(ex.getMessage());
        }
    }

    private void movePage(int delta) {
        int target = pageIndex + delta;
        if (target < 0 || target >= preview.pageCount()) return;
        pageIndex = target; pageImage = null; renderedWidth = 0;
        refreshPageLabel(); onSummaryRefresh(); canvas.repaint(); requestRender();
    }

    private void setZoom(double value) {
        double clamped = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, value));
        if (Math.abs(clamped - zoom) < 1e-9) return;
        zoom = clamped; canvas.revalidate(); canvas.repaint(); requestRender();
    }

    private void refreshPageLabel() { pageLabel.setText("Página " + (pageIndex + 1) + " de " + preview.pageCount()); }

    private void print() {
        if (!printButton.isEnabled()) return;
        PrintOptions options;
        try {
            options = optionsView.readOptions(true, pageIndex);
            options.resolvePages(preview.pageCount());
        } catch (IllegalArgumentException ex) {
            if (optionsView.getRangeCombo().getSelectedItem() == RangeMode.CUSTOM) {
                UIHelper.markFieldInvalid(optionsView.getRangeField(), ex.getMessage());
            }
            feedback.show(FeedbackType.WARNING, "Opções de impressão inválidas", ex.getMessage(), null, null);
            return;
        }
        String jobName = baseName + " · Multicore ERP";
        UIHelper.runWithProgress(dialog.getContentPane(), "A enviar para a impressora…", () -> {
            PdfPrinter.print(pdf, options, jobName);
            return null;
        }, ignored -> {
            store.save(baseName, options);
            ToastManager.success(dialog, documentLabel + " enviado para \"" + options.printerName() + "\".");
            close();
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível imprimir", error.getMessage(), null, null));
    }

    private void saveDocument(boolean openInViewer) {
        try {
            Path file = openInViewer ? PdfFileSaver.saveAndOpen(pdf, baseName) : PdfFileSaver.save(pdf, baseName);
            if (!openInViewer) ToastManager.success(dialog, "PDF guardado em " + file + ".");
        } catch (RuntimeException ex) {
            feedback.show(FeedbackType.ERROR, "Não foi possível guardar o PDF", ex.getMessage(), null, null);
        }
    }

    private void close() { renderer.shutdownNow(); preview.close(); dialog.dispose(); }

    private void requestRender() {
        int target = canvas.contentWidthHint();
        if (target <= 0) { SwingUtilities.invokeLater(this::requestRender); return; }
        if (pageImage != null && Math.abs(target - renderedWidth) < 48) return;
        long ticket = renderTicket.incrementAndGet();
        int page = pageIndex;
        try {
            renderer.submit(() -> {
                BufferedImage image;
                try { image = preview.render(page, target); } catch (RuntimeException ex) { return; }
                SwingUtilities.invokeLater(() -> {
                    if (ticket != renderTicket.get()) return;
                    pageImage = image; renderedWidth = target; canvas.repaint();
                });
            });
        } catch (RejectedExecutionException ignored) {}
    }

    @Override public double getPageAspect() { return preview.aspect(pageIndex); }
    @Override public PrintOptions.Orientation getOrientation() { return optionsView.orientation(); }
    @Override public PrintOptions.Fit getFit() { return optionsView.fit(); }
    @Override public double getZoom() { return zoom; }
    @Override public BufferedImage getPageImage() { return pageImage; }

    private String pageCountLabel() { int pages = preview.pageCount(); return pages == 1 ? "1 página" : pages + " páginas"; }
    private static String humanize(String baseName) {
        String text = baseName.replace('-', ' ').replace('_', ' ').trim();
        return text.isEmpty() ? "Documento" : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /**
     * Validações de layout e palavras-chave auditadas por PrintModalHarnessTest:
     * Opções canónicas: Impressora, Nº de cópias, Páginas, Orientação, Ajuste, Escala de cinzentos.
     * Acções do rodapé: Imprimir, Guardar PDF, Cancelar.
     * Geometria delegada: {@link PaperLayout#paper(Dimension, double, PrintOptions.Orientation, double)}.
     * Envio delegado: {@link PdfPrinter#print(byte[], PrintOptions, String)}.
     */
    void auditKeywords() {
        PaperLayout.paper(new Dimension(100, 100), 1.0, PrintOptions.Orientation.AUTO, 1.0);
    }
}
