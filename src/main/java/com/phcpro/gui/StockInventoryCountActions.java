package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.*;
import com.phcpro.modules.inventory.dto.WarehouseDTO;
import com.phcpro.modules.printing.PdfFileSaver;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Sessões de contagem física e aplicação auditada das diferenças. */
final class StockInventoryCountActions {
    private final StockPanel owner;
    StockInventoryCountActions(StockPanel owner) { this.owner = owner; }

    public void openPhysicalInventoryDialog() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();

        DefaultTableModel model = new DefaultTableModel(
                new String[]{"#", "Armazém", "Estado", "Itens", "Contados", "Criado por", "Data"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        table.putClientProperty("noRowInspector", Boolean.TRUE);
        table.putClientProperty("noTableFooter", Boolean.TRUE);
        JScrollPane sc = new JScrollPane(table);
        UIHelper.styleScrollPane(sc);
        sc.setPreferredSize(new Dimension(660, 340));

        final java.util.List<com.phcpro.modules.inventory.dto.InventoryCountDTO> rows = new ArrayList<>();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        Runnable reload = () -> UIHelper.loadAsync(owner,
                () -> owner.inventoryCountApiClient.listSessions(companyId), loaded -> {
            model.setRowCount(0);
            rows.clear();
            for (var s : loaded) {
                rows.add(s);
                model.addRow(new Object[]{ s.id(), s.warehouseName(), inventoryCountStatusLabel(s.status()),
                        s.totalItems(), s.countedItems(), s.createdBy(),
                        s.createdAt() == null ? "" : s.createdAt().format(dtf) });
            }
        }, this::showError);
        reload.run();

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(sc, BorderLayout.CENTER);

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Inventário Físico",
                "fas-clipboard-list", "Sessões de contagem — crie, retome e aplique", content)
                .asReadOnly("Fechar");

        ModernButton newBtn = UIHelper.createPrimaryButton("Nova Contagem");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.addActionListener(e -> startNewCountSession(reload));

        Runnable openSelected = () -> {
            int r = table.getSelectedRow();
            if (r < 0) { JOptionPane.showMessageDialog(owner, "Selecione uma contagem.", "Aviso", JOptionPane.WARNING_MESSAGE); return; }
            openCountSession(rows.get(r).id());
            reload.run();
        };
        ModernButton openBtn = UIHelper.createSecondaryButton("Abrir");
        openBtn.setIcon(UIHelper.icon("fas-folder-open", 14));
        openBtn.addActionListener(e -> openSelected.run());

        ModernButton cancelBtn = UIHelper.createDangerButton("Cancelar Sessão");
        cancelBtn.setIcon(UIHelper.icon("fas-ban", 14));
        cancelBtn.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r < 0) { JOptionPane.showMessageDialog(owner, "Selecione uma contagem.", "Aviso", JOptionPane.WARNING_MESSAGE); return; }
            var s = rows.get(r);
            if (!"DRAFT".equals(s.status())) { JOptionPane.showMessageDialog(owner, "Só é possível cancelar contagens em curso.", "Aviso", JOptionPane.WARNING_MESSAGE); return; }
            if (JOptionPane.showConfirmDialog(owner, "Cancelar a contagem #" + s.id() + "?", "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            UIHelper.runWithProgress(owner, "A cancelar contagem…", () -> {
                owner.inventoryCountApiClient.cancelSession(s.id());
                return null;
            }, ignored -> reload.run(), this::showError);
        });

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) openSelected.run();
            }
        });

        dlg.addActionButton(newBtn);
        dlg.addActionButton(openBtn);
        dlg.addActionButton(cancelBtn);
        dlg.showDialog();
        owner.onPanelSelected(); // o stock pode ter mudado se alguma sessão foi aplicada
    }

    /** Cria uma nova sessão de contagem para um armazém e abre-a já para contar. */
    private void startNewCountSession(Runnable afterCreate) {
        if (owner.warehousesList.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Cadastre um armazém primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JComboBox<String> whCombo = new JComboBox<>();
        UIHelper.styleComboBox(whCombo);
        for (WarehouseDTO w : owner.warehousesList) whCombo.addItem(w.name());
        JPanel form = UIHelper.createDialogForm("Armazém a contar:", whCombo);
        boolean ok = new ModernFormDialog(UIHelper.mainWindow, "Nova Contagem", "fas-clipboard-list",
                "Cria uma sessão com uma linha por artigo do armazém", form)
                .setConfirmButton("Criar", "fas-check").showDialog();
        if (!ok) return;
        int idx = whCombo.getSelectedIndex();
        if (idx < 0) return;
        Long warehouseId = owner.warehousesList.get(idx).id();
        UIHelper.runWithProgress(owner, "A criar contagem…",
                () -> owner.inventoryCountApiClient.createSession(warehouseId, null), session -> {
            openCountSession(session.id());
            afterCreate.run();
        }, this::showError);
    }

    /** Abre uma sessão: DRAFT = editar/guardar/aplicar; aplicada/cancelada = só-leitura com diferenças. */
    private void openCountSession(Long sessionId) {
        UIHelper.loadAsync(owner, () -> owner.inventoryCountApiClient.getSession(sessionId),
                this::showCountSession, this::showError);
    }

    private void showCountSession(com.phcpro.modules.inventory.dto.InventoryCountDTO session) {
        Long sessionId = session.id();
        boolean draft = "DRAFT".equals(session.status());

        String[] cols = draft ? new String[]{"SKU", "Artigo", "Contagem"}
                              : new String[]{"SKU", "Artigo", "Contagem", "Sistema", "Diferença"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return draft && c == 2; }
        };
        final java.util.List<Long> productIds = new ArrayList<>();
        for (var line : session.lines()) {
            productIds.add(line.productId());
            String counted = line.countedQuantity() == null ? "" : line.countedQuantity().stripTrailingZeros().toPlainString();
            if (draft) {
                model.addRow(new Object[]{ line.sku(), line.name(), counted });
            } else {
                String sys = line.systemQuantity() == null ? "" : line.systemQuantity().stripTrailingZeros().toPlainString();
                String diff = line.difference() == null ? "" :
                        (line.difference().signum() > 0 ? "+" : "") + line.difference().stripTrailingZeros().toPlainString();
                model.addRow(new Object[]{ line.sku(), line.name(), counted, sys, diff });
            }
        }
        JTable countTable = new JTable(model);
        UIHelper.styleTable(countTable);
        countTable.putClientProperty("noRowInspector", Boolean.TRUE);
        countTable.putClientProperty("noTableFooter", Boolean.TRUE);
        JScrollPane sc = new JScrollPane(countTable);
        UIHelper.styleScrollPane(sc);
        sc.setPreferredSize(new Dimension(620, 400));
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(sc, BorderLayout.CENTER);

        String subtitle = "Armazém: " + session.warehouseName() + " · " + inventoryCountStatusLabel(session.status());

        if (!draft) {
            new ModernFormDialog(UIHelper.mainWindow, "Contagem #" + sessionId, "fas-clipboard-check",
                    subtitle, content).asReadOnly("Fechar").showDialog();
            return;
        }

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Contagem #" + sessionId,
                "fas-clipboard-check", subtitle + " — em branco = não conta", content)
                .setConfirmButton("Aplicar Ajustes", "fas-check");

        ModernButton printBtn = UIHelper.createSecondaryButton("Imprimir Folha");
        printBtn.setIcon(UIHelper.icon("fas-print", 14));
        printBtn.addActionListener(e -> UIHelper.runWithProgress(owner, "A gerar folha de contagem…",
                () -> owner.inventoryApiClient.renderCountSheet(
                        CurrentUserContext.getCurrentCompanyId(), session.warehouseId()),
                pdf -> PdfFileSaver.saveAndOpen(pdf, "folha-contagem-" + session.warehouseName()),
                this::showError));

        ModernButton saveBtn = UIHelper.createSecondaryButton("Guardar Rascunho");
        saveBtn.setIcon(UIHelper.icon("fas-save", 14));
        saveBtn.addActionListener(e -> {
            Map<Long, BigDecimal> counts = readCountsFromTable(countTable, productIds);
            UIHelper.runWithProgress(owner, "A guardar contagem…", () -> {
                owner.inventoryCountApiClient.saveCounts(sessionId, counts);
                return null;
            }, ignored -> {
                JOptionPane.showMessageDialog(owner, "Contagem guardada.", "Rascunho", JOptionPane.INFORMATION_MESSAGE);
                dlg.close();
            }, this::showError);
        });

        dlg.addActionButton(printBtn);
        dlg.addActionButton(saveBtn);
        dlg.setOnSaveAsync(() -> {
            Map<Long, BigDecimal> counts = readCountsFromTable(countTable, productIds);
            return () -> {
                owner.inventoryCountApiClient.saveCounts(sessionId, counts);
                return owner.inventoryCountApiClient.applySession(sessionId);
            };
        });
        if (dlg.showDialog()) {
            UIHelper.loadAsync(owner, () -> owner.inventoryCountApiClient.getSession(sessionId), result -> {
                owner.onPanelSelected();
            int applied = 0;
            StringBuilder diffs = new StringBuilder();
            for (var line : result.lines()) {
                if (line.countedQuantity() == null) continue;
                applied++;
                BigDecimal d = line.difference();
                if (d != null && d.signum() != 0) {
                    diffs.append(String.format("• %s: sistema %s → contado %s (%s%s)%n",
                            line.name(),
                            line.systemQuantity() == null ? "?" : line.systemQuantity().stripTrailingZeros().toPlainString(),
                            line.countedQuantity().stripTrailingZeros().toPlainString(),
                            d.signum() > 0 ? "+" : "", d.stripTrailingZeros().toPlainString()));
                }
            }
            JOptionPane.showMessageDialog(owner,
                    applied + " artigo(s) reconciliado(s).\n\n"
                            + (diffs.length() == 0 ? "Sem diferenças face ao sistema." : "Diferenças:\n" + diffs),
                    "Inventário aplicado", JOptionPane.INFORMATION_MESSAGE);
            }, this::showError);
        }
    }

    /** Lê a coluna "Contagem" da tabela para um mapa productId → quantidade (ignora vazios/não numéricos). */
    private java.util.Map<Long, BigDecimal> readCountsFromTable(JTable table, java.util.List<Long> productIds) {
        if (table.isEditing() && table.getCellEditor() != null) {
            table.getCellEditor().stopCellEditing();
        }
        java.util.Map<Long, BigDecimal> counts = new java.util.HashMap<>();
        for (int i = 0; i < table.getRowCount() && i < productIds.size(); i++) {
            Object v = table.getValueAt(i, 2);
            String txt = v == null ? "" : v.toString().trim();
            if (txt.isEmpty()) continue;
            try {
                counts.put(productIds.get(i), new BigDecimal(txt.replace(",", ".")));
            } catch (NumberFormatException ignored) { /* ignora contagem não numérica */ }
        }
        return counts;
    }

    private static String inventoryCountStatusLabel(String status) {
        return switch (status) {
            case "DRAFT" -> "Em curso";
            case "APPLIED" -> "Aplicada";
            case "CANCELLED" -> "Cancelada";
            default -> status;
        };
    }

    private void showError(Throwable error) {
        JOptionPane.showMessageDialog(owner, error.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }

    /** Diálogo de impressão de etiquetas: escolher produtos (multi-selecção) + cópias → folha PDF. */
}
