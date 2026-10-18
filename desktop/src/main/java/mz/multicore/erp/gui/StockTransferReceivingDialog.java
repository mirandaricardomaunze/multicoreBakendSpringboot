package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ClientTablePagination;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.inventory.dto.ReceiveStockTransferRequest;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.dto.StockTransferLineDTO;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Diálogo de conferência física e recepção de transferências de stock em trânsito.
 * Suporta recepção integral ou parcial (com divergências devolvidas à origem).
 * Ver {@code docs/STOCK_TRANSFER_RECEIPT_DISCREPANCY_SPEC.md}.
 */
public class StockTransferReceivingDialog {

    public static final int COL_PRODUCT = 0;
    public static final int COL_BATCH = 1;
    public static final int COL_SENT = 2;
    public static final int COL_RECEIVED = 3;
    public static final int COL_RETURNED = 4;
    public static final int COL_REASON = 5;

    public static void show(Window parent, StockTransferDTO transfer,
                            BiConsumer<String, List<ReceiveStockTransferRequest.ReceiveLine>> onConfirm) {
        if (transfer == null || transfer.lines() == null || transfer.lines().isEmpty()) return;

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);

        // Card superior com contexto da transferência
        JPanel infoCard = new JPanel(new GridLayout(1, 4, 12, 0));
        infoCard.setOpaque(true);
        infoCard.setBackground(UIHelper.BG_CARD);
        infoCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.GRID),
                new EmptyBorder(10, 14, 10, 14)
        ));

        infoCard.add(createHeaderField("Nº Guia:", transfer.transferNumber() != null ? transfer.transferNumber() : "#" + transfer.id()));
        infoCard.add(createHeaderField("Origem:", transfer.originWarehouseName() != null ? transfer.originWarehouseName() : "—"));
        infoCard.add(createHeaderField("Destino:", transfer.destinationWarehouseName() != null ? transfer.destinationWarehouseName() : "—"));
        String transportInfo = (transfer.driverName() != null && !transfer.driverName().isBlank())
                ? transfer.driverName() + (transfer.vehiclePlate() != null ? " (" + transfer.vehiclePlate() + ")" : "")
                : (transfer.vehicle() != null ? transfer.vehicle() : "—");
        infoCard.add(createHeaderField("Transportador:", transportInfo));
        content.add(infoCard, BorderLayout.NORTH);

        // Grelha de conferência
        String[] cols = {
                "Produto", "Lote", "Despachado", "A Receber (Destino)", "Divergência (Volta à Origem)", "Motivo da Divergência"
        };

        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == COL_RECEIVED || c == COL_REASON;
            }
        };

        List<StockTransferLineDTO> lines = transfer.lines();
        for (StockTransferLineDTO l : lines) {
            BigDecimal qty = l.quantity() != null ? l.quantity() : BigDecimal.ZERO;
            model.addRow(new Object[]{
                    l.productName() != null ? l.productName() : (l.productSku() != null ? l.productSku() : "Artigo #" + l.productId()),
                    l.batchNumber() != null && !l.batchNumber().isBlank() ? l.batchNumber() : "—",
                    qty,
                    qty, // Sugestão inicial: recepção integral
                    BigDecimal.ZERO,
                    ""
            });
        }

        JTable table = new JTable(model);
        table.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(table);

        // Listener para recalcular devolução em tempo real quando o operador altera 'A Receber'
        model.addTableModelListener(e -> {
            if (e.getColumn() == COL_RECEIVED) {
                int row = e.getFirstRow();
                if (row >= 0 && row < lines.size()) {
                    BigDecimal sent = parseDecimal(model.getValueAt(row, COL_SENT));
                    BigDecimal received = parseDecimal(model.getValueAt(row, COL_RECEIVED));
                    BigDecimal returned = sent.subtract(received).max(BigDecimal.ZERO);
                    if (!returned.equals(model.getValueAt(row, COL_RETURNED))) {
                        model.setValueAt(returned, row, COL_RETURNED);
                    }
                }
            }
        });

        table.getColumnModel().getColumn(COL_SENT).setCellRenderer(TableCellRenderers.quantity());
        table.getColumnModel().getColumn(COL_RECEIVED).setCellRenderer(TableCellRenderers.quantity());
        table.getColumnModel().getColumn(COL_RETURNED).setCellRenderer(TableCellRenderers.quantity());

        table.getColumnModel().getColumn(COL_PRODUCT).setPreferredWidth(190);
        table.getColumnModel().getColumn(COL_BATCH).setPreferredWidth(100);
        table.getColumnModel().getColumn(COL_SENT).setPreferredWidth(95);
        table.getColumnModel().getColumn(COL_RECEIVED).setPreferredWidth(125);
        table.getColumnModel().getColumn(COL_RETURNED).setPreferredWidth(145);
        table.getColumnModel().getColumn(COL_REASON).setPreferredWidth(180);
        UIHelper.ensureHeadersFit(table);

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(850, 240));

        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.setOpaque(false);
        centerPanel.add(scroll, BorderLayout.CENTER);

        // Campo de notas gerais de recepção
        JPanel notesPanel = new JPanel(new BorderLayout(8, 0));
        notesPanel.setOpaque(false);
        JLabel notesLabel = new JLabel("Observações da Recepção:");
        notesLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        notesLabel.setForeground(UIHelper.TEXT_LIGHT);
        JTextField notesField = new JTextField();
        notesField.setPreferredSize(new Dimension(300, UIHelper.FORM_CONTROL_HEIGHT));
        notesField.setBackground(UIHelper.FIELD_BG);
        notesField.setForeground(UIHelper.TEXT_LIGHT);
        notesField.setCaretColor(UIHelper.TEXT_LIGHT);
        notesField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.GRID),
                new EmptyBorder(4, 8, 4, 8)
        ));
        notesPanel.add(notesLabel, BorderLayout.WEST);
        notesPanel.add(notesField, BorderLayout.CENTER);
        centerPanel.add(notesPanel, BorderLayout.SOUTH);

        content.add(centerPanel, BorderLayout.CENTER);

        ModernFormDialog dlg = new ModernFormDialog(
                parent,
                "Conferência e Recepção de Mercadoria",
                "fas-dolly-flatbed",
                "Indique as quantidades recebidas no armazém de destino. Qualquer diferença voltará automaticamente à origem.",
                content
        );
        dlg.setConfirmButton("Confirmar Recepção", "fas-check-circle");

        dlg.setOnSave(() -> {
            if (table.isEditing()) table.getCellEditor().stopCellEditing();

            List<ReceiveStockTransferRequest.ReceiveLine> receiveLines = new ArrayList<>();
            boolean hasDiscrepancy = false;

            for (int i = 0; i < lines.size(); i++) {
                StockTransferLineDTO line = lines.get(i);
                BigDecimal sent = parseDecimal(model.getValueAt(i, COL_SENT));
                BigDecimal received = parseDecimal(model.getValueAt(i, COL_RECEIVED));
                String reason = String.valueOf(model.getValueAt(i, COL_REASON)).trim();

                if (received.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Linha '" + line.productName() + "': a quantidade recebida não pode ser negativa.");
                }
                if (received.compareTo(sent) > 0) {
                    throw new IllegalArgumentException(
                            "Linha '" + line.productName() + "': a quantidade recebida (" + received
                                    + ") não pode exceder a quantidade despachada (" + sent + ")."
                    );
                }
                if (received.compareTo(sent) < 0) {
                    if (reason.isEmpty()) {
                        throw new IllegalArgumentException(
                                "Linha '" + line.productName() + "': indique o motivo da divergência (falta, avaria ou dano no transporte) para justificar a devolução de "
                                        + sent.subtract(received) + " un à origem."
                        );
                    }
                    hasDiscrepancy = true;
                }

                receiveLines.add(new ReceiveStockTransferRequest.ReceiveLine(line.id(), received, reason.isEmpty() ? null : reason));
            }

            String notes = notesField.getText().trim();
            onConfirm.accept(notes.isEmpty() ? null : notes, receiveLines);
        });

        dlg.showDialog();
    }

    private static JPanel createHeaderField(String label, String value) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        lbl.setForeground(UIHelper.TEXT_MUTED);
        JLabel val = new JLabel(value);
        val.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        val.setForeground(UIHelper.TEXT_LIGHT);
        p.add(lbl, BorderLayout.NORTH);
        p.add(val, BorderLayout.CENTER);
        return p;
    }

    private static BigDecimal parseDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal bd) return bd.max(BigDecimal.ZERO);
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue()).max(BigDecimal.ZERO);
        String s = val.toString().trim().replace(" ", "").replace(',', '.');
        if (s.isEmpty()) return BigDecimal.ZERO;
        try {
            BigDecimal d = new BigDecimal(s);
            return d.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : d;
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }
}
