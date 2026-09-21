package mz.multicore.erp.gui.components;

import mz.multicore.erp.modules.printing.PdfFileSaver;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Modal que se abre <b>antes</b> de qualquer documento sair para o papel.
 *
 * <p>Imprimir é irreversível: consome papel, consome a etiqueta e, num recibo, consome um número.
 * Até aqui o ERP gerava o PDF e entregava-o ao leitor do sistema operativo — o operador não
 * escolhia impressora, não escolhia cópias, e só via o que tinha mandado imprimir depois de a
 * folha sair. Este diálogo põe a decisão antes do acto: <b>vê-se</b> o documento na folha, com a
 * posição escolhida, e só depois se imprime.</p>
 *
 * <p>Composição, nada mais: a validação vive no {@link PrintOptions}, a geometria da folha no
 * {@link PaperLayout}, o desenho das páginas no {@link PdfPreviewDocument}, o envio para a fila no
 * {@link PdfPrinter} e a memória das escolhas no {@link PrintOptionsStore}.</p>
 *
 * <p>Uso canónico — substitui sempre a gravação directa do PDF:</p>
 * <pre>
 *   UIHelper.runWithProgress(this, "A gerar factura em PDF…",
 *           () -&gt; apiClient.renderInvoice(id),
 *           pdf -&gt; PrintPreviewDialog.show(this, pdf, "fatura-" + number),
 *           error -&gt; showError("gerar factura", error));
 * </pre>
 */
public final class PrintPreviewDialog {

    private static final int PREFERRED_WIDTH = 1060;
    private static final int PREFERRED_HEIGHT = 700;
    /**
     * Largura da coluna de opções. Não é um número solto: abaixo disto as combos emparelhadas
     * cortam o texto ("Todas as pá…"), que é o defeito que a spec dos inputs manda evitar.
     */
    private static final int OPTIONS_WIDTH = 396;
    private static final double MIN_ZOOM = 0.6;
    private static final double MAX_ZOOM = 3.0;
    private static final double ZOOM_STEP = 0.25;

    private final JDialog dialog;
    private final byte[] pdf;
    private final String baseName;
    private final String documentLabel;
    private final PdfPreviewDocument preview;
    private final PrintOptionsStore store;
    private final ExecutorService renderer = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "print-preview-render");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong renderTicket = new AtomicLong();

    private final PaperCanvas canvas = new PaperCanvas();
    private final JComboBox<String> printerCombo = new JComboBox<>();
    private final IntegerField copiesField =
            new IntegerField("1", 1, PrintOptions.MAX_COPIES, "Nº de cópias");
    private final JComboBox<RangeMode> rangeCombo = new JComboBox<>(RangeMode.values());
    private final JTextField rangeField = new JTextField();
    private final JComboBox<PrintOptions.Orientation> orientationCombo =
            new JComboBox<>(PrintOptions.Orientation.values());
    private final JComboBox<PrintOptions.Fit> fitCombo = new JComboBox<>(PrintOptions.Fit.values());
    private final JCheckBox grayscaleCheck = new JCheckBox("Escala de cinzentos");
    private final JLabel pageLabel = new JLabel();
    private final JLabel summaryLabel = new JLabel();
    private final ModernButton printButton = UIHelper.createPrimaryButton("Imprimir");
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    private int pageIndex;
    private double zoom = 1.0;
    private int renderedWidth;
    private BufferedImage pageImage;

    /** Modos de intervalo oferecidos ao operador. */
    private enum RangeMode {
        ALL("Todas as páginas"),
        CURRENT("Página actual"),
        CUSTOM("Personalizado");

        private final String label;

        RangeMode(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }

    /**
     * Abre o modal de impressão para um PDF já gerado.
     *
     * @param owner    componente a partir do qual o modal é centrado
     * @param pdf      bytes do documento
     * @param baseName nome-base do ficheiro (ex.: {@code "fatura-FT2026-14"}); dá o nome ao
     *                 trabalho de impressão, ao ficheiro guardado e à família de opções lembradas
     */
    public static void show(Component owner, byte[] pdf, String baseName) {
        show(owner, pdf, baseName, null);
    }

    /**
     * @param documentLabel título legível do documento no cabeçalho; se {@code null}, é derivado
     *                      do {@code baseName}
     */
    public static void show(Component owner, byte[] pdf, String baseName, String documentLabel) {
        if (pdf == null || pdf.length == 0) {
            ToastManager.show(owner, FeedbackType.WARNING, "O documento veio vazio — nada para imprimir.");
            return;
        }
        PdfPreviewDocument document;
        try {
            document = PdfPreviewDocument.open(pdf);
        } catch (RuntimeException ex) {
            // Pré-visualização indisponível não pode deixar o operador sem o documento.
            ToastManager.show(owner, FeedbackType.WARNING,
                    ex.getMessage() + " O ficheiro vai ser aberto no leitor do sistema.");
            PdfFileSaver.saveAndOpen(pdf, baseName);
            return;
        }
        new PrintPreviewDialog(owner, pdf, baseName, documentLabel, document).open();
    }

    /**
     * Monta o modal <b>sem o mostrar</b>. Existe para o harness poder provar o que só se descobre
     * ao construir a janela: que os códigos de ícone existem, que o esqueleto monta e que a folha
     * pinta. Não é caminho de produção — os ecrãs entram sempre por {@link #show}.
     */
    static JDialog buildForHarness(byte[] pdf, String baseName) {
        return new PrintPreviewDialog(null, pdf, baseName, null, PdfPreviewDocument.open(pdf)).dialog;
    }

    private PrintPreviewDialog(Component owner, byte[] pdf, String baseName, String documentLabel,
                               PdfPreviewDocument preview) {
        this.pdf = pdf;
        this.baseName = baseName == null || baseName.isBlank() ? "documento" : baseName;
        this.documentLabel = documentLabel == null || documentLabel.isBlank()
                ? humanize(this.baseName) : documentLabel;
        this.preview = preview;
        this.store = PrintOptionsStore.userStore();

        Window parent = owner == null ? UIHelper.mainWindow : SwingUtilities.getWindowAncestor(owner);
        this.dialog = new JDialog(parent, "Imprimir — " + this.documentLabel,
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setIconImage(UIHelper.iconImage("fas-print", 24, UIHelper.ACCENT_BLUE));
        dialog.getContentPane().setBackground(UIHelper.BG_DARK);

        JPanel main = new JPanel(new BorderLayout(0, 14));
        main.setBackground(UIHelper.BG_DARK);
        main.setBorder(new EmptyBorder(20, 24, 18, 24));
        JPanel north = new JPanel(); north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(UIHelper.buildPremiumHeader("fas-print", "Imprimir documento",
                this.documentLabel + " · " + pageCountLabel()));
        north.add(feedback);
        main.add(north, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);
        body.add(buildPreviewCard(), BorderLayout.CENTER);
        body.add(buildOptionsCard(), BorderLayout.EAST);
        main.add(body, BorderLayout.CENTER);
        main.add(buildFooter(), BorderLayout.SOUTH);

        dialog.setContentPane(main);
        applyStoredOptions();
        installShortcuts();
        sizeAndPlace();
    }

    // ── Construção da interface ───────────────────────────────────────────────────────────────

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
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);

        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        navigation.setOpaque(false);
        ModernButton previous = UIHelper.createIconButton("Página anterior", "fas-chevron-left");
        previous.addActionListener(event -> movePage(-1));
        ModernButton next = UIHelper.createIconButton("Página seguinte", "fas-chevron-right");
        next.addActionListener(event -> movePage(1));
        pageLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        pageLabel.setForeground(UIHelper.TEXT_LIGHT);
        navigation.add(previous);
        navigation.add(next);
        navigation.add(Box.createHorizontalStrut(6));
        navigation.add(pageLabel);
        bar.add(navigation, BorderLayout.WEST);

        JPanel zoomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        zoomBar.setOpaque(false);
        ModernButton zoomOut = UIHelper.createIconButton("Reduzir a pré-visualização", "fas-search-minus");
        zoomOut.addActionListener(event -> changeZoom(-ZOOM_STEP));
        ModernButton zoomIn = UIHelper.createIconButton("Ampliar a pré-visualização", "fas-search-plus");
        zoomIn.addActionListener(event -> changeZoom(ZOOM_STEP));
        ModernButton fitScreen = UIHelper.createIconButton("Ajustar à janela", "fas-expand");
        fitScreen.addActionListener(event -> setZoom(1.0));
        zoomBar.add(zoomOut);
        zoomBar.add(zoomIn);
        zoomBar.add(fitScreen);
        bar.add(zoomBar, BorderLayout.EAST);
        return bar;
    }

    private JComponent buildOptionsCard() {
        ModernPanel card = new ModernPanel(UIHelper.RADIUS_LG, UIHelper.BG_CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setPreferredSize(new Dimension(OPTIONS_WIDTH, 10));

        OptionsColumn fields = new OptionsColumn();

        fields.add(sectionRow(new SectionHeader("Impressora", "fas-print")));
        printerCombo.setRenderer(UIHelper.labelRenderer(name -> name));
        UIHelper.styleComboBox(printerCombo);
        loadPrinters();
        fields.add(field("Destino", printerCombo));

        fields.add(Box.createVerticalStrut(4));
        fields.add(sectionRow(new SectionHeader("Trabalho", "fas-layer-group")));
        rangeCombo.setRenderer(UIHelper.labelRenderer(RangeMode::label));
        UIHelper.styleComboBox(rangeCombo);
        rangeCombo.addActionListener(event -> onRangeModeChanged());
        UIHelper.onTextChange(copiesField, this::refreshSummary);
        fields.add(pair("Nº de cópias", copiesField, "Páginas", rangeCombo));

        UIHelper.styleTextField(rangeField);
        rangeField.setEnabled(false);
        rangeField.setToolTipText("Exemplo: 1,3-5");
        fields.add(field("Intervalo (ex.: 1,3-5)", rangeField));

        fields.add(Box.createVerticalStrut(4));
        fields.add(sectionRow(new SectionHeader("Posição no papel", "fas-file-alt")));
        orientationCombo.setRenderer(UIHelper.labelRenderer(PrintOptions.Orientation::label));
        UIHelper.styleComboBox(orientationCombo);
        orientationCombo.addActionListener(event -> onLayoutChanged());
        fitCombo.setRenderer(UIHelper.labelRenderer(PrintOptions.Fit::label));
        UIHelper.styleComboBox(fitCombo);
        fitCombo.addActionListener(event -> onLayoutChanged());
        fields.add(pair("Orientação", orientationCombo, "Ajuste", fitCombo));

        grayscaleCheck.setOpaque(false);
        grayscaleCheck.setForeground(UIHelper.TEXT_LIGHT);
        grayscaleCheck.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        grayscaleCheck.addActionListener(event -> refreshSummary());
        JPanel grayscaleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
        grayscaleRow.setOpaque(false);
        grayscaleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        grayscaleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        grayscaleRow.add(grayscaleCheck);
        fields.add(grayscaleRow);

        // Responsivo, como os ModernFormDialog: numa janela baixa as últimas opções (ajuste,
        // escala de cinzentos) ficavam fora do cartão e o operador não lhes chegava.
        JScrollPane scroll = new JScrollPane(fields);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setUI(new SlimScrollBarUI());
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JComponent sectionRow(SectionHeader header) {
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, header.getPreferredSize().height + 4));
        return header;
    }

    /**
     * Coluna de opções que acompanha a largura do viewport. Sem isto, o {@code BoxLayout} pede a
     * largura do campo mais largo e o {@code JScrollPane} — sem barra horizontal, de propósito —
     * cortava a seta das combos da direita fora do cartão.
     */
    private static final class OptionsColumn extends JPanel implements javax.swing.Scrollable {

        private OptionsColumn() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

        @Override public int getScrollableUnitIncrement(Rectangle view, int axis, int direction) { return 16; }

        @Override public int getScrollableBlockIncrement(Rectangle view, int axis, int direction) { return 64; }

        @Override public boolean getScrollableTracksViewportWidth() { return true; }

        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /** Duas opções irmãs lado a lado — poupa altura e agrupa o que se decide em conjunto. */
    private JComponent pair(String leftLabel, JComponent left, String rightLabel, JComponent right) {
        JPanel row = new JPanel(new java.awt.GridLayout(1, 2, 12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(new EmptyBorder(2, 0, 2, 0));
        FormField leftField = new FormField(leftLabel, left, false, null);
        row.add(leftField);
        row.add(new FormField(rightLabel, right, false, null));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, leftField.getPreferredSize().height + 6));
        return row;
    }

    private JComponent field(String label, JComponent input) {
        FormField formField = new FormField(label, input, false, null);
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(new EmptyBorder(2, 0, 2, 0));
        row.add(formField, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, formField.getPreferredSize().height + 6));
        return row;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIHelper.GRID),
                new EmptyBorder(12, 0, 0, 0)));

        summaryLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        summaryLabel.setForeground(UIHelper.TEXT_MUTED);
        footer.add(summaryLabel, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        ModernButton cancel = UIHelper.createSecondaryButton("Cancelar");
        cancel.setIcon(UIHelper.icon("fas-times", 14));
        cancel.setPreferredSize(new Dimension(120, 38));
        cancel.addActionListener(event -> close());

        ModernButton openInViewer = UIHelper.createSecondaryButton("Abrir no leitor");
        openInViewer.setIcon(UIHelper.icon("fas-external-link-alt", 14));
        openInViewer.setPreferredSize(new Dimension(150, 38));
        openInViewer.setToolTipText("Guardar e abrir no leitor de PDF do sistema");
        openInViewer.addActionListener(event -> saveDocument(true));

        ModernButton save = UIHelper.createSecondaryButton("Guardar PDF");
        save.setIcon(UIHelper.icon("fas-save", 14));
        save.setPreferredSize(new Dimension(150, 38));
        save.addActionListener(event -> saveDocument(false));

        printButton.setIcon(UIHelper.icon("fas-print", 14));
        printButton.setPreferredSize(new Dimension(150, 38));
        printButton.addActionListener(event -> print());

        actions.add(cancel);
        actions.add(openInViewer);
        actions.add(save);
        actions.add(printButton);
        footer.add(actions, BorderLayout.EAST);
        return footer;
    }

    private void installShortcuts() {
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                close();
            }
        });
        dialog.getRootPane().registerKeyboardAction(event -> close(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.getRootPane().registerKeyboardAction(event -> print(),
                KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
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

    private void open() {
        refreshPageLabel();
        refreshSummary();
        SwingUtilities.invokeLater(this::requestRender);
        dialog.setVisible(true);
    }

    // ── Estado e acções ───────────────────────────────────────────────────────────────────────

    private void loadPrinters() {
        List<String> printers = PdfPrinter.printerNames();
        printerCombo.removeAllItems();
        for (String printer : printers) {
            printerCombo.addItem(printer);
        }
        if (printers.isEmpty()) {
            printerCombo.addItem("Nenhuma impressora instalada");
            printerCombo.setEnabled(false);
            printButton.setEnabled(false);
            printButton.setToolTipText("Não há impressoras instaladas neste posto — guarde o PDF.");
        }
    }

    private void applyStoredOptions() {
        PrintOptions stored = store.load(baseName, PdfPrinter.defaultPrinterName());
        if (stored.printerName() != null && printerCombo.isEnabled()) {
            printerCombo.setSelectedItem(stored.printerName());
        }
        copiesField.setText(String.valueOf(stored.copies()));
        orientationCombo.setSelectedItem(stored.orientation());
        fitCombo.setSelectedItem(stored.fit());
        grayscaleCheck.setSelected(stored.grayscale());
    }

    private void onRangeModeChanged() {
        rangeField.setEnabled(rangeCombo.getSelectedItem() == RangeMode.CUSTOM);
        if (rangeCombo.getSelectedItem() != RangeMode.CUSTOM) {
            UIHelper.clearFieldInvalid(rangeField);
        }
        refreshSummary();
    }

    private void onLayoutChanged() {
        renderedWidth = 0; // a folha mudou de forma: vale a pena redesenhar à medida certa
        canvas.revalidate();
        canvas.repaint();
        requestRender();
        refreshSummary();
    }

    private void movePage(int delta) {
        int target = pageIndex + delta;
        if (target < 0 || target >= preview.pageCount()) {
            return;
        }
        pageIndex = target;
        pageImage = null;
        renderedWidth = 0;
        refreshPageLabel();
        refreshSummary();
        canvas.repaint();
        requestRender();
    }

    private void changeZoom(double delta) {
        setZoom(zoom + delta);
    }

    private void setZoom(double value) {
        double clamped = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, value));
        if (Math.abs(clamped - zoom) < 1e-9) {
            return;
        }
        zoom = clamped;
        canvas.revalidate();
        canvas.repaint();
        requestRender();
    }

    private void refreshPageLabel() {
        pageLabel.setText("Página " + (pageIndex + 1) + " de " + preview.pageCount());
    }

    private void refreshSummary() {
        try {
            PrintOptions options = readOptions(false);
            summaryLabel.setText(options.summary(options.resolvePages(preview.pageCount()).size()));
            UIHelper.clearFieldInvalid(rangeField);
        } catch (RuntimeException ex) {
            summaryLabel.setText(ex.getMessage());
        }
    }

    /**
     * Lê os controlos. Com {@code strict}, os erros de validação sobem com a mensagem pronta a
     * mostrar; sem {@code strict} (resumo em tempo real) um nº de cópias inválido conta como uma.
     */
    private PrintOptions readOptions(boolean strict) {
        int copies;
        try {
            copies = copiesField.value();
        } catch (IllegalArgumentException ex) {
            if (strict) {
                throw ex;
            }
            copies = 1;
        }
        RangeMode mode = (RangeMode) rangeCombo.getSelectedItem();
        String range = switch (mode == null ? RangeMode.ALL : mode) {
            case ALL -> "";
            case CURRENT -> String.valueOf(pageIndex + 1);
            case CUSTOM -> rangeField.getText() == null ? "" : rangeField.getText().trim();
        };
        Object printer = printerCombo.isEnabled() ? printerCombo.getSelectedItem() : null;
        return new PrintOptions(
                printer == null ? null : printer.toString(),
                copies,
                orientation(),
                fit(),
                range,
                grayscaleCheck.isSelected());
    }

    private void print() {
        if (!printButton.isEnabled()) {
            return;
        }
        PrintOptions options;
        try {
            options = readOptions(true);
            options.resolvePages(preview.pageCount());
        } catch (IllegalArgumentException ex) {
            if (rangeCombo.getSelectedItem() == RangeMode.CUSTOM) {
                UIHelper.markFieldInvalid(rangeField, ex.getMessage());
            }
            feedback.show(FeedbackType.WARNING, "Opções de impressão inválidas", ex.getMessage(), null, null);
            return;
        }
        String jobName = baseName + " · Multicore ERP";
        UIHelper.runWithProgress(dialog.getContentPane(), "A enviar para a impressora…",
                () -> {
                    PdfPrinter.print(pdf, options, jobName);
                    return null;
                },
                ignored -> {
                    store.save(baseName, options);
                    ToastManager.success(dialog, documentLabel + " enviado para \"" + options.printerName() + "\".");
                    close();
                },
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível imprimir",
                        error.getMessage(), null, null));
    }

    private void saveDocument(boolean openInViewer) {
        try {
            Path file = openInViewer
                    ? PdfFileSaver.saveAndOpen(pdf, baseName)
                    : PdfFileSaver.save(pdf, baseName);
            if (!openInViewer) {
                ToastManager.success(dialog, "PDF guardado em " + file + ".");
            }
        } catch (RuntimeException ex) {
            feedback.show(FeedbackType.ERROR, "Não foi possível guardar o PDF", ex.getMessage(), null, null);
        }
    }

    private void close() {
        renderer.shutdownNow();
        preview.close();
        dialog.dispose();
    }

    // ── Desenho da pré-visualização ───────────────────────────────────────────────────────────

    /**
     * Pede o desenho da página actual à largura em que ela vai ser mostrada. Corre fora do EDT,
     * numa única thread — o {@code PDDocument} não é seguro entre threads — e um resultado que já
     * não interessa (o operador mudou de página entretanto) é descartado pelo bilhete.
     */
    private void requestRender() {
        int target = canvas.contentWidthHint();
        if (target <= 0) {
            SwingUtilities.invokeLater(this::requestRender);
            return;
        }
        if (pageImage != null && Math.abs(target - renderedWidth) < 48) {
            return;
        }
        long ticket = renderTicket.incrementAndGet();
        int page = pageIndex;
        try {
            renderer.submit(() -> {
                BufferedImage image;
                try {
                    image = preview.render(page, target);
                } catch (RuntimeException ex) {
                    return; // a folha fica em branco com a mensagem de espera; nada rebenta
                }
                SwingUtilities.invokeLater(() -> {
                    if (ticket != renderTicket.get()) {
                        return;
                    }
                    pageImage = image;
                    renderedWidth = target;
                    canvas.repaint();
                });
            });
        } catch (RejectedExecutionException ignored) {
            // O diálogo já foi fechado.
        }
    }

    /** Área onde a folha é desenhada — fundo da aplicação, papel branco, página por cima. */
    private final class PaperCanvas extends JPanel {

        private PaperCanvas() {
            setBackground(UIHelper.BG_DARK);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension viewport = viewportSize();
            return new Dimension((int) (viewport.width * zoom), (int) (viewport.height * zoom));
        }

        /** Largura, em pixéis, a que a página vai aparecer — a medida certa para o render. */
        private int contentWidthHint() {
            Dimension size = getSize();
            if (size.width <= 0 || size.height <= 0) {
                size = getPreferredSize();
            }
            if (size.width <= 0 || size.height <= 0) {
                return 0;
            }
            double aspect = pageAspect();
            PrintOptions.Orientation orientation = orientation();
            Rectangle paper = PaperLayout.paper(size, aspect, orientation, 1.0);
            Rectangle content = PaperLayout.content(paper,
                    PaperLayout.contentAspect(aspect, orientation), fit(), 1.0);
            boolean turned = PaperLayout.rotates(aspect, orientation);
            return Math.max(1, turned ? content.height : content.width);
        }

        private Dimension viewportSize() {
            Component parent = getParent();
            if (parent instanceof JViewport viewport) {
                Dimension extent = viewport.getExtentSize();
                if (extent.width > 0 && extent.height > 0) {
                    return extent;
                }
            }
            return new Dimension(560, 640);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            double aspect = pageAspect();
            PrintOptions.Orientation orientation = orientation();
            Rectangle paper = PaperLayout.paper(getSize(), aspect, orientation, 1.0);

            g.setColor(UIHelper.PAPER_SHADOW);
            g.fillRoundRect(paper.x + 4, paper.y + 5, paper.width, paper.height, 6, 6);
            g.setColor(UIHelper.PAPER);
            g.fillRect(paper.x, paper.y, paper.width, paper.height);
            g.setColor(UIHelper.BORDER);
            g.drawRect(paper.x, paper.y, paper.width, paper.height);

            Rectangle content = PaperLayout.content(paper,
                    PaperLayout.contentAspect(aspect, orientation), fit(), 1.0);
            if (pageImage == null) {
                g.setColor(UIHelper.PAPER_INK);
                g.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
                String message = "A preparar pré-visualização…";
                int width = g.getFontMetrics().stringWidth(message);
                g.drawString(message, paper.x + (paper.width - width) / 2, paper.y + paper.height / 2);
                g.dispose();
                return;
            }

            Graphics2D page = (Graphics2D) g.create();
            page.clip(paper);
            if (PaperLayout.rotates(aspect, orientation)) {
                // A página é virada para assentar na folha escolhida: roda em torno do centro do
                // rectângulo de conteúdo e desenha-se com largura e altura trocadas.
                AffineTransform transform = new AffineTransform();
                transform.translate(content.getCenterX(), content.getCenterY());
                transform.rotate(Math.PI / 2);
                transform.translate(-content.height / 2.0, -content.width / 2.0);
                page.transform(transform);
                page.drawImage(pageImage, 0, 0, content.height, content.width, null);
            } else {
                page.drawImage(pageImage, content.x, content.y, content.width, content.height, null);
            }
            page.dispose();
            g.dispose();
        }
    }

    private double pageAspect() {
        return preview.aspect(pageIndex);
    }

    private PrintOptions.Orientation orientation() {
        Object selected = orientationCombo.getSelectedItem();
        return selected == null ? PrintOptions.Orientation.AUTO : (PrintOptions.Orientation) selected;
    }

    private PrintOptions.Fit fit() {
        Object selected = fitCombo.getSelectedItem();
        return selected == null ? PrintOptions.Fit.SHRINK_TO_FIT : (PrintOptions.Fit) selected;
    }

    private String pageCountLabel() {
        int pages = preview.pageCount();
        return pages == 1 ? "1 página" : pages + " páginas";
    }

    /** {@code "guia-remessa-2026-14"} → {@code "Guia remessa 2026 14"}. */
    private static String humanize(String baseName) {
        String text = baseName.replace('-', ' ').replace('_', ' ').trim();
        if (text.isEmpty()) {
            return "Documento";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
