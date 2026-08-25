package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.ActionMenuButton;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.SimpleBarChart;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.CircularAvatar;
import mz.multicore.erp.modules.hr.model.ExpenseStatus;
import mz.multicore.erp.modules.hr.dto.AbsenceDTO;
import mz.multicore.erp.modules.hr.dto.CreateAbsenceRequest;
import mz.multicore.erp.modules.hr.dto.CreateExpenseClaimRequest;
import mz.multicore.erp.modules.hr.dto.CreatePayslipRequest;
import mz.multicore.erp.modules.hr.dto.CreateVacationRequest;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import mz.multicore.erp.modules.hr.dto.ExpenseClaimDTO;
import mz.multicore.erp.modules.hr.dto.PayslipDTO;
import mz.multicore.erp.modules.hr.dto.VacationDTO;
import mz.multicore.erp.modules.hr.dto.UpsertEmployeeRequest;
import mz.multicore.erp.modules.hr.dto.OccupationalHealthSummaryDTO;
import mz.multicore.erp.desktop.client.HRApiClient;
import mz.multicore.erp.modules.printing.PdfFileSaver;
import mz.multicore.erp.modules.printing.TablePdfExporter;

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
    private final HRContractsPanel contractsPanel;
    private final HRVacationsPanel vacationsPanel;
    private final HRTimeSheetPanel timeSheetPanel;
    private final HRLiabilitiesPanel liabilitiesPanel;
    private final HRDeductionsPanel deductionsPanel;
    private final HRTerminationsPanel terminationsPanel;
    private final HRPayrollActions payrollActions;
    private final HREmployeeActions employeeActions;

    List<EmployeeDTO> employeesList = new ArrayList<>();
    private List<PayslipDTO> payslipsList = new ArrayList<>();
    private List<AbsenceDTO> absencesList = new ArrayList<>();
    List<VacationDTO> vacationsList = new ArrayList<>();
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
    DefaultTableModel vacationsModel;
    JTable vacationsTable;

    // Expenses tab
    DefaultTableModel expensesModel;
    JTable expensesTable;

    public HRPanel(HRApiClient hrApiClient) {
        this.hrApiClient = hrApiClient;
        this.expensesPanel = new HRExpensesPanel(this);
        this.contractsPanel = new HRContractsPanel(this);
        this.vacationsPanel = new HRVacationsPanel(this);
        this.timeSheetPanel = new HRTimeSheetPanel(this);
        this.liabilitiesPanel = new HRLiabilitiesPanel(this);
        this.deductionsPanel = new HRDeductionsPanel(this);
        this.terminationsPanel = new HRTerminationsPanel(this);
        this.payrollActions = new HRPayrollActions(this, this::selectedPayslip, this::loadPayslips);
        this.employeeActions = new HREmployeeActions(this, this::selectedEmployee,
                this::selectedAbsence, this::loadAbsences);

        setLayout(new BorderLayout(0, 10));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(UIHelper.createHeading("Recursos Humanos"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);

        tabs.addTab("Visão Geral",        UIHelper.icon("fas-chart-pie", 16, UIHelper.TEXT_LIGHT),     buildOverviewTab());
        tabs.addTab("Colaboradores",     UIHelper.icon("fas-users", 16, UIHelper.TEXT_LIGHT),         buildEmployeesTab());
        tabs.addTab("Contratos",         UIHelper.icon("fas-file-signature", 16, UIHelper.TEXT_LIGHT), contractsPanel.buildPanel());
        tabs.addTab("Recibos de Salário", UIHelper.icon("fas-file-invoice-dollar", 16, UIHelper.TEXT_LIGHT), buildPayslipsTab());
        tabs.addTab("Faltas",            UIHelper.icon("fas-user-times", 16, UIHelper.TEXT_LIGHT),    buildAbsencesTab());
        tabs.addTab("Ponto",             UIHelper.icon("fas-clock", 16, UIHelper.TEXT_LIGHT),        timeSheetPanel.buildPanel());
        tabs.addTab("Férias",            UIHelper.icon("fas-umbrella-beach", 16, UIHelper.TEXT_LIGHT),buildVacationsTab());
        tabs.addTab("Descontos",         UIHelper.icon("fas-hand-holding-usd", 16, UIHelper.TEXT_LIGHT), deductionsPanel.buildPanel());
        tabs.addTab("Retenções",         UIHelper.icon("fas-landmark", 16, UIHelper.TEXT_LIGHT),     liabilitiesPanel.buildPanel());
        tabs.addTab("Cessações",         UIHelper.icon("fas-user-slash", 16, UIHelper.TEXT_LIGHT),   terminationsPanel.buildPanel());
        tabs.addTab("Despesas",          UIHelper.icon("fas-receipt", 16, UIHelper.TEXT_LIGHT),       buildExpensesTab());

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
        deductionsPanel.load();
        liabilitiesPanel.load();
        terminationsPanel.load();
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

        ModernButton newBtn = UIHelper.createSuccessButton("Novo Colaborador");
        newBtn.setIcon(UIHelper.icon("fas-user-plus", 14));
        newBtn.addActionListener(e -> openEmployeeDialog(null));
        ModernButton editBtn = UIHelper.createPrimaryButton("Editar");
        editBtn.setIcon(UIHelper.icon("fas-edit", 14));
        editBtn.addActionListener(e -> editSelectedEmployee());
        ModernButton profileBtn = UIHelper.createSecondaryButton("Ver Perfil");
        profileBtn.setIcon(UIHelper.icon("fas-id-card", 14));
        profileBtn.addActionListener(e -> openSelectedEmployeeProfile());
        ActionMenuButton moreBtn = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Evolução Salarial", UIHelper.icon("fas-chart-line", 14),
                        employeeActions::openSalaryHistory)
                .addAction("Documentos", UIHelper.icon("fas-id-card", 14), employeeActions::openDocuments)
                .addAction("Saúde Ocupacional", UIHelper.icon("fas-heartbeat", 14),
                        employeeActions::openOccupationalHealth)
                .addAction("Alterar Estado", UIHelper.icon("fas-user-shield", 14), this::changeSelectedEmployeeStatus)
                .addAction("Exportar PDF", UIHelper.icon("fas-file-pdf", 14),
                        () -> exportTable("colaboradores", "Colaboradores", employeesTable));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(moreBtn);
        actions.add(profileBtn);
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
        employeesTable.setToolTipText("Selecione uma linha e use Ver Perfil para abrir a ficha completa");
        employeesTable.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.role());
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

    private void openSelectedEmployeeProfile() {
        EmployeeDTO employee = selectedEmployee();
        if (employee == null) return;
        UIHelper.runWithProgress(this, "A carregar perfil do trabalhador…",
                () -> hrApiClient.getOccupationalHealthSummary(employee.id()),
                health -> HREmployeeProfileDialog.show(SwingUtilities.getWindowAncestor(this), employee,
                        payslipsList, absencesList, vacationsList, health),
                this::showActionError);
    }

    private void openEmployeeDialog(EmployeeDTO existing) {
        JTextField numberField = new JTextField(existing == null ? "" : existing.employeeNumber());
        JTextField nameField = new JTextField(existing == null ? "" : existing.name());
        JTextField emailField = new JTextField(existing == null ? "" : existing.email());
        JTextField phoneField = new JTextField(existing == null || existing.phone() == null ? "" : existing.phone());
        final byte[][] photoHolder = {existing == null ? null : existing.photo()};
        CircularAvatar photoPreview = new CircularAvatar(photoHolder[0],
                existing == null ? "+" : CircularAvatar.initials(existing.name()), 104);
        photoPreview.setCameraOverlay(true);
        ModernButton choosePhotoButton = UIHelper.createSecondaryButton("Escolher foto…");
        choosePhotoButton.setIcon(UIHelper.icon("fas-camera", 14));
        ModernButton removePhotoButton = UIHelper.createSecondaryButton("Remover");
        removePhotoButton.setIcon(UIHelper.icon("fas-trash-alt", 14));
        removePhotoButton.setEnabled(photoHolder[0] != null && photoHolder[0].length > 0);
        choosePhotoButton.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Escolher fotografia do trabalhador");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Imagens (JPG, PNG)", "jpg", "jpeg", "png"));
            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            try {
                byte[] photo = UIHelper.readScaledImage(chooser.getSelectedFile(), 512);
                photoHolder[0] = photo;
                photoPreview.setPhoto(photo);
                removePhotoButton.setEnabled(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Não foi possível ler a fotografia seleccionada.",
                        "Fotografia inválida", JOptionPane.ERROR_MESSAGE);
            }
        });
        removePhotoButton.addActionListener(event -> {
            photoHolder[0] = null;
            photoPreview.setPhoto(null);
            removePhotoButton.setEnabled(false);
        });
        JPanel photoActions = new JPanel();
        photoActions.setOpaque(false);
        photoActions.setLayout(new BoxLayout(photoActions, BoxLayout.Y_AXIS));
        choosePhotoButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        removePhotoButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        photoActions.add(choosePhotoButton);
        photoActions.add(Box.createVerticalStrut(7));
        photoActions.add(removePhotoButton);
        JLabel photoHint = new JLabel("JPG ou PNG · máximo 2 MB");
        photoHint.setForeground(UIHelper.TEXT_MUTED);
        photoHint.setFont(photoHint.getFont().deriveFont(11f));
        photoHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        photoActions.add(Box.createVerticalStrut(8));
        photoActions.add(photoHint);
        JPanel photoPanel = new JPanel(new BorderLayout(14, 0));
        photoPanel.setOpaque(false);
        photoPanel.add(photoPreview, BorderLayout.WEST);
        photoPanel.add(photoActions, BorderLayout.CENTER);
        JTextField taxIdField = new JTextField(existing == null || existing.taxId() == null ? "" : existing.taxId());
        JTextField inssField = new JTextField(existing == null || existing.inssNumber() == null ? "" : existing.inssNumber());
        JSpinner dependentsSpinner = new JSpinner(new SpinnerNumberModel(existing == null ? 0 : existing.dependentsCount(), 0, 20, 1));
        JTextField departmentField = new JTextField(existing == null ? "" : existing.department());
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"EMPLOYEE", "SELLER", "MANAGER", "ADMIN"});
        roleCombo.setSelectedItem(existing == null ? "EMPLOYEE" : existing.role());
        MoneyField salaryField = new MoneyField(existing == null ? "0" : existing.baseSalary().toPlainString());
        DateField hireDateField = new DateField(existing == null || existing.hireDate() == null
                ? LocalDate.now() : existing.hireDate());
        JTextField contractEndField = new JTextField(existing == null || existing.contractEndDate() == null
                ? "" : existing.contractEndDate().toString());
        JTextField usernameField = new JTextField(existing == null || existing.username() == null
                ? "" : existing.username());
        JTextField bankNameField = new JTextField(existing == null || existing.bankName() == null
                ? "" : existing.bankName());
        JTextField bankAccountField = new JTextField(existing == null || existing.bankAccount() == null
                ? "" : existing.bankAccount());

        for (JTextField field : new JTextField[]{numberField, nameField, emailField, phoneField, taxIdField,
                inssField, departmentField, contractEndField, usernameField, bankNameField, bankAccountField}) {
            UIHelper.styleTextField(field);
        }
        UIHelper.styleComboBox(roleCombo);
        UIHelper.humanizeRoleCombo(roleCombo);

        JPanel personalForm = UIHelper.createDialogForm(
                "Fotografia:", photoPanel,
                "Número Interno:", numberField,
                "Nome Completo:", nameField,
                "Email:", emailField,
                "Telefone:", phoneField,
                "NUIT:", taxIdField,
                "Nº INSS:", inssField,
                "Dependentes IRPS:", dependentsSpinner
        );
        JPanel employmentForm = UIHelper.createDialogForm(
                "Departamento:", departmentField,
                "Cargo / Perfil:", roleCombo,
                "Salário Base (MT):", salaryField,
                "Data de Admissão (yyyy-MM-dd):", hireDateField,
                "Fim do Contrato (opcional):", contractEndField
        );
        JPanel accessForm = UIHelper.createDialogForm(
                "Conta de Utilizador (opcional):", usernameField,
                "Banco (opcional):", bankNameField,
                "Conta bancária (opcional):", bankAccountField
        );
        JTabbedPane form = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(form);
        form.addTab("Dados pessoais", UIHelper.icon("fas-user", 14, UIHelper.TEXT_LIGHT), personalForm);
        form.addTab("Vínculo", UIHelper.icon("fas-briefcase", 14, UIHelper.TEXT_LIGHT), employmentForm);
        form.addTab("Acesso e pagamento", UIHelper.icon("fas-credit-card", 14, UIHelper.TEXT_LIGHT), accessForm);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow,
                existing == null ? "Novo Colaborador" : "Editar Colaborador", "fas-users",
                "Ficha organizada por dados pessoais, vínculo e pagamento", form)
                .setSize(790, 650)
                .showDialog();
        if (!confirmed) return;

        try {
            UpsertEmployeeRequest request = new UpsertEmployeeRequest(
                    numberField.getText().trim(),
                    nameField.getText().trim(),
                    emailField.getText().trim(),
                    phoneField.getText().trim(),
                    photoHolder[0],
                    taxIdField.getText().trim(),
                    inssField.getText().trim(),
                    (Integer) dependentsSpinner.getValue(),
                    departmentField.getText().trim(),
                    String.valueOf(roleCombo.getSelectedItem()),
                    salaryField.value(),
                    hireDateField.value(),
                    contractEndField.getText().isBlank() ? null : LocalDate.parse(contractEndField.getText().trim()),
                    usernameField.getText().trim(),
                    bankNameField.getText().trim(),
                    bankAccountField.getText().trim()
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
        ModernButton approveBtn = UIHelper.createPrimaryButton("Aprovar");
        approveBtn.setIcon(UIHelper.icon("fas-clipboard-check", 14));
        ModernButton payBtn = UIHelper.createSuccessButton("Marcar Pago");
        payBtn.setIcon(UIHelper.icon("fas-check", 14));
        ActionMenuButton documentsBtn = UIHelper.createActionMenuButton("Documentos")
                .addAction("Imprimir PDF", UIHelper.icon("fas-print", 14), this::printSelectedPayslip)
                .addAction("Exportar Lista", UIHelper.icon("fas-file-pdf", 14),
                        () -> exportTable("recibos-salario", "Recibos de Salário", payslipsTable))
                .addAction("Ficheiro de Pagamento", UIHelper.icon("fas-university", 14),
                        payrollActions::bankPaymentFile)
                .addAction("Fechar Mês", UIHelper.icon("fas-lock", 14), payrollActions::closeMonth)
                .addAction("Reabrir Mês", UIHelper.icon("fas-lock-open", 14), payrollActions::reopenMonth);
        ModernButton processBtn = UIHelper.createPrimaryButton("Processar Mês");
        processBtn.setIcon(UIHelper.icon("fas-calculator", 14));
        newBtn.addActionListener(e -> openCreatePayslipDialog());
        approveBtn.addActionListener(e -> payrollActions.approveSelected());
        payBtn.addActionListener(e -> markSelectedPayslipPaid());
        processBtn.addActionListener(e -> processMonthlyPayroll());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(documentsBtn);
        actions.add(processBtn);
        actions.add(payBtn);
        actions.add(approveBtn);
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
        JComboBox<String> psEstado = TableFilter.combo("Todos os estados", "DRAFT", "APPROVED", "PAID", "CANCELLED");
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
        JTextField overtimeReasonField = new JTextField();
        UIHelper.styleTextField(notesField);
        UIHelper.styleTextField(overtimeReasonField);

        JPanel form = UIHelper.createDialogForm(
                "Colaborador:", empCombo,
                "Ano:", yearSpinner,
                "Mês:", monthSpinner,
                "Subsídios / Abonos (MT):", allowancesField,
                "Horas Extras (MT):", overtimeField,
                "Justificação (só se divergir do ponto):", overtimeReasonField,
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
                    notesField.getText().trim().isEmpty() ? null : notesField.getText().trim(),
                    // Com o ponto fechado, o servidor recusa um valor manual divergente sem
                    // justificação — e é aí que este campo é pedido ao operador.
                    overtimeReasonField.getText().trim().isEmpty() ? null : overtimeReasonField.getText().trim()
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
                    JOptionPane.showMessageDialog(this, created.summaryMessage(),
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
        ModernButton justifyBtn = UIHelper.createSuccessButton("Justificar");
        justifyBtn.setIcon(UIHelper.icon("fas-user-check", 14));
        ModernButton deleteBtn = UIHelper.createDangerButton("Eliminar");
        deleteBtn.setIcon(UIHelper.icon("fas-trash", 14));
        ModernButton exportBtn = UIHelper.createSecondaryButton("Exportar PDF");
        exportBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        newBtn.addActionListener(e -> openCreateAbsenceDialog());
        justifyBtn.addActionListener(e -> employeeActions.justifyAbsence());
        deleteBtn.addActionListener(e -> deleteSelectedAbsence());
        exportBtn.addActionListener(e -> exportTable("faltas", "Mapa de Faltas", absencesTable));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(exportBtn);
        actions.add(deleteBtn);
        actions.add(justifyBtn);
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

    AbsenceDTO selectedAbsence() {
        int row = TableFilter.selectedModelRow(absencesTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma falta na tabela.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return absencesList.get(row);
    }

    private void deleteSelectedAbsence() {
        AbsenceDTO sel = selectedAbsence();
        if (sel == null) return;
        int ok = JOptionPane.showConfirmDialog(this, "Eliminar a falta selecionada?", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        UIHelper.runWithProgress(this, "A eliminar falta…", () -> {
            hrApiClient.deleteAbsence(sel.id());
            return null;
        }, ignored -> loadAbsences(), this::showActionError);
    }

    // ─── Vacations tab ────────────────────────────────────────────────────────
    // O corpo vive em HRVacationsPanel — extraído a 2026-08-23 para o painel voltar a caber no
    // limite do UiPanelDecompositionTest antes de o ponto (B2) acrescentar o seu separador.

    private JPanel buildVacationsTab() { return vacationsPanel.buildPanel(); }

    private void loadVacations() { vacationsPanel.load(); }

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
            mz.multicore.erp.modules.company.model.Company company = resolveCompany();
            byte[] pdf = TablePdfExporter.renderFromSwing(company, title, table);
            PdfFileSaver.saveAndOpen(pdf, baseName + "-export");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao exportar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private mz.multicore.erp.modules.company.model.Company resolveCompany() {
        mz.multicore.erp.modules.company.model.Company c = new mz.multicore.erp.modules.company.model.Company();
        c.setId(mz.multicore.erp.architecture.security.CurrentUserContext.getCurrentCompanyId());
        return c;
    }
}
