package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.modules.hr.dto.ThirteenthMonthDTO;
import mz.multicore.erp.modules.hr.dto.VacationAllowanceDTO;
import mz.multicore.erp.modules.hr.dto.VacationDTO;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.LocalDate;

/** Acções Swing dos subsídios legais; todo o apuramento permanece no backend. */
final class HRBonusActions {

    private final HRPanel owner;

    HRBonusActions(HRPanel owner) {
        this.owner = owner;
    }

    void openThirteenthMonth() {
        JSpinner year = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2000, 2100, 1));
        JPanel form = UIHelper.createDialogForm("Ano de referência:", year);
        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Apurar 13.º Mês",
                "fas-gift", "Cálculo proporcional devolvido pelo servidor", form)
                .setConfirmButton("Apurar", "fas-calculator")
                .showDialog();
        if (!confirmed) return;

        int selectedYear = (Integer) year.getValue();
        UIHelper.loadAsync(owner, () -> owner.hrApiClient.getThirteenthMonth(selectedYear),
                this::showThirteenthMonth, owner::showActionError);
    }

    private void showThirteenthMonth(ThirteenthMonthDTO dto) {
        String[] columns = {"Nº", "Colaborador", "Meses", "Salário Base (MT)", "13.º Mês (MT)"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        for (var line : dto.lines()) {
            model.addRow(new Object[]{line.employeeNumber(), line.employeeName(), line.monthsWorked(),
                    line.baseSalary(), line.amount()});
        }
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.money());
        UIHelper.ensureHeadersFit(table);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(720, 310));

        JLabel total = new JLabel(String.format("Total apurado: %,.2f MT · %d colaborador(es)",
                dto.total(), dto.lines().size()));
        total.setForeground(UIHelper.TEXT_LIGHT);
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.add(scroll, BorderLayout.CENTER);
        content.add(total, BorderLayout.SOUTH);

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow,
                "13.º Mês — " + dto.year(), "fas-gift", "Confirme apenas depois de rever o apuramento", content)
                .setConfirmButton("Continuar", "fas-arrow-right")
                .setSize(820, 520);
        if (!dialog.showDialog()) return;
        if (dto.lines().isEmpty()) {
            owner.showNotice(FeedbackType.INFO, "Sem valores a pagar", "Não existem valores apurados neste ano.");
            return;
        }
        if (!ModernMessageDialog.confirm(UIHelper.mainWindow, FeedbackType.WARNING,
                "Confirmar Pagamento", String.format("Pagar %,.2f MT de 13.º mês a %d colaborador(es)?",
                        dto.total(), dto.lines().size()), "Pagar")) return;
        UIHelper.runWithProgress(owner, "A pagar o 13.º mês…",
                () -> owner.hrApiClient.payThirteenthMonth(dto.year()), paid ->
                        owner.showSuccess(String.format("13.º mês pago a %d colaborador(es) · %,.2f MT.",
                                paid.lines().size(), paid.total())), owner::showActionError);
    }

    void openVacationAllowance(VacationDTO vacation) {
        UIHelper.loadAsync(owner, () -> owner.hrApiClient.getVacationAllowance(vacation.id()),
                dto -> showVacationAllowance(vacation, dto), owner::showActionError);
    }

    private void showVacationAllowance(VacationDTO vacation, VacationAllowanceDTO dto) {
        JPanel form = UIHelper.createDialogForm(
                "Colaborador:", value(dto.employeeName()),
                "Pedido de férias:", value("#" + dto.vacationId()),
                "Período:", value(vacation.startDate() + " a " + vacation.endDate()),
                "Dias:", value(String.valueOf(dto.days())),
                "Valor diário:", value(String.format("%,.2f MT", dto.dailyRate())),
                "Total:", value(String.format("%,.2f MT", dto.amount())));
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Subsídio de Férias",
                "fas-umbrella-beach", "Apuramento do pedido aprovado", form)
                .setConfirmButton("Continuar", "fas-arrow-right");
        if (!dialog.showDialog()) return;
        if (!ModernMessageDialog.confirm(UIHelper.mainWindow, FeedbackType.WARNING,
                "Confirmar Pagamento", String.format("Pagar %,.2f MT de subsídio de férias a %s?",
                        dto.amount(), dto.employeeName()), "Pagar")) return;
        UIHelper.runWithProgress(owner, "A pagar o subsídio de férias…",
                () -> owner.hrApiClient.payVacationAllowance(dto.vacationId()), paid ->
                        owner.showSuccess(String.format("Subsídio de férias pago · %,.2f MT.", paid.amount())),
                owner::showActionError);
    }

    private JLabel value(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UIHelper.TEXT_LIGHT);
        return label;
    }
}
