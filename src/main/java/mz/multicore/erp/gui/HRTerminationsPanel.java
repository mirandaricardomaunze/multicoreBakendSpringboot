package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.CreateTerminationRequest;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import mz.multicore.erp.modules.hr.dto.TerminationDTO;
import mz.multicore.erp.modules.printing.PdfFileSaver;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Separador das <b>cessações e acertos finais</b>. Ver docs/RH_COMPLETO_SPEC.md §B3.
 *
 * <p>Substitui o que era uma mudança de texto de {@code ACTIVE} para {@code TERMINATED}. O
 * colaborador que sai tem direito a proporcionais que o sistema já sabia calcular — 13.º, férias
 * não gozadas — e que nunca calculava nesta situação: era feito à mão, em papel, ou não era feito.
 *
 * <p><b>Cessar mostra a conta primeiro.</b> É irreversível, e a conta confere-se em papel contra o
 * que o sistema apurou; obrigar a cessar para ver os números seria pedir para descobrir o erro
 * tarde de mais.
 */
final class HRTerminationsPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[] REASONS = {
            "INICIATIVA_TRABALHADOR", "INICIATIVA_EMPREGADOR", "MUTUO_ACORDO",
            "FIM_DO_TERMO", "JUSTA_CAUSA"};
    private static final String[] REASON_LABELS = {
            "Iniciativa do trabalhador", "Iniciativa do empregador", "Mútuo acordo",
            "Fim do termo", "Justa causa"};

    private final HRPanel owner;

    private DefaultTableModel model;
    private JTable table;
    private List<TerminationDTO> terminations = List.of();

    HRTerminationsPanel(HRPanel owner) {
        this.owner = owner;
    }

    JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Cessações e Acertos Finais"), BorderLayout.WEST);

        ModernButton newBtn = UIHelper.createPrimaryButton("Cessar Vínculo");
        newBtn.setIcon(UIHelper.icon("fas-user-slash", 14));
        newBtn.addActionListener(e -> openTerminationDialog());
        ModernButton payBtn = UIHelper.createSuccessButton("Pagar Acerto");
        payBtn.setIcon(UIHelper.icon("fas-money-check-alt", 14));
        payBtn.addActionListener(e -> paySettlement());
        ModernButton settlementBtn = UIHelper.createSecondaryButton("Acerto PDF");
        settlementBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        settlementBtn.addActionListener(e -> printSettlement());
        ModernButton certificateBtn = UIHelper.createSecondaryButton("Certificado");
        certificateBtn.setIcon(UIHelper.icon("fas-file-alt", 14));
        certificateBtn.addActionListener(e -> printCertificate());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(certificateBtn);
        actions.add(settlementBtn);
        actions.add(payBtn);
        actions.add(newBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"ID", "Acerto", "Colaborador", "Saída", "Motivo", "Aviso Prévio",
                "Ganhos (MT)", "Descontos (MT)", "Líquido (MT)", "Estado"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        for (int column : new int[]{6, 7, 8}) {
            table.getColumnModel().getColumn(column).setCellRenderer(TableCellRenderers.money());
        }
        table.getColumnModel().getColumn(9).setCellRenderer(TableCellRenderers.status());
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        JTextField search = TableFilter.searchField("Colaborador ou acerto…");
        JComboBox<String> estado = TableFilter.combo("Todos os estados", "Por pagar", "Pago");
        JComboBox<String> periodo = TableFilter.periodCombo();
        TableFilter.install(table, search, List.of(new TableFilter.ColumnFilter(estado, 9)),
                List.of(new TableFilter.PeriodFilter(periodo, 3)));
        JPanel bar = TableFilter.bar(search, TableFilter.label("Estado:"), estado,
                TableFilter.label("Saída:", "fas-calendar-alt"), periodo);
        bar.setBorder(new EmptyBorder(0, 0, 10, 0));

        card.add(bar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    void load() {
        UIHelper.loadAsync(owner, owner.hrApiClient::getTerminations, this::apply,
                error -> owner.showLoadError("cessações", error));
    }

    private void apply(List<TerminationDTO> loaded) {
        terminations = loaded;
        model.setRowCount(0);
        for (TerminationDTO t : terminations) {
            model.addRow(new Object[]{
                    t.id(), t.settlementNumber(), t.employeeName(),
                    t.terminationDate() == null ? "-" : t.terminationDate().format(DATE_FMT),
                    t.reasonLabel(), t.noticeServed() ? "Cumprido" : "Não cumprido",
                    t.totalEarnings(), t.totalDeductions(), t.netAmount(), t.statusLabel()
            });
        }
    }

    private void openTerminationDialog() {
        if (owner.employeesList.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Cadastre colaboradores primeiro.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JComboBox<String> empCombo = new JComboBox<>();
        UIHelper.styleComboBox(empCombo);
        for (EmployeeDTO e : owner.employeesList) {
            empCombo.addItem(e.name() + " — " + e.department());
        }
        JComboBox<String> reasonCombo = new JComboBox<>(REASON_LABELS);
        UIHelper.styleComboBox(reasonCombo);
        DateField exitField = new DateField(LocalDate.now());
        JCheckBox noticeBox = new JCheckBox("Aviso prévio cumprido", true);
        noticeBox.setOpaque(false);
        noticeBox.setForeground(UIHelper.TEXT_LIGHT);
        MoneyField compensationField = new MoneyField("0.00");
        JTextField notesField = new JTextField();
        UIHelper.styleTextField(notesField);

        JPanel form = UIHelper.createDialogForm(
                "Colaborador:", empCombo,
                "Data de saída:", exitField,
                "Motivo:", reasonCombo,
                "Aviso prévio:", noticeBox,
                "Compensação (0 = nenhuma):", compensationField,
                "Observações:", notesField
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Cessar Vínculo",
                "fas-user-slash",
                "A compensação por cessação é introduzida por si: a fórmula é legal, varia com o "
                        + "motivo e a antiguidade, e não é o sistema que a decide.", form).showDialog();
        if (!confirmed) {
            return;
        }

        EmployeeDTO employee = owner.employeesList.get(empCombo.getSelectedIndex());
        CreateTerminationRequest request = new CreateTerminationRequest(
                employee.id(), exitField.value(), REASONS[reasonCombo.getSelectedIndex()],
                noticeBox.isSelected(),
                compensationField.value().signum() > 0 ? compensationField.value() : null,
                notesField.getText().trim().isEmpty() ? null : notesField.getText().trim());

        UIHelper.runWithProgress(owner, "A apurar o acerto…",
                () -> owner.hrApiClient.previewTermination(request),
                preview -> confirmAndTerminate(request, preview),
                owner::showActionError);
    }

    /**
     * Mostra a conta e só depois pergunta. Os <b>avisos</b> vêm com destaque: um acerto que esconde
     * o que não sabe calcular — porque o direito a férias ou o aviso prévio não estão configurados —
     * é muito pior do que um acerto incompleto que o declara.
     */
    private void confirmAndTerminate(CreateTerminationRequest request, TerminationDTO preview) {
        StringBuilder detail = new StringBuilder();
        detail.append("<html><b>").append(preview.employeeName()).append("</b> — saída a ")
                .append(preview.terminationDate().format(DATE_FMT)).append("<br><br>");
        for (TerminationDTO.TerminationLineDTO line : preview.lines()) {
            detail.append(line.earning() ? "+ " : "− ").append(line.description()).append(" — ")
                    .append(String.format("%,.2f MT", line.amount())).append("<br>");
        }
        detail.append("<br><b>").append(preview.netAmount().signum() < 0
                        ? "Saldo a favor da empresa: " : "Líquido a receber: ")
                .append(String.format("%,.2f MT", preview.netAmount().abs())).append("</b>");
        if (!preview.warnings().isEmpty()) {
            detail.append("<br><br><b>Atenção:</b><br>");
            for (String warning : preview.warnings()) {
                detail.append("• ").append(warning).append("<br>");
            }
        }
        detail.append("<br>Cessar é irreversível. Confirmar?</html>");

        int answer = JOptionPane.showConfirmDialog(owner, new JLabel(detail.toString()),
                "Acerto Final", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        UIHelper.runWithProgress(owner, "A cessar o vínculo…",
                () -> owner.hrApiClient.terminate(request),
                ignored -> {
                    JOptionPane.showMessageDialog(owner,
                            "Cessação registada. O acerto fica por pagar até alguém o liquidar.",
                            "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                    load();
                    owner.refreshData();
                }, owner::showActionError);
    }

    private void paySettlement() {
        TerminationDTO sel = selected();
        if (sel == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(owner, String.format(
                "Pagar o acerto %s a %s, no valor de %,.2f MT?",
                sel.settlementNumber(), sel.employeeName(), sel.netAmount()),
                "Pagar Acerto Final", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        UIHelper.runWithProgress(owner, "A pagar o acerto…",
                () -> owner.hrApiClient.paySettlement(sel.id()),
                ignored -> load(), owner::showActionError);
    }

    private void printSettlement() {
        TerminationDTO sel = selected();
        if (sel == null) {
            return;
        }
        UIHelper.runWithProgress(owner, "A gerar o acerto…",
                () -> owner.hrApiClient.renderSettlement(sel.id()),
                pdf -> PdfFileSaver.saveAndOpen(pdf, "acerto-final-" + sel.settlementNumber()),
                owner::showActionError);
    }

    private void printCertificate() {
        TerminationDTO sel = selected();
        if (sel == null) {
            return;
        }
        UIHelper.runWithProgress(owner, "A gerar o certificado…",
                () -> owner.hrApiClient.renderWorkCertificate(sel.id()),
                pdf -> PdfFileSaver.saveAndOpen(pdf, "certificado-" + sel.settlementNumber()),
                owner::showActionError);
    }

    private TerminationDTO selected() {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0) {
            JOptionPane.showMessageDialog(owner, "Selecione uma cessação na tabela.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return terminations.get(row);
    }
}
