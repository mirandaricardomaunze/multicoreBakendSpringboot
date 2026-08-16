package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.*;
import com.phcpro.modules.financeira.dto.TreasuryAccountDTO;
import com.phcpro.modules.pos.dto.TillSessionDTO;

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
        UIHelper.runWithProgress(owner, "A abrir caixa…",
                () -> owner.posApiClient.openSession(operator, bal, companyId), opened -> {
            JOptionPane.showMessageDialog(owner, "Sessão de caixa aberta com sucesso!", "Informação", JOptionPane.INFORMATION_MESSAGE);
            owner.refreshSessionState();
        }, error -> showError("Não foi possível abrir a sessão de caixa", error));
    }

    public void closeSession() {
        if (owner.activeSession == null) return;

        BigDecimal closingReal = UIHelper.promptAmount("Fechar Caixa", "fas-lock",
                "Numerário fisicamente contado na gaveta", "Saldo Físico no Fecho (MT):", BigDecimal.ZERO);
        if (closingReal == null) return;

        // Conta de tesouraria que recebe o depósito do numerário da sessão (opcional).
        Long depositAccountId = chooseDepositAccount();

        Long sessionId = owner.activeSession.id();
        UIHelper.runWithProgress(owner, "A fechar caixa…",
                () -> owner.posApiClient.closeSession(sessionId, closingReal, depositAccountId), closed -> {
            Long closedId = closed.id();

            String summary = String.format("Sessão Fechada com sucesso!\n" +
                    "Saldo Esperado: %,.2f MT\n" +
                    "Saldo Real: %,.2f MT\n" +
                    "Diferença: %,.2f MT\n\nImprimir o fecho de caixa (Z)?",
                    closed.closingBalanceExpected(), closed.closingBalanceReal(), closed.difference());
            int print = JOptionPane.showConfirmDialog(owner, summary, "Fecho de Caixa",
                    JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (print == JOptionPane.YES_OPTION) {
                UIHelper.runWithProgress(owner, "A gerar fecho (Z)…",
                        () -> owner.posApiClient.renderZReport(closedId),
                        pdf -> com.phcpro.modules.printing.PdfFileSaver.saveAndOpen(pdf, "fecho-caixa-Z-" + closedId),
                        err -> JOptionPane.showMessageDialog(owner, "Erro ao gerar o Z: " + err.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE));
            }
            owner.refreshSessionState();
        }, error -> showError("Não foi possível fechar a sessão de caixa", error));
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

        String[] options = {"SUPRIMENTO (Entrada de Dinheiro)", "SANGRIA (Retirada de Dinheiro)"};
        int opt = JOptionPane.showOptionDialog(owner, "Selecione o tipo de movimento:", "Movimentar Caixa",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (opt < 0) return;
        String type = (opt == 0) ? "SUPRIMENTO" : "SANGRIA";

        MoneyField amountField = new MoneyField();
        JTextField descField = new JTextField();
        JPanel dialogPanel = UIHelper.createDialogForm(
                "Valor (MT):", amountField,
                "Descrição / Motivo:", descField
        );

        int confirm = JOptionPane.showConfirmDialog(owner, dialogPanel, type, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (confirm == JOptionPane.OK_OPTION) {
            try {
                BigDecimal amt = amountField.value();
                String desc = descField.getText().trim();
                if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                    JOptionPane.showMessageDialog(owner, "O valor deve ser maior do que zero.", "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Long sessionId = owner.activeSession.id();
                UIHelper.runWithProgress(owner, "A registar movimento…",
                        () -> owner.posApiClient.addCashMovement(sessionId, type, amt, desc), ignored -> {
                            JOptionPane.showMessageDialog(owner, "Movimento de caixa registado com sucesso!",
                                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                            owner.refreshSessionState();
                        }, error -> showError("Não foi possível registar o movimento de caixa", error));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(owner, "Valor de montante inválido.", "Erro", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(owner, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showError(String action, Throwable error) {
        JOptionPane.showMessageDialog(owner, action + ": " + error.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
    }

    // (Adicionar ao carrinho agora é feito por clique no card — ver addProductToCart. O FEFO é
    //  aplicado pelo backend no checkout; deixou de haver pré-visualização no formulário.)

}
