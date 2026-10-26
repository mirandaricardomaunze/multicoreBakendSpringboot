package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.AssetApiClient;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.assets.dto.AssetDTO;
import mz.multicore.erp.modules.assets.dto.DepreciationScheduleDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/** Coordenador de cálculo e visualização de histórico de amortizações de activos. */
public final class AssetDepreciationCoordinator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private AssetDepreciationCoordinator() {}

    public static boolean openDepreciationDialog(Component parent, AssetApiClient assetApiClient) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        LocalDate now = LocalDate.now();

        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(now.getYear(), 2000, 2099, 1));
        JSpinner monthSpinner = new JSpinner(new SpinnerNumberModel(now.getMonthValue(), 1, 12, 1));

        JPanel form = UIHelper.createDialogForm(
                "Ano de Competência:", yearSpinner,
                "Mês de Competência:", monthSpinner
        );

        ModernFormDialog dialog = new ModernFormDialog(window, "Calcular Amortizações do Período", "fas-calculator",
                "Executa o apuramento mensal das quotas de depreciação e actualiza o VLC", form);

        dialog.setOnSave(() -> {
            int year = (Integer) yearSpinner.getValue();
            int month = (Integer) monthSpinner.getValue();
            List<DepreciationScheduleDTO> res = assetApiClient.runMonthlyDepreciation(year, month);
            if (res.isEmpty()) {
                throw new IllegalArgumentException("Nenhum activo elegível para amortização no período " + month + "/" + year + ".");
            }
        });

        return dialog.showDialog();
    }

    public static void showDepreciationHistory(Component parent, AssetDTO asset, AssetApiClient assetApiClient,
                                              Consumer<String> onError) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        JPanel detailsPanel = new JPanel(new BorderLayout(0, 10));
        detailsPanel.setOpaque(false);

        String[] cols = {"Período", "Data Cálculo", "VLC Inicial", "Quota Amortização", "Amort. Acumulada", "VLC Final"};
        DefaultTableModel histModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable histTable = new JTable(histModel);
        UIHelper.styleTable(histTable);
        histTable.getColumnModel().getColumn(2).setCellRenderer(TableCellRenderers.money());
        histTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        histTable.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.money());
        histTable.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.money());

        JScrollPane histScroll = new JScrollPane(histTable);
        histScroll.setPreferredSize(new Dimension(650, 240));
        detailsPanel.add(histScroll, BorderLayout.CENTER);

        try {
            List<DepreciationScheduleDTO> history = assetApiClient.getDepreciationHistory(asset.id());
            for (DepreciationScheduleDTO d : history) {
                String period = d.periodMonth() + "/" + d.periodYear();
                String calcDate = d.calculationDate() != null ? d.calculationDate().format(DATE_FMT) : "—";
                histModel.addRow(new Object[]{
                        period, calcDate, d.openingBookValue(), d.depreciationAmount(),
                        d.accumulatedDepreciation(), d.closingBookValue()
                });
            }
        } catch (Exception ex) {
            onError.accept("Falha ao obter histórico de depreciações: " + ex.getMessage());
        }

        ModernFormDialog dialog = new ModernFormDialog(window,
                "Histórico de Amortizações: " + asset.assetCode() + " - " + asset.name(),
                "fas-list-alt", "Registro cronológico das quotas de amortização processadas", detailsPanel);
        dialog.showDialog();
    }
}
