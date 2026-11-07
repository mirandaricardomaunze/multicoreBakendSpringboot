package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.QuickPeekPanel;
import mz.multicore.erp.gui.components.TableExportAction;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordenador de operações visuais e de exportação para a consulta de movimentos comerciais.
 */
public final class CommercialMovementsOperationsCoordinator {

    private CommercialMovementsOperationsCoordinator() {
    }

    public static void populatePeek(QuickPeekPanel peek, JTable table, DefaultTableModel model, int mr) {
        int row = mr; // o controlador já entrega a linha do modelo
        peek.setHeaderIcon("fas-exchange-alt", UIHelper.ACCENT_BLUE);
        peek.setTitle(String.valueOf(model.getValueAt(row, 0)) + " " + model.getValueAt(row, 1));
        peek.setSubtitle("Entidade: " + model.getValueAt(row, 2) + " • " + model.getValueAt(row, 6));
        peek.setStatus(UIHelper.humanStatus(String.valueOf(model.getValueAt(row, 7))), UIHelper.ACCENT_BLUE);

        List<QuickPeekPanel.PeekItem> items = new ArrayList<>();
        items.add(new QuickPeekPanel.PeekItem("Tipo Documento", peek.cell(model.getValueAt(row, 0), 0), false));
        items.add(new QuickPeekPanel.PeekItem("Número", peek.cell(model.getValueAt(row, 1), 1), false));
        items.add(new QuickPeekPanel.PeekItem("Cliente / Destino", peek.cell(model.getValueAt(row, 2), 2), false));
        items.add(new QuickPeekPanel.PeekItem("Artigos", peek.cell(model.getValueAt(row, 3), 3), false));
        items.add(new QuickPeekPanel.PeekItem("Qtd Movimentada", peek.cell(model.getValueAt(row, 4), 4), true));
        items.add(new QuickPeekPanel.PeekItem("Armazém", peek.cell(model.getValueAt(row, 5), 5), false));
        items.add(new QuickPeekPanel.PeekItem("Data Emissão", peek.cell(model.getValueAt(row, 6), 6), false));
        items.add(new QuickPeekPanel.PeekItem("Total Geral", peek.cell(model.getValueAt(row, 8), 8), true));
        peek.setItems(items);
    }

    public static void exportMovements(
            Component parent,
            PrintApiClient printApiClient,
            JTable table,
            JComboBox<String> warehouseFilter,
            JComboBox<String> productFilter,
            JComboBox<String> typeFilter,
            DateField fromField,
            DateField toField
    ) {
        StringBuilder title = new StringBuilder("Movimentos Globais");
        if (warehouseFilter.getSelectedIndex() > 0) {
            title.append(" — Armazém: ").append(warehouseFilter.getSelectedItem());
        }
        if (productFilter.getSelectedIndex() > 0) {
            title.append(" — Artigo: ").append(productFilter.getSelectedItem());
        }
        if (typeFilter.getSelectedIndex() > 0) {
            title.append(" — Tipo: ").append(typeFilter.getSelectedItem());
        }
        if (fromField.getDate() != null || toField.getDate() != null) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String dFrom = fromField.getDate() != null ? fromField.getDate().format(dtf) : "";
            String dTo = toField.getDate() != null ? toField.getDate().format(dtf) : "";
            if (!dFrom.isEmpty() && !dTo.isEmpty()) {
                title.append(" (").append(dFrom).append(" a ").append(dTo).append(")");
            } else if (!dFrom.isEmpty()) {
                title.append(" (desde ").append(dFrom).append(")");
            } else {
                title.append(" (até ").append(dTo).append(")");
            }
        }
        TableExportAction.export(parent, printApiClient, table, title.toString(), "movimentos-globais");
    }
}
