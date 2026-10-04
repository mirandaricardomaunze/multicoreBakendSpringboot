package mz.multicore.erp.gui.pos.contingency;

import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.modules.pos.dto.PosContingencyStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Diálogo modal para inspecção, reimpressão e sincronização manual de vendas em contingência do POS.
 */
public class PosContingencyDialog extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PosContingencyManager manager;
    private final PosContingencySyncService syncService;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel summaryLabel;

    public PosContingencyDialog(Window owner, PosContingencyManager manager, PosContingencySyncService syncService) {
        super(owner, "Vendas em Regime de Contingência — POS", ModalityType.APPLICATION_MODAL);
        this.manager = manager;
        this.syncService = syncService;

        setSize(800, 520);
        setLocationRelativeTo(owner);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(UIHelper.BG_DARK);
        root.setBorder(new EmptyBorder(16, 18, 16, 18));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout(0, 6));
        headerPanel.setOpaque(false);
        JLabel titleLabel = UIHelper.createHeading("Fila Local de Vendas em Contingência");
        titleLabel.setIcon(UIHelper.icon("fas-exclamation-triangle", UIHelper.ICON_LG));

        summaryLabel = new JLabel();
        summaryLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        summaryLabel.setForeground(UIHelper.TEXT_MUTED);

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(summaryLabel, BorderLayout.SOUTH);
        root.add(headerPanel, BorderLayout.NORTH);

        // Table
        String[] cols = {"Referência", "Data/Hora", "Operador", "Valor (MT)", "Estado", "Documento Oficial"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        UIHelper.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean isSelected, boolean hasFocus, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, isSelected, hasFocus, r, c);
                setHorizontalAlignment(SwingConstants.RIGHT);
                return comp;
            }
        });

        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean isSelected, boolean hasFocus, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, isSelected, hasFocus, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (v instanceof PosContingencyStatus status) {
                    setText(formatStatusLabel(status));
                    if (!isSelected) {
                        if (status == PosContingencyStatus.PENDING_SYNC) {
                            setForeground(UIHelper.PENDING_YELLOW);
                        } else if (status == PosContingencyStatus.SYNCED) {
                            setForeground(UIHelper.APPROVED_GREEN);
                        } else {
                            setForeground(UIHelper.REJECTED_RED);
                        }
                    }
                }
                return comp;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        root.add(scrollPane, BorderLayout.CENTER);

        // Bottom Actions
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionsPanel.setOpaque(false);

        ModernButton reprintBtn = UIHelper.createButton("Reimprimir Talão", UIHelper.icon("fas-print", UIHelper.ICON_SM, Color.WHITE), UIHelper.ACCENT_CYAN, e -> reprintSelected());

        ModernButton syncBtn = UIHelper.createPrimaryButton("Sincronizar Agora");
        syncBtn.setIcon(UIHelper.icon("fas-sync-alt", UIHelper.ICON_SM));
        syncBtn.addActionListener(e -> {
            if (syncService != null) {
                syncService.triggerManualSync(this, this::loadData);
            }
        });

        ModernButton closeBtn = UIHelper.createDangerButton("Fechar");
        closeBtn.addActionListener(e -> dispose());

        actionsPanel.add(reprintBtn);
        actionsPanel.add(syncBtn);
        actionsPanel.add(closeBtn);
        root.add(actionsPanel, BorderLayout.SOUTH);

        setContentPane(root);
        loadData();
    }

    private void loadData() {
        tableModel.setRowCount(0);
        List<PosContingencySale> sales = manager.getAllSales();
        for (PosContingencySale s : sales) {
            String op = s.request() != null && s.request().operator() != null ? s.request().operator() : "—";
            String date = s.createdAt() != null ? s.createdAt().format(DATE_FMT) : "—";
            String total = s.cartTotal() != null ? String.format("%.2f", s.cartTotal()) : "0.00";
            String officialDoc = s.syncedInvoiceNumber() != null ? s.syncedInvoiceNumber()
                    : (s.errorMessage() != null ? s.errorMessage() : "Pendente");

            tableModel.addRow(new Object[]{
                    s.contingencyReference(),
                    date,
                    op,
                    total,
                    s.status(),
                    officialDoc
            });
        }

        int pending = manager.getPendingCount();
        BigDecimal pendingTotal = manager.getPendingTotal();
        summaryLabel.setText(String.format("Vendas pendentes de envio: %d  |  Total acumulado: %.2f MT",
                pending, pendingTotal));
    }

    private void reprintSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            ToastManager.show(this, FeedbackType.WARNING, "Seleccione uma venda para reimprimir o talão.");
            return;
        }
        String ref = (String) tableModel.getValueAt(row, 0);
        PosContingencySale selected = manager.getAllSales().stream()
                .filter(s -> s.contingencyReference().equals(ref))
                .findFirst().orElse(null);

        if (selected != null) {
            PosThermalReceiptPrinter.printSilent(selected);
            ToastManager.success(this, "Talão de contingência (" + ref + ") reenviado para a impressora.");
        }
    }

    private static String formatStatusLabel(PosContingencyStatus status) {
        if (status == null) return "—";
        return switch (status) {
            case PENDING_SYNC -> "Pendente";
            case SYNCED -> "Sincronizado";
            case REVISION_NEEDED -> "Atenção";
        };
    }
}
