package com.phcpro.gui;

import com.phcpro.gui.components.KpiCard;
import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernFormDialog;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.SimpleBarChart;
import com.phcpro.gui.components.TableFilter;
import com.phcpro.gui.components.TableCellRenderers;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.gui.components.DateField;
import com.phcpro.gui.components.MoneyField;
import com.phcpro.modules.hr.model.ExpenseStatus;
import com.phcpro.modules.hr.dto.AbsenceDTO;
import com.phcpro.modules.hr.dto.CreateAbsenceRequest;
import com.phcpro.modules.hr.dto.CreateExpenseClaimRequest;
import com.phcpro.modules.hr.dto.CreatePayslipRequest;
import com.phcpro.modules.hr.dto.CreateVacationRequest;
import com.phcpro.modules.hr.dto.EmployeeDTO;
import com.phcpro.modules.hr.dto.ExpenseClaimDTO;
import com.phcpro.modules.hr.dto.PayslipDTO;
import com.phcpro.modules.hr.dto.VacationDTO;
import com.phcpro.modules.hr.dto.UpsertEmployeeRequest;
import com.phcpro.desktop.client.HRApiClient;
import com.phcpro.modules.printing.PdfFileSaver;
import com.phcpro.modules.printing.TablePdfExporter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HRPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] ABSENCE_TYPES = {"JUSTIFIED", "UNJUSTIFIED", "SICK", "MATERNITY", "OTHER"};

    final HRApiClient hrApiClient;
    private final HRExpensesPanel expensesPanel;

    List<EmployeeDTO> employeesList = new ArrayList<>();
    private List<PayslipDTO> payslipsList = new ArrayList<>();
    private List<AbsenceDTO> absencesList = new ArrayList<>();
    private List<VacationDTO> vacationsList = new ArrayList<>();
    List<ExpenseClaimDTO> expensesList = new ArrayList<>();

    // Overview ("Visão Geral") tab — KPIs + gráficos
    private JLabel ovActiveEmployees, ovActiveEmployeesSub;
    private JLabel ovPayrollNet, ovPayrollGrossSub;
    private JLabel ovEmployerInss, ovEmployerInssSub;
    private JLabel ovPendingVacations, ovPendingVacationsSub;
    private JLabel ovMonthAbsences, ovMonthAbsencesSub;
    private JLabel ovPendingExpenses, ovPendingExpensesSub;
    private SimpleBarChart ovPayrollChart, ovDeptChart;

    // Employees tab
    private DefaultTableModel employeesModel;
    private JTable employeesTable;

    // Payslips tab
    private DefaultTableModel payslipsModel;
    private JTable payslipsTable;

    // Absences tab
    private DefaultTableModel absencesModel;
    private JTable absencesTable;

    // Vacations tab
    private DefaultTableModel vacationsModel;
    private JTable vacationsTable;

    // Expenses tab
    DefaultTableModel expensesModel;
    JTable expensesTable;

    public HRPanel(HRApiClient hrApiClient) {
        this.hrApiClient = hrApiClient;
        this.expensesPanel = new HRExpensesPanel(this);

        setLayout(new BorderLayout(0, 10));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(UIHelper.createHeading("Recursos Humanos"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPanePHC(tabs);

        tabs.addTab("Visão Geral",        UIHelper.icon("fas-chart-pie", 16, UIHelper.TEXT_LIGHT),     buildOverviewTab());
        tabs.addTab("Colaboradores",     UIHelper.icon("fas-users", 16, UIHelper.TEXT_LIGHT),         buildEmployeesTab());
        tabs.addTab("Recibos de Salário", UIHelper.icon("fas-file-invoice-dollar", 16, UIHelper.TEXT_LIGHT), buildPayslipsTab());
        tabs.addTab("Faltas",            UIHelper.icon("fas-user-times", 16, UIHelper.TEXT_LIGHT),    buildAbsencesTab());
        tabs.addTab("Férias",            UIHelper.icon("fas-umbrella-beach", 16, UIHelper.TEXT_LIGHT),buildVacationsTab());
        tabs.addTab("Notas de Despesas", UIHelper.icon("fas-receipt", 16, UIHelper.TEXT_LIGHT),       buildExpensesTab());

        add(tabs, BorderLayout.CENTER);

        // Carregamento preguiçoso: dados por HTTP em onPanelSelected() (via navigate), não no
        // construtor — evita chamadas à API no arranque para quem não tem empresa activa.
    }

    public void onPanelSelected() {
        refreshData();
    }

    public void refreshData() {
        loadEmployees();
        loadPayslips();
        loadAbsences();
        loadVacations();
        loadExpenses();
    }

    // ─── Overview ("Visão Geral") tab — cards de KPI + gráficos ────────────────

    private JPanel buildOverviewTab() {
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel grid = new JPanel(new GridLayout(0, 3, 12, 12));
        grid.setOpaque(false);

        ovActiveEmployees = KpiCard.valueLabel("0", 20);
        ovActiveEmployeesSub = HROverviewUi.subtitle("");
        grid.add(KpiCard.create("COLABORADORES ATIVOS", "fas-users", UIHelper.KPI_INFO_SOFT,
                ovActiveEmployees, ovActiveEmployeesSub, UIHelper.KPI_INFO_DARK, UIHelper.KPI_INFO_END));

        ovPayrollNet = KpiCard.valueLabel("0,00 MT", 20);
        ovPayrollGrossSub = HROverviewUi.subtitle("");
        grid.add(KpiCard.create("MASSA SALARIAL (MÊS)", "fas-money-check-alt", UIHelper.KPI_PURPLE_SOFT,
                ovPayrollNet, ovPayrollGrossSub, UIHelper.KPI_PURPLE_DARK, UIHelper.KPI_PURPLE_END));

        ovEmployerInss = KpiCard.valueLabel("0,00 MT", 20);
        ovEmployerInssSub = HROverviewUi.subtitle("");
        grid.add(KpiCard.create("INSS PATRONAL (MÊS)", "fas-hand-holding-usd", UIHelper.KPI_SUCCESS_SOFT,
                ovEmployerInss, ovEmployerInssSub, UIHelper.KPI_INFO_END, UIHelper.APPROVED_GREEN));

        ovPendingVacations = KpiCard.valueLabel("0", 20);
        ovPendingVacationsSub = HROverviewUi.subtitle("");
        grid.add(KpiCard.create("FÉRIAS PENDENTES", "fas-umbrella-beach", UIHelper.KPI_WARNING_SOFT,
                ovPendingVacations, ovPendingVacationsSub, UIHelper.KPI_WARNING_DARK, UIHelper.KPI_WARNING_END));

        ovMonthAbsences = KpiCard.valueLabel("0", 20);
        ovMonthAbsencesSub = HROverviewUi.subtitle("");
        grid.add(KpiCard.create("FALTAS (MÊS)", "fas-user-times", UIHelper.KPI_DANGER_SOFT,
                ovMonthAbsences, ovMonthAbsencesSub, UIHelper.KPI_DANGER_DARK, UIHelper.KPI_DANGER_END));

        ovPendingExpenses = KpiCard.valueLabel("0", 20);
        ovPendingExpensesSub = HROverviewUi.subtitle("");
        grid.add(KpiCard.create("DESPESAS PENDENTES", "fas-receipt", UIHelper.KPI_NEUTRAL_SOFT,
                ovPendingExpenses, ovPendingExpensesSub, UIHelper.KPI_NEUTRAL_DARK, UIHelper.KPI_NEUTRAL_END));

        content.add(grid, BorderLayout.NORTH);

        JPanel charts = new JPanel(new GridLayout(1, 2, 16, 0));
        charts.setOpaque(false);
        ovPayrollChart = new SimpleBarChart("Massa salarial líquida (6 meses)");
        ovDeptChart = new SimpleBarChart("Colaboradores por departamento");
        charts.add(HROverviewUi.chartCard(ovPayrollChart));
        charts.add(HROverviewUi.chartCard(ovDeptChart));
        content.add(charts, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel tab = new JPanel(new BorderLayout());
        tab.setOpaque(false);
        tab.add(scroll, BorderLayout.CENTER);
        return tab;
    }

    void refreshOverview() {
        if (ovActiveEmployees == null) return; // aba ainda não construída

        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();

        // 1. Colaboradores ativos
        long active = employeesList.stream().filter(e -> "ACTIVE".equalsIgnoreCase(e.status())).count();
        ovActiveEmployees.setText(String.valueOf(active));
        ovActiveEmployeesSub.setText("de " + employeesList.size() + " no quadro");

        // 2/3. Recibos do mês corrente (não cancelados)
        List<PayslipDTO> monthSlips = payslipsList.stream()
                .filter(p -> p.year() == year && p.month() == month && !"CANCELLED".equalsIgnoreCase(p.status()))
                .toList();
        BigDecimal net = monthSlips.stream().map(PayslipDTO::netPay).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gross = monthSlips.stream().map(PayslipDTO::grossPay).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal employerInss = monthSlips.stream().map(PayslipDTO::employerInss).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal retained = monthSlips.stream()
                .map(p -> p.irpsDeduction().add(p.inssDeduction())).reduce(BigDecimal.ZERO, BigDecimal::add);
        ovPayrollNet.setText(String.format("%,.2f MT", net));
        ovPayrollGrossSub.setText(String.format("Bruto: %,.2f MT · %d recibo(s)", gross, monthSlips.size()));
        ovEmployerInss.setText(String.format("%,.2f MT", employerInss));
        ovEmployerInssSub.setText(String.format("IRPS+INSS retido: %,.2f MT", retained));

        // 4. Férias pendentes
        List<VacationDTO> pendingVac = vacationsList.stream()
                .filter(v -> "PENDING".equalsIgnoreCase(v.status())).toList();
        int pendingVacDays = pendingVac.stream().mapToInt(VacationDTO::totalDays).sum();
        ovPendingVacations.setText(String.valueOf(pendingVac.size()));
        ovPendingVacationsSub.setText(pendingVacDays + " dia(s) por decidir");

        // 5. Faltas que se sobrepõem ao mês corrente
        LocalDate first = today.withDayOfMonth(1);
        LocalDate last = today.withDayOfMonth(today.lengthOfMonth());
        List<AbsenceDTO> monthAbs = absencesList.stream()
                .filter(a -> a.startDate() != null && a.endDate() != null
                        && !a.startDate().isAfter(last) && !a.endDate().isBefore(first))
                .toList();
        int unjustifiedDays = monthAbs.stream()
                .filter(a -> "UNJUSTIFIED".equalsIgnoreCase(a.absenceType()))
                .mapToInt(AbsenceDTO::totalDays).sum();
        ovMonthAbsences.setText(String.valueOf(monthAbs.size()));
        ovMonthAbsencesSub.setText(unjustifiedDays + " dia(s) não justificados");

        // 6. Despesas pendentes de aprovação
        List<ExpenseClaimDTO> pendingExp = expensesList.stream()
                .filter(c -> c.status() == ExpenseStatus.PENDING_APPROVAL).toList();
        BigDecimal pendingExpSum = pendingExp.stream().map(ExpenseClaimDTO::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        ovPendingExpenses.setText(String.valueOf(pendingExp.size()));
        ovPendingExpensesSub.setText(String.format("%,.2f MT por aprovar", pendingExpSum));

        // Gráfico A: massa salarial líquida nos últimos 6 meses com recibos
        Map<YearMonth, BigDecimal> byMonth = new TreeMap<>();
        for (PayslipDTO p : payslipsList) {
            if ("CANCELLED".equalsIgnoreCase(p.status())) continue;
            byMonth.merge(YearMonth.of(p.year(), p.month()), p.netPay(), BigDecimal::add);
        }
        List<YearMonth> months = new ArrayList<>(byMonth.keySet());
        List<YearMonth> last6 = months.subList(Math.max(0, months.size() - 6), months.size());
        String[] payLabels = new String[last6.size()];
        BigDecimal[] payValues = new BigDecimal[last6.size()];
        Color[] payColors = new Color[last6.size()];
        DateTimeFormatter ymFmt = DateTimeFormatter.ofPattern("MM/yy");
        for (int i = 0; i < last6.size(); i++) {
            payLabels[i] = last6.get(i).format(ymFmt);
            payValues[i] = byMonth.get(last6.get(i));
            payColors[i] = UIHelper.ACCENT;
        }
        ovPayrollChart.setData(payLabels, payValues, payColors);

        // Gráfico B: colaboradores activos por departamento (top 5)
        Map<String, Integer> byDept = new HashMap<>();
        for (EmployeeDTO e : employeesList) {
            if (!"ACTIVE".equalsIgnoreCase(e.status())) continue;
            String dept = (e.department() == null || e.department().isBlank()) ? "—" : e.department();
            byDept.merge(dept, 1, Integer::sum);
        }
        List<Map.Entry<String, Integer>> top = byDept.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(5).toList();
        String[] deptLabels = new String[top.size()];
        BigDecimal[] deptValues = new BigDecimal[top.size()];
        Color[] deptColors = new Color[top.size()];
        Color[] palette = {UIHelper.ACCENT_BLUE, UIHelper.APPROVED_GREEN, UIHelper.PENDING_YELLOW, UIHelper.ACCENT, UIHelper.REJECTED_RED};
        for (int i = 0; i < top.size(); i++) {
            String d = top.get(i).getKey();
            deptLabels[i] = d.length() > 9 ? d.substring(0, 9) : d;
            deptValues[i] = BigDecimal.valueOf(top.get(i).getValue());
            deptColors[i] = palette[i % palette.length];
        }
        ovDeptChart.setData(deptLabels, deptValues, deptColors);
    }

    // ─── Employees tab ────────────────────────────────────────────────────────

    private JPanel buildEmployeesTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Quadro de Colaboradores"), BorderLayout.WEST);

        ModernButton exportBtn = UIHelper.createSecondaryButton("Exportar PDF");
        exportBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        exportBtn.addActionListener(e -> exportTable("colaboradores", "Colaboradores", employeesTable));
        ModernButton newBtn = UIHelper.createSuccessButton("Novo Colaborador");
        newBtn.setIcon(UIHelper.icon("fas-user-plus", 14));
        newBtn.addActionListener(e -> openEmployeeDialog(null));
        ModernButton editBtn = UIHelper.createPrimaryButton("Editar");
        editBtn.setIcon(UIHelper.icon("fas-edit", 14));
        editBtn.addActionListener(e -> editSelectedEmployee());
        ModernButton statusBtn = UIHelper.createSecondaryButton("Alterar Estado");
        statusBtn.setIcon(UIHelper.icon("fas-user-shield", 14));
        statusBtn.addActionListener(e -> changeSelectedEmployeeStatus());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(exportBtn);
        actions.add(statusBtn);
        actions.add(editBtn);
        actions.add(newBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Nº", "Nome", "Email", "Telefone", "Departamento", "Cargo", "Admissão", "Estado", "Salário Base"};
        employeesModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        employeesTable = new JTable(employeesModel);
        UIHelper.styleTable(employeesTable);
        employeesTable.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.status());
        employeesTable.getColumnModel().getColumn(8).setCellRenderer(TableCellRenderers.money());
        JScrollPane scroll = new JScrollPane(employeesTable);
        UIHelper.styleScrollPane(scroll);

        JTextField empSearch = TableFilter.searchField("Nome, email, departamento ou cargo…");
        JComboBox<String> empEstado = TableFilter.combo("Todos os estados",
                "ACTIVE", "SUSPENDED", "TERMINATED");
        TableFilter.install(employeesTable, empSearch,
                new TableFilter.ColumnFilter(empEstado, 7));
        JPanel empBar = TableFilter.bar(empSearch, TableFilter.label("Estado:"), empEstado);
        empBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(empBar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadEmployees() {
        UIHelper.loadAsync(this, hrApiClient::getAllEmployees, this::applyEmployees,
                error -> showLoadError("colaboradores", error));
    }

    private void applyEmployees(List<EmployeeDTO> loaded) {
        employeesList = loaded;
        employeesModel.setRowCount(0);
        for (EmployeeDTO e : employeesList) {
            employeesModel.addRow(new Object[]{
                    e.employeeNumber(), e.name(), e.email(), e.phone() == null ? "-" : e.phone(),
                    e.department(), e.role(),
                    e.hireDate() == null ? "-" : e.hireDate().format(DATE_FMT),
                    e.status(),
                    e.baseSalary()
            });
        }
        refreshOverview();
    }

    private void editSelectedEmployee() {
        EmployeeDTO employee = selectedEmployee();
        if (employee != null) openEmployeeDialog(employee);
    }

    private void openEmployeeDialog(EmployeeDTO existing) {
        JTextField numberField = new JTextField(existing == null ? "" : existing.employeeNumber());
        JTextField nameField = new JTextField(existing == null ? "" : existing.name());
        JTextField emailField = new JTextField(existing == null ? "" : existing.email());
        JTextField phoneField = new JTextField(existing == null || existing.phone() == null ? "" : existing.phone());
        JTextField taxIdField = new JTextField(existing == null || existing.taxId() == null ? "" : existing.taxId());
        JTextField inssField = new JTextField(existing == null || existing.inssNumber() == null ? "" : existing.inssNumber());
        JSpinner dependentsSpinner = new JSpinner(new SpinnerNumberModel(existing == null ? 0 : existing.dependentsCount(), 0, 20, 1));
        JTextField departmentField = new JTextField(existing == null ? "" : existing.department());
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"EMPLOYEE", "MANAGER", "ADMIN"});
        roleCombo.setSelectedItem(existing == null ? "EMPLOYEE" : existing.role());
        MoneyField salaryField = new MoneyField(existing == null ? "0" : existing.baseSalary().toPlainString());
        DateField hireDateField = new DateField(existing == null || existing.hireDate() == null
                ? LocalDate.now() : existing.hireDate());
        JTextField contractEndField = new JTextField(existing == null || existing.contractEndDate() == null
                ? "" : existing.contractEndDate().toString());

        for (JTextField field : new JTextField[]{numberField, nameField, emailField, phoneField, taxIdField,
                inssField, departmentField, contractEndField}) {
            UIHelper.styleTextField(field);
        }
        UIHelper.styleComboBox(roleCombo);

        JPanel form = UIHelper.createDialogForm(
                "Número Interno:", numberField,
                "Nome Completo:", nameField,
                "Email:", emailField,
                "Telefone:", phoneField,
                "NUIT:", taxIdField,
                "Nº INSS:", inssField,
                "Dependentes IRPS:", dependentsSpinner,
                "Departamento:", departmentField,
                "Cargo / Perfil:", roleCombo,
                "Salário Base (MT):", salaryField,
                "Data de Admissão (yyyy-MM-dd):", hireDateField,
                "Fim do Contrato (opcional):", contractEndField
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow,
                existing == null ? "Novo Colaborador" : "Editar Colaborador", "fas-users", "Dados do colaborador", form).showDialog();
        if (!confirmed) return;

        try {
            UpsertEmployeeRequest request = new UpsertEmployeeRequest(
                    numberField.getText().trim(),
                    nameField.getText().trim(),
                    emailField.getText().trim(),
                    phoneField.getText().trim(),
                    taxIdField.getText().trim(),
                    inssField.getText().trim(),
                    (Integer) dependentsSpinner.getValue(),
                    departmentField.getText().trim(),
                    String.valueOf(roleCombo.getSelectedItem()),
                    salaryField.value(),
                    hireDateField.value(),
                    contractEndField.getText().isBlank() ? null : LocalDate.parse(contractEndField.getText().trim())
            );
            UIHelper.runWithProgress(this, "A guardar colaborador…",
                    () -> existing == null ? hrApiClient.createEmployee(request)
                            : hrApiClient.updateEmployee(existing.id(), request), ignored -> {
                        loadEmployees();
                        loadExpenses();
                        JOptionPane.showMessageDialog(this, "Dados do colaborador guardados.", "Sucesso",
                                JOptionPane.INFORMATION_MESSAGE);
                    }, this::showActionError);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void changeSelectedEmployeeStatus() {
        EmployeeDTO employee = selectedEmployee();
        if (employee == null) return;
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"ACTIVE", "SUSPENDED", "TERMINATED"});
        statusCombo.setSelectedItem(employee.status());
        UIHelper.styleComboBox(statusCombo);
        int option = JOptionPane.showConfirmDialog(this, statusCombo,
                "Alterar Estado Laboral", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option != JOptionPane.OK_OPTION) return;
        String status = String.valueOf(statusCombo.getSelectedItem());
        UIHelper.runWithProgress(this, "A actualizar estado laboral…",
                () -> hrApiClient.changeEmployeeStatus(employee.id(), status), ignored -> loadEmployees(),
                this::showActionError);
    }

    private EmployeeDTO selectedEmployee() {
        int row = employeesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um colaborador na tabela.", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return employeesList.get(employeesTable.convertRowIndexToModel(row));
    }

    // ─── Payslips tab ─────────────────────────────────────────────────────────

    private JPanel buildPayslipsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Recibos de Salário"), BorderLayout.WEST);

        ModernButton newBtn = UIHelper.createSuccessButton("Gerar Recibo");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        ModernButton payBtn = UIHelper.createSuccessButton("Marcar Pago");
        payBtn.setIcon(UIHelper.icon("fas-check", 14));
        ModernButton printBtn = UIHelper.createSecondaryButton("Imprimir PDF");
        printBtn.setIcon(UIHelper.icon("fas-print", 14));
        ModernButton exportBtn = UIHelper.createSecondaryButton("Exportar Lista");
        exportBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        ModernButton processBtn = UIHelper.createPrimaryButton("Processar Mês");
        processBtn.setIcon(UIHelper.icon("fas-calculator", 14));
        newBtn.addActionListener(e -> openCreatePayslipDialog());
        payBtn.addActionListener(e -> markSelectedPayslipPaid());
        printBtn.addActionListener(e -> printSelectedPayslip());
        exportBtn.addActionListener(e -> exportTable("recibos-salario", "Recibos de Salário", payslipsTable));
        processBtn.addActionListener(e -> processMonthlyPayroll());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(exportBtn);
        actions.add(processBtn);
        actions.add(printBtn);
        actions.add(payBtn);
        actions.add(newBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Nº Recibo", "Colaborador", "Período", "Bruto", "Descontos", "Líquido", "Estado", "Data Pagamento"};
        payslipsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        payslipsTable = new JTable(payslipsModel);
        UIHelper.styleTable(payslipsTable);
        for (int column : new int[]{3, 4, 5}) {
            payslipsTable.getColumnModel().getColumn(column).setCellRenderer(TableCellRenderers.money());
        }
        payslipsTable.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.status());
        JScrollPane scroll = new JScrollPane(payslipsTable);
        UIHelper.styleScrollPane(scroll);

        JTextField psSearch = TableFilter.searchField("Nº recibo, colaborador ou período…");
        JComboBox<String> psEstado = TableFilter.combo("Todos os estados", "DRAFT", "PAID", "CANCELLED");
        JComboBox<String> psPeriodo = TableFilter.periodCombo();
        TableFilter.install(payslipsTable, psSearch,
                java.util.List.of(new TableFilter.ColumnFilter(psEstado, 6)),
                java.util.List.of(new TableFilter.PeriodFilter(psPeriodo, 7)));
        JPanel psBar = TableFilter.bar(psSearch,
                TableFilter.label("Estado:"), psEstado,
                TableFilter.label("Data pag.:", "fas-calendar-alt"), psPeriodo);
        psBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(psBar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadPayslips() {
        UIHelper.loadAsync(this, hrApiClient::getAllPayslips, this::applyPayslips,
                error -> showLoadError("recibos de salário", error));
    }

    private void applyPayslips(List<PayslipDTO> loaded) {
        payslipsList = loaded;
        payslipsModel.setRowCount(0);
        for (PayslipDTO p : payslipsList) {
            payslipsModel.addRow(new Object[]{
                    p.payslipNumber(),
                    p.employeeName(),
                    String.format("%02d/%d", p.month(), p.year()),
                    p.grossPay(),
                    p.totalDeductions(),
                    p.netPay(),
                    p.status(),
                    p.paymentDate() != null ? p.paymentDate().format(DATE_FMT) : "-"
            });
        }
        refreshOverview();
    }

    private void openCreatePayslipDialog() {
        if (employeesList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cadastre colaboradores primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JComboBox<String> empCombo = new JComboBox<>();
        UIHelper.styleComboBox(empCombo);
        for (EmployeeDTO e : employeesList) empCombo.addItem(e.name() + " — " + e.department());

        LocalDate today = LocalDate.now();
        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(today.getYear(), 2000, 2100, 1));
        JSpinner monthSpinner = new JSpinner(new SpinnerNumberModel(today.getMonthValue(), 1, 12, 1));

        MoneyField allowancesField = new MoneyField("0");
        MoneyField overtimeField = new MoneyField("0");
        MoneyField otherField = new MoneyField("0");
        JTextField notesField = new JTextField();
        UIHelper.styleTextField(notesField);

        JPanel form = UIHelper.createDialogForm(
                "Colaborador:", empCombo,
                "Ano:", yearSpinner,
                "Mês:", monthSpinner,
                "Subsídios / Abonos (MT):", allowancesField,
                "Horas Extras (MT):", overtimeField,
                "IRPS / INSS:", new JLabel("Cálculo automático pela configuração fiscal vigente"),
                "Outros Descontos (MT):", otherField,
                "Observações:", notesField
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Gerar Recibo de Salário", "fas-file-invoice-dollar", "Processamento de salário do colaborador", form).setConfirmButton("Gerar", "fas-check").showDialog();
        if (!confirmed) return;

        try {
            EmployeeDTO emp = employeesList.get(empCombo.getSelectedIndex());
            CreatePayslipRequest req = new CreatePayslipRequest(
                    emp.id(),
                    (Integer) yearSpinner.getValue(),
                    (Integer) monthSpinner.getValue(),
                    allowancesField.value(),
                    overtimeField.value(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    otherField.value(),
                    notesField.getText().trim().isEmpty() ? null : notesField.getText().trim()
            );
            UIHelper.runWithProgress(this, "A gerar recibo de salário…", () -> hrApiClient.createPayslip(req),
                    created -> {
                        loadPayslips();
                        int print = JOptionPane.showConfirmDialog(this,
                                "Recibo " + created.payslipNumber() + " gerado. Deseja imprimir agora?",
                                "Sucesso", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                        if (print == JOptionPane.YES_OPTION) printPayslip(created.id(), created.payslipNumber());
                    }, this::showActionError);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void processMonthlyPayroll() {
        LocalDate today = LocalDate.now();
        JSpinner year = new JSpinner(new SpinnerNumberModel(today.getYear(), 2000, 2100, 1));
        JSpinner month = new JSpinner(new SpinnerNumberModel(today.getMonthValue(), 1, 12, 1));
        JPanel form = UIHelper.createDialogForm("Ano:", year, "Mês:", month);
        int option = JOptionPane.showConfirmDialog(this, form, "Processar Folha Salarial",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option != JOptionPane.OK_OPTION) return;
        int selectedYear = (Integer) year.getValue();
        int selectedMonth = (Integer) month.getValue();
        UIHelper.runWithProgress(this, "A processar folha salarial…",
                () -> hrApiClient.processMonthlyPayroll(selectedYear, selectedMonth), created -> {
                    loadPayslips();
                    JOptionPane.showMessageDialog(this, created.size() + " recibos processados automaticamente.",
                            "Folha Salarial", JOptionPane.INFORMATION_MESSAGE);
                }, this::showActionError);
    }

    private void markSelectedPayslipPaid() {
        PayslipDTO sel = selectedPayslip();
        if (sel == null) return;
        UIHelper.runWithProgress(this, "A actualizar recibo…", () -> {
            hrApiClient.markPayslipPaid(sel.id());
            return null;
        }, ignored -> {
            JOptionPane.showMessageDialog(this, "Recibo marcado como pago.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadPayslips();
        }, this::showActionError);
    }

    private void printSelectedPayslip() {
        PayslipDTO sel = selectedPayslip();
        if (sel == null) return;
        printPayslip(sel.id(), sel.payslipNumber());
    }

    private void printPayslip(Long id, String number) {
        UIHelper.runWithProgress(this, "A gerar recibo em PDF…", () -> hrApiClient.renderPayslip(id),
                pdf -> PdfFileSaver.saveAndOpen(pdf, "recibo-salario-" + number),
                error -> JOptionPane.showMessageDialog(this, "Erro ao gerar PDF: " + error.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE));
    }

    private PayslipDTO selectedPayslip() {
        int row = TableFilter.selectedModelRow(payslipsTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um recibo na tabela primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return payslipsList.get(row);
    }

    // ─── Absences tab ─────────────────────────────────────────────────────────

    private JPanel buildAbsencesTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Registo de Faltas"), BorderLayout.WEST);

        ModernButton newBtn = UIHelper.createPrimaryButton("Registar Falta");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        ModernButton deleteBtn = UIHelper.createDangerButton("Eliminar");
        deleteBtn.setIcon(UIHelper.icon("fas-trash", 14));
        ModernButton exportBtn = UIHelper.createSecondaryButton("Exportar PDF");
        exportBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        newBtn.addActionListener(e -> openCreateAbsenceDialog());
        deleteBtn.addActionListener(e -> deleteSelectedAbsence());
        exportBtn.addActionListener(e -> exportTable("faltas", "Mapa de Faltas", absencesTable));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(exportBtn);
        actions.add(deleteBtn);
        actions.add(newBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"ID", "Colaborador", "Tipo", "Início", "Fim", "Dias", "Justificada", "Motivo"};
        absencesModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        absencesTable = new JTable(absencesModel);
        UIHelper.styleTable(absencesTable);
        JScrollPane scroll = new JScrollPane(absencesTable);
        UIHelper.styleScrollPane(scroll);

        JTextField absSearch = TableFilter.searchField("Colaborador ou motivo…");
        JComboBox<String> absTipo = TableFilter.combo("Todos os tipos",
                "JUSTIFIED", "UNJUSTIFIED", "SICK", "MATERNITY", "OTHER");
        JComboBox<String> absPeriodo = TableFilter.periodCombo();
        TableFilter.install(absencesTable, absSearch,
                java.util.List.of(new TableFilter.ColumnFilter(absTipo, 2)),
                java.util.List.of(new TableFilter.PeriodFilter(absPeriodo, 3)));
        JPanel absBar = TableFilter.bar(absSearch,
                TableFilter.label("Tipo:"), absTipo,
                TableFilter.label("Início:", "fas-calendar-alt"), absPeriodo);
        absBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(absBar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadAbsences() {
        UIHelper.loadAsync(this, hrApiClient::getAllAbsences, this::applyAbsences,
                error -> showLoadError("faltas", error));
    }

    private void applyAbsences(List<AbsenceDTO> loaded) {
        absencesList = loaded;
        absencesModel.setRowCount(0);
        for (AbsenceDTO a : absencesList) {
            absencesModel.addRow(new Object[]{
                    a.id(), a.employeeName(), a.absenceType(),
                    a.startDate().format(DATE_FMT), a.endDate().format(DATE_FMT),
                    a.totalDays(),
                    a.hasSupportingDocument() ? "Sim" : "Não",
                    a.reason() == null ? "" : a.reason()
            });
        }
        refreshOverview();
    }

    private void openCreateAbsenceDialog() {
        if (employeesList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cadastre colaboradores primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JComboBox<String> empCombo = new JComboBox<>();
        UIHelper.styleComboBox(empCombo);
        for (EmployeeDTO e : employeesList) empCombo.addItem(e.name() + " — " + e.department());

        JComboBox<String> typeCombo = new JComboBox<>(ABSENCE_TYPES);
        UIHelper.styleComboBox(typeCombo);

        DateField startField = new DateField(LocalDate.now());
        DateField endField = new DateField(LocalDate.now());
        JTextField reasonField = new JTextField();
        JCheckBox docCheck = new JCheckBox("Possui documento de justificação");
        docCheck.setForeground(UIHelper.TEXT_LIGHT);
        docCheck.setOpaque(false);
        UIHelper.styleTextField(reasonField);

        JPanel form = UIHelper.createDialogForm(
                "Colaborador:", empCombo,
                "Tipo de Falta:", typeCombo,
                "Data Início (yyyy-MM-dd):", startField,
                "Data Fim (yyyy-MM-dd):", endField,
                "Motivo:", reasonField,
                "Documento:", docCheck
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Registar Falta", "fas-user-times", "Ausência do colaborador", form).showDialog();
        if (!confirmed) return;

        try {
            EmployeeDTO emp = employeesList.get(empCombo.getSelectedIndex());
            CreateAbsenceRequest req = new CreateAbsenceRequest(
                    emp.id(),
                    (String) typeCombo.getSelectedItem(),
                    startField.value(),
                    endField.value(),
                    reasonField.getText().trim().isEmpty() ? null : reasonField.getText().trim(),
                    docCheck.isSelected()
            );
            UIHelper.runWithProgress(this, "A registar falta…", () -> hrApiClient.recordAbsence(req), ignored -> {
                JOptionPane.showMessageDialog(this, "Falta registada.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                loadAbsences();
            }, this::showActionError);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedAbsence() {
        int row = TableFilter.selectedModelRow(absencesTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma falta na tabela.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        AbsenceDTO sel = absencesList.get(row);
        int ok = JOptionPane.showConfirmDialog(this, "Eliminar a falta selecionada?", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        UIHelper.runWithProgress(this, "A eliminar falta…", () -> {
            hrApiClient.deleteAbsence(sel.id());
            return null;
        }, ignored -> loadAbsences(), this::showActionError);
    }

    // ─── Vacations tab ────────────────────────────────────────────────────────

    private JPanel buildVacationsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Pedidos de Férias"), BorderLayout.WEST);

        ModernButton newBtn = UIHelper.createPrimaryButton("Novo Pedido");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        ModernButton approveBtn = UIHelper.createSuccessButton("Aprovar");
        approveBtn.setIcon(UIHelper.icon("fas-check", 14));
        ModernButton rejectBtn = UIHelper.createDangerButton("Rejeitar");
        rejectBtn.setIcon(UIHelper.icon("fas-times", 14));
        ModernButton exportBtn = UIHelper.createSecondaryButton("Exportar PDF");
        exportBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        newBtn.addActionListener(e -> openCreateVacationDialog());
        approveBtn.addActionListener(e -> decideVacation(true));
        rejectBtn.addActionListener(e -> decideVacation(false));
        exportBtn.addActionListener(e -> exportTable("ferias", "Mapa de Férias", vacationsTable));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(exportBtn);
        actions.add(rejectBtn);
        actions.add(approveBtn);
        actions.add(newBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"ID", "Colaborador", "Início", "Fim", "Dias", "Ano Ref.", "Estado", "Decidido Por"};
        vacationsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        vacationsTable = new JTable(vacationsModel);
        UIHelper.styleTable(vacationsTable);
        vacationsTable.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.status());
        JScrollPane scroll = new JScrollPane(vacationsTable);
        UIHelper.styleScrollPane(scroll);

        JTextField vacSearch = TableFilter.searchField("Colaborador ou decisor…");
        JComboBox<String> vacEstado = TableFilter.combo("Todos os estados",
                "PENDING", "APPROVED", "REJECTED", "CANCELLED");
        JComboBox<String> vacPeriodo = TableFilter.periodCombo();
        TableFilter.install(vacationsTable, vacSearch,
                java.util.List.of(new TableFilter.ColumnFilter(vacEstado, 6)),
                java.util.List.of(new TableFilter.PeriodFilter(vacPeriodo, 2)));
        JPanel vacBar = TableFilter.bar(vacSearch,
                TableFilter.label("Estado:"), vacEstado,
                TableFilter.label("Início:", "fas-calendar-alt"), vacPeriodo);
        vacBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(vacBar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadVacations() {
        UIHelper.loadAsync(this, hrApiClient::getAllVacations, this::applyVacations,
                error -> showLoadError("férias", error));
    }

    private void applyVacations(List<VacationDTO> loaded) {
        vacationsList = loaded;
        vacationsModel.setRowCount(0);
        for (VacationDTO v : vacationsList) {
            vacationsModel.addRow(new Object[]{
                    v.id(), v.employeeName(),
                    v.startDate().format(DATE_FMT), v.endDate().format(DATE_FMT),
                    v.totalDays(), v.yearReference(), v.status(),
                    v.decisionBy() == null ? "-" : v.decisionBy()
            });
        }
        refreshOverview();
    }

    private void openCreateVacationDialog() {
        if (employeesList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cadastre colaboradores primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JComboBox<String> empCombo = new JComboBox<>();
        UIHelper.styleComboBox(empCombo);
        for (EmployeeDTO e : employeesList) empCombo.addItem(e.name() + " — " + e.department());

        DateField startField = new DateField(LocalDate.now());
        DateField endField = new DateField(LocalDate.now().plusDays(15));
        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2000, 2100, 1));
        JTextField notesField = new JTextField();
        UIHelper.styleTextField(notesField);

        JPanel form = UIHelper.createDialogForm(
                "Colaborador:", empCombo,
                "Data Início (yyyy-MM-dd):", startField,
                "Data Fim (yyyy-MM-dd):", endField,
                "Ano de Referência:", yearSpinner,
                "Observações:", notesField
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Novo Pedido de Férias", "fas-umbrella-beach", "Marcação de férias do colaborador", form).showDialog();
        if (!confirmed) return;

        try {
            EmployeeDTO emp = employeesList.get(empCombo.getSelectedIndex());
            CreateVacationRequest req = new CreateVacationRequest(
                    emp.id(),
                    startField.value(),
                    endField.value(),
                    (Integer) yearSpinner.getValue(),
                    notesField.getText().trim().isEmpty() ? null : notesField.getText().trim()
            );
            UIHelper.runWithProgress(this, "A submeter pedido de férias…", () -> hrApiClient.submitVacation(req), ignored -> {
                JOptionPane.showMessageDialog(this, "Pedido de férias submetido.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                loadVacations();
            }, this::showActionError);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void decideVacation(boolean approve) {
        int row = TableFilter.selectedModelRow(vacationsTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um pedido na tabela.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        VacationDTO sel = vacationsList.get(row);
        String reason = null;
        if (!approve) {
            reason = UIHelper.promptRequiredText("Rejeitar Pedido de Férias", "fas-times-circle",
                    "Colaborador: " + sel.employeeName(), "Motivo da rejeição:");
            if (reason == null) return;
        }
        String decisionReason = reason;
        UIHelper.runWithProgress(this, approve ? "A aprovar férias…" : "A rejeitar férias…", () -> {
            hrApiClient.decideVacation(sel.id(), approve, decisionReason);
            return null;
        }, ignored -> loadVacations(), this::showActionError);
    }

    // ─── Expenses tab ─────────────────────────────────────────────────────────

    private JPanel buildExpensesTab() { return expensesPanel.buildPanel(); }

    private void loadExpenses() { expensesPanel.refresh(); }

    void showLoadError(String area, Throwable error) {
        JOptionPane.showMessageDialog(this, "Não foi possível carregar " + area + ": " + error.getMessage(),
                "Erro de ligação", JOptionPane.ERROR_MESSAGE);
    }

    void showActionError(Throwable error) {
        JOptionPane.showMessageDialog(this, error.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }

    // ─── Shared export helper ─────────────────────────────────────────────────

    void exportTable(String baseName, String title, JTable table) {
        if (table.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Nada para exportar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            com.phcpro.modules.company.model.Company company = resolveCompany();
            byte[] pdf = TablePdfExporter.renderFromSwing(company, title, table);
            PdfFileSaver.saveAndOpen(pdf, baseName + "-export");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao exportar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private com.phcpro.modules.company.model.Company resolveCompany() {
        com.phcpro.modules.company.model.Company c = new com.phcpro.modules.company.model.Company();
        c.setId(com.phcpro.architecture.security.CurrentUserContext.getCurrentCompanyId());
        return c;
    }
}
