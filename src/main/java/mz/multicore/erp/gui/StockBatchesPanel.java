package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.inventory.dto.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Lotes, validades, filtros e exportação. */
final class StockBatchesPanel {
    private static final String MASK = "•••";
    private final StockPanel owner;
    StockBatchesPanel(StockPanel owner) { this.owner = owner; }

    public JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Lotes & Validades"), BorderLayout.WEST);
        ModernButton addBatchBtn = UIHelper.createSuccessButton("Adicionar Lote/Validade");
        addBatchBtn.setIcon(UIHelper.icon("fas-plus", 14));
        addBatchBtn.addActionListener(e -> owner.createBatchEntryDialog(null));
        ModernButton exportBtn = UIHelper.createSecondaryButton("Exportar PDF");
        exportBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        exportBtn.addActionListener(e -> exportBatchesPdf());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(addBatchBtn);
        actions.add(exportBtn);
        header.add(actions, BorderLayout.EAST);

        // Filter bar
        JPanel filters = new JPanel(new GridBagLayout());
        filters.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 0, 12);

        owner.batchWarehouseCombo = new JComboBox<>();
        UIHelper.styleComboBox(owner.batchWarehouseCombo);
        owner.batchWarehouseCombo.setPreferredSize(new Dimension(220, UIHelper.FORM_CONTROL_HEIGHT));
        owner.batchWarehouseCombo.addActionListener(e -> filterBatches());

        owner.batchExpirationCombo = new JComboBox<>(new String[]{
                "Todos os lotes",
                "Vencidos",
                "Vence em ≤ 30 dias",
                "Vence em ≤ 90 dias",
                "Válidos (> 90 dias)"
        });
        UIHelper.styleComboBox(owner.batchExpirationCombo);
        owner.batchExpirationCombo.setPreferredSize(new Dimension(200, UIHelper.FORM_CONTROL_HEIGHT));
        owner.batchExpirationCombo.addActionListener(e -> filterBatches());

        owner.batchSearchField = new SearchField("Pesquisar por SKU, nome ou lote…");
        UIHelper.onTextChange(owner.batchSearchField, this::filterBatches);

        g.gridy = 0;
        g.gridx = 0; g.weightx = 0; filters.add(filterLabel("Armazém"), g);
        g.gridx = 1; g.weightx = 0; filters.add(filterLabel("Validade"), g);
        g.gridx = 2; g.weightx = 1.0; g.insets = new Insets(0, 0, 0, 0);
        filters.add(filterLabel("Pesquisa"), g);
        g.gridy = 1; g.insets = new Insets(4, 0, 0, 12);
        g.gridx = 0; g.weightx = 0; filters.add(owner.batchWarehouseCombo, g);
        g.gridx = 1; g.weightx = 0; filters.add(owner.batchExpirationCombo, g);
        g.gridx = 2; g.weightx = 1.0; g.insets = new Insets(4, 0, 0, 0);
        filters.add(owner.batchSearchField, g);

        owner.batchesSummary = new JLabel(" ");
        owner.batchesSummary.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        owner.batchesSummary.setForeground(UIHelper.TEXT_MUTED);
        owner.batchesSummary.setBorder(new EmptyBorder(2, 2, 0, 0));

        JPanel topStack = new JPanel(new BorderLayout(0, 10));
        topStack.setOpaque(false);
        topStack.add(header, BorderLayout.NORTH);
        topStack.add(filters, BorderLayout.CENTER);
        topStack.add(owner.batchesSummary, BorderLayout.SOUTH);
        tab.add(topStack, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Artigo (SKU)", "Nome do Artigo", "Armazém", "Nº Lote", "Validade", "Dias", "Quantidade", "Estado"};
        owner.batchesModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.batchesTable = new JTable(owner.batchesModel);
        UIHelper.styleTable(owner.batchesTable);
        owner.batchesTable.setAutoCreateRowSorter(true);
        JScrollPane scroll = new JScrollPane(owner.batchesTable);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        card.add(mz.multicore.erp.gui.components.ClientTablePagination.install(owner.batchesTable), BorderLayout.SOUTH);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    public void refresh() {
        if (owner.batchesModel == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> owner.inventoryApiClient.findBatchesByCompany(companyId), this::applyBatches,
                error -> owner.showStockLoadError("lotes", error));
    }

    private void applyBatches(List<ProductBatchDTO> loaded) {
        owner.batchesList = loaded;

        // Sync the batch warehouse combo with the warehouses list (skipping the first time)
        if (owner.batchWarehouseCombo != null) {
            Object selected = owner.batchWarehouseCombo.getSelectedItem();
            owner.batchWarehouseCombo.removeAllItems();
            owner.batchWarehouseCombo.addItem("--- Todos os Armazéns ---");
            for (WarehouseDTO w : owner.warehousesList) {
                owner.batchWarehouseCombo.addItem(w.name());
            }
            if (selected != null) owner.batchWarehouseCombo.setSelectedItem(selected);
        }
        updateBatchesSummary();
        filterBatches();
    }

    /** Resumo proativo: conta lotes (com stock) vencidos e a vencer em ≤ {@link #owner.EXPIRY_SOON_DAYS} dias. */
    private void updateBatchesSummary() {
        if (owner.batchesSummary == null) return;
        LocalDate today = LocalDate.now();
        long expired = 0;
        long soon = 0;
        for (var b : owner.batchesList) {
            if (b.expirationDate() == null) continue;
            if (b.quantity() == null || b.quantity().compareTo(BigDecimal.ZERO) <= 0) continue;
            long days = java.time.temporal.ChronoUnit.DAYS.between(today, b.expirationDate());
            if (days < 0) expired++;
            else if (days <= owner.EXPIRY_SOON_DAYS) soon++;
        }
        if (expired == 0 && soon == 0) {
            owner.batchesSummary.setForeground(UIHelper.APPROVED_GREEN);
            owner.batchesSummary.setText("Sem lotes vencidos ou a vencer em breve.");
        } else {
            owner.batchesSummary.setForeground(expired > 0 ? UIHelper.REJECTED_RED : UIHelper.PENDING_YELLOW);
            owner.batchesSummary.setText(String.format("%d lote%s vencido%s · %d a vencer em ≤ %d dias",
                    expired, expired == 1 ? "" : "s", expired == 1 ? "" : "s", soon, owner.EXPIRY_SOON_DAYS));
        }
    }

    private void filterBatches() {
        if (owner.batchesModel == null) return;
        owner.batchesModel.setRowCount(0);
        boolean hide = owner.stockHidden();

        Long filterWarehouseId = null;
        if (owner.batchWarehouseCombo != null) {
            int idx = owner.batchWarehouseCombo.getSelectedIndex();
            if (idx > 0 && (idx - 1) < owner.warehousesList.size()) {
                filterWarehouseId = owner.warehousesList.get(idx - 1).id();
            }
        }

        String query = owner.batchSearchField == null ? "" : owner.batchSearchField.getText().trim().toLowerCase();
        int expIdx = owner.batchExpirationCombo == null ? 0 : owner.batchExpirationCombo.getSelectedIndex();
        java.time.LocalDate today = java.time.LocalDate.now();

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (var b : owner.batchesList) {
            if (filterWarehouseId != null && !b.warehouseId().equals(filterWarehouseId)) continue;

            if (!query.isEmpty()) {
                String sku = b.sku() == null ? "" : b.sku().toLowerCase();
                String name = b.productName() == null ? "" : b.productName().toLowerCase();
                String lote = b.batchNumber() == null ? "" : b.batchNumber().toLowerCase();
                if (!sku.contains(query) && !name.contains(query) && !lote.contains(query)) continue;
            }

            long daysToExp = b.expirationDate() == null
                    ? Long.MAX_VALUE
                    : java.time.temporal.ChronoUnit.DAYS.between(today, b.expirationDate());

            boolean match = switch (expIdx) {
                case 1 -> daysToExp < 0;                   // Vencidos
                case 2 -> daysToExp >= 0 && daysToExp <= 30;
                case 3 -> daysToExp >= 0 && daysToExp <= 90;
                case 4 -> daysToExp > 90;                  // Válidos
                default -> true;
            };
            if (!match) continue;

            String status;
            if (daysToExp == Long.MAX_VALUE) status = "—";
            else if (daysToExp < 0) status = "VENCIDO";
            else if (daysToExp <= 30) status = "VENCE EM BREVE";
            else status = "VÁLIDO";

            String daysCell = daysToExp == Long.MAX_VALUE ? "—" : String.valueOf(daysToExp);

            owner.batchesModel.addRow(new Object[]{
                    b.sku(),
                    b.productName(),
                    b.warehouseName(),
                    b.batchNumber(),
                    b.expirationDate() == null ? "—" : b.expirationDate().format(fmt),
                    daysCell,
                    hide ? MASK : String.format("%,.3f", b.quantity()),
                    status
            });
        }
    }

    private void exportBatchesPdf() {
        if (owner.batchesTable.getRowCount() == 0) {
            JOptionPane.showMessageDialog(owner, "Nada para exportar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            mz.multicore.erp.modules.company.model.Company company = new mz.multicore.erp.modules.company.model.Company();
            company.setId(CurrentUserContext.getCurrentCompanyId());
            byte[] pdf = mz.multicore.erp.modules.printing.TablePdfExporter.renderFromSwing(company, "Lotes & Validades", owner.batchesTable);
            mz.multicore.erp.modules.printing.PdfFileSaver.saveAndOpen(pdf, "lotes-validades");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(owner, "Erro ao exportar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JLabel filterLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UIHelper.TEXT_MUTED);
        label.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        return label;
    }
}
