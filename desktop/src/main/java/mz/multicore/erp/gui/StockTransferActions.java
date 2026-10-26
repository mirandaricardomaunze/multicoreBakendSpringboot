package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.inventory.dto.*;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Fluxo de criação, decisão e impressão de transferências de stock. */
final class StockTransferActions {
    private final StockPanel owner;
    StockTransferActions(StockPanel owner) { this.owner = owner; }

    void openCreateEditor() {
        if (owner.warehousesList.size() < 2) {
            owner.showStockNotice(FeedbackType.WARNING, "Armazéns insuficientes", "Registe pelo menos dois armazéns para realizar uma transferência.");
            return;
        }
        if (owner.catalogProducts.isEmpty()) {
            owner.showStockNotice(FeedbackType.WARNING, "Produto necessário", "Registe produtos antes de realizar uma transferência.");
            return;
        }
        owner.transferEditorForm.setOptions(owner.warehousesList, owner.catalogProducts);
        owner.transferEditorForm.prepareCreate();
        owner.transferEditorHost.setEditorTitle("Nova Transferência — Rascunho");
        owner.transferEditorHost.setSaveText("Guardar Rascunho");
        owner.transferEditorHost.setSaveEnabled(true);
        owner.transferPagesLayout.show(owner.transferPages, "editor");
    }

    void openSelectedEditor() {
        StockTransferDTO selected = selected("Seleccione uma guia", "Escolha uma guia na tabela para editar ou consultar.");
        if (selected == null) return;
        UIHelper.loadAsync(owner, () -> owner.stockTransferApiClient.findById(selected.id()), transfer -> {
            owner.transferEditorForm.setOptions(owner.warehousesList, owner.catalogProducts);
            owner.transferEditorForm.load(transfer);
            boolean editable = "DRAFT".equals(transfer.status());
            owner.transferEditorHost.setEditorTitle((editable ? "Editar " : "Consultar ") + "Transferência " + transfer.transferNumber());
            owner.transferEditorHost.setSaveText(editable ? "Guardar Alterações" : "Apenas Consulta");
            owner.transferEditorHost.setSaveEnabled(editable);
            owner.transferPagesLayout.show(owner.transferPages, "editor");
        }, owner::showStockError);
    }

    void saveEditor() {
        StockTransferDTO loaded = owner.transferEditorForm.loaded();
        if (loaded != null && !"DRAFT".equals(loaded.status())) {
            owner.showStockNotice(FeedbackType.INFO, "Documento encerrado", "Esta transferência está em modo de consulta e já não pode ser alterada.");
            return;
        }
        try {
            if (loaded == null) {
                CreateStockTransferRequest request = owner.transferEditorForm.createRequest();
                UIHelper.runWithProgress(owner, "A guardar rascunho…", () -> owner.stockTransferApiClient.create(request), this::savedDraft, owner::showStockError);
            } else {
                UpdateStockTransferRequest request = owner.transferEditorForm.updateRequest();
                UIHelper.runWithProgress(owner, "A actualizar rascunho…", () -> owner.stockTransferApiClient.update(loaded.id(), request), this::savedDraft, owner::showStockError);
            }
        } catch (RuntimeException error) {
            owner.showStockNotice(FeedbackType.ERROR, "Não foi possível guardar", error.getMessage());
        }
    }

    private void savedDraft(StockTransferDTO saved) {
        owner.transferEditorForm.markClean();
        showList();
        owner.onPanelSelected();
        owner.showStockSuccess("Rascunho " + saved.transferNumber() + " guardado. Submeta-o quando estiver pronto para aprovação.");
    }

    void showList() {
        if (owner.transferPagesLayout != null && owner.transferPages != null) owner.transferPagesLayout.show(owner.transferPages, "list");
    }

    void submitSelectedTransfer() {
        StockTransferDTO selected = selected("Seleccione um rascunho", "Escolha uma transferência em rascunho para submeter.");
        if (selected == null) return;
        if (!"DRAFT".equals(selected.status())) {
            owner.showStockNotice(FeedbackType.INFO, "Estado incompatível", "Apenas transferências em rascunho podem ser submetidas.");
            return;
        }
        boolean confirm = ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(owner), FeedbackType.INFO,
                "Submeter Transferência",
                "Submeter a guia " + selected.transferNumber() + " para separação no armazém?\n\nDepois da submissão, os artigos devem ser separados antes da aprovação final.",
                "Submeter Guia");
        if (!confirm) return;
        UIHelper.runWithProgress(owner, "A submeter transferência…", () -> owner.stockTransferApiClient.submit(selected.id()), ignored -> {
            owner.onPanelSelected();
            owner.showStockSuccess("Guia " + selected.transferNumber() + " submetida para separação no armazém.");
        }, owner::showStockError);
    }

    void printPickingSheetForSelected() {
        StockTransferDTO selected = selected("Seleccione uma transferência", "Escolha uma transferência na tabela para emitir a guia de separação.");
        if (selected == null) return;
        if (owner.printApiClient == null) {
            UIHelper.runWithProgress(owner, "A gerar guia de separação…", () -> owner.stockTransferApiClient.renderPickingSheet(selected.id()),
                    pdf -> PrintPreviewDialog.show(owner, pdf, "separacao-transferencia-" + selected.transferNumber()), owner::showStockError);
            return;
        }
        SensitivePrintCoordinator.print(owner, owner.printApiClient,
                mz.multicore.erp.modules.printing.dto.SensitiveDocumentType.STOCK_TRANSFER_PICKING,
                selected.id(), "Guia de separação " + selected.transferNumber(), "separacao-transferencia-" + selected.transferNumber(),
                () -> owner.stockTransferApiClient.renderPickingSheet(selected.id()), owner::onPanelSelected);
    }

    void completeSeparationForSelected() {
        StockTransferDTO selected = selected("Seleccione uma transferência", "Escolha uma transferência para concluir a separação de artigos.");
        if (selected == null) return;
        if (!"AWAITING_SEPARATION".equals(selected.status()) && !"IN_SEPARATION".equals(selected.status())) {
            owner.showStockNotice(FeedbackType.INFO, "Estado incompatível",
                    "Apenas transferências a aguardar ou em separação podem ser concluídas. Estado atual: " + selected.status());
            return;
        }
        boolean confirm = ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(owner), FeedbackType.INFO,
                "Concluir separação de transferência",
                "Confirmar que todos os artigos da guia " + selected.transferNumber() + " foram separados e conferidos no armazém?\nA transferência ficará pronta para aprovação final da gerência.",
                "Concluir Separação");
        if (!confirm) return;
        UIHelper.runWithProgress(owner, "A concluir separação…", () -> owner.stockTransferApiClient.completeSeparation(selected.id()), updated -> {
            owner.onPanelSelected();
            owner.showStockSuccess("Separação da guia " + selected.transferNumber() + " concluída · pronta para aprovação.");
        }, owner::showStockError);
    }

    void cancelSelectedTransfer() {
        StockTransferDTO selected = selected("Seleccione uma guia", "Escolha uma transferência para cancelar.");
        if (selected == null) return;
        if ("IN_TRANSIT".equals(selected.status())) {
            owner.showStockNotice(FeedbackType.WARNING, "Transferência em trânsito",
                    "A guia " + selected.transferNumber() + " está em trânsito com mercadoria já despachada da origem e não pode ser cancelada.");
            return;
        }
        if ("RECEIVED".equals(selected.status()) || "RECEIVED_WITH_DISCREPANCY".equals(selected.status())
                || "APPROVED".equals(selected.status()) || "REJECTED".equals(selected.status()) || "CANCELLED".equals(selected.status())) {
            owner.showStockNotice(FeedbackType.INFO, "Estado incompatível",
                    "Transferências concluídas, recebidas, rejeitadas ou já canceladas não podem ser canceladas.");
            return;
        }
        boolean confirm = ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(owner), FeedbackType.WARNING,
                "Cancelar transferência", "Cancelar a guia " + selected.transferNumber() + "?\nEsta operação não move stock.", "Cancelar Guia");
        if (!confirm) return;
        UIHelper.runWithProgress(owner, "A cancelar transferência…", () -> owner.stockTransferApiClient.cancel(selected.id()), ignored -> {
            owner.onPanelSelected();
            owner.showStockSuccess("Guia " + selected.transferNumber() + " cancelada.");
        }, owner::showStockError);
    }

    private StockTransferDTO selected(String title, String message) {
        int row = TableFilter.selectedModelRow(owner.transferTable);
        if (row < 0) {
            owner.showStockNotice(FeedbackType.WARNING, title, message);
            return null;
        }
        return owner.transfersList.get(row);
    }

    public void createTransferDialog() {
        StockTransferCreateModalDialog.show(owner);
    }

    public void approveSelectedTransfer() {
        StockTransferDTO selected = selected("Seleccione uma guia", "Escolha uma guia na tabela para continuar.");
        if (selected == null) return;
        if (!"SEPARATED".equals(selected.status()) && !"PENDING_APPROVAL".equals(selected.status())) {
            owner.showStockNotice(FeedbackType.WARNING, "Separação necessária",
                    "A transferência " + selected.transferNumber() + " ainda não tem a separação concluída (Estado: " + selected.status() + ").\n"
                            + "Emita a Guia de Separação e marque 'Concluir Separação' antes da aprovação.");
            return;
        }
        String confirmMsg = selected.requiresReceipt()
                ? "Aprovar a guia " + selected.transferNumber() + "?\n\nO stock vai sair de '" + selected.originWarehouseName()
                  + "' e ficar EM TRÂNSITO até que o armazém '" + selected.destinationWarehouseName() + "' confirme a recepção física dos artigos."
                : "Aprovar a guia " + selected.transferNumber() + "?\n\nO stock vai sair de '" + selected.originWarehouseName()
                  + "' e entrar em '" + selected.destinationWarehouseName() + "'.";
        boolean confirm = ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(owner),
                FeedbackType.INFO, "Confirmar Aprovação", confirmMsg, "Aprovar Guia");
        if (!confirm) return;
        UIHelper.runWithProgress(owner, "A aprovar transferência…", () -> owner.stockTransferApiClient.approve(selected.id()), approved -> {
            owner.onPanelSelected();
            String successMsg = selected.requiresReceipt()
                    ? "Guia " + selected.transferNumber() + " aprovada · mercadoria despachada em trânsito para '" + selected.destinationWarehouseName() + "'."
                    : "Guia " + selected.transferNumber() + " aprovada · stock movimentado.";
            owner.showStockSuccess(successMsg);
            boolean print = ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(owner), FeedbackType.INFO, "Impressão de Guia",
                    "Guia " + selected.transferNumber() + " aprovada com sucesso!\n\nDeseja imprimir a Guia de Transferência Oficial (3 vias) agora?", "Imprimir");
            if (print) printTransfer(approved.id(), approved.transferNumber());
        }, owner::showStockError);
    }

    public void rejectSelectedTransfer() {
        StockTransferDTO selected = selected("Seleccione uma guia", "Escolha uma guia na tabela para continuar.");
        if (selected == null) return;
        if ("IN_TRANSIT".equals(selected.status()) || "RECEIVED".equals(selected.status()) || "RECEIVED_WITH_DISCREPANCY".equals(selected.status())) {
            owner.showStockNotice(FeedbackType.WARNING, "Operação bloqueada",
                    "A guia " + selected.transferNumber() + " já tem mercadoria despachada e não pode ser rejeitada.");
            return;
        }
        String reason = UIHelper.promptRequiredText("Rejeitar Guia", "fas-times-circle", "Guia " + selected.transferNumber(), "Motivo da rejeição:");
        if (reason == null) return;
        UIHelper.runWithProgress(owner, "A rejeitar transferência…", () -> owner.stockTransferApiClient.reject(selected.id(), reason), ignored -> {
            owner.onPanelSelected();
            owner.showStockSuccess("Guia " + selected.transferNumber() + " rejeitada · nenhum stock movimentado.");
        }, owner::showStockError);
    }

    public void printSelectedTransfer() {
        StockTransferDTO selected = selected("Seleccione uma transferência", "Escolha uma transferência na tabela para continuar.");
        if (selected == null) return;
        if (!"APPROVED".equals(selected.status()) && !"IN_TRANSIT".equals(selected.status()) && !"RECEIVED".equals(selected.status()) && !"RECEIVED_WITH_DISCREPANCY".equals(selected.status())) {
            owner.showStockNotice(FeedbackType.INFO, "Guia oficial bloqueada",
                    "A Guia de Transferência oficial (3 vias) só pode ser emitida após a separação e aprovação.\nPara conferência física no armazém, utilize a opção 'Guia de Separação'.");
            return;
        }
        printTransfer(selected.id(), selected.transferNumber());
    }

    public void receiveSelectedTransfer() {
        StockTransferDTO selected = selected("Seleccione uma guia", "Escolha uma guia em trânsito na tabela para confirmar a recepção.");
        if (selected == null) return;
        if (!"IN_TRANSIT".equals(selected.status())) {
            owner.showStockNotice(FeedbackType.WARNING, "Estado incompatível",
                    "Apenas transferências com mercadoria em trânsito podem ter a recepção confirmada no destino.\nEstado atual: " + UIHelper.humanStatus(selected.status()));
            return;
        }
        StockTransferReceivingDialog.show(SwingUtilities.getWindowAncestor(owner), selected, (notes, lines) -> {
            UIHelper.runWithProgress(owner, "A confirmar recepção no destino…", () -> owner.stockTransferApiClient.receive(selected.id(), notes, lines),
                    (StockTransferDTO received) -> {
                        owner.onPanelSelected();
                        boolean withDiscrepancy = "RECEIVED_WITH_DISCREPANCY".equals(received.status());
                        String msg = withDiscrepancy
                                ? "Recepção da guia " + selected.transferNumber() + " confirmada com divergência · quantidades recebidas creditadas em '"
                                + selected.destinationWarehouseName() + "' e faltas devolvidas à origem '" + selected.originWarehouseName() + "'."
                                : "Recepção da guia " + selected.transferNumber() + " confirmada com sucesso · stock creditado em '"
                                + selected.destinationWarehouseName() + "'.";
                        owner.showStockSuccess(msg);
                    }, owner::showStockError);
        });
    }

    public void recordOrderForSelectedTransfer() {
        StockTransferDTO selected = selected("Seleccione uma transferência", "Escolha uma transferência na tabela para continuar.");
        if (selected == null) return;
        if (selected.orderId() != null) {
            owner.showStockNotice(FeedbackType.INFO, "Encomenda já registada",
                    "A transferência " + selected.transferNumber() + " já tem a encomenda " + selected.orderNumber() + " registada.");
            return;
        }
        boolean confirm = ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(owner), FeedbackType.INFO,
                "Registar Encomenda em Falta",
                "Deseja registar a encomenda de reposição em falta para a guia " + selected.transferNumber() + "?\n\n"
                        + "A mercadoria já mudou de armazém: a encomenda nasce cumprida e não move stock nenhum. Serve para manter o registo histórico do pedido do armazém de destino.",
                "Registar Encomenda");
        if (!confirm) return;

        UIHelper.runWithProgress(owner, "A registar encomenda…", () -> owner.stockTransferApiClient.recordOrder(selected.id()), order -> {
            owner.onPanelSelected();
            owner.showStockSuccess("Encomenda " + order.orderNumber() + " registada para a transferência " + selected.transferNumber() + ".");
        }, owner::showStockError);
    }

    public void viewSelectedTransferLines() {
        StockTransferDTO selected = selected("Seleccione uma guia", "Escolha uma guia na tabela para consultar as linhas.");
        if (selected == null) return;
        UIHelper.loadAsync(owner, () -> owner.stockTransferApiClient.findById(selected.id()), this::displayTransferLinesModal, owner::showStockError);
    }

    public void displayTransferLinesModal(StockTransferDTO transfer) {
        StockTransferLinesDialog.show(SwingUtilities.getWindowAncestor(owner), transfer,
                () -> printTransfer(transfer.id(), transfer.transferNumber()));
    }

    public void printVolumeLabelsForSelected() {
        StockTransferDTO selected = selected("Seleccione uma transferência", "Escolha uma transferência na tabela para emitir etiquetas de volume.");
        if (selected != null) StockTransferVolumeLabelsDialog.show(SwingUtilities.getWindowAncestor(owner), selected);
    }

    private void printTransfer(Long transferId, String transferNumber) {
        if (owner.printApiClient == null) {
            UIHelper.runWithProgress(owner, "A gerar guia de transferência…", () -> owner.stockTransferApiClient.renderTransfer(transferId),
                    pdf -> PrintPreviewDialog.show(owner, pdf, "transferencia-" + transferNumber), owner::showStockError);
            return;
        }
        SensitivePrintCoordinator.print(owner, owner.printApiClient,
                mz.multicore.erp.modules.printing.dto.SensitiveDocumentType.STOCK_TRANSFER,
                transferId, "Guia de transferência " + transferNumber, "transferencia-" + transferNumber,
                () -> owner.stockTransferApiClient.renderTransfer(transferId), owner::onPanelSelected);
    }

    void applyTransfers(List<StockTransferDTO> loaded) {
        owner.transferModel.setRowCount(0);
        owner.transfersList = loaded != null ? loaded : List.of();
        int separationCount = 0, pendingCount = 0, inTransitCount = 0, completedCount = 0;
        BigDecimal totalValue = BigDecimal.ZERO;
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (StockTransferDTO t : owner.transfersList) {
            String status = t.status() != null ? t.status() : "";
            if ("AWAITING_SEPARATION".equals(status) || "IN_SEPARATION".equals(status)) {
                separationCount++;
            } else if ("SEPARATED".equals(status) || "PENDING_APPROVAL".equals(status) || "DRAFT".equals(status)) {
                pendingCount++;
            } else if ("IN_TRANSIT".equals(status)) {
                inTransitCount++;
            } else if ("APPROVED".equals(status) || "RECEIVED".equals(status) || "RECEIVED_WITH_DISCREPANCY".equals(status)) {
                if (t.transferDate() == null || t.transferDate().isAfter(thirtyDaysAgo)) completedCount++;
            }
            if (t.lines() != null) {
                for (StockTransferLineDTO line : t.lines()) {
                    totalValue = totalValue.add((line.quantity() != null ? line.quantity() : BigDecimal.ZERO).multiply(line.safeUnitPrice()));
                }
            }
            String driver = t.driverName() != null ? t.driverName() : (t.responsible() != null ? t.responsible() : "-");
            String plate = t.vehiclePlate() != null ? t.vehiclePlate() : (t.vehicle() != null ? t.vehicle() : "-");
            String trBu = t.businessUnitName() != null && !t.businessUnitName().isBlank() ? t.businessUnitName() : "—";
            owner.transferModel.addRow(new Object[]{t.transferNumber(), t.transferDate() != null ? t.transferDate().format(dtf) : "-",
                    t.originWarehouseName(), t.destinationWarehouseName(), driver, plate,
                    t.lines() == null ? 0 : t.lines().size(), t.status(), trBu});
        }
        owner.kpiTransfersSeparation.setText(String.valueOf(separationCount));
        owner.kpiTransfersPending.setText(String.valueOf(pendingCount));
        owner.kpiTransfersInTransit.setText(String.valueOf(inTransitCount));
        owner.kpiTransfersCompleted.setText(String.valueOf(completedCount));
        owner.kpiTransfersTotalValue.setText(MultiCurrencyEngine.formatCurrency(totalValue, MultiCurrencyEngine.Currency.MZN));
        if (owner.transferBusinessUnitCombo != null) {
            Object prev = owner.transferBusinessUnitCombo.getSelectedItem();
            Set<String> bus = new TreeSet<>();
            for (StockTransferDTO t : owner.transfersList) if (t.businessUnitName() != null && !t.businessUnitName().isBlank()) bus.add(t.businessUnitName());
            owner.transferBusinessUnitCombo.removeAllItems();
            owner.transferBusinessUnitCombo.addItem("Todos os negócios");
            for (String b : bus) owner.transferBusinessUnitCombo.addItem(b);
            if (prev != null) owner.transferBusinessUnitCombo.setSelectedItem(prev);
        }
    }
}
