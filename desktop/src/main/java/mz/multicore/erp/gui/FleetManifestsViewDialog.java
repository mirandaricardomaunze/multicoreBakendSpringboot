package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.FleetApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.fleet.dto.FleetManifestDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo para visualização, rastreio e reimpressão de Manifestos de Carga.
 * Ver {@code docs/MANIFESTO_CARGA_ROTAS_SPEC.md}.
 */
public class FleetManifestsViewDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat CURRENCY_FMT = new DecimalFormat("#,##0.00 MT");

    public static void show(Window parent, FleetApiClient fleetApi) {
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.setPreferredSize(new Dimension(980, 520));

        String[] cols = {
                "ID", "Nº Manifesto", "Data / Hora", "Viatura", "Motorista",
                "Rota de Entrega", "Paradas", "Caixas (Cx.)", "Total Carga", "A Cobrar", "Estado"
        };

        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        UIHelper.styleTable(table);

        int[] widths = {0, 120, 130, 110, 140, 160, 70, 90, 110, 110, 100};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.removeColumn(table.getColumnModel().getColumn(0)); // ID hidden

        table.getColumnModel().getColumn(9).setCellRenderer(TableCellRenderers.status());

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        JTextField searchField = TableFilter.searchField("Pesquisar por número, viatura, motorista ou rota...");
        JComboBox<String> statusFilter = TableFilter.combo("Todos os Estados", "Em Trânsito", "Concluído", "Cancelado");
        TableFilter.install(table, searchField, new TableFilter.ColumnFilter(statusFilter, 9));

        JPanel filterBar = TableFilter.bar(searchField, TableFilter.label("Estado:"), statusFilter);

        content.add(filterBar, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);

        List<FleetManifestDTO> loadedManifests = new ArrayList<>();

        Runnable reload = () -> UIHelper.runWithProgress(parent, "A carregar manifestos de carga…", fleetApi::listManifests, manifests -> {
            loadedManifests.clear();
            loadedManifests.addAll(manifests);
            model.setRowCount(0);
            for (FleetManifestDTO m : manifests) {
                String dateStr = m.manifestDate() != null ? m.manifestDate().format(DATE_FMT) : "";
                String boxesStr = (m.totalBoxes() != null ? m.totalBoxes().toPlainString() : "0.00") + " Cx";
                String totalStr = CURRENCY_FMT.format(m.totalAmount() != null ? m.totalAmount() : BigDecimal.ZERO);
                String collectStr = CURRENCY_FMT.format(m.totalAmountToCollect() != null ? m.totalAmountToCollect() : BigDecimal.ZERO);

                model.addRow(new Object[]{
                        m.id(),
                        m.manifestNumber(),
                        dateStr,
                        m.vehiclePlate(),
                        m.driverName(),
                        m.routeName(),
                        m.totalStops(),
                        boxesStr,
                        totalStr,
                        collectStr,
                        UIHelper.humanStatus(m.status())
                });
            }
        }, error -> ToastManager.show(parent, FeedbackType.ERROR, "Erro ao carregar manifestos: " + error.getMessage()));

        reload.run();

        ModernFormDialog dialog = new ModernFormDialog(
                parent,
                "Manifestos de Carga & Rotas de Distribuição",
                "fas-route",
                content
        );

        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> reload.run());

        ModernButton printBtn = UIHelper.createSecondaryButton("Imprimir A4");
        printBtn.setIcon(UIHelper.icon("fas-print", 14));
        printBtn.addActionListener(e -> {
            int row = TableFilter.selectedModelRow(table);
            if (row < 0) {
                ToastManager.show(dialog.getDialog(), FeedbackType.WARNING, "Seleccione um manifesto para imprimir.");
                return;
            }
            Long id = (Long) model.getValueAt(row, 0);
            String number = String.valueOf(model.getValueAt(row, 1));
            UIHelper.runWithProgress(dialog.getDialog(), "A gerar PDF do manifesto…", () -> fleetApi.printManifest(id),
                    pdf -> PrintPreviewDialog.show(dialog.getDialog(), pdf, "manifesto-" + number),
                    error -> ToastManager.show(dialog.getDialog(), FeedbackType.ERROR, "Erro ao imprimir: " + error.getMessage())
            );
        });

        ModernButton completeBtn = UIHelper.createSuccessButton("Concluir Rota");
        completeBtn.setIcon(UIHelper.icon("fas-check-circle", 14));
        completeBtn.addActionListener(e -> {
            int row = TableFilter.selectedModelRow(table);
            if (row < 0) {
                ToastManager.show(dialog.getDialog(), FeedbackType.WARNING, "Seleccione um manifesto para concluir.");
                return;
            }
            Long id = (Long) model.getValueAt(row, 0);
            UIHelper.runWithProgress(dialog.getDialog(), "A concluir rota…", () -> fleetApi.updateManifestStatus(id, "COMPLETED"),
                    updated -> {
                        ToastManager.show(dialog.getDialog(), FeedbackType.SUCCESS, "Rota concluída com sucesso.");
                        reload.run();
                    },
                    error -> ToastManager.show(dialog.getDialog(), FeedbackType.ERROR, "Erro ao concluir: " + error.getMessage())
            );
        });

        ModernButton newManifestBtn = UIHelper.createPrimaryButton("Novo Manifesto");
        newManifestBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newManifestBtn.addActionListener(e -> {
            UIHelper.runWithProgress(dialog.getDialog(), "A carregar guias de remessa disponíveis…", fleetApi::listDeliveryGuides, guides -> {
                if (guides == null || guides.isEmpty()) {
                    ToastManager.show(dialog.getDialog(), FeedbackType.WARNING, "Não existem guias de remessa disponíveis para expedição.");
                    return;
                }
                CreateFleetManifestDialog.show(dialog.getDialog(), guides, fleetApi, created -> {
                    reload.run();
                    UIHelper.runWithProgress(dialog.getDialog(), "A gerar PDF do manifesto…", () -> fleetApi.printManifest(created.id()),
                            pdf -> PrintPreviewDialog.show(dialog.getDialog(), pdf, "manifesto-" + created.manifestNumber()),
                            err -> ToastManager.show(dialog.getDialog(), FeedbackType.ERROR, "Erro ao abrir PDF: " + err.getMessage()));
                });
            }, err -> ToastManager.show(dialog.getDialog(), FeedbackType.ERROR, "Erro ao carregar guias: " + err.getMessage()));
        });

        dialog.addActionButton(refreshBtn);
        dialog.addActionButton(newManifestBtn);
        dialog.addActionButton(printBtn);
        dialog.addActionButton(completeBtn);
        dialog.setConfirmButton("Fechar", "fas-times");
        dialog.setOnSave(dialog::close);

        dialog.showDialog();
    }
}
