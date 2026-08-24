package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.SimpleBarChart;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.AbsenceDTO;
import mz.multicore.erp.modules.hr.dto.CreateSalaryChangeRequest;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import mz.multicore.erp.modules.hr.dto.EmployeeDocumentDTO;
import mz.multicore.erp.modules.hr.dto.SalaryChangeDTO;
import mz.multicore.erp.modules.hr.dto.SaveEmployeeDocumentRequest;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

/**
 * Acções sobre um colaborador que não cabem mais no {@link HRPanel}: evolução salarial (§B4),
 * documentos com validade (§B8.8) e justificação de faltas (§B2 / RHC-25).
 *
 * <p>São <b>diálogos e não separadores</b> de propósito. Um documento e uma alteração salarial
 * pertencem a <i>uma pessoa</i>: procurá-los numa lista de toda a empresa é a forma errada de os
 * encontrar. E a barra de separadores do RH já está a 1352 px dos 1382 que a
 * {@code TabStripFitsTest} permite — mais um separador e algo desaparecia atrás das setas.
 */
final class HREmployeeActions {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[] SALARY_REASONS = {
            "AUMENTO", "PROMOCAO", "REVISAO_ANUAL", "ACORDO", "CORRECCAO"};
    private static final String[] SALARY_REASON_LABELS = {
            "Aumento", "Promoção", "Revisão anual", "Acordo", "Correcção"};

    private static final String[] DOCUMENT_TYPES = {
            "BI", "DIRE", "PASSAPORTE", "NUIT", "CERTIFICADO", "OUTRO"};

    /** Tipos de falta para justificação. O primeiro é o que a falta gerada pelo ponto deixa de ser. */
    private static final String[] ABSENCE_TYPES = {
            "JUSTIFIED", "SICK", "MATERNITY", "UNJUSTIFIED", "UNPAID_LEAVE"};
    private static final String[] ABSENCE_TYPE_LABELS = {
            "Justificada (remunerada)", "Baixa médica (remunerada)", "Maternidade (remunerada)",
            "Injustificada (desconta)", "Licença sem vencimento (desconta)"};

    private final HRPanel owner;
    private final Supplier<EmployeeDTO> employeeSelection;
    private final Supplier<AbsenceDTO> absenceSelection;
    private final Runnable afterAbsenceChange;

    HREmployeeActions(HRPanel owner,
                      Supplier<EmployeeDTO> employeeSelection,
                      Supplier<AbsenceDTO> absenceSelection,
                      Runnable afterAbsenceChange) {
        this.owner = owner;
        this.employeeSelection = employeeSelection;
        this.absenceSelection = absenceSelection;
        this.afterAbsenceChange = afterAbsenceChange;
    }

    // ─── §B4: evolução salarial ───────────────────────────────────────────────

    /**
     * A série datada do colaborador, em gráfico e em tabela. É o ecrã que responde a "quanto é que
     * esta pessoa ganhava em Março?" — a pergunta que cada recibo faz e que, antes do B4, não tinha
     * resposta possível porque o salário anterior não ficava em lado nenhum.
     */
    void openSalaryHistory() {
        EmployeeDTO employee = employeeSelection.get();
        if (employee == null) {
            return;
        }
        UIHelper.runWithProgress(owner, "A carregar evolução salarial…",
                () -> owner.hrApiClient.getSalaryHistory(employee.id()),
                history -> showSalaryHistory(employee, history), owner::showActionError);
    }

    private void showSalaryHistory(EmployeeDTO employee, List<SalaryChangeDTO> history) {
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.setPreferredSize(new Dimension(720, 460));

        SimpleBarChart chart = new SimpleBarChart("Evolução do salário base");
        // A série vem da mais recente para a mais antiga; o gráfico lê-se da esquerda para a
        // direita, pelo que a ordem tem de ser invertida — senão a subida aparece como descida.
        int size = history.size();
        String[] labels = new String[size];
        java.math.BigDecimal[] values = new java.math.BigDecimal[size];
        Color[] colors = new Color[size];
        for (int i = 0; i < size; i++) {
            SalaryChangeDTO change = history.get(size - 1 - i);
            labels[i] = change.effectiveDate().format(DATE_FMT);
            values[i] = change.newSalary() == null ? java.math.BigDecimal.ZERO : change.newSalary();
            colors[i] = UIHelper.KPI_PURPLE_DARK;
        }
        chart.setData(labels, values, colors);
        content.add(chart, BorderLayout.CENTER);

        String[] cols = {"Data de efeito", "Anterior (MT)", "Novo (MT)", "Diferença (MT)",
                "Motivo", "Função", "Aprovado por"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (SalaryChangeDTO change : history) {
            model.addRow(new Object[]{
                    change.effectiveDate().format(DATE_FMT),
                    change.previousSalary(), change.newSalary(), change.difference(),
                    change.reasonLabel(),
                    change.jobTitle() == null ? "-" : change.jobTitle(),
                    change.approvedBy()});
        }
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(720, 180));
        content.add(scroll, BorderLayout.SOUTH);

        Object[] options = {"Registar Alteração", "Fechar"};
        int answer = JOptionPane.showOptionDialog(owner, content,
                "Evolução Salarial — " + employee.name(), JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[1]);
        if (answer == 0) {
            openSalaryChangeDialog(employee);
        }
    }

    private void openSalaryChangeDialog(EmployeeDTO employee) {
        MoneyField salaryField = new MoneyField(
                employee.baseSalary() == null ? "0.00" : employee.baseSalary().toPlainString());
        DateField effectiveField = new DateField(LocalDate.now());
        JComboBox<String> reasonCombo = new JComboBox<>(SALARY_REASON_LABELS);
        UIHelper.styleComboBox(reasonCombo);
        JTextField jobField = new JTextField();
        UIHelper.styleTextField(jobField);
        JTextField notesField = new JTextField();
        UIHelper.styleTextField(notesField);

        JPanel form = UIHelper.createDialogForm(
                "Novo salário:", salaryField,
                "Data de efeito:", effectiveField,
                "Motivo:", reasonCombo,
                "Nova função (vazio = mantém):", jobField,
                "Observações:", notesField);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow,
                "Alteração Salarial — " + employee.name(), "fas-chart-line",
                "Uma data de efeito futura é um compromisso: não mexe na ficha até lá chegar, mas "
                        + "já manda no recibo desse mês.", form).showDialog();
        if (!confirmed) {
            return;
        }

        CreateSalaryChangeRequest request = new CreateSalaryChangeRequest(
                employee.id(), salaryField.value(), effectiveField.value(),
                SALARY_REASONS[reasonCombo.getSelectedIndex()],
                jobField.getText().trim().isEmpty() ? null : jobField.getText().trim(), null,
                notesField.getText().trim().isEmpty() ? null : notesField.getText().trim());
        UIHelper.runWithProgress(owner, "A registar alteração salarial…",
                () -> owner.hrApiClient.registerSalaryChange(request),
                ignored -> {
                    JOptionPane.showMessageDialog(owner, "Alteração salarial registada.",
                            "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                    owner.refreshData();
                }, owner::showActionError);
    }

    // ─── §B8.8: documentos do colaborador ─────────────────────────────────────

    /** Documentos e validades. O DIRE de um trabalhador estrangeiro caducar sem aviso é multa. */
    void openDocuments() {
        EmployeeDTO employee = employeeSelection.get();
        if (employee == null) {
            return;
        }
        UIHelper.runWithProgress(owner, "A carregar documentos…",
                () -> owner.hrApiClient.getEmployeeDocuments(employee.id()),
                documents -> showDocuments(employee, documents), owner::showActionError);
    }

    private void showDocuments(EmployeeDTO employee, List<EmployeeDocumentDTO> documents) {
        String[] cols = {"Tipo", "Número", "Emissão", "Validade", "Situação"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (EmployeeDocumentDTO d : documents) {
            model.addRow(new Object[]{
                    d.documentType(),
                    d.documentNumber() == null ? "-" : d.documentNumber(),
                    d.issueDate() == null ? "-" : d.issueDate().format(DATE_FMT),
                    d.expiryDate() == null ? "Não caduca" : d.expiryDate().format(DATE_FMT),
                    situationOf(d)});
        }
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(640, 260));

        Object[] options = {"Novo Documento", "Fechar"};
        int answer = JOptionPane.showOptionDialog(owner, scroll,
                "Documentos — " + employee.name(), JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[1]);
        if (answer == 0) {
            openDocumentDialog(employee);
        }
    }

    /** Nunca "-": um documento sem validade diz que não caduca, e um caducado diz há quantos dias. */
    private String situationOf(EmployeeDocumentDTO d) {
        if (d.expiryDate() == null) {
            return "Sem validade";
        }
        if (d.expired()) {
            return "Caducado há " + Math.abs(d.daysUntilExpiry()) + " dia(s)";
        }
        return "Válido — faltam " + d.daysUntilExpiry() + " dia(s)";
    }

    private void openDocumentDialog(EmployeeDTO employee) {
        JComboBox<String> typeCombo = new JComboBox<>(DOCUMENT_TYPES);
        UIHelper.styleComboBox(typeCombo);
        JTextField numberField = new JTextField();
        UIHelper.styleTextField(numberField);
        DateField issueField = new DateField(null);
        DateField expiryField = new DateField(null);
        JTextField notesField = new JTextField();
        UIHelper.styleTextField(notesField);

        JPanel form = UIHelper.createDialogForm(
                "Tipo:", typeCombo,
                "Número:", numberField,
                "Emissão:", issueField,
                "Validade (vazio = não caduca):", expiryField,
                "Observações:", notesField);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow,
                "Novo Documento — " + employee.name(), "fas-id-card",
                "Deixar a validade vazia significa que o documento não caduca — não é o mesmo que "
                        + "ainda não a saber.", form).showDialog();
        if (!confirmed) {
            return;
        }
        SaveEmployeeDocumentRequest request = new SaveEmployeeDocumentRequest(
                employee.id(), (String) typeCombo.getSelectedItem(),
                numberField.getText().trim().isEmpty() ? null : numberField.getText().trim(),
                issueField.value(), expiryField.value(),
                notesField.getText().trim().isEmpty() ? null : notesField.getText().trim());
        UIHelper.runWithProgress(owner, "A gravar documento…",
                () -> owner.hrApiClient.saveEmployeeDocument(request),
                ignored -> JOptionPane.showMessageDialog(owner, "Documento registado.",
                        "Sucesso", JOptionPane.INFORMATION_MESSAGE),
                owner::showActionError);
    }

    // ─── §B2 (RHC-25): justificar uma falta ───────────────────────────────────

    /**
     * Justificar muda o <b>tipo</b> da falta, com motivo obrigatório. É aqui que se decide se a
     * ausência desconta: uma falta nascida do fecho do ponto nasce por justificar justamente para
     * que essa decisão seja de alguém, e fique com nome.
     */
    void justifyAbsence() {
        AbsenceDTO absence = absenceSelection.get();
        if (absence == null) {
            return;
        }
        JComboBox<String> typeCombo = new JComboBox<>(ABSENCE_TYPE_LABELS);
        UIHelper.styleComboBox(typeCombo);
        JTextField reasonField = new JTextField();
        UIHelper.styleTextField(reasonField);
        JCheckBox documentBox = new JCheckBox("Com documento comprovativo");
        documentBox.setOpaque(false);
        documentBox.setForeground(UIHelper.TEXT_LIGHT);

        JPanel form = UIHelper.createDialogForm(
                "Passa a ser:", typeCombo,
                "Motivo:", reasonField,
                "Documento:", documentBox);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Justificar Falta",
                "fas-user-check",
                String.format("%s — %s (%d dia(s)), hoje %s", absence.employeeName(),
                        absence.startDate().format(DATE_FMT), absence.totalDays(),
                        absence.absenceType()), form).showDialog();
        if (!confirmed) {
            return;
        }
        String reason = reasonField.getText().trim();
        if (reason.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Justificar uma falta exige um motivo.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String type = ABSENCE_TYPES[typeCombo.getSelectedIndex()];
        boolean hasDocument = documentBox.isSelected();
        UIHelper.runWithProgress(owner, "A justificar falta…",
                () -> owner.hrApiClient.justifyAbsence(absence.id(), type, reason, hasDocument),
                ignored -> afterAbsenceChange.run(), owner::showActionError);
    }
}
