package com.phcpro.gui;

import com.phcpro.gui.components.KpiCard;
import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernFormDialog;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.SimpleBarChart;
import com.phcpro.gui.components.UIHelper;
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
import com.phcpro.modules.hr.service.HRService;
import com.phcpro.modules.printing.PayslipPrintService;
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

    private final HRService hrService;
    private final PayslipPrintService payslipPrintService;

    private List<EmployeeDTO> employeesList = new ArrayList<>();
    private List<PayslipDTO> payslipsList = new ArrayList<>();
    private List<AbsenceDTO> absencesList = new ArrayList<>();
    private List<VacationDTO> vacationsList = new ArrayList<>();

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
    private DefaultTableModel expensesModel;
    private JTable expensesTable;

    public HRPanel(HRService hrService, PayslipPrintService payslipPrintService) {
        this.hrService = hrService;
        this.payslipPrintService = payslipPrintService;

        setLayout(new BorderLayout(0, 10));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(UIHelper.createHeading("Recursos Humanos"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPane(tabs);

        tabs.addTab("Visão Geral",        UIHelper.icon("fas-chart-pie", 16, UIHelper.TEXT_LIGHT),     buildOverviewTab());
        tabs.addTab("Colaboradores",     UIHelper.icon("fas-users", 16, UIHelper.TEXT_LIGHT),         buildEmployeesTab());
        tabs.addTab("Recibos de Salário", UIHelper.icon("fas-file-invoice-dollar", 16, UIHelper.TEXT_LIGHT), buildPayslipsTab());
        tabs.addTab("Faltas",            UIHelper.icon("fas-user-times", 16, UIHelper.TEXT_LIGHT),    buildAbsencesTab());
        tabs.addTab("Férias",            UIHelper.icon("fas-umbrella-beach", 16, UIHelper.TEXT_LIGHT),buildVacationsTab());
        tabs.addTab("Notas de Despesas", UIHelper.icon("fas-receipt", 16, UIHelper.TEXT_LIGHT),       buildExpensesTab());

        add(tabs, BorderLayout.CENTER);

        refreshData();
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
        refreshOverview();
    }

    // ─── Overview ("Visão Geral") tab — cards de KPI + gráficos ────────────────

    private JPanel buildOverviewTab() {
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel grid = new JPanel(new GridLayout(0, 3, 12, 12));
        grid.setOpaque(false);
        grid.setPreferredSize(new Dimension(0, 240));

        ovActiveEmployees = KpiCard.valueLabel("0", 20);
        ovActiveEmployeesSub = overviewSub("");
        grid.add(KpiCard.create("COLABORADORES ATIVOS", "fas-users", new Color(224, 242, 254),
                ovActiveEmployees, ovActiveEmployeesSub, new Color(9, 79, 172), new Color(13, 148, 136)));

        ovPayrollNet = KpiCard.valueLabel("0,00 MT", 20);
        ovPayrollGrossSub = overviewSub("");
        grid.add(KpiCard.create("MASSA SALARIAL (MÊS)", "fas-money-check-alt", new Color(243, 232, 255),
                ovPayrollNet, ovPayrollGrossSub, new Color(109, 40, 217), new Color(147, 51, 234)));

        ovEmployerInss = KpiCard.valueLabel("0,00 MT", 20);
        ovEmployerInssSub = overviewSub("");
        grid.add(KpiCard.create("INSS PATRONAL (MÊS)", "fas-hand-holding-usd", new Color(204, 251, 241),
                ovEmployerInss, ovEmployerInssSub, new Color(13, 148, 136), new Color(20, 184, 166)));

        ovPendingVacations = KpiCard.valueLabel("0", 20);
        ovPendingVacationsSub = overviewSub("");
        grid.add(KpiCard.create("FÉRIAS PENDENTES", "fas-umbrella-beach", new Color(254, 243, 199),
                ovPendingVacations, ovPendingVacationsSub, new Color(180, 83, 9), new Color(217, 119, 6)));

        ovMonthAbsences = KpiCard.valueLabel("0", 20);
        ovMonthAbsencesSub = overviewSub("");
        grid.add(KpiCard.create("FALTAS (MÊS)", "fas-user-times", new Color(254, 226, 226),
                ovMonthAbsences, ovMonthAbsencesSub, new Color(220, 38, 38), new Color(185, 28, 28)));

        ovPendingExpenses = KpiCard.valueLabel("0", 20);
        ovPendingExpensesSub = overviewSub("");
        grid.add(KpiCard.create("DESPESAS PENDENTES", "fas-receipt", new Color(209, 213, 219),
                ovPendingExpenses, ovPendingExpensesSub, new Color(15, 23, 42), new Color(30, 41, 59)));

        content.add(grid, BorderLayout.NORTH);

        JPanel charts = new JPanel(new GridLayout(1, 2, 16, 0));
        charts.setOpaque(false);
        charts.setPreferredSize(new Dimension(0, 280));
        ovPayrollChart = new SimpleBarChart("Massa salarial líquida (6 meses)");
        ovDeptChart = new SimpleBarChart("Colaboradores por departamento");
        charts.add(overviewChartCard(ovPayrollChart));
        charts.add(overviewChartCard(ovDeptChart));
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

    private static JLabel overviewSub(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        l.setForeground(new Color(229, 231, 235));
        return l;
    }

    private ModernPanel overviewChartCard(SimpleBarChart chart) {
        ModernPanel card = new ModernPanel(12, UIHelper.BG_CARD, UIHelper.BG_CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    private void refreshOverview() {
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
        List<ExpenseClaimDTO> claims = hrService.getAllExpenses();
        List<ExpenseClaimDTO> pendingExp = claims.stream()
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
        JScrollPane scroll = new JScrollPane(employeesTable);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadEmployees() {
        employeesList = hrService.getAllEmployees();
        employeesModel.setRowCount(0);
        for (EmployeeDTO e : employeesList) {
            employeesModel.addRow(new Object[]{
                    e.employeeNumber(), e.name(), e.email(), e.phone() == null ? "-" : e.phone(),
                    e.department(), e.role(),
                    e.hireDate() == null ? "-" : e.hireDate().format(DATE_FMT),
                    e.status(),
                    String.format("%,.2f MT", e.baseSalary())
            });
        }
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
        JTextField salaryField = new JTextField(existing == null ? "0" : existing.baseSalary().toPlainString());
        JTextField hireDateField = new JTextField(existing == null || existing.hireDate() == null
                ? LocalDate.now().toString() : existing.hireDate().toString());
        JTextField contractEndField = new JTextField(existing == null || existing.contractEndDate() == null
                ? "" : existing.contractEndDate().toString());

        for (JTextField field : new JTextField[]{numberField, nameField, emailField, phoneField, taxIdField,
                inssField, departmentField, salaryField, hireDateField, contractEndField}) {
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
                    new BigDecimal(salaryField.getText().trim()),
                    LocalDate.parse(hireDateField.getText().trim()),
                    contractEndField.getText().isBlank() ? null : LocalDate.parse(contractEndField.getText().trim())
            );
            if (existing == null) {
                hrService.createEmployee(request);
            } else {
                hrService.updateEmployee(existing.id(), request);
            }
            loadEmployees();
            loadExpenses();
            JOptionPane.showMessageDialog(this, "Dados do colaborador guardados.", "Sucesso",
                    JOptionPane.INFORMATION_MESSAGE);
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
        try {
            hrService.changeEmployeeStatus(employee.id(), String.valueOf(statusCombo.getSelectedItem()));
            loadEmployees();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
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
        JScrollPane scroll = new JScrollPane(payslipsTable);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadPayslips() {
        payslipsList = hrService.getAllPayslips();
        payslipsModel.setRowCount(0);
        for (PayslipDTO p : payslipsList) {
            payslipsModel.addRow(new Object[]{
                    p.payslipNumber(),
                    p.employeeName(),
                    String.format("%02d/%d", p.month(), p.year()),
                    String.format("%,.2f MT", p.grossPay()),
                    String.format("%,.2f MT", p.totalDeductions()),
                    String.format("%,.2f MT", p.netPay()),
                    p.status(),
                    p.paymentDate() != null ? p.paymentDate().format(DATE_FMT) : "-"
            });
        }
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

        JTextField allowancesField = new JTextField("0");
        JTextField overtimeField = new JTextField("0");
        JTextField otherField = new JTextField("0");
        JTextField notesField = new JTextField();
        UIHelper.styleTextField(allowancesField);
        UIHelper.styleTextField(overtimeField);
        UIHelper.styleTextField(otherField);
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
                    new BigDecimal(allowancesField.getText().trim()),
                    new BigDecimal(overtimeField.getText().trim()),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    new BigDecimal(otherField.getText().trim()),
                    notesField.getText().trim().isEmpty() ? null : notesField.getText().trim()
            );
            PayslipDTO created = hrService.createPayslip(req);
            loadPayslips();
            int print = JOptionPane.showConfirmDialog(this,
                    "Recibo " + created.payslipNumber() + " gerado. Deseja imprimir agora?",
                    "Sucesso", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (print == JOptionPane.YES_OPTION) {
                printPayslip(created.id(), created.payslipNumber());
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Valores numéricos inválidos.", "Erro", JOptionPane.ERROR_MESSAGE);
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
        try {
            List<PayslipDTO> created = hrService.processMonthlyPayroll((Integer) year.getValue(), (Integer) month.getValue());
            loadPayslips();
            JOptionPane.showMessageDialog(this, created.size() + " recibos processados automaticamente.",
                    "Folha Salarial", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void markSelectedPayslipPaid() {
        PayslipDTO sel = selectedPayslip();
        if (sel == null) return;
        try {
            hrService.markPayslipPaid(sel.id());
            JOptionPane.showMessageDialog(this, "Recibo marcado como pago.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadPayslips();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printSelectedPayslip() {
        PayslipDTO sel = selectedPayslip();
        if (sel == null) return;
        printPayslip(sel.id(), sel.payslipNumber());
    }

    private void printPayslip(Long id, String number) {
        try {
            byte[] pdf = payslipPrintService.render(id);
            PdfFileSaver.saveAndOpen(pdf, "recibo-salario-" + number);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar PDF: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private PayslipDTO selectedPayslip() {
        int row = payslipsTable.getSelectedRow();
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
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadAbsences() {
        absencesList = hrService.getAllAbsences();
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

        JTextField startField = new JTextField(LocalDate.now().toString());
        JTextField endField = new JTextField(LocalDate.now().toString());
        JTextField reasonField = new JTextField();
        JCheckBox docCheck = new JCheckBox("Possui documento de justificação");
        docCheck.setForeground(UIHelper.TEXT_LIGHT);
        docCheck.setOpaque(false);
        UIHelper.styleTextField(startField);
        UIHelper.styleTextField(endField);
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
                    LocalDate.parse(startField.getText().trim()),
                    LocalDate.parse(endField.getText().trim()),
                    reasonField.getText().trim().isEmpty() ? null : reasonField.getText().trim(),
                    docCheck.isSelected()
            );
            hrService.recordAbsence(req);
            JOptionPane.showMessageDialog(this, "Falta registada.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadAbsences();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedAbsence() {
        int row = absencesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma falta na tabela.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        AbsenceDTO sel = absencesList.get(row);
        int ok = JOptionPane.showConfirmDialog(this, "Eliminar a falta selecionada?", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            hrService.deleteAbsence(sel.id());
            loadAbsences();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
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
        JScrollPane scroll = new JScrollPane(vacationsTable);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadVacations() {
        vacationsList = hrService.getAllVacations();
        vacationsModel.setRowCount(0);
        for (VacationDTO v : vacationsList) {
            vacationsModel.addRow(new Object[]{
                    v.id(), v.employeeName(),
                    v.startDate().format(DATE_FMT), v.endDate().format(DATE_FMT),
                    v.totalDays(), v.yearReference(), v.status(),
                    v.decisionBy() == null ? "-" : v.decisionBy()
            });
        }
    }

    private void openCreateVacationDialog() {
        if (employeesList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cadastre colaboradores primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JComboBox<String> empCombo = new JComboBox<>();
        UIHelper.styleComboBox(empCombo);
        for (EmployeeDTO e : employeesList) empCombo.addItem(e.name() + " — " + e.department());

        JTextField startField = new JTextField(LocalDate.now().toString());
        JTextField endField = new JTextField(LocalDate.now().plusDays(15).toString());
        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2000, 2100, 1));
        JTextField notesField = new JTextField();
        UIHelper.styleTextField(startField);
        UIHelper.styleTextField(endField);
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
                    LocalDate.parse(startField.getText().trim()),
                    LocalDate.parse(endField.getText().trim()),
                    (Integer) yearSpinner.getValue(),
                    notesField.getText().trim().isEmpty() ? null : notesField.getText().trim()
            );
            hrService.submitVacation(req);
            JOptionPane.showMessageDialog(this, "Pedido de férias submetido.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadVacations();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void decideVacation(boolean approve) {
        int row = vacationsTable.getSelectedRow();
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
        try {
            hrService.decideVacation(sel.id(), approve, reason);
            loadVacations();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─── Expenses tab ─────────────────────────────────────────────────────────

    private JPanel buildExpensesTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Notas de Despesas"), BorderLayout.WEST);

        ModernButton exportBtn = UIHelper.createSecondaryButton("Exportar PDF");
        exportBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        exportBtn.addActionListener(e -> exportTable("despesas", "Notas de Despesas", expensesTable));
        ModernButton submitBtn = UIHelper.createPrimaryButton("Submeter Despesa");
        submitBtn.setIcon(UIHelper.icon("fas-paper-plane", 14));
        submitBtn.addActionListener(e -> submitExpense());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(exportBtn);
        actions.add(submitBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        String[] cols = {"Colaborador", "Valor", "Categoria", "Estado", "Motivo Rejeição"};
        expensesModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        expensesTable = new JTable(expensesModel);
        UIHelper.styleTable(expensesTable);
        JScrollPane scroll = new JScrollPane(expensesTable);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadExpenses() {
        expensesModel.setRowCount(0);
        List<ExpenseClaimDTO> claims = hrService.getAllExpenses();
        for (ExpenseClaimDTO c : claims) {
            expensesModel.addRow(new Object[]{
                    c.employeeName(),
                    String.format("%,.2f MT", c.amount()),
                    c.category(),
                    c.status().name(),
                    c.rejectionReason() == null ? "" : c.rejectionReason()
            });
        }
    }

    private void submitExpense() {
        if (employeesList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cadastre colaboradores primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<String> empCombo = new JComboBox<>();
        UIHelper.styleComboBox(empCombo);
        for (EmployeeDTO e : employeesList) empCombo.addItem(e.name() + " — " + e.department());

        JComboBox<String> categoryCombo = new JComboBox<>(new String[]{
                "MEALS (Alimentação)", "TRAVEL (Deslocações)", "LODGING (Alojamento)", "OTHER (Outros)"
        });
        UIHelper.styleComboBox(categoryCombo);
        JTextField amountField = new JTextField();
        JTextField descField = new JTextField();
        UIHelper.styleTextField(amountField);
        UIHelper.styleTextField(descField);

        JPanel form = UIHelper.createDialogForm(
                "Colaborador:", empCombo,
                "Categoria:", categoryCombo,
                "Valor (MT):", amountField,
                "Descrição:", descField
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Submeter Despesa", "fas-receipt",
                "Nota de despesa do colaborador", form)
                .setConfirmButton("Submeter", "fas-paper-plane").showDialog();
        if (!confirmed) return;

        try {
            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            String desc = descField.getText().trim();
            if (desc.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Indique uma descrição.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            EmployeeDTO emp = employeesList.get(empCombo.getSelectedIndex());
            String cat = categoryCombo.getSelectedItem().toString().split(" ")[0];
            hrService.submitExpense(new CreateExpenseClaimRequest(emp.id(), amount, cat, desc));
            JOptionPane.showMessageDialog(this, "Despesa submetida.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadExpenses();
            refreshOverview();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Valor inválido.", "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─── Shared export helper ─────────────────────────────────────────────────

    private void exportTable(String baseName, String title, JTable table) {
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
