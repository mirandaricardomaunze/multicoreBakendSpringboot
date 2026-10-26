package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.dto.StockTransferLineDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Diálogo modal para consulta detalhada das linhas e transporte de uma Guia de Transferência. */
final class StockTransferLinesDialog {

    private StockTransferLinesDialog() {}

    public static void show(Component parent, StockTransferDTO transfer, Runnable onPrintTransfer) {
        if (transfer == null) return;
        List<StockTransferLineDTO> lines = transfer.lines() != null ? transfer.lines() : List.of();
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);

        ModernPanel infoCard = new ModernPanel(16);
        infoCard.setLayout(new GridLayout(2, 3, 14, 8));
        infoCard.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        String driver = transfer.driverName() != null && !transfer.driverName().isBlank()
                ? transfer.driverName() : (transfer.responsible() != null && !transfer.responsible().isBlank() ? transfer.responsible() : "—");
        String plate = transfer.vehiclePlate() != null && !transfer.vehiclePlate().isBlank()
                ? transfer.vehiclePlate() : (transfer.vehicle() != null && !transfer.vehicle().isBlank() ? transfer.vehicle() : "—");
        String dateStr = transfer.transferDate() != null
                ? transfer.transferDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—";

        infoCard.add(createMetaItem("Armazém de Origem", transfer.originWarehouseName()));
        infoCard.add(createMetaItem("Armazém de Destino", transfer.destinationWarehouseName()));
        infoCard.add(createMetaItem("Data da Transferência", dateStr));
        infoCard.add(createMetaItem("Motorista", driver));
        infoCard.add(createMetaItem("Matrícula do Veículo", plate));
        infoCard.add(createMetaItem("Estado", UIHelper.humanStatus(transfer.status())));

        String[] cols = {"Referência", "Cód. Barras", "Produto", "Quantidade", "Embalagem", "Caixa", "% da Caixa", "Valor Unitário", "IVA"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (StockTransferLineDTO line : lines) {
            String ref = line.reference() != null && !line.reference().isBlank() ? line.reference() : (line.productSku() != null ? line.productSku() : "—");
            String barcode = line.barcode() != null && !line.barcode().isBlank() ? line.barcode() : "—";
            String prod = line.productName() != null ? line.productName() : "—";
            if (line.batchNumber() != null && !line.batchNumber().isBlank()
                    && !"-".equals(line.batchNumber()) && !line.batchNumber().toUpperCase().startsWith("LEGACY")) {
                prod += " [Lote: " + line.batchNumber() + "]";
            }

            BigDecimal qty = line.quantity() != null ? line.quantity() : BigDecimal.ZERO;
            int pkgsPerBox = line.safePackagesPerBox();
            int unitsPerPkg = line.safeUnitsPerPackage();
            long unitsPerBox = (long) pkgsPerBox * unitsPerPkg;

            BigDecimal pkgs = unitsPerPkg > 0 ? qty.divide(BigDecimal.valueOf(unitsPerPkg), 2, RoundingMode.HALF_UP) : qty;
            BigDecimal boxes = unitsPerBox > 0 ? qty.divide(BigDecimal.valueOf(unitsPerBox), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal boxPct = unitsPerBox > 0 ? qty.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(unitsPerBox), 1, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            BigDecimal unitPrice = line.safeUnitPrice();
            BigDecimal taxRate = line.safeTaxRate();
            BigDecimal lineNet = unitPrice.multiply(qty);
            BigDecimal lineTax = lineNet.multiply(taxRate);

            totalQty = totalQty.add(qty);
            totalNet = totalNet.add(lineNet);
            totalTax = totalTax.add(lineTax);

            model.addRow(new Object[]{
                    ref, barcode, prod, qty.stripTrailingZeros().toPlainString(),
                    pkgs.stripTrailingZeros().toPlainString(), boxes.stripTrailingZeros().toPlainString(),
                    boxPct.stripTrailingZeros().toPlainString() + "%", unitPrice,
                    taxRate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%"
            });
        }

        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i : new int[]{3, 4, 5, 6, 8}) table.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.money());

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(880, 260));

        totalNet = totalNet.setScale(2, RoundingMode.HALF_UP);
        totalTax = totalTax.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalGross = totalNet.add(totalTax);

        ModernPanel totalsCard = new ModernPanel(16);
        totalsCard.setLayout(new FlowLayout(FlowLayout.RIGHT, 18, 8));
        totalsCard.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        totalsCard.add(createTotalBadge("Linhas", String.valueOf(lines.size())));
        totalsCard.add(createTotalBadge("Qtd Total", totalQty.stripTrailingZeros().toPlainString()));
        totalsCard.add(createTotalBadge("Total Líquido", MultiCurrencyEngine.formatCurrency(totalNet, MultiCurrencyEngine.Currency.MZN)));
        totalsCard.add(createTotalBadge("Total IVA", MultiCurrencyEngine.formatCurrency(totalTax, MultiCurrencyEngine.Currency.MZN)));
        totalsCard.add(createTotalBadge("Total Geral", MultiCurrencyEngine.formatCurrency(totalGross, MultiCurrencyEngine.Currency.MZN)));

        panel.add(infoCard, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(totalsCard, BorderLayout.SOUTH);

        Window ownerWin = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        ModernFormDialog dlg = new ModernFormDialog(ownerWin, "Guia de Transferência " + transfer.transferNumber(),
                "fas-truck", "Consulta de artigos, caixas e valores da transferência", panel);
        dlg.asReadOnly("Fechar");

        ModernButton volumeBtn = UIHelper.createSecondaryButton("Etiquetas de Volume");
        volumeBtn.setIcon(UIHelper.icon("fas-tags", 14));
        volumeBtn.addActionListener(e -> StockTransferVolumeLabelsDialog.show(ownerWin, transfer));
        dlg.addActionButton(volumeBtn);

        ModernButton printBtn = UIHelper.createSecondaryButton("Imprimir Guia");
        printBtn.setIcon(UIHelper.icon("fas-print", 14));
        printBtn.addActionListener(e -> {
            dlg.close();
            if (onPrintTransfer != null) onPrintTransfer.run();
        });
        dlg.addActionButton(printBtn);

        dlg.setSize(940, 540);
        dlg.showDialog();
    }

    private static JPanel createMetaItem(String label, String value) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        l.setForeground(UIHelper.TEXT_MUTED);
        JLabel v = new JLabel(value != null && !value.isBlank() ? value : "—");
        v.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        v.setForeground(UIHelper.TEXT_LIGHT);
        p.add(l, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    private static JPanel createTotalBadge(String label, String value) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        l.setForeground(UIHelper.TEXT_MUTED);
        JLabel v = new JLabel(value);
        v.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        v.setForeground(UIHelper.ACCENT_BLUE);
        p.add(l, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }
}
