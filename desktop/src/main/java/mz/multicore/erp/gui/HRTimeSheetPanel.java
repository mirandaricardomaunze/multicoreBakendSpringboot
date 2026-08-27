package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.CreateTimeEntryRequest;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import mz.multicore.erp.modules.hr.dto.TimeSheetDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Separador de ponto e assiduidade. Ver docs/RH_COMPLETO_SPEC.md §B2.
 *
 * <p>Mostra o <b>apuramento</b>, não as marcações em bruto: quem abre este ecrã quer saber quantas
 * horas extra há para pagar e a quem, e as horas extra vêm separadas por escalão porque a lei as
 * trata de maneira diferente. As marcações são a origem; o que se lê aqui é a conta.
 */
final class HRTimeSheetPanel {

    private final HRPanel owner;
    private DefaultTableModel model;
    private JTable table;
    private JSpinner yearSpinner;
    private JSpinner monthSpinner;
    private JLabel statusLabel;
    private ModernButton closeBtn;
    private List<TimeSheetDTO.TimeSheetLineDTO> lines = List.of();

    HRTimeSheetPanel(HRPanel owner) {
        this.owner = owner;
    }

    JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        LocalDate today = LocalDate.now();
        yearSpinner = new JSpinner(new SpinnerNumberModel(today.getYear(), 2000, 2100, 1));
        monthSpinner = new JSpinner(new SpinnerNumberModel(today.getMonthValue(), 1, 12, 1));
        yearSpinner.addChangeListener(e -> load());
        monthSpinner.addChangeListener(e -> load());

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(UIHelper.TEXT_LIGHT);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(UIHelper.createSubheading("Folha de Ponto"));
        left.add(new JLabel("Ano:"));
        left.add(yearSpinner);
        left.add(new JLabel("Mês:"));
        left.add(monthSpinner);
        left.add(statusLabel);
        header.add(left, BorderLayout.WEST);

        ModernButton exportBtn = UIHelper.createSecondaryButton("Exportar PDF");
        exportBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        exportBtn.addActionListener(e -> owner.exportTable("folha-ponto", "Folha de Ponto", table));

        ModernButton entryBtn = UIHelper.createPrimaryButton("Registar Marcação");
        entryBtn.setIcon(UIHelper.icon("fas-clock", 14));
        entryBtn.addActionListener(e -> recordEntry());

        closeBtn = UIHelper.createSecondaryButton("Fechar Mês");
        closeBtn.setIcon(UIHelper.icon("fas-lock", 14));
        closeBtn.addActionListener(e -> toggleClose());

        ModernButton ratesBtn = UIHelper.createSecondaryButton("Acréscimos");
        ratesBtn.setIcon(UIHelper.icon("fas-percentage", 14));
        ratesBtn.addActionListener(e -> openOvertimeRatesDialog());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(exportBtn);
        actions.add(ratesBtn);
        actions.add(closeBtn);
        actions.add(entryBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Colaborador", "Dias prev.", "Dias trab.", "Sem marcação",
                "H. normais", "Extra dia", "Extra noite", "Descanso/feriado", "Atrasos"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        JTextField search = TableFilter.searchField("Colaborador…");
        TableFilter.install(table, search);
        JPanel bar = TableFilter.bar(search);
        bar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(bar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);

        load();
        return tab;
    }

    void load() {
        int year = (Integer) yearSpinner.getValue();
        int month = (Integer) monthSpinner.getValue();
        UIHelper.loadAsync(owner, () -> owner.hrApiClient.getMonthlySheet(year, month), this::apply,
                error -> owner.showLoadError("folha de ponto", error));
    }

    private void apply(TimeSheetDTO sheet) {
        lines = sheet.lines();
        model.setRowCount(0);
        for (TimeSheetDTO.TimeSheetLineDTO line : lines) {
            model.addRow(new Object[]{
                    line.employeeName(), line.expectedDays(), line.workedDays(), line.missingDays(),
                    line.normalHours(), line.overtimeDayHours(), line.overtimeNightHours(),
                    line.restDayHours(), line.lateArrivals()});
        }
        // O estado do mês manda no que se pode fazer: fechado, não se marca nem se apaga.
        statusLabel.setText("  •  " + sheet.statusLabel()
                + (sheet.closedBy() == null ? "" : " por " + sheet.closedBy()));
        closeBtn.setText(sheet.closed() ? "Reabrir Mês" : "Fechar Mês");
        closeBtn.setIcon(UIHelper.icon(sheet.closed() ? "fas-lock-open" : "fas-lock", 14));
    }

    private void toggleClose() {
        int year = (Integer) yearSpinner.getValue();
        int month = (Integer) monthSpinner.getValue();
        boolean closed = closeBtn.getText().startsWith("Reabrir");

        if (closed) {
            String reason = UIHelper.promptRequiredText("Reabrir Folha de Ponto", "fas-lock-open",
                    "Mês " + month + "/" + year, "Motivo da reabertura:");
            if (reason == null) return;
            UIHelper.runWithProgress(owner, "A reabrir folha de ponto…",
                    () -> owner.hrApiClient.reopenTimeSheet(year, month, reason),
                    this::apply, owner::showActionError);
            return;
        }
        int option = JOptionPane.showConfirmDialog(owner,
                "Fechar a folha de ponto de " + month + "/" + year + "?\n"
                        + "Depois de fechada só se altera com reabertura justificada.",
                "Fechar Mês", JOptionPane.YES_NO_OPTION);
        if (option != JOptionPane.YES_OPTION) return;
        UIHelper.runWithProgress(owner, "A fechar folha de ponto…",
                () -> owner.hrApiClient.closeTimeSheet(year, month), this::apply, owner::showActionError);
    }

    private void recordEntry() {
        if (owner.employeesList.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Cadastre colaboradores primeiro.", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        JComboBox<String> empCombo = new JComboBox<>();
        UIHelper.styleComboBox(empCombo);
        for (EmployeeDTO e : owner.employeesList) empCombo.addItem(e.name() + " — " + e.department());

        DateField dateField = new DateField(LocalDate.now());
        JTextField inField = new JTextField("08:00");
        JTextField outField = new JTextField("17:00");
        JSpinner breakSpinner = new JSpinner(new SpinnerNumberModel(60, 0, 480, 15));
        JTextField notesField = new JTextField();
        for (JTextField f : new JTextField[]{inField, outField, notesField}) UIHelper.styleTextField(f);

        JPanel form = UIHelper.createDialogForm(
                "Colaborador:", empCombo,
                "Data (yyyy-MM-dd):", dateField,
                "Entrada (HH:mm):", inField,
                "Saída (HH:mm):", outField,
                "Pausa (minutos):", breakSpinner,
                "Observação:", notesField);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Registar Marcação",
                "fas-clock", "Marcação de ponto", form).showDialog();
        if (!confirmed) return;

        try {
            EmployeeDTO employee = owner.employeesList.get(empCombo.getSelectedIndex());
            CreateTimeEntryRequest request = new CreateTimeEntryRequest(
                    employee.id(), dateField.value(),
                    LocalTime.parse(inField.getText().trim()),
                    LocalTime.parse(outField.getText().trim()),
                    (Integer) breakSpinner.getValue(), "MANUAL",
                    notesField.getText().trim().isEmpty() ? null : notesField.getText().trim());
            UIHelper.runWithProgress(owner, "A registar marcação…",
                    () -> owner.hrApiClient.recordTimeEntry(request),
                    ignored -> load(), owner::showActionError);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(owner, "Hora inválida. Use o formato HH:mm.", "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Configuração dos <b>acréscimos de hora extra</b> (§B2 e §6).
     *
     * <p>Vive ao lado do apuramento porque é aqui que se dá pela falta dela: sem configuração em
     * vigor, o recibo <b>recusa-se a valorizar horas extra</b> e diz que os valores têm de vir do
     * contabilista. Escrever uma percentagem à sorte era o pior resultado possível — parecia certo
     * e pagava mal, e ninguém reparava até alguém reclamar.
     */
    private void openOvertimeRatesDialog() {
        java.util.List<mz.multicore.erp.modules.hr.dto.OvertimeRateConfigDTO> existing;
        try {
            existing = owner.hrApiClient.getOvertimeRates();
        } catch (RuntimeException ex) {
            owner.showActionError(ex);
            return;
        }
        var current = existing.stream()
                .filter(mz.multicore.erp.modules.hr.dto.OvertimeRateConfigDTO::active)
                .findFirst().orElse(null);

        JTextField nameField = new JTextField(current == null ? "Acréscimos de hora extra" : current.name());
        UIHelper.styleTextField(nameField);
        DateField fromField = new DateField(LocalDate.now().withDayOfMonth(1));
        JTextField dayField = new JTextField(current == null ? "" : current.dayMultiplier().toPlainString());
        UIHelper.styleTextField(dayField);
        JTextField nightField = new JTextField(current == null ? "" : current.nightMultiplier().toPlainString());
        UIHelper.styleTextField(nightField);
        JTextField restField = new JTextField(current == null ? "" : current.restDayMultiplier().toPlainString());
        UIHelper.styleTextField(restField);
        JTextField basisField = new JTextField(
                current == null || current.legalBasis() == null ? "" : current.legalBasis());
        UIHelper.styleTextField(basisField);

        JPanel form = UIHelper.createDialogForm(
                "Nome:", nameField,
                "Vigora a partir de:", fromField,
                "Extra diurna (ex.: 1.50):", dayField,
                "Extra nocturna:", nightField,
                "Dia de descanso/feriado:", restField,
                "Base legal:", basisField);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Acréscimos de Hora Extra",
                "fas-percentage",
                "Multiplicadores sobre o valor/hora normal. Confirme-os com o contabilista da "
                        + "empresa — não são valores que o sistema possa decidir.", form).showDialog();
        if (!confirmed) {
            return;
        }
        try {
            var request = new mz.multicore.erp.modules.hr.dto.SaveOvertimeRateConfigRequest(
                    nameField.getText().trim(), fromField.value(), null,
                    new java.math.BigDecimal(dayField.getText().trim()),
                    new java.math.BigDecimal(nightField.getText().trim()),
                    new java.math.BigDecimal(restField.getText().trim()),
                    basisField.getText().trim().isEmpty() ? null : basisField.getText().trim());
            UIHelper.runWithProgress(owner, "A gravar acréscimos…",
                    () -> owner.hrApiClient.saveOvertimeRates(request),
                    ignored -> JOptionPane.showMessageDialog(owner,
                            "Acréscimos gravados. Os recibos passam a valorizar horas extra por eles.",
                            "Sucesso", JOptionPane.INFORMATION_MESSAGE),
                    owner::showActionError);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(owner,
                    "Os multiplicadores têm de ser números (ex.: 1.50).", "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
