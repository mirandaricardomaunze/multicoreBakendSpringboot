package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.gui.pos.PosBlindCloseDialog;
import mz.multicore.erp.gui.pos.PosSessionHistoryDialog;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.pos.dto.TillSessionDTO;

import javax.swing.*;
import java.math.BigDecimal;

/** Abertura, fecho e movimentos manuais da sessão de caixa. */
final class PosCashSessionActions {
    private final POSPanel owner;
    PosCashSessionActions(POSPanel owner) { this.owner = owner; }

    public void openSession() {
        BigDecimal bal = UIHelper.promptAmount("Abrir Caixa", "fas-lock-open",
                "Saldo inicial em numerário na gaveta", "Saldo de Abertura (MT):", BigDecimal.ZERO);
        if (bal == null) return;
        String operator = CurrentUserContext.getUsername();
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.runWithProgress(owner, "A abrir caixa",
                () -> owner.posApiClient.openSession(operator, bal, companyId), opened -> {
            owner.showPosSuccess("Sessão de caixa aberta com sucesso.");
            owner.refreshSessionState();
        }, error -> showError("Não foi possível abrir a sessão de caixa", error));
    }

    public void closeSession() {
        if (owner.activeSession == null) return;
        PosBlindCloseDialog dialog = new PosBlindCloseDialog(
                SwingUtilities.getWindowAncestor(owner),
                owner.posApiClient,
                owner.activeSession,
                owner::refreshSessionState
        );
        dialog.setVisible(true);
    }

    public void showSessionHistory() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (companyId == null) return;
        PosSessionHistoryDialog dialog = new PosSessionHistoryDialog(
                SwingUtilities.getWindowAncestor(owner),
                owner.posApiClient,
                companyId
        );
        dialog.setVisible(true);
    }

    /**
     * Passagem de turno: o operador actual faz contagem da gaveta, selecciona o
     * operador seguinte, e transfere a posse da sessão sem a fechar.
     */
    public void shiftHandover() {
        if (owner.activeSession == null) return;
        mz.multicore.erp.gui.pos.PosShiftHandoverDialog dialog =
                new mz.multicore.erp.gui.pos.PosShiftHandoverDialog(
                        SwingUtilities.getWindowAncestor(owner),
                        owner.posApiClient,
                        owner.activeSession,
                        owner::refreshSessionState
                );
        dialog.setVisible(true);
    }

    /**
     * Pergunta ao operador para que conta de tesouraria deve ir o depósito do numerário
     * da sessão. Devolve o id da conta, ou null se o operador optar por não depositar
     * agora (ou não houver contas configuradas).
     */
    private Long chooseDepositAccount() {
        if (owner.accountsList == null || owner.accountsList.isEmpty()) return null;

        String[] options = new String[owner.accountsList.size() + 1];
        for (int i = 0; i < owner.accountsList.size(); i++) {
            options[i] = owner.accountsList.get(i).name();
        }
        options[owner.accountsList.size()] = "Não depositar agora";

        int choice = JOptionPane.showOptionDialog(owner,
                "Depositar o numerário da sessão em que conta de tesouraria?",
                "Depósito de Fecho de Caixa",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (choice < 0 || choice == owner.accountsList.size()) return null;
        return owner.accountsList.get(choice).id();
    }

    public void manageCashMovements() {
        if (owner.activeSession == null) return;
        Long sessionId = owner.activeSession.id();

        mz.multicore.erp.gui.pos.PosCashMovementDialog.show(
                SwingUtilities.getWindowAncestor(owner),
                sessionId,
                (type, amt, desc) -> UIHelper.runWithProgress(
                        owner,
                        "A registar movimento de caixa…",
                        () -> owner.posApiClient.addCashMovement(sessionId, type, amt, desc),
                        ignored -> {
                            owner.showPosSuccess("Movimento de caixa (" + type + ") de " + amt + " MT registado com sucesso.");
                            owner.refreshSessionState();
                        },
                        error -> showError("Não foi possível registar o movimento de caixa", error)
                )
        );
    }

    private void showError(String action, Throwable error) {
        owner.showPosNotice(FeedbackType.ERROR, action, error.getMessage());
    }

    // (Adicionar ao carrinho agora é feito por clique no card — ver addProductToCart. O FEFO é
    //  aplicado pelo backend no checkout; deixou de haver pré-visualização no formulário.)

}
