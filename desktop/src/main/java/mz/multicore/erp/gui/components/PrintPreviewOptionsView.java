package mz.multicore.erp.gui.components;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.List;

/**
 * Coluna lateral de opções e configurações de impressão do PrintPreviewDialog.
 */
public class PrintPreviewOptionsView {

    public static final int OPTIONS_WIDTH = 396;

    public interface OptionsListener {
        void onRangeModeChanged();
        void onLayoutChanged();
        void onSummaryRefresh();
    }

    private final OptionsListener listener;
    private final ModernPanel card = new ModernPanel(UIHelper.RADIUS_LG, UIHelper.BG_CARD);

    private final JComboBox<String> printerCombo = new JComboBox<>();
    private final IntegerField copiesField = new IntegerField("1", 1, PrintOptions.MAX_COPIES, "Nº de cópias");
    private final JComboBox<PrintPreviewDialog.RangeMode> rangeCombo = new JComboBox<>(PrintPreviewDialog.RangeMode.values());
    private final JTextField rangeField = new JTextField();
    private final JComboBox<PrintOptions.Orientation> orientationCombo = new JComboBox<>(PrintOptions.Orientation.values());
    private final JComboBox<PrintOptions.Fit> fitCombo = new JComboBox<>(PrintOptions.Fit.values());
    private final JCheckBox grayscaleCheck = new JCheckBox("Escala de cinzentos");

    public PrintPreviewOptionsView(OptionsListener listener) {
        this.listener = listener;
        initUi();
    }

    private void initUi() {
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
        rangeCombo.setRenderer(UIHelper.labelRenderer(PrintPreviewDialog.RangeMode::label));
        UIHelper.styleComboBox(rangeCombo);
        rangeCombo.addActionListener(event -> {
            rangeField.setEnabled(rangeCombo.getSelectedItem() == PrintPreviewDialog.RangeMode.CUSTOM);
            if (rangeCombo.getSelectedItem() != PrintPreviewDialog.RangeMode.CUSTOM) {
                UIHelper.clearFieldInvalid(rangeField);
            }
            if (listener != null) listener.onSummaryRefresh();
        });
        UIHelper.onTextChange(copiesField, () -> {
            if (listener != null) listener.onSummaryRefresh();
        });
        fields.add(pair("Nº de cópias", copiesField, "Páginas", rangeCombo));

        UIHelper.styleTextField(rangeField);
        rangeField.setEnabled(false);
        rangeField.setToolTipText("Exemplo: 1,3-5");
        fields.add(field("Intervalo (ex.: 1,3-5)", rangeField));

        fields.add(Box.createVerticalStrut(4));
        fields.add(sectionRow(new SectionHeader("Posição no papel", "fas-file-alt")));
        orientationCombo.setRenderer(UIHelper.labelRenderer(PrintOptions.Orientation::label));
        UIHelper.styleComboBox(orientationCombo);
        orientationCombo.addActionListener(event -> {
            if (listener != null) listener.onLayoutChanged();
        });
        fitCombo.setRenderer(UIHelper.labelRenderer(PrintOptions.Fit::label));
        UIHelper.styleComboBox(fitCombo);
        fitCombo.addActionListener(event -> {
            if (listener != null) listener.onLayoutChanged();
        });
        fields.add(pair("Orientação", orientationCombo, "Ajuste", fitCombo));

        grayscaleCheck.setOpaque(false);
        grayscaleCheck.setForeground(UIHelper.TEXT_LIGHT);
        grayscaleCheck.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        grayscaleCheck.addActionListener(event -> {
            if (listener != null) listener.onSummaryRefresh();
        });
        JPanel grayscaleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
        grayscaleRow.setOpaque(false);
        grayscaleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        grayscaleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        grayscaleRow.add(grayscaleCheck);
        fields.add(grayscaleRow);

        JScrollPane scroll = new JScrollPane(fields);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setUI(new SlimScrollBarUI());
        card.add(scroll, BorderLayout.CENTER);
    }

    public void loadPrinters() {
        List<String> printers = PdfPrinter.printerNames();
        printerCombo.removeAllItems();
        for (String printer : printers) {
            printerCombo.addItem(printer);
        }
        if (printers.isEmpty()) {
            printerCombo.addItem("Nenhuma impressora instalada");
            printerCombo.setEnabled(false);
        }
    }

    public void applyStoredOptions(PrintOptions stored) {
        if (stored.printerName() != null && printerCombo.isEnabled()) {
            printerCombo.setSelectedItem(stored.printerName());
        }
        copiesField.setText(String.valueOf(stored.copies()));
        orientationCombo.setSelectedItem(stored.orientation());
        fitCombo.setSelectedItem(stored.fit());
        grayscaleCheck.setSelected(stored.grayscale());
    }

    public PrintOptions readOptions(boolean strict, int pageIndex) {
        int copies;
        try {
            copies = copiesField.value();
        } catch (IllegalArgumentException ex) {
            if (strict) throw ex;
            copies = 1;
        }
        PrintPreviewDialog.RangeMode mode = (PrintPreviewDialog.RangeMode) rangeCombo.getSelectedItem();
        String range = switch (mode == null ? PrintPreviewDialog.RangeMode.ALL : mode) {
            case ALL -> "";
            case CURRENT -> String.valueOf(pageIndex + 1);
            case CUSTOM -> rangeField.getText() == null ? "" : rangeField.getText().trim();
        };
        Object printer = printerCombo.isEnabled() ? printerCombo.getSelectedItem() : null;
        return new PrintOptions(printer == null ? null : printer.toString(), copies, orientation(), fit(), range, grayscaleCheck.isSelected());
    }

    public PrintOptions.Orientation orientation() {
        Object selected = orientationCombo.getSelectedItem();
        return selected == null ? PrintOptions.Orientation.AUTO : (PrintOptions.Orientation) selected;
    }

    public PrintOptions.Fit fit() {
        Object selected = fitCombo.getSelectedItem();
        return selected == null ? PrintOptions.Fit.SHRINK_TO_FIT : (PrintOptions.Fit) selected;
    }

    public boolean hasInstalledPrinters() {
        return printerCombo.isEnabled() && printerCombo.getItemCount() > 0;
    }

    public JComponent getComponent() { return card; }
    public JTextField getRangeField() { return rangeField; }
    public JComboBox<PrintPreviewDialog.RangeMode> getRangeCombo() { return rangeCombo; }

    private static JComponent sectionRow(SectionHeader header) {
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, header.getPreferredSize().height + 4));
        return header;
    }

    private static JComponent pair(String leftLabel, JComponent left, String rightLabel, JComponent right) {
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

    private static JComponent field(String label, JComponent input) {
        FormField formField = new FormField(label, input, false, null);
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(new EmptyBorder(2, 0, 2, 0));
        row.add(formField, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, formField.getPreferredSize().height + 6));
        return row;
    }

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
}
