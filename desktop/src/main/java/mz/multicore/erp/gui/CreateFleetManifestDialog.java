package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.FleetApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.DeliveryGuideDTO;
import mz.multicore.erp.modules.fleet.dto.CreateFleetManifestRequest;
import mz.multicore.erp.modules.fleet.dto.FleetManifestDTO;
import mz.multicore.erp.modules.fleet.dto.VehicleDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Diálogo modal para criação de Manifesto de Carga e Rota de Distribuição.
 * Ver {@code docs/MANIFESTO_CARGA_ROTAS_SPEC.md}.
 */
public class CreateFleetManifestDialog {

    private static final DecimalFormat CURRENCY_FMT = new DecimalFormat("#,##0.00 MT");

    public static void show(Window parent, List<DeliveryGuideDTO> selectedGuides,
                            FleetApiClient fleetApi, Consumer<FleetManifestDTO> onSuccess) {
        if (selectedGuides == null || selectedGuides.isEmpty()) {
            ToastManager.show(parent, FeedbackType.WARNING, "Seleccione pelo menos uma Guia de Remessa para o manifesto.");
            return;
        }

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.setPreferredSize(new Dimension(680, 480));

        // 1. Painel de Dados de Transporte
        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setOpaque(true);
        formCard.setBackground(UIHelper.BG_CARD);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.GRID),
                new EmptyBorder(12, 14, 12, 14)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 6, 4, 6);

        JComboBox<VehicleDTO> vehicleCombo = new JComboBox<>();
        UIHelper.styleComboBox(vehicleCombo);
        vehicleCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof VehicleDTO v) {
                    setText(v.licensePlate() + " — " + v.make() + " " + v.model() + (v.assignedDriverName() != null ? " (" + v.assignedDriverName() + ")" : ""));
                } else if (value == null) {
                    setText("— Seleccione a Viatura —");
                }
                return this;
            }
        });

        JTextField driverField = new JTextField();
        JTextField phoneField = new JTextField();
        JTextField routeField = new JTextField();
        JTextField timeField = new JTextField();
        JTextField notesField = new JTextField();

        timeField.setText("08:00");

        // Carregar Viaturas
        UIHelper.runWithProgress(parent, "A carregar viaturas da frota…", fleetApi::listVehicles, vehicles -> {
            vehicleCombo.removeAllItems();
            vehicleCombo.addItem(null);
            for (VehicleDTO v : vehicles) {
                vehicleCombo.addItem(v);
            }
        }, error -> ToastManager.show(parent, FeedbackType.ERROR, "Erro ao carregar viaturas: " + error.getMessage()));

        vehicleCombo.addActionListener(e -> {
            VehicleDTO v = (VehicleDTO) vehicleCombo.getSelectedItem();
            if (v != null && v.assignedDriverName() != null && !v.assignedDriverName().isBlank()) {
                driverField.setText(v.assignedDriverName());
            }
        });

        // Linha 0: Viatura
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.weightx = 0.2;
        formCard.add(new JLabel("Viatura:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 0.8;
        formCard.add(vehicleCombo, gbc);

        // Linha 1: Motorista e Telefone
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.2;
        formCard.add(new JLabel("Motorista / Condutor:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 1; gbc.weightx = 0.35;
        formCard.add(driverField, gbc);

        gbc.gridx = 2; gbc.gridwidth = 1; gbc.weightx = 0.15;
        formCard.add(new JLabel("Contacto / Tel:"), gbc);
        gbc.gridx = 3; gbc.gridwidth = 1; gbc.weightx = 0.3;
        formCard.add(phoneField, gbc);

        // Linha 2: Rota e Hora de Saída
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.weightx = 0.2;
        formCard.add(new JLabel("Rota de Distribuição:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 1; gbc.weightx = 0.35;
        formCard.add(routeField, gbc);

        gbc.gridx = 2; gbc.gridwidth = 1; gbc.weightx = 0.15;
        formCard.add(new JLabel("Hora Prevista:"), gbc);
        gbc.gridx = 3; gbc.gridwidth = 1; gbc.weightx = 0.3;
        formCard.add(timeField, gbc);

        // Linha 3: Notas
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.weightx = 0.2;
        formCard.add(new JLabel("Observações:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 0.8;
        formCard.add(notesField, gbc);

        content.add(formCard, BorderLayout.NORTH);

        // 2. Tabela de Guias Incluídas
        String[] cols = {"Nº Guia", "Destinatário / Cliente", "Valor Total (MT)"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        BigDecimal totalSum = BigDecimal.ZERO;
        for (DeliveryGuideDTO g : selectedGuides) {
            BigDecimal amt = g.totalAmount() != null ? g.totalAmount() : BigDecimal.ZERO;
            totalSum = totalSum.add(amt);
            model.addRow(new Object[]{g.guideNumber(), g.clientName(), CURRENCY_FMT.format(amt)});
        }

        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(320);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(2).setCellRenderer(TableCellRenderers.quantity());

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        JPanel tableCard = new JPanel(new BorderLayout(0, 6));
        tableCard.setOpaque(false);
        JLabel tableHeader = new JLabel("Guias de Remessa Incluídas (" + selectedGuides.size() + ") — Total: " + CURRENCY_FMT.format(totalSum));
        tableHeader.setFont(tableHeader.getFont().deriveFont(Font.BOLD));
        tableCard.add(tableHeader, BorderLayout.NORTH);
        tableCard.add(scroll, BorderLayout.CENTER);

        content.add(tableCard, BorderLayout.CENTER);

        ModernFormDialog dialog = new ModernFormDialog(
                parent,
                "Gerar Manifesto de Carga e Rota",
                "fas-truck-loading",
                content
        );

        dialog.setConfirmButton("Emitir Manifesto", "fas-check");
        dialog.setOnSave(() -> {
            String driver = driverField.getText().trim();
            if (driver.isBlank()) {
                ToastManager.show(dialog.getDialog(), FeedbackType.WARNING, "Indique o condutor / motorista da rota.");
                driverField.requestFocusInWindow();
                return;
            }

            String route = routeField.getText().trim();
            if (route.isBlank()) {
                ToastManager.show(dialog.getDialog(), FeedbackType.WARNING, "Indique a rota de distribuição.");
                routeField.requestFocusInWindow();
                return;
            }

            VehicleDTO v = (VehicleDTO) vehicleCombo.getSelectedItem();
            Long vehicleId = v != null ? v.id() : null;
            List<Long> guideIds = selectedGuides.stream().map(DeliveryGuideDTO::id).toList();

            CreateFleetManifestRequest request = new CreateFleetManifestRequest(
                    vehicleId,
                    driver,
                    phoneField.getText().trim(),
                    route,
                    timeField.getText().trim(),
                    notesField.getText().trim(),
                    guideIds
            );

            UIHelper.runWithProgress(dialog.getDialog(), "A emitir manifesto de carga…", () -> fleetApi.createManifest(request),
                    created -> {
                        dialog.close();
                        ToastManager.show(parent, FeedbackType.SUCCESS, "Manifesto " + created.manifestNumber() + " emitido com sucesso.");
                        if (onSuccess != null) {
                            onSuccess.accept(created);
                        }
                    },
                    error -> ToastManager.show(dialog.getDialog(), FeedbackType.ERROR, "Erro ao emitir manifesto: " + error.getMessage())
            );
        });

        dialog.showDialog();
    }
}
