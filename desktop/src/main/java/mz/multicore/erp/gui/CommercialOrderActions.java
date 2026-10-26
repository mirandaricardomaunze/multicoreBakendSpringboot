package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.desktop.client.ProductionApiClient;
import mz.multicore.erp.gui.commercial.CommercialProductionOrderDialog;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.gui.components.SensitivePrintCoordinator;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.DeliveryGuideDTO;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.dto.OrderLineDTO;
import mz.multicore.erp.modules.comercial.model.OrderKind;
import mz.multicore.erp.modules.printing.dto.SensitiveDocumentType;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;

/**
 * Operações corporativas de ciclo de vida de encomendas no {@link ComercialPanel}:
 * faturação, aprovação/rejeição por alçada, conversão em guias e geração de ordem de fabrico.
 */
final class CommercialOrderActions {

    private CommercialOrderActions() {
    }

    static void billSelectedOrder(ComercialPanel panel) {
        int row = TableFilter.selectedModelRow(panel.ordersTable);
        if (row < 0) {
            panel.showCommercialNotice(FeedbackType.WARNING, "Seleccione uma encomenda", "Escolha na tabela a encomenda que pretende faturar.");
            return;
        }

        Long orderId = (Long) panel.ordersTableModel.getValueAt(row, 0);
        String orderNum = (String) panel.ordersTableModel.getValueAt(row, 1);
        String statusStr = (String) panel.ordersTableModel.getValueAt(row, 3);

        Object kindObj = panel.ordersTableModel.getValueAt(row, ComercialPanel.ORDERS_COL_KIND);
        OrderKind kind = kindObj instanceof OrderKind k ? k : OrderKind.FORMAL_ORDER;

        if ("BILLED".equalsIgnoreCase(statusStr) || "INVOICED".equalsIgnoreCase(statusStr)
                || "Facturado".equalsIgnoreCase(statusStr) || "Faturado".equalsIgnoreCase(statusStr)) {
            panel.showCommercialNotice(FeedbackType.WARNING, "Encomenda já faturada",
                    "A encomenda " + orderNum + " já foi faturada anteriormente. Não é permitido faturar a mesma encomenda novamente.");
            return;
        }

        if (!"SEPARATED".equalsIgnoreCase(statusStr) && !(!"PENDING".equalsIgnoreCase(statusStr) ? false : !kind.usesSeparationFlow())) {
            if ("PENDING_APPROVAL".equalsIgnoreCase(statusStr)) {
                boolean choice = ModernMessageDialog.confirm(
                        SwingUtilities.getWindowAncestor(panel),
                        FeedbackType.INFO,
                        "Aprovação de Encomenda Pendente",
                        "A encomenda " + orderNum + " está com aprovação pendente (retida por alçada ou risco de crédito/desconto).\n\n"
                                + "Deseja analisar e autorizar/aprovar esta encomenda agora?",
                        "Analisar"
                );
                if (choice) {
                    promptApproveOrRejectOrder(panel, orderId, orderNum);
                }
                return;
            }
            panel.showCommercialNotice(FeedbackType.WARNING, "Encomenda ainda não separada",
                    "Apenas pedidos separados podem ser faturados. Estado actual: " + UIHelper.humanStatus(statusStr) + ".");
            return;
        }

        java.util.concurrent.Callable<InvoiceDTO> billAction = kind.usesSeparationFlow()
                ? () -> panel.comercialApiClient.billFulfillmentOrder(orderId, CustomerOrderFulfillmentActions.terminalName())
                : () -> panel.comercialApiClient.billOrder(orderId);

        UIHelper.runWithProgress(panel, "A faturar encomenda…", billAction, invoice -> {
            ToastManager.success(panel, "Encomenda " + orderNum + " faturada · fatura " + invoice.invoiceNumber() + ".");

            panel.loadOrdersTable();
            panel.loadInvoicesTable();

            boolean choice = ModernMessageDialog.confirm(
                    SwingUtilities.getWindowAncestor(panel),
                    FeedbackType.INFO,
                    "Emitir Guia de Transporte",
                    "Encomenda " + orderNum + " faturada com sucesso!\n"
                            + "Fatura: " + invoice.invoiceNumber() + " (stock movimentado pelo faturador).\n\n"
                            + "Deseja emitir e visualizar a Guia de Remessa / Transporte agora para acompanhar a mercadoria?",
                    "Emitir Guia"
            );
            if (choice) {
                SensitivePrintCoordinator.print(panel, panel.printApiClient,
                        SensitiveDocumentType.REMITTANCE_GUIDE, invoice.id(),
                        "Guia de remessa da fatura " + invoice.invoiceNumber(),
                        "guia-remessa-" + invoice.invoiceNumber(),
                        () -> panel.comercialApiClient.renderGuide(invoice.id()), null);
            }
        }, error -> panel.showCommercialError("faturar encomenda", error));
    }

    static void promptApproveOrRejectOrder(ComercialPanel panel, Long orderId, String orderNum) {
        boolean approveChoice = ModernMessageDialog.confirm(
                SwingUtilities.getWindowAncestor(panel),
                FeedbackType.INFO,
                "Aprovação de Encomenda — " + orderNum,
                "Decisão de Alçada e Risco Corporativo para a Encomenda " + orderNum + ":\n\n"
                        + "• Clique em 'Aprovar Encomenda' para libertar a encomenda para separação e faturação (Green Lane).\n"
                        + "• Para rejeitar ou cancelar, clique em 'Cancelar'.",
                "Aprovar Encomenda"
        );

        if (approveChoice) {
            String notes = UIHelper.promptRequiredText("Aprovar Encomenda", "fas-check-circle",
                    "Encomenda " + orderNum, "Justificação da aprovação:");
            if (notes == null) return;

            UIHelper.runWithProgress(panel, "A aprovar encomenda…",
                    () -> panel.comercialApiClient.approveOrder(orderId, notes),
                    approved -> {
                        ToastManager.success(panel, "Encomenda " + orderNum + " aprovada com sucesso! Liberada para o armazém.");
                        panel.loadOrdersTable();
                    },
                    error -> panel.showCommercialError("aprovar encomenda", error));
        } else {
            boolean wantReject = ModernMessageDialog.confirm(
                SwingUtilities.getWindowAncestor(panel),
                    FeedbackType.WARNING,
                    "Rejeitar Encomenda — " + orderNum,
                    "Deseja rejeitar formalmente a Encomenda " + orderNum + "?\nSerá solicitado o motivo da rejeição para arquivo e auditoria.",
                    "Rejeitar Encomenda"
            );
            if (!wantReject) return;

            String reason = UIHelper.promptRequiredText("Rejeitar Encomenda", "fas-times-circle",
                    "Encomenda " + orderNum, "Indique o motivo da rejeição:");
            if (reason == null || reason.trim().isBlank()) return;

            UIHelper.runWithProgress(panel, "A rejeitar encomenda…",
                    () -> panel.comercialApiClient.rejectOrder(orderId, reason.trim()),
                    rejected -> {
                        ToastManager.show(panel, FeedbackType.WARNING, "Encomenda " + orderNum + " rejeitada com sucesso.");
                        panel.loadOrdersTable();
                    },
                    error -> panel.showCommercialError("rejeitar encomenda", error));
        }
    }

    static void convertSelectedOrderToGuide(ComercialPanel panel) {
        int row = TableFilter.selectedModelRow(panel.ordersTable);
        if (row < 0) {
            panel.showCommercialNotice(FeedbackType.WARNING, "Seleccione uma encomenda",
                    "Escolha na tabela a encomenda que pretende converter em guia.");
            return;
        }

        Long orderId = (Long) panel.ordersTableModel.getValueAt(row, ComercialPanel.ORDERS_COL_ID);
        String status = String.valueOf(panel.ordersTableModel.getValueAt(row, ComercialPanel.ORDERS_COL_STATUS));

        Object kindCell = panel.ordersTableModel.getValueAt(row, ComercialPanel.ORDERS_COL_KIND);
        if (kindCell instanceof OrderKind kind && kind.usesWarehouseTransfer()) {
            UIHelper.loadAsync(panel, () -> panel.comercialApiClient.getOrderById(orderId),
                    order -> InternalReplenishmentActions.showConvertDialog(panel, panel.comercialApiClient, order),
                    error -> panel.showCommercialLoadError("encomenda", error));
            return;
        }

        if (!"PENDING".equals(status)) {
            panel.showCommercialNotice(FeedbackType.WARNING, "Encomenda indisponível para guia",
                    "A encomenda deve estar pendente e aprovada. Estado actual: " + UIHelper.humanStatus(status) + ".");
            return;
        }

        UIHelper.loadAsync(panel, () -> panel.comercialApiClient.getOrderById(orderId),
                order -> showConvertOrderToGuideDialog(panel, order),
                error -> panel.showCommercialLoadError("encomenda", error));
    }

    private static void showConvertOrderToGuideDialog(ComercialPanel panel, OrderDTO order) {
        JTextField orderField = new JTextField(order.orderNumber());
        JTextField clientField = new JTextField(order.clientName());
        JTextField totalField = new JTextField(String.format("%,.2f MT", order.totalAmount()));
        JTextField responsibleField = new JTextField();
        JTextField vehicleField = new JTextField();
        JTextArea notesArea = new JTextArea(3, 28);
        for (JTextField field : List.of(orderField, clientField, totalField, responsibleField, vehicleField)) {
            UIHelper.styleTextField(field);
        }
        orderField.setEditable(false);
        clientField.setEditable(false);
        totalField.setEditable(false);
        responsibleField.putClientProperty("JTextField.placeholderText", "Nome de quem acompanha a expedição");
        vehicleField.putClientProperty("JTextField.placeholderText", "Matrícula ou identificação da viatura");
        UIHelper.styleTextArea(notesArea);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(notesArea);
        UIHelper.styleScrollPane(notesScroll);

        JComboBox<String> fleetCombo = UIHelper.createComboBox(
                new String[]{"(Viatura externa / digitação manual)"}, s -> s);
        if (panel.getFleetApiClient() != null) {
            UIHelper.loadAsync(panel, panel.getFleetApiClient()::listVehicles, vehicles -> {
                if (vehicles != null && !vehicles.isEmpty()) {
                    vehicles.forEach(v -> fleetCombo.addItem(v.licensePlate() + " • " + v.make() + " " + v.model()
                            + (v.assignedDriverName() != null && !v.assignedDriverName().isBlank() ? " (" + v.assignedDriverName() + ")" : "")));
                }
            }, null);
            fleetCombo.addActionListener(e -> {
                int idx = fleetCombo.getSelectedIndex();
                if (idx > 0) {
                    String selected = (String) fleetCombo.getSelectedItem();
                    if (selected != null) {
                        String[] parts = selected.split(" • ");
                        if (parts.length > 0) vehicleField.setText(parts[0].trim());
                        if (selected.contains("(") && selected.contains(")")) {
                            String driver = selected.substring(selected.lastIndexOf('(') + 1, selected.lastIndexOf(')'));
                            responsibleField.setText(driver);
                        }
                    }
                }
            });
        }

        JPanel form = UIHelper.createDialogForm(
                "Encomenda:", orderField,
                "Cliente:", clientField,
                "Total:", totalField,
                "Viatura da Frota:", fleetCombo,
                "Responsável pelo transporte *:", responsibleField,
                "Viatura / Matrícula *:", vehicleField,
                "Observações:", notesScroll
        );

        DeliveryGuideDTO[] created = new DeliveryGuideDTO[1];
        ModernFormDialog dialog = new ModernFormDialog(SwingUtilities.getWindowAncestor(panel),
                "Converter em Guia", "fas-truck",
                "Dados de transporte da Guia de Remessa", form)
                .setConfirmButton("Criar Guia", "fas-truck")
                .setOnSaveAsync(() -> {
                    String responsible = blankToNull(responsibleField.getText());
                    String vehicle = blankToNull(vehicleField.getText());
                    String notes = blankToNull(notesArea.getText());
                    if (responsible == null) {
                        responsibleField.requestFocusInWindow();
                        throw new IllegalArgumentException("O transportador / responsável é obrigatório para emitir a guia de remessa.");
                    }
                    if (vehicle == null) {
                        vehicleField.requestFocusInWindow();
                        throw new IllegalArgumentException("A viatura / matrícula é obrigatória para emitir a guia de remessa.");
                    }
                    return () -> created[0] = panel.comercialApiClient.createDeliveryGuide(
                            order.id(), responsible, vehicle, notes);
                });

        if (dialog.showDialog() && created[0] != null) {
            ToastManager.success(panel, "Guia " + created[0].guideNumber() + " criada e submetida para aprovação.");
            panel.loadOrdersTable();
            panel.getDeliveryGuidesPanel().refresh();
        }
    }

    static void generateProductionOrderFromSelectedOrder(ComercialPanel panel) {
        int row = TableFilter.selectedModelRow(panel.ordersTable);
        if (row < 0) {
            panel.showCommercialNotice(FeedbackType.WARNING, "Seleccione uma encomenda",
                    "Escolha na tabela a encomenda para gerar a Ordem de Fabrico.");
            return;
        }
        ProductionApiClient productionApiClient = panel.getProductionApiClient();
        if (productionApiClient == null) {
            panel.showCommercialNotice(FeedbackType.INFO, "Manufatura Indisponível",
                    "O módulo de Produção não está inicializado nesta sessão.");
            return;
        }

        Long orderId = (Long) panel.ordersTableModel.getValueAt(row, ComercialPanel.ORDERS_COL_ID);
        UIHelper.loadAsync(panel, () -> panel.comercialApiClient.getOrderById(orderId), order -> {
            List<CommercialProductionOrderDialog.DocumentItem> items = new ArrayList<>();
            for (OrderLineDTO line : order.lines()) {
                items.add(new CommercialProductionOrderDialog.DocumentItem(
                        line.productId(), line.productName(), line.quantity()));
            }
            CommercialProductionOrderDialog.open(
                    panel,
                    productionApiClient,
                    "Encomenda " + order.orderNumber(),
                    items,
                    panel.warehousesList,
                    panel::loadOrdersTable
            );
        }, error -> panel.showCommercialLoadError("encomenda", error));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
