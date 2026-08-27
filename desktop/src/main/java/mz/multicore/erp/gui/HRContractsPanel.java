package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.CreateContractRequest;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import mz.multicore.erp.modules.hr.dto.EmploymentContractDTO;
import mz.multicore.erp.modules.hr.dto.RenewContractRequest;
import mz.multicore.erp.modules.printing.PdfFileSaver;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Separador de contratos de trabalho. Ver docs/RH_COMPLETO_SPEC.md §B1.
 *
 * <p>Classe própria como o {@link HRExpensesPanel} — e não mais um {@code buildXTab()} dentro do
 * {@code HRPanel}, que está a dois passos do limite de 1000 linhas do
 * {@code UiPanelDecompositionTest}.
 */
final class HRContractsPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] TYPES = {
            "SEM_TERMO", "TERMO_CERTO", "TERMO_INCERTO", "TEMPORARIO", "ESTAGIO"};

    private final HRPanel owner;
    private DefaultTableModel model;
    private JTable table;
    private List<EmploymentContractDTO> loaded = List.of();

    HRContractsPanel(HRPanel owner) {
        this.owner = owner;
    }

    JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Contratos de Trabalho"), BorderLayout.WEST);

        ModernButton printBtn = UIHelper.createSecondaryButton("Imprimir PDF");
        printBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        printBtn.addActionListener(e -> withSelection(this::printContract));

        ModernButton activateBtn = UIHelper.createSecondaryButton("Activar");
        activateBtn.setIcon(UIHelper.icon("fas-check", 14));
        activateBtn.addActionListener(e -> withSelection(this::activateContract));

        ModernButton renewBtn = UIHelper.createSecondaryButton("Renovar");
        renewBtn.setIcon(UIHelper.icon("fas-redo", 14));
        renewBtn.addActionListener(e -> withSelection(this::renewContract));

        ModernButton terminateBtn = UIHelper.createSecondaryButton("Cessar");
        terminateBtn.setIcon(UIHelper.icon("fas-ban", 14));
        terminateBtn.addActionListener(e -> withSelection(this::terminateContract));

        ModernButton newBtn = UIHelper.createPrimaryButton("Novo Contrato");
        newBtn.setIcon(UIHelper.icon("fas-file-signature", 14));
        newBtn.addActionListener(e -> createContract());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(printBtn);
        actions.add(activateBtn);
        actions.add(renewBtn);
        actions.add(terminateBtn);
        actions.add(newBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        String[] cols = {"Nº", "Colaborador", "Tipo", "Função", "Início", "Fim", "Salário", "Estado"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.status());

        JTextField search = TableFilter.searchField("Nº, colaborador ou função…");
        JComboBox<String> statusFilter = TableFilter.combo("Todos os estados",
                "Rascunho", "Vigente", "Cessado", "Prazo terminado");
        TableFilter.install(table, search, new TableFilter.ColumnFilter(statusFilter, 7));

        JPanel bar = TableFilter.bar(search, TableFilter.label("Estado:"), statusFilter);
        bar.setBorder(new EmptyBorder(0, 0, 10, 0));
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(bar, BorderLayout.NORTH);
        body.add(new JScrollPane(table), BorderLayout.CENTER);
        tab.add(body, BorderLayout.CENTER);

        load();
        return tab;
    }

    void load() {
        UIHelper.loadAsync(owner, owner.hrApiClient::getAllContracts, this::apply,
                error -> owner.showLoadError("contratos", error));
    }

    private void apply(List<EmploymentContractDTO> contracts) {
        loaded = contracts;
        model.setRowCount(0);
        for (EmploymentContractDTO c : contracts) {
            model.addRow(new Object[]{
                    c.contractNumber(),
                    c.employeeName(),
                    c.contractTypeLabel(),
                    c.jobTitle(),
                    c.startDate() == null ? "" : c.startDate().format(DATE_FMT),
                    c.endDate() == null ? "Sem termo" : c.endDate().format(DATE_FMT),
                    c.agreedSalary(),
                    // A caducidade é derivada, não gravada: o ecrã diz o mesmo que o DTO calcula.
                    c.expired() ? "Prazo terminado" : c.statusLabel()
            });
        }
    }

    /** Corre a acção sobre o contrato seleccionado, ou avisa que falta seleccionar. */
    private void withSelection(java.util.function.Consumer<EmploymentContractDTO> action) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(owner, "Seleccione um contrato.", "Contratos",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        action.accept(loaded.get(table.convertRowIndexToModel(row)));
    }

    private void printContract(EmploymentContractDTO contract) {
        UIHelper.runWithProgress(owner, "A gerar contrato…",
                () -> owner.hrApiClient.renderContract(contract.id()),
                pdf -> PdfFileSaver.saveAndOpen(pdf, "contrato-" + contract.contractNumber()),
                owner::showActionError);
    }

    private void activateContract(EmploymentContractDTO contract) {
        int option = JOptionPane.showConfirmDialog(owner,
                "Pôr o contrato " + contract.contractNumber() + " a vigorar?\n"
                        + "O salário acordado passa a ser o da ficha do colaborador.",
                "Activar Contrato", JOptionPane.YES_NO_OPTION);
        if (option != JOptionPane.YES_OPTION) return;
        UIHelper.runWithProgress(owner, "A activar contrato…",
                () -> owner.hrApiClient.activateContract(contract.id()),
                ignored -> load(), owner::showActionError);
    }

    private void createContract() {
        List<EmployeeDTO> employees = owner.hrApiClient.getAllEmployees();
        if (employees.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Registe primeiro um colaborador.", "Contratos",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        JComboBox<EmployeeDTO> employeeCombo = new JComboBox<>(employees.toArray(new EmployeeDTO[0]));
        employeeCombo.setRenderer(employeeRenderer());
        UIHelper.styleComboBox(employeeCombo);
        JComboBox<String> typeCombo = new JComboBox<>(TYPES);
        UIHelper.styleComboBox(typeCombo);

        JTextField jobField = new JTextField();
        DateField startField = new DateField(LocalDate.now());
        JTextField endField = new JTextField();
        JTextField probationField = new JTextField();
        MoneyField salaryField = new MoneyField("0");
        JSpinner hoursSpinner = new JSpinner(new SpinnerNumberModel(40, 1, 80, 1));
        JTextField locationField = new JTextField();
        JTextField reasonField = new JTextField();
        for (JTextField f : new JTextField[]{jobField, endField, probationField, locationField, reasonField}) {
            UIHelper.styleTextField(f);
        }

        JPanel form = UIHelper.createDialogForm(
                "Colaborador:", employeeCombo,
                "Tipo de contrato:", typeCombo,
                "Função:", jobField,
                "Início (yyyy-MM-dd):", startField,
                "Fim (opcional, yyyy-MM-dd):", endField,
                "Fim da experiência (opcional):", probationField,
                "Salário acordado (MT):", salaryField,
                "Horas semanais:", hoursSpinner,
                "Local de trabalho:", locationField,
                "Motivo do termo (obrigatório a termo):", reasonField);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Novo Contrato",
                "fas-file-signature", "Condições acordadas", form).showDialog();
        if (!confirmed) return;

        try {
            CreateContractRequest request = new CreateContractRequest(
                    ((EmployeeDTO) employeeCombo.getSelectedItem()).id(),
                    String.valueOf(typeCombo.getSelectedItem()),
                    startField.value(),
                    parseDate(endField),
                    parseDate(probationField),
                    salaryField.value(),
                    (Integer) hoursSpinner.getValue(),
                    jobField.getText().trim(),
                    locationField.getText().trim(),
                    reasonField.getText().trim());
            UIHelper.runWithProgress(owner, "A criar contrato…",
                    () -> owner.hrApiClient.createContract(request),
                    ignored -> {
                        load();
                        JOptionPane.showMessageDialog(owner,
                                "Contrato criado em rascunho. Use \"Activar\" para o pôr a vigorar.",
                                "Contratos", JOptionPane.INFORMATION_MESSAGE);
                    }, owner::showActionError);
        } catch (RuntimeException ex) {
            owner.showActionError(ex);
        }
    }

    private void renewContract(EmploymentContractDTO contract) {
        DateField startField = new DateField(contract.endDate() == null
                ? LocalDate.now() : contract.endDate().plusDays(1));
        JTextField endField = new JTextField();
        MoneyField salaryField = new MoneyField(contract.agreedSalary().toPlainString());
        UIHelper.styleTextField(endField);

        JPanel form = UIHelper.createDialogForm(
                "Novo início (yyyy-MM-dd):", startField,
                "Novo fim (opcional):", endField,
                "Salário acordado (MT):", salaryField);
        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow,
                "Renovar " + contract.contractNumber(), "fas-redo",
                "O contrato anterior fecha na véspera do novo", form).showDialog();
        if (!confirmed) return;

        RenewContractRequest request = new RenewContractRequest(
                startField.value(), parseDate(endField), salaryField.value(), null);
        UIHelper.runWithProgress(owner, "A renovar contrato…",
                () -> owner.hrApiClient.renewContract(contract.id(), request),
                ignored -> load(), owner::showActionError);
    }

    private void terminateContract(EmploymentContractDTO contract) {
        DateField dateField = new DateField(LocalDate.now());
        JTextField reasonField = new JTextField();
        UIHelper.styleTextField(reasonField);
        JPanel form = UIHelper.createDialogForm(
                "Data da cessação:", dateField,
                "Motivo (obrigatório):", reasonField);
        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow,
                "Cessar " + contract.contractNumber(), "fas-ban",
                "A cessação fica auditada", form).showDialog();
        if (!confirmed) return;

        UIHelper.runWithProgress(owner, "A cessar contrato…",
                () -> owner.hrApiClient.terminateContract(
                        contract.id(), dateField.value(), reasonField.getText().trim()),
                ignored -> load(), owner::showActionError);
    }

    private LocalDate parseDate(JTextField field) {
        String text = field.getText().trim();
        return text.isBlank() ? null : LocalDate.parse(text);
    }

    private ListCellRenderer<Object> employeeRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof EmployeeDTO employee) {
                    setText(employee.name() + " (" + employee.employeeNumber() + ")");
                }
                return this;
            }
        };
    }
}
