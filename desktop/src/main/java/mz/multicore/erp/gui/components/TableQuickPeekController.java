package mz.multicore.erp.gui.components;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

/**
 * Controlador de «Quick Peek» Silencioso para Tabelas do Multicore ERP.
 * Permite alternar um painel lateral de inspeção rápida através da tecla de Espaço (SPACE),
 * setas UP/DOWN para navegação reativa e tecla ESC para fecho, sem criar diálogos modais nem ruído visual.
 */
public class TableQuickPeekController {

    @FunctionalInterface
    public interface CustomPeekPopulator {
        void populate(QuickPeekPanel panel, int modelRow);
    }

    private final JTable table;
    private final JPanel container;
    private final QuickPeekPanel peekPanel;
    private CustomPeekPopulator customPopulator;
    private IntConsumer onOpenFullCallback;

    public static TableQuickPeekController install(JTable table, JPanel container) {
        return new TableQuickPeekController(table, container, null);
    }

    public static TableQuickPeekController install(JTable table, JPanel container, CustomPeekPopulator populator) {
        return new TableQuickPeekController(table, container, populator);
    }

    public TableQuickPeekController(JTable table, JPanel container, CustomPeekPopulator populator) {
        this.table = Objects.requireNonNull(table, "table cannot be null");
        this.container = Objects.requireNonNull(container, "container cannot be null");
        this.customPopulator = populator;

        this.peekPanel = new QuickPeekPanel();
        this.peekPanel.setVisible(false);

        // Encaixa no lado direito do contentor (BorderLayout.EAST)
        if (container.getLayout() instanceof BorderLayout) {
            container.add(peekPanel, BorderLayout.EAST);
        } else {
            container.setLayout(new BorderLayout());
            container.add(peekPanel, BorderLayout.EAST);
        }

        setupListeners();
    }

    private void setupListeners() {
        // 1. Tecla Espaço (SPACE) e ESC na tabela
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE && e.getModifiersEx() == 0) {
                    e.consume();
                    toggle();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE && peekPanel.isVisible()) {
                    e.consume();
                    close();
                }
            }
        });

        // 2. Atualização reativa de linha ao navegar por setas ou clique
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && peekPanel.isVisible()) {
                    int viewRow = table.getSelectedRow();
                    if (viewRow >= 0) {
                        refresh(viewRow);
                    }
                }
            }
        });

        // 3. Ações no painel lateral
        peekPanel.setOnCloseAction(this::close);
        peekPanel.setOnOpenFullAction(ignored -> {
            int viewRow = table.getSelectedRow();
            if (viewRow >= 0 && onOpenFullCallback != null) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                onOpenFullCallback.accept(modelRow);
            }
        });
    }

    public void toggle() {
        if (peekPanel.isVisible()) {
            close();
        } else {
            open();
        }
    }

    public void open() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            if (table.getRowCount() > 0) {
                table.setRowSelectionInterval(0, 0);
                viewRow = 0;
            } else {
                return;
            }
        }

        refresh(viewRow);
        peekPanel.setVisible(true);
        container.revalidate();
        container.repaint();
        table.requestFocusInWindow();
    }

    public void close() {
        if (peekPanel.isVisible()) {
            peekPanel.setVisible(false);
            container.revalidate();
            container.repaint();
            table.requestFocusInWindow();
        }
    }

    public boolean isOpen() {
        return peekPanel.isVisible();
    }

    public void refresh(int viewRow) {
        if (viewRow < 0 || viewRow >= table.getRowCount()) return;

        int modelRow = table.convertRowIndexToModel(viewRow);
        if (customPopulator != null) {
            customPopulator.populate(peekPanel, modelRow);
        } else {
            defaultPopulate(viewRow, modelRow);
        }
    }

    private void defaultPopulate(int viewRow, int modelRow) {
        int cols = table.getColumnCount();
        if (cols == 0) return;

        String mainTitle = "Registo #" + (viewRow + 1);
        String subtitle = "Linha " + (viewRow + 1) + " de " + table.getRowCount();
        String statusText = null;
        Color statusColor = UIHelper.ACCENT_BLUE;

        List<QuickPeekPanel.PeekItem> items = new ArrayList<>();

        for (int c = 0; c < cols; c++) {
            String colName = table.getColumnName(c);
            Object rawVal = table.getValueAt(viewRow, c);
            if (rawVal == null) continue;
            String valStr = rawVal.toString().trim();
            if (valStr.isEmpty()) continue;

            String lower = colName.toLowerCase();

            // Identificador principal (código / número / referência)
            if (c == 0 || lower.contains("código") || lower.contains("numero") || lower.contains("número") || lower.contains("fatura")) {
                if ("Registo #".equals(mainTitle.substring(0, Math.min(9, mainTitle.length())))) {
                    mainTitle = valStr;
                }
            }

            // Descrição ou nome da entidade
            if (c == 1 || lower.contains("cliente") || lower.contains("nome") || lower.contains("fornecedor") || lower.contains("descrição")) {
                subtitle = valStr;
            }

            // Estado / Status
            if (lower.contains("estado") || lower.contains("status")) {
                statusText = valStr;
                if (lower.contains("pago") || lower.contains("aprovad") || lower.contains("activ") || lower.contains("concluíd")) {
                    statusColor = UIHelper.APPROVED_GREEN;
                } else if (lower.contains("pendente") || lower.contains("emitid") || lower.contains("aguard")) {
                    statusColor = UIHelper.PENDING_YELLOW;
                } else if (lower.contains("cancel") || lower.contains("rejeit") || lower.contains("anulad")) {
                    statusColor = UIHelper.REJECTED_RED;
                }
            }

            // Destaque de moeda / montantes
            boolean isMonetary = lower.contains("total") || lower.contains("valor") || lower.contains("saldo") || lower.contains("preço") || lower.contains("mzn") || lower.contains("mt");
            items.add(new QuickPeekPanel.PeekItem(colName, valStr, isMonetary));
        }

        peekPanel.setTitle(mainTitle);
        peekPanel.setSubtitle(subtitle);
        peekPanel.setStatus(statusText, statusColor);
        peekPanel.setItems(items);
    }

    public void setCustomPopulator(CustomPeekPopulator populator) {
        this.customPopulator = populator;
    }

    public void setOnOpenFullCallback(IntConsumer callback) {
        this.onOpenFullCallback = callback;
    }

    public QuickPeekPanel getPeekPanel() {
        return peekPanel;
    }

    public JTable getTable() {
        return table;
    }
}
