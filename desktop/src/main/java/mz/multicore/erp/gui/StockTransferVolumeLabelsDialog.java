package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.dto.StockTransferLineDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Diálogo corporativo de emissão de etiquetas de volume e expedição de carga
 * para transferências de stock entre armazéns (compatível com Bobina Térmica 80mm e Folha A4).
 */
public class StockTransferVolumeLabelsDialog extends JDialog {

    private final StockTransferDTO transfer;
    private final JSpinner volumesSpinner;
    private final JSpinner copiesPerVolumeSpinner;
    private final JPanel previewCardContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));

    public StockTransferVolumeLabelsDialog(Window owner, StockTransferDTO transfer) {
        super(owner, "Etiquetas de Volume — " + transfer.transferNumber(), ModalityType.APPLICATION_MODAL);
        this.transfer = transfer;

        setLayout(new BorderLayout(0, 12));
        getContentPane().setBackground(UIHelper.BG_DARK);
        setSize(580, 520);
        setLocationRelativeTo(owner);

        // Calcular sugestão de volumes baseada em caixas
        int suggestedVolumes = 1;
        if (transfer.lines() != null && !transfer.lines().isEmpty()) {
            BigDecimal totalBoxes = BigDecimal.ZERO;
            for (StockTransferLineDTO l : transfer.lines()) {
                long unitsPerBox = (long) l.safePackagesPerBox() * l.safeUnitsPerPackage();
                if (unitsPerBox > 0 && l.quantity() != null) {
                    BigDecimal boxes = l.quantity().divide(BigDecimal.valueOf(unitsPerBox), 0, java.math.RoundingMode.CEILING);
                    totalBoxes = totalBoxes.add(boxes);
                }
            }
            if (totalBoxes.compareTo(BigDecimal.ONE) >= 0) {
                suggestedVolumes = Math.min(500, Math.max(1, totalBoxes.intValue()));
            }
        }

        volumesSpinner = new JSpinner(new SpinnerNumberModel(suggestedVolumes, 1, 500, 1));
        copiesPerVolumeSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        UIHelper.styleSpinner(volumesSpinner);
        UIHelper.styleSpinner(copiesPerVolumeSpinner);

        volumesSpinner.addChangeListener(e -> updatePreview());

        // Cabeçalho do modal
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(16, 20, 4, 20));

        JLabel title = new JLabel("Emissão de Etiquetas de Volume");
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 17));
        title.setForeground(Color.WHITE);
        title.setIcon(UIHelper.icon("fas-tags", 18, UIHelper.ACCENT_BLUE));

        JLabel subtitle = new JLabel("Identificação de caixas e paletes para conferência de trânsito e carga.");
        subtitle.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        subtitle.setForeground(UIHelper.TEXT_MUTED);

        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // Centro: Parâmetros e Pré-visualização
        JPanel centerPanel = new JPanel(new BorderLayout(0, 12));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(0, 20, 0, 20));

        ModernPanel paramsCard = new ModernPanel(16);
        paramsCard.setLayout(new GridLayout(1, 2, 16, 0));
        paramsCard.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel volWrap = new JPanel(new BorderLayout(0, 4));
        volWrap.setOpaque(false);
        JLabel volLabel = new JLabel("Número Total de Volumes (Caixas):");
        volLabel.setForeground(UIHelper.TEXT_MUTED);
        volWrap.add(volLabel, BorderLayout.NORTH);
        volWrap.add(volumesSpinner, BorderLayout.CENTER);

        JPanel copyWrap = new JPanel(new BorderLayout(0, 4));
        copyWrap.setOpaque(false);
        JLabel copyLabel = new JLabel("Cópias por Volume:");
        copyLabel.setForeground(UIHelper.TEXT_MUTED);
        copyWrap.add(copyLabel, BorderLayout.NORTH);
        copyWrap.add(copiesPerVolumeSpinner, BorderLayout.CENTER);

        paramsCard.add(volWrap);
        paramsCard.add(copyWrap);

        centerPanel.add(paramsCard, BorderLayout.NORTH);

        previewCardContainer.setOpaque(false);
        updatePreview();
        centerPanel.add(previewCardContainer, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // Rodapé: Botões
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(0, 20, 8, 20));

        ModernButton closeBtn = UIHelper.createSecondaryButton("Fechar");
        closeBtn.addActionListener(e -> dispose());

        ModernButton printBtn = UIHelper.createPrimaryButton("Imprimir Etiquetas");
        printBtn.setIcon(UIHelper.icon("fas-print", 14));
        printBtn.addActionListener(e -> printLabels());

        footer.add(closeBtn);
        footer.add(printBtn);
        add(footer, BorderLayout.SOUTH);
    }

    public static void show(Window owner, StockTransferDTO transfer) {
        StockTransferVolumeLabelsDialog dlg = new StockTransferVolumeLabelsDialog(owner, transfer);
        dlg.setVisible(true);
    }

    private void updatePreview() {
        previewCardContainer.removeAll();
        int totalVols = (Integer) volumesSpinner.getValue();
        previewCardContainer.add(createLabelCard(1, totalVols));
        previewCardContainer.revalidate();
        previewCardContainer.repaint();
    }

    private JPanel createLabelCard(int volumeIndex, int totalVolumes) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(203, 213, 225));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(0, 6));
        card.setPreferredSize(new Dimension(340, 210));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        // Topo da etiqueta: Logotipo e Guia
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel comp = new JLabel("MULTICORE LOGÍSTICA & CARGA");
        comp.setFont(new Font("Arial", Font.BOLD, 10));
        comp.setForeground(new Color(71, 85, 105));

        JLabel date = new JLabel(transfer.transferDate() != null
                ? transfer.transferDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "");
        date.setFont(new Font("Arial", Font.PLAIN, 10));
        date.setForeground(new Color(100, 116, 139));
        top.add(comp, BorderLayout.WEST);
        top.add(date, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        // Centro da etiqueta: Volume em destaque e Guia
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JLabel guide = new JLabel("GUIA: " + transfer.transferNumber());
        guide.setFont(new Font("Arial", Font.BOLD, 16));
        guide.setForeground(new Color(15, 23, 42));

        JLabel vol = new JLabel("VOLUME " + volumeIndex + " DE " + totalVolumes);
        vol.setFont(new Font("Arial", Font.BOLD, 19));
        vol.setForeground(new Color(37, 99, 235));

        JLabel route = new JLabel(transfer.originWarehouseName() + " → " + transfer.destinationWarehouseName());
        route.setFont(new Font("Arial", Font.BOLD, 12));
        route.setForeground(new Color(51, 65, 85));

        center.add(guide);
        center.add(Box.createVerticalStrut(4));
        center.add(vol);
        center.add(Box.createVerticalStrut(6));
        center.add(route);
        card.add(center, BorderLayout.CENTER);

        // Rodapé da etiqueta: Motorista, Matrícula e Linhas
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        String driver = transfer.driverName() != null && !transfer.driverName().isBlank()
                ? transfer.driverName() : (transfer.responsible() != null ? transfer.responsible() : "—");
        String plate = transfer.vehiclePlate() != null && !transfer.vehiclePlate().isBlank()
                ? transfer.vehiclePlate() : (transfer.vehicle() != null ? transfer.vehicle() : "—");

        JLabel trans = new JLabel("Motorista: " + driver + " | Viatura: " + plate);
        trans.setFont(new Font("Arial", Font.PLAIN, 10));
        trans.setForeground(new Color(100, 116, 139));

        int linesCount = transfer.lines() != null ? transfer.lines().size() : 0;
        JLabel items = new JLabel(linesCount + " itens");
        items.setFont(new Font("Arial", Font.BOLD, 10));
        items.setForeground(new Color(15, 23, 42));

        bottom.add(trans, BorderLayout.WEST);
        bottom.add(items, BorderLayout.EAST);
        card.add(bottom, BorderLayout.SOUTH);

        return card;
    }

    private void printLabels() {
        PrinterJob job = PrinterJob.getPrinterJob();
        final int totalVolumes = (Integer) volumesSpinner.getValue();
        final int copiesPerVol = (Integer) copiesPerVolumeSpinner.getValue();

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            int x = 10;
            int y = 10;
            for (int v = 1; v <= totalVolumes; v++) {
                for (int c = 0; c < copiesPerVol; c++) {
                    JPanel card = createLabelCard(v, totalVolumes);
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
                ToastManager.show(this, FeedbackType.SUCCESS, "Etiquetas de volume enviadas para a impressora.");
                dispose();
            } catch (PrinterException ex) {
                ToastManager.show(this, FeedbackType.ERROR, "Erro ao imprimir etiquetas: " + ex.getMessage());
            }
        }
    }
}
