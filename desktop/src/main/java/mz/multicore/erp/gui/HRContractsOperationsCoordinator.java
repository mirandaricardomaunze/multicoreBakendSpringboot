package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.hr.dto.EmploymentContractDTO;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordenador operacional para impressão, ativação e Quick Peek de contratos de trabalho.
 */
public final class HRContractsOperationsCoordinator {

    private HRContractsOperationsCoordinator() {}

    public static void printContract(HRPanel owner, EmploymentContractDTO contract, Runnable reloadCallback) {
        if (owner.printApiClient == null) {
            UIHelper.runWithProgress(owner, "A gerar contrato…",
                    () -> owner.hrApiClient.renderContract(contract.id()),
                    pdf -> PrintPreviewDialog.show(owner, pdf, "contrato-" + contract.contractNumber()),
                    owner::showActionError);
            return;
        }
        SensitivePrintCoordinator.print(owner, owner.printApiClient,
                mz.multicore.erp.modules.printing.dto.SensitiveDocumentType.EMPLOYMENT_CONTRACT,
                contract.id(), "Contrato " + contract.contractNumber(),
                "contrato-" + contract.contractNumber(),
                () -> owner.hrApiClient.renderContract(contract.id()), reloadCallback);
    }

    public static void activateContract(HRPanel owner, EmploymentContractDTO contract, Runnable reloadCallback) {
        boolean confirmed = UIHelper.confirm(owner, "Activar Contrato",
                "Pôr o contrato " + contract.contractNumber() + " a vigorar?\n"
                        + "O salário acordado passa a ser o da ficha do colaborador.");
        if (!confirmed) return;
        UIHelper.runWithProgress(owner, "A activar contrato…",
                () -> owner.hrApiClient.activateContract(contract.id()),
                ignored -> { if (reloadCallback != null) reloadCallback.run(); }, owner::showActionError);
    }

    public static void populatePeek(QuickPeekPanel peek, DefaultTableModel model, int row) {
        if (model == null || row < 0 || row >= model.getRowCount()) return;
        peek.setHeaderIcon("fas-file-signature", UIHelper.ACCENT_BLUE);
        peek.setTitle(String.valueOf(model.getValueAt(row, 1)));
        peek.setSubtitle("Contrato " + model.getValueAt(row, 0) + " • " + model.getValueAt(row, 3));
        peek.setStatus(String.valueOf(model.getValueAt(row, 7)), UIHelper.ACCENT_BLUE);

        List<QuickPeekPanel.PeekItem> items = new ArrayList<>();
        items.add(new QuickPeekPanel.PeekItem("Tipo", peek.cell(model.getValueAt(row, 2), 2), false));
        items.add(new QuickPeekPanel.PeekItem("Início", peek.cell(model.getValueAt(row, 4), 4), false));
        items.add(new QuickPeekPanel.PeekItem("Fim", peek.cell(model.getValueAt(row, 5), 5), false));
        items.add(new QuickPeekPanel.PeekItem("Salário Acordado", peek.cell(model.getValueAt(row, 6), 6), true));
        peek.setItems(items);
    }
}
