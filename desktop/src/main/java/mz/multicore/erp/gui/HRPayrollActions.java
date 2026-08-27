package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.BankPaymentFileDTO;
import mz.multicore.erp.modules.hr.dto.PayslipDTO;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.time.LocalDate;
import java.util.function.Supplier;

/**
 * Acções da folha salarial que não cabem mais no {@link HRPanel}: aprovar o recibo (§B8.4), fechar
 * e reabrir o mês (§B8.6) e gerar o ficheiro de pagamento bancário (§B8.7).
 *
 * <p>Classe própria pela mesma razão do {@code CommercialOrderSubmission} e do
 * {@code OrderToTransferAction}: o painel tem uma guarda de 1000 linhas
 * ({@code UiPanelDecompositionTest}) e estava a 949 quando este bloco chegou. A alternativa era
 * empurrar o painel para lá do limite, que é exactamente o que a guarda existe para impedir.
 */
final class HRPayrollActions {

    private final HRPanel owner;
    private final Supplier<PayslipDTO> selection;
    private final Runnable afterChange;

    HRPayrollActions(HRPanel owner, Supplier<PayslipDTO> selection, Runnable afterChange) {
        this.owner = owner;
        this.selection = selection;
        this.afterChange = afterChange;
    }

    /**
     * Segunda vista antes de o dinheiro sair (§B8.4). Até aqui, quem processava a folha pagava-a
     * sozinho: o recibo ia de {@code DRAFT} direito a {@code PAID}, sem ninguém ter de olhar para
     * os números primeiro.
     */
    void approveSelected() {
        PayslipDTO sel = selection.get();
        if (sel == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(owner, String.format(
                "Aprovar o recibo %s de %s?%n%nLíquido a pagar: %,.2f MT",
                sel.payslipNumber(), sel.employeeName(), sel.netPay()),
                "Aprovar Recibo", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        UIHelper.runWithProgress(owner, "A aprovar recibo…",
                () -> owner.hrApiClient.approvePayslip(sel.id()),
                ignored -> afterChange.run(), owner::showActionError);
    }

    /**
     * Fecha o mês da folha. Um mês já pago, entregue ao Estado e contabilizado deixa de aceitar
     * recibos novos — cada recibo novo desalinhava a retenção já declarada.
     */
    void closeMonth() {
        int[] period = askPeriod("Fechar Mês da Folha");
        if (period == null) {
            return;
        }
        UIHelper.runWithProgress(owner, "A fechar o mês…",
                () -> owner.hrApiClient.closePayrollPeriod(period[0], period[1]),
                dto -> JOptionPane.showMessageDialog(owner, String.format(
                        "Folha de %02d/%d fechada. Emitir recibos nesse mês passa a exigir reabertura.",
                        dto.month(), dto.year()), "Sucesso", JOptionPane.INFORMATION_MESSAGE),
                owner::showActionError);
    }

    /** Reabrir exige motivo e fica auditado — um fecho que se desfaz em silêncio não protege nada. */
    void reopenMonth() {
        int[] period = askPeriod("Reabrir Mês da Folha");
        if (period == null) {
            return;
        }
        String reason = UIHelper.promptRequiredText("Reabrir Mês da Folha", "fas-lock-open",
                String.format("Folha de %02d/%d", period[1], period[0]), "Motivo da reabertura:");
        if (reason == null) {
            return;
        }
        UIHelper.runWithProgress(owner, "A reabrir o mês…",
                () -> owner.hrApiClient.reopenPayrollPeriod(period[0], period[1], reason),
                ignored -> JOptionPane.showMessageDialog(owner, "Mês reaberto.",
                        "Sucesso", JOptionPane.INFORMATION_MESSAGE),
                owner::showActionError);
    }

    /**
     * Ficheiro de pagamento bancário do mês (§B8.7). Numa folha de 30 pessoas pagava-se uma a uma.
     * O CSV é mostrado para conferência antes de ser gravado — um ficheiro de pagamento que se
     * submete sem ninguém o ver é a forma cara de descobrir um engano.
     */
    void bankPaymentFile() {
        int[] period = askPeriod("Ficheiro de Pagamento");
        if (period == null) {
            return;
        }
        UIHelper.runWithProgress(owner, "A gerar o ficheiro…",
                () -> owner.hrApiClient.getBankPaymentFile(period[0], period[1]),
                this::showBankFile, owner::showActionError);
    }

    private void showBankFile(BankPaymentFileDTO file) {
        StringBuilder text = new StringBuilder(file.csv());
        if (!file.missingAccount().isEmpty()) {
            // Fora do ficheiro, mas nunca em silêncio: um pagamento que falta é a coisa que menos
            // pode desaparecer sem aviso.
            text.append("\n--- SEM CONTA BANCÁRIA (ficam de fora do ficheiro) ---\n");
            file.missingAccount().forEach(name -> text.append(name).append('\n'));
        }
        JTextArea area = new JTextArea(text.toString());
        area.setEditable(false);
        area.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(760, 340));

        JOptionPane.showMessageDialog(owner, scroll, String.format(
                "Pagamento %02d/%d — %d pagamento(s), total %,.2f MT",
                file.month(), file.year(), file.paymentCount(), file.totalAmount()),
                JOptionPane.INFORMATION_MESSAGE);
    }

    /** Ano e mês, na mesma pergunta. Devolve nulo se o utilizador cancelar. */
    private int[] askPeriod(String title) {
        LocalDate today = LocalDate.now();
        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(today.getYear(), 2000, 2100, 1));
        yearSpinner.setEditor(new JSpinner.NumberEditor(yearSpinner, "#"));
        JSpinner monthSpinner = new JSpinner(new SpinnerNumberModel(today.getMonthValue(), 1, 12, 1));
        JPanel form = UIHelper.createDialogForm("Ano:", yearSpinner, "Mês:", monthSpinner);

        int answer = JOptionPane.showConfirmDialog(owner, form, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return null;
        }
        return new int[]{(Integer) yearSpinner.getValue(), (Integer) monthSpinner.getValue()};
    }
}
